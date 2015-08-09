(ns btc-browser.core
  (:refer-clojure :exclude [mapcat])
  (:require [mutils.seq.lazy :refer [mapcat]]
            [btc-browser.protocol :refer :all]))

(defn ingest
  "step: (partial btc-browser.protocol/whatever impl-thing)"
  ([step storage addrs]
     (ingest 100 step storage addrs))
  ([limit step storage addrs]
     (when (pos? limit)
       (let [results (map (juxt identity step) addrs)]
         (doseq [[addr connections] results]
           (time (save! storage addr connections)))
         (recur (dec limit) step storage (distinct (mapcat (comp (partial map :address) second) results)))))))

(defn async-ingest
  "Will expect step to return a promise"
  ([step storage addrs]
     (async-ingest 100 step storage addrs))
  ([limit step storage addrs]
     (when (pos? limit)
       (let [results (doall (map (juxt identity step) addrs))]
         (doseq [[addr connections] results]
           (time (save! storage addr @connections)))
         (recur (dec limit) step storage (distinct (mapcat (comp (partial map :address) deref second) results)))))))
