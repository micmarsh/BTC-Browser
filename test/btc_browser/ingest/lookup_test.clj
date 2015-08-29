(ns btc-browser.ingest.lookup-test
  (:require [btc-browser.ingest.lookup :refer :all]
            [clojure.core.async :as a]
            [clojure.test :refer :all]))

(def as-maps
  (comp (partial map (partial hash-map :address)) vector))

(def mock-addresses
  {"address1" (as-maps "address2" "address3")
   "address2" (as-maps "address4" "address3")
   "address3" []
   "address4" (as-maps "address5")
   "address5" []})

(defn mock-lookup [address chan]
  (a/put! chan [address (mock-addresses address)]))

(deftest no-lookups
  (let [{:keys [input output futures]}
        (start-connections-lookup mock-lookup "address3")]
    (Thread/sleep 100)
    (a/close! input)
    (is (= ["address3" []] (a/<!! output)))
    (is (nil? (a/<!! output)))
    (is (nil? @(first futures)))))
