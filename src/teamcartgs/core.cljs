(ns teamcartgs.core
  (:require [teamcartgs.entry-points]))

(def code-version "1.0.0")
(def product-lookup-url "https://lambda.teamcart.io/items-details")
(def checkout-url "https://teamcart.io/checkout/#")

;; assumes range is 1d either columns or vectors
(defn range-to-vec [range]
  (->
   range
   js->clj
   flatten))

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
       result (->>
               (.fetch
                js/UrlFetchApp
                url
                (clj->js options))
               (.getContentText)
               (.parse js/JSON))]
    result))

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

         results-map (->
                      url-vec
                      fetch-products-from-api
                      (aget "result")
                      js->clj)

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
           #(extract-keys (get results-map %)))
          (to-array-2d))]
      output-grid)))

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
