;; project.clj
(defproject teamcart-gs "0.1.0-SNAPSHOT"
  :license {:name "Mozilla Public License 2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha13"]
                 [org.clojure/clojurescript "1.9.229"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :cljsbuild {:builds
              {:main {:source-paths ["src"]
                      :compiler {:main teamcartgs.core
                                 :optimizations :advanced
                                 :output-to "export/Code.gs"
                                 :output-dir "target"
                                 :pretty-print false
                                 :externs ["resources/gas.ext.js"]
                                 :foreign-libs [{:file "src/entry_points.js"
                                                 :provides ["teamcartgs.entry-points"]}]}}
               :redirect {:source-paths ["src_redirect"]
                          :compiler {:main teamcartgs.redirect
                                     :optimizations :advanced
                                     :output-to "export/checkout-redirect.js"
                                     :output-dir "target"
                                     :pretty-print false
                                     :foreign-libs [{:file "src_redirect/entry.js"
                                                     :provides "teamcartgs.redirect-entry"}]}}}})
