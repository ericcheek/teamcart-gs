(set-env!
 :source-paths #{"src"}
 ;;:resource-paths #{"resources"}
 :compiler-options {:optimizations :advanced
                    :pretty-print false}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha13"]
                [org.clojure/clojurescript "1.9.229"]
                [funcool/cats "2.0.0"]
                [adzerk/boot-cljs "1.7.228-2" :scope "test"]])

(require '[adzerk.boot-cljs :refer [cljs]])
 
 
