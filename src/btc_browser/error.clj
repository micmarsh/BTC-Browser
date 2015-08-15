(ns btc-browser.error)

(defn wrap-error [callback]
  (fn [arg]
    (try
      (callback arg)
      (catch Exception e
        {::error? true
         ::exception e
         ::message "Error in API query"}))))
