(ns teamcartgs.core
  (:require
   [teamcartgs.entry-points]
   [cats.core :refer-macros [mlet]]
   [cats.monad.either
    :refer [branch left right lefts rights left? right?]
    :refer-macros [try-either]]))

(def code-version "1.0.0")
(def product-lookup-url "https://lambda.teamcart.io/items-details")
(def checkout-url "https://teamcart.io/checkout/#")
(def cache-expiration-seconds 1800)
(def item-resolve-failed #js {"title" "Failed to find product"})
(def remote-fetch-failed #js {"title" "Failed to reach TeamCart"})

;; assumes range is 1d either columns or vectors
(defn range-to-vec [range]
  (->
   range
   js->clj
   flatten))

(defn filter-by-val [pred m]
  (->>
   m
   (filter #(-> % val pred))
   (into {})))

(defn map-vals [f m]
  (->>
   m
   (map (fn [[k v]] [k (f v)]))
   (into {})))

(defn batch-fetch-cache [ids cache-fetch remote-fetch cache-store]
  (let
      [cache-fetched (cache-fetch ids)

       cache-resolved
       (branch
        cache-fetched
        (constantly {})
        #(filter-by-val right? %))

       remaining-ids
       (branch
        cache-fetched
        (constantly ids)
        (fn [results]
          (->>
           results
           (filter-by-val left?)
           keys)))


       remote-fetched
       (if (-> remaining-ids empty?)
         (right {})
         (remote-fetch remaining-ids))]
    (mlet
     [rf remote-fetched]
     (when (pos? (count rf)) (cache-store rf))
     (right (merge rf cache-resolved)))))

(defn fetch-in-cache [cache ids]
  (try-either
   (->>
    (.getAll cache (into-array ids))
    js->clj
    (map
     (fn [[k v]]
       [k (->> v (.parse js/JSON) js->clj right)]))
    (into
     (->>
      ids
      (map (fn [k] [k (left :not-found)])) ;; default is unresolved
      (into {}))))))

(defn store-in-cache [cache fetched-values]
  (.putAll
   cache
   (->>
    fetched-values
    (reduce
     (fn [m [k v]]
       (branch
        v
        (constantly m)
        #(assoc m k
                (->> % clj->js (.stringify js/JSON)))))
     {})
    clj->js)
   cache-expiration-seconds))

(defn fetch-products-from-api [product-urls]
  (let
      [url product-lookup-url
       payload (->>
                {:product-urls (distinct product-urls)
                 :version code-version}
                clj->js
                (.stringify js/JSON))
       options {:method "post"
                :contentType "text/json"
                :payload payload
                }
       response
       (->>
        (.fetch
         js/UrlFetchApp
         url
         (clj->js options))
        (.getContentText)
        (.parse js/JSON))]

  (if (= (aget response "status") "success")
    (right
     (->>
      (aget response "result")
      js->clj
      (map-vals
       #(if (= (get % "product-id") nil)
          (left :product-lookup-failed)
          (right %)))))
    (left :remote-fetch-failed))))

(defn ^:export get-amazon-product-data [url-range key-range]
  (cond
    (or
     (not (array? url-range))
     (not (array? key-range)))
    "Arguments must be ranges"
    :default
    (let
        [url-vec (range-to-vec url-range)
         key-vec (range-to-vec key-range)
         doc-cache (.getDocumentCache js/CacheService)
         cache-fetch (partial fetch-in-cache doc-cache)
         cache-store (partial store-in-cache doc-cache)

         results
         (batch-fetch-cache
          url-vec
          cache-fetch
          fetch-products-from-api
          cache-store)

         get-result
         (fn [url]
           (if (pos? (.-length url))
           (branch
            results
            (constantly remote-fetch-failed)
            (fn [result-map]
              (branch
               (get result-map url)
               (constantly item-resolve-failed)
               identity)))
           {}))

         extract-keys
         (fn [item]
           (map
            #(if (= % "fetch-date")
               (some->
                (get item %)
                (js/Date.))
               (get item %))
            key-vec))

         output-grid
         (->>
          url-vec
          (map
           #(extract-keys (get-result %)))
          (to-array-2d))
         ]
      ;;(pr-str results)
      output-grid
      )))

(defn ^:export get-checkout-link [product-id-range quantity-range]
  (cond
    (or
     (not (array? product-id-range))
     (not (array? quantity-range)))
    "Arguments must be ranges"

    (not=
     (.-length product-id-range)
     (.-length quantity-range))
    "Argument rangges must match in size"

    :default
    (let
        [product-vec (range-to-vec product-id-range)
         qty-vec (range-to-vec quantity-range)

         valid-items (->>
                      (zipmap product-vec qty-vec)
                      (filter (fn [[product qty]]
                                   (and
                                    (string? product)
                                    (number? qty)
                                    (pos? qty))))
                      (into {}))
         ;; TODO: assert all items have same locale
         locale (some->
                 valid-items
                 first
                 first
                 (.substring 0 2))

         link-url (.join
                   (->>
                    valid-items
                    (mapcat (fn [[product qty]]
                              [(.substring product 2) qty]))
                    (into [(str checkout-url locale)])
                    into-array)
                   ",")]
      (if (empty? valid-items)
        "Cart is empty"
        link-url))))
