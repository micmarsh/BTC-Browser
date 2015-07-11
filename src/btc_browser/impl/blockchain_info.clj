(ns btc-browser.impl.blockchain-info
  (:require [btc-browser.protocol :refer :all]
            [clj-http.client :as http]))

(def ^:const http-options
  (zipmap [:accept :content-type :as]
          (repeat :json)))

(defn blockchain-query [address]
  (-> (str "https://blockchain.info/rawaddr/" address)
      (http/get http-options)
      (:body)
      (:txs)))

(def blockchain-query-memo (memoize blockchain-query))

(def blockchain-info
  (reify AddressGraph
    (received [_ address]
      (let [txs (blockchain-query-memo address)]
        (for [{:keys [hash time inputs out]} txs
              :when ((set (map :addr out)) address)
              {{:keys [addr value]} :prev_out} inputs]
          {:address addr :tx hash :time time :amount value})))

    (sent [_ address]
      (let [txs (blockchain-query-memo address)]
        (for [{:keys [hash time inputs out]} txs
              :when ((set (map (comp :addr :prev_out) inputs)) address)
              {:keys [addr value]} out]
          {:address addr :tx hash :time time :amount value})))))
