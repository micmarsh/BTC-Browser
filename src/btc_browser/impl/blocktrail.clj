(ns btc-browser.impl.blocktrail
  (:refer-clojure :exclude [vector])
  (:require [btc-browser.protocol :refer :all]
            [cheshire.core :refer [parse-string]]
            [clojure.core.async :refer [put!]]
            [clj-tuple :refer [vector]]
            [mutils.fn.compose :refer [comp']]
            [org.httpkit.client :as http]))

(defn blocktrail-query [api-key address callback]
  (-> "https://api.blocktrail.com/v1/btc/address/%s/transactions?api_key=%s"
      (format address api-key)
      (http/get (comp callback :data #(parse-string % true) :body))))

(defn received-helper [address txs]
  (for [{:keys [hash time inputs outputs]} txs
        :when ((set (map :address outputs)) address)
        {:keys [address value]} inputs]
    {:address address :tx hash :time time :amount value}))

(defn sent-helper [address txs]
  (for [{:keys [hash time inputs outputs]} txs
        :when ((set (map :address inputs)) address)
        {:keys [address value]} outputs]
    {:address address :tx hash :time time :amount value}))

(defn- async-helper [api-key address channel helper]
  (let [callback (comp' (put! channel) (vector address) (helper address))]
      (blocktrail-query api-key address callback)))

(defrecord blocktrail [api-key]
  AddressGraphAsync
  (received-async [_ address channel]
    (async-helper api-key address channel received-helper))
  (sent-async [_ address channel]
    (async-helper api-key address channel sent-helper))
  AddressGraph
  (received [_ address]
    (blocktrail-query api-key address (partial received-helper address)))
  (sent [_ address]
    (blocktrail-query api-key address (partial sent-helper address))))
