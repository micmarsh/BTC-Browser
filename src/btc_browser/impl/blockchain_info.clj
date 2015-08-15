(ns btc-browser.impl.blockchain-info
  (:refer-clojure :exclude [vector])
  (:require [btc-browser.protocol :refer :all]
            [clojure.core.async :refer [put!]]
            [clj-tuple :refer [vector]]
            [cheshire.core :refer [parse-string]]
            [mutils.fn.compose :refer [comp']]
            [org.httpkit.client :as http]
            [btc-browser.error :refer [wrap-error]]))

(defn blockchain-query [address callback]
  (-> (str "https://blockchain.info/rawaddr/" address)
      (http/get (comp callback :txs #(parse-string % true) :body))))

(defn received-helper [address txs]
  (for [{:keys [hash time inputs out]} txs
        :when ((set (map :addr out)) address)
        {{:keys [addr value]} :prev_out} inputs]
    {:address addr :tx hash
     :time time :amount value
     :direction :incoming}))

(defn sent-helper [address txs]
  (for [{:keys [hash time inputs out]} txs
        :when ((set (map (comp :addr :prev_out) inputs)) address)
        {:keys [addr value]} out]
    {:address addr :tx hash
     :time time :amount value
     :direction :outgoing}))

(defn- async-helper [address channel helper]
  (let [handle-error (wrap-error (comp' (vector address) (helper address)))
        callback (comp' (put! channel) handle-error)]
    (blockchain-query address callback)))

(def blockchain-info
  (reify
    AddressGraph
    (received [_ address]
      (blockchain-query address (partial received-helper address)))
    (sent [_ address]
      (blockchain-query address (partial sent-helper address)))
    AddressGraphAsync
    (received-async [_ address channel]
      (async-helper address channel received-helper))
    (sent-async [_ address channel]
      (async-helper address channel sent-helper))))
