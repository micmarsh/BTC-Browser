(ns btc-browser.impl.blockchain-info
  (:require [btc-browser.protocol :refer :all]
            [cheshire.core :refer [parse-string]]
            [org.httpkit.client :as http]))

(defn blockchain-query [address callback]
  (-> (str "https://blockchain.info/rawaddr/" address)
      (http/get (comp callback :txs #(parse-string % true) :body))))

(defn received-helper [address txs]
  (for [{:keys [hash time inputs out]} txs
        :when ((set (map :addr out)) address)
        {{:keys [addr value]} :prev_out} inputs]
    {:address addr :tx hash :time time :amount value}))

(defn sent-helper [address txs]
  (for [{:keys [hash time inputs out]} txs
        :when ((set (map (comp :addr :prev_out) inputs)) address)
        {:keys [addr value]} out]
    {:address addr :tx hash :time time :amount value}))

(def blockchain-info
  (reify AddressGraph
    (received [_ address]
      (blockchain-query address (partial received-helper address)))
    (sent [_ address]
      (blockchain-query address (partial sent-helper address)))))
