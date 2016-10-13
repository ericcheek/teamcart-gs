(ns teamcartgs.core
  (:require [teamcartgs.entry-points]))

(def code-version "1.0.0")

(defn fetch-products-from-api [product-urls]
  (let
      [url "https://lambda.teamcart.io/items-details"
       
       payload (->>
                {:product-urls (distinct product-urls)
                 ;; (map
                 ;;  #(js/encodeURIComponent %)
                 ;;  product-urls)
                 :version code-version
                 }
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
        [url-vec (->
                  url-range
                  js->clj
                  flatten)

         key-vec (->
                  key-range
                  js->clj
                  flatten)

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


(defn ^:export get-checkout-link [product-range quantity-range]
  )

(defn ^:export teamcart-test [url-range key-range]
  ;;(test-generate-grid url-range key-range)
  )
