(ns btc-browser.ingest.storage
  (:require [clojure.core.async :as a]
            [btc-browser.protocol :refer :all]
            [btc-browser.async.util :as u]))

(defmacro ^:private for' [& body]
  `(doall (for ~@body)))

(defn start-connections-save [storage threads raw-incoming]
  (let [to-save (a/chan u/*buffer-size*)
        output (a/chan u/*buffer-size*)]
    (u/split-pipe raw-incoming to-save)
    {:input raw-incoming :output output
     :futures
     (for' [_ (range threads)]
       (future
         (loop [[address connections :as result] (a/<!! to-save)]
           (when-not (nil? result)
             (printf "yay saving %d connection for %s" (count connections) address)
             (save! storage address connections)
             (a/put! output {:status :success :data result})
             (recur (a/<!! to-save))))))}))
