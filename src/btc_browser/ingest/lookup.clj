(ns btc-browser.ingest.lookup
  (:require [clojure.core.async :as a]
            [btc-browser.async.util :as u]))

(defn start-connections-lookup [lookup-fn address]
  (let [publish-results (a/chan u/*buffer-size*)
        lookup-more (a/chan u/*buffer-size*)]
    (u/split-pipe publish-results lookup-more)
    (lookup-fn address publish-results)
    {:input lookup-more :output publish-results
     :futures
     (-> (loop [[_ connections] (a/<!! lookup-more)]
           (when-not (nil? connections)
             (doseq [:let [more-addresses (map :address connections)]
                     address more-addresses]
               (lookup-fn address publish-results))
             (recur (a/<!! lookup-more))))
         (future)
         (vector))}))
