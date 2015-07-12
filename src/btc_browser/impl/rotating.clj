(ns btc-browser.impl.rotating
  (:require [btc-browser.protocol :refer :all]
            [btc-browser.impl
             [blocktrail :refer [->blocktrail]]
             [blockchain-info :refer [blockchain-info]]]))

(defprotocol ^:private Rotate (rotate [this]))

(defrecord rotating-apis [current-api apis]
  Rotate
  (rotate [_]
    (swap! current-api (comp #(mod % (count apis)) inc)))
  AddressGraph
  (received [this address]
    (received (apis (rotate this)) address))
  (sent [this address]
    (sent (apis (rotate this)) address)))

(defn init-rotating-apis [blocktrail-key]
  (map->rotating-apis
   {:current-api (atom 0)
    :apis [blockchain-info
           (->blocktrail blocktrail-key)]}))
