(ns btc-browser.async.util
  (:require [clojure.core.async :as a]))

(def ^:dynamic *buffer-size* 100)

(defn split-pipe
  "pipe all items from `from` to `to`, using a mult to avoid
   destructively reading from `from`"
  [from to]
  (-> from
      (a/mult)
      (a/tap to)))
