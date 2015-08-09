(ns btc-browser.protocol)

(defprotocol AddressGraph
  "An interface (usually over some API) for determining
   the inputs and outputs of bitcoin addresses"
  (received [this address]
    "Accepts a string of a Bitcoin address, returns a seq of maps
     [{:address \"bitcoinAddress\" :tx \"transactionHash\"
       :time <a timestamp, type doesn't matter for now>
       :amount <satoshis, amount transferred\"} ...]
     representing all transactions sent to the given address")
  (sent [this address]
    "Accepts a string of a Bitcoin address, returns a seq of maps
     [{:address \"bitcoinAddress\" :tx \"transactionHash\"
       :time <a timestamp, type doesn't matter for now>
       :amount <satoshis, amount transferred\"} ...]
     representing all transaction sent from the given address"))

(defprotocol AddressGraphAsync

  (received-async [this address publish-to])

  (sent-async [this address publish-to]))

(defprotocol ConnectionStorage
  "An interface to represent basic persistence and querying operations"
  (save! [this address connections])
  (query [this address]))
