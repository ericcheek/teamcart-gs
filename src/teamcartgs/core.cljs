(ns teamcartgs.core
  (:require [teamcartgs.entry-points]))

(defn ^:export get-amazon-product-data [productUrlRange & headers]
  (let [output 
        (->>
         headers
         (map (fn [header] [header]))
         to-array-2d)
        ]
    output))
