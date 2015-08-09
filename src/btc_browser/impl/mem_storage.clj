(ns btc-browser.impl.mem-storage
  (require [btc-browser.protocol :refer :all]))

(defrecord mem-storage [atom]
  ConnectionStorage
  (save! [_ address connections]
    (swap! atom update-in [address] #(distinct (into % connections))))
  (query [_ address] (get @atom address)))
