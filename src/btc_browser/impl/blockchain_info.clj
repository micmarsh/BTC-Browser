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

(defn txs->address-conns
  "A General, pluggable way to convert an api response of transactions
   to the format we're using. Takes a map of options, a single address and
   the sequence of transactions to convert.
   Options:
    relevant-tx?:
      Whether or not to use the given transaction to build the result set.
      This is needed because most transaction api responses don't tell you explictly
      if a transaction is incoming to or outgoing from a given address, requiring
      deeper inspection.
    get-tx-parts:
      Given a single transaction, return a sequence of {:address :value} maps to construct
      the final seqeuence with. Needed to extract either the inputs or outputs to a transaction,
      depending on its relation the the given address (see above)
    get-tx-info:
      Create a single map with two keys: {:time <timestamp of tx> :tx <hash of tx>}, used
      to construct final return value "
  [{:keys [get-tx-parts relevant-tx?
           get-tx-info]} address txs]
  (let [get-tx-info (memoize get-tx-info)]
    (for [tx txs
          :when (relevant-tx? address tx)
          :let [tx-parts (get-tx-parts tx)]
          {:keys [address value]} tx-parts]
      (merge (get-tx-info tx)
             {:address address :amount value}))))

(defn- relevant-tx?-helper [map-fn values]
  #((set (map map-fn (values %2))) %1))

(defn- select-transform [key-map map]
  (let [map (select-keys map (keys keys))]
    (into {}
          (for [[k v] map]
            (if-let [new-k (get key-map k)]
              [new-k v]
              [k v])))))

(defmacro haskell [& body]
  )

(defmacro h [& body] `(haskell ~@body))

(def blockchain-info
  (reify AddressGraph
    (received [_ address]
      (txs->address-conns
       {:relevant-tx? (relevant-tx?-helper :addr :out)
        :get-tx-parts
     ;   (h (map ((select-transform {:addr :address :value nil}) . :prev_out)) . :inputs)
      ;  (comp (partial map (comp (partial select-transform {:addr :address :value nil}) :prev_out)) :inputs)
        (comp (partial map (fn [{{:keys [addr value]} :prev_out}] {:address addr :value value})) :inputs)
        :get-tx-info
        (h (select-transform {:hash :tx :time nil}))
        (fn [{:keys [time hash]}] {:time time :tx hash})}
       address
       (blockchain-query-memo address)))

    (sent [_ address]
      (txs->address-conns
       {:relevant-tx? (relevant-tx?-helper (comp :addr :prev_out) :inputs)
        :get-tx-parts (comp (partial map (fn [{:keys [addr value]}] {:address addr :value value})) :out)
        :get-tx-info (fn [{:keys [time hash]}] {:time time :tx hash})}
       address
       (blockchain-query-memo address))
      #_(let [txs (blockchain-query-memo address)]
        (for [{:keys [hash time inputs out]} txs
              :when ((set (map (comp :addr :prev_out) inputs)) address)
              {:keys [addr value]} out]
          {:address addr :tx hash :time time :amount value})))))
