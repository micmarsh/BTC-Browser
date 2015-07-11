(ns btc-browser.core
  (:require [clj-http.client :as http]))

(defprotocol AddressGraph
  "An interface (usually over some API) for determining
   the inputs and outputs of bitcoin addresses"
  (inputs [this address]
    "Accepts a string of a Bitcoin address, returns a seq of maps
     [{:address \"bitcoinAddress\" :tx \"transactionHash\"
       :time <a timestamp, type doesn't matter for now>
       :amount <satoshis, amount transferred\"} ...]
     representing all of the inputs to the given address")
  (outputs [this address]
    "Accepts a string of a Bitcoin address, returns a seq of maps
     [{:address \"bitcoinAddress\" :tx \"transactionHash\"
       :time <a timestamp, type doesn't matter for now>
       :amount <satoshis, amount transferred\"} ...]
     representing all of the inputs to the given address"))

(def ^:const http-options
  (zipmap [:accept :content-type :as]
          (repeat :json)))

(defn blockchain-query [address]
  (-> (str "https://blockchain.info/rawaddr/" address)
      (http/get http-options)
      (:body)
      (:txs)))

#_(def blockchain-dot-info
  (reify AddressGraph
    ))
