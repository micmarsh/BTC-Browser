(ns btc-browser.core
  (:refer-clojure :exclude [mapcat])
  (:require [mutils.seq.lazy :refer [mapcat]]
            [btc-browser.protocol :refer :all]
            [clojure.core.async :as a]))

(def ^:const buffer-size 100)

;; (def publish-results (a/chan buffer-size))

;; (def m-publish-results (a/mult publish-results))

;; (def lookup-more (a/chan buffer-size))

;; (def persist (a/chan buffer-size))

;; (a/tap m-publish-results lookup-more)
;; (a/tap m-publish-results persist)

(defmacro thread-loop [count & body]
  `(let [amount# (atom ~count)]
     (future (println "STARTING")
             (while (pos? (swap! amount# dec)) ~@body)
             (println "DONE"))))

(comment (defn start-ingest [amount lookup storage]
   (let [c (a/chan buffer-size)]
     (thread-loop
      amount
      (println "looping it up")
      (let [[address connections] (a/<!! c)
            _ (println "yay read a pair thing" address (count connections))
            more-addresses (map :address connections)]
        (printf "yay saving %d connections for %s\n" (count connections) address)
        (save! storage address connections)
        (doseq [address more-addresses]
          (lookup address c))))
     c)))

(defn start-pump
  "lookup: (partial protocol/something-async impl-thing)"
  [lookup lookup-more publish-results]
  (thread-loop 99999
   (let [[_ connections] (a/<!! lookup-more)
         more-addresses (map :address connections)]
     (doseq [address more-addresses]
       (lookup address publish-results)))))

(defn start-saving
  [storage persist]
  (thread-loop 99999
   (let [[address connections] (a/<!! persist)]
     (printf "yay saving %d connection for %s" (count connections) address)
     (save! storage address connections))))

(defn start-ingest [lookup storage]
  (let [publish-results (a/chan buffer-size)
        m-publish-results (a/mult publish-results)
        lookup-more (a/chan buffer-size)
        persist (a/chan buffer-size)]
    (a/tap m-publish-results lookup-more)
    (a/tap m-publish-results persist)

    (start-saving storage persist)
    (start-pump lookup lookup-more publish-results)
    publish-results))
