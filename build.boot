(set-env!
 :source-paths #{"src"}
 :target-path "target"

 :compiler-options {:optimizations :advanced
                    :pretty-print false}
 :dependencies #(into %
                 '[[org.clojure/clojure "1.9.0-alpha13"]
                   [org.clojure/clojurescript "1.9.229"]
                   [funcool/cats "2.0.0"]
                   [adzerk/boot-cljs "1.7.228-2" :scope "test"]]))

(require '[adzerk.boot-cljs :refer [cljs]]
         '[boot.pod :as pod]
         '[clojure.java.shell :as shell-helper]
         '[clojure.java.io :as io])

(def copyright-message
  (str
   "/**\n"
   " * Copyright TeamCart 2016\n"
   " * Usage is subject to Terms of Service and Privacy Policy specified on https://teamcart.io\n"
   " */\n"))

(deftask appscript
  "Builds the Teamcart Goole Appscript plugin"
  [c clipboard bool "Copy result to clipboard"]
  (comp
   (with-post-wrap fs
     (let
         [appscript-file (->
                          fs
                          (tmp-get "js/teamcartgs.js")
                          tmp-file)]
       (when clipboard
         (println "Copying to clipboard")
         (shell-helper/sh
          "pbcopy"
          :in appscript-file))
       fs))
   (cljs :optimizations :advanced)
   ;; inject copyright message
   (with-pre-wrap fs
     (let [new-dir (tmp-dir!)
           new-file (->
                     new-dir
                     (io/file "js/teamcartgs.js"))]
       (io/make-parents new-file)
       (spit new-file copyright-message)
       (-> fs
           (add-resource
            new-dir
            :mergers [[#".*"
                       (fn [if1 if2 of]
                         (pod/concat-merger if2 if1 of))]]
            :include #{#".*"}
            :exclude #{})
           commit!)))
   (target)))
