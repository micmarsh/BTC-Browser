(ns btc-browser.core
  (:refer-clojure :exclude [tree-seq])
  (:require [mutils.seq.tree :refer [tree-seq]]))

(def ^:const bf-path
  {:breadth true :path true})

(defn path?
  ([step addr-from addr-to]
     (path? 1000 step addr-from addr-to))
  ([limit step addr-from addr-to]
     (let [cutoff (atom 0)
           branch? (fn [_] (< (swap! cutoff inc) limit))
           children (comp step :address)]
       (->>  {:address addr-from}
             (tree-seq bf-path branch? children)
             (drop-while (comp (partial not= addr-to) :address :node))
             (first)
             (:path)))))
