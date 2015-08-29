(ns btc-browser.ingest.lookup
  (:require [clojure.core.async :as a]
            [btc-browser.async.util :as u]))

(defn start-connections-lookup [lookup-fn address]
  (let [base-chan (a/chan u/*buffer-size*)
        base-mult (a/mult base-chan)
        lookup-more (a/chan u/*buffer-size*)
        publish-results (a/chan u/*buffer-size*)]
    (a/tap base-mult lookup-more)
    (a/tap base-mult publish-results)

    (lookup-fn address base-chan)
    {:input lookup-more :output publish-results
     :shutdown #(a/close! base-chan)
     :futures
     (-> (loop [[_ connections :as result] (a/<!! lookup-more)]
           (if (nil? result)
             (a/close! publish-results)
             (do
               (doseq [address (map :address connections)]
                 (lookup-fn address base-chan))
               (recur (a/<!! lookup-more)))))
         (future)
         (vector))}))
