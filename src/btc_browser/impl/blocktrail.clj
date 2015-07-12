(ns btc-browser.impl.blocktrail
  (:require [btc-browser.protocol :refer :all]
            [clj-http.client :as http]))

(def ^:const http-options
  (zipmap [:accept :content-type :as]
          (repeat :json)))

(defn blocktrail-query [api-key address]
  (-> "https://api.blocktrail.com/v1/btc/address/%s/transactions?api_key=%s"
      (format address api-key)
      (http/get http-options)
      (:body)
      (:data)))

(def blocktrail-query-memo (memoize blocktrail-query))

(defrecord blocktrail [api-key]
  AddressGraph
  (received [_ address]
    (let [txs (blocktrail-query-memo api-key address)]
      (for [{:keys [hash time inputs outputs]} txs
            :when ((set (map :address outputs)) address)
            {:keys [address value]} inputs]
        {:address address :tx hash :time time :amount value})))

  (sent [_ address]
    (let [txs (blocktrail-query-memo api-key address)]
      (for [{:keys [hash time inputs outputs]} txs
            :when ((set (map :address inputs)) address)
            {:keys [address value]} outputs]
        {:address address :tx hash :time time :amount value}))))
