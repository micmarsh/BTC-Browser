(ns btc-browser.core)

(defn lazy-mapcat [f coll]
  (for [item coll x (f item)] x))

(def lazy-concat
  (comp (partial lazy-mapcat identity) (fn [& args] args)))

(defn bf-tree-seq
  "Returns a lazy sequence of the nodes in a tree, via a breadth-first walk.
   branch? must be a fn of one arg that returns true if passed a node
   that can have children (but may not).  children must be a fn of one
   arg that returns a sequence of the children. Will only be called on
   nodes for which branch? returns true. Root is the root node of the
  tree."
  [branch? children root]
  (let [walk (fn walk [prev-nodes]
               (lazy-seq
                (let [branches (filter branch? prev-nodes)
                       kids (lazy-mapcat children branches)]
                  (lazy-concat kids (walk kids)))))]
    (cons root (walk [root]))))

(defn- tree-seq-path* [tree branch? children root]
  (tree (comp branch? :node)
        (fn [{:keys [node path]}]
          (map (partial hash-map :path (conj path node) :node)
               (children node)))
        {:node root :path []}))

(def tree-seq-path (partial tree-seq-path* tree-seq))
(def bf-tree-seq-path (partial tree-seq-path* bf-tree-seq))

(defn path?
  ([step addr-from addr-to]
     (path? 1000 step addr-from addr-to))
  ([limit step addr-from addr-to]
     (let [cutoff (atom 0)
           branch? (fn [_] (< (swap! cutoff inc) limit))
           children (comp step :address)]
       (->>  {:address addr-from}
             (bf-tree-seq-path branch? children)
             (drop-while (comp (partial not= addr-to) :address :node))
             (first)
             (:path)))))
