(ns btc-browser.core
  (:refer-clojure :exclude [vector])
  (:require [btc-browser.ingest
             [lookup :refer [start-connections-lookup]]
             [storage :refer [start-connections-save]]]))

(defn start-ingest [lookup-fn storage {:keys [save-threads]}]
  (let [{incoming :output :as lookup} (start-connections-lookup lookup-fn "")
        saving (start-connections-save storage save-threads incoming)]
    {:lookup lookup
     :saving saving}))

;; TODO should start automating. The amazing fuckin thing is that
;; you've abstracted enough that you can run ingest logic on sample
;; data wrapped up in dummy protocol instances, and fuck, already even
;; have a storage implementation.
;; Probably can do some repl action like before to generate data,

;; (can just call sent/received/whatever a bunch of times. Hmmmm. might
;; need to paramterize that lookup like before instead of just passing
;; in api. Heh, even easier to mock!),

;; that you can save into an edn
;; file and read into mem when tests run. Hot.

;; woah, httpkit has a way to mock responses, even. Huh. Nah, don't
;; actually need that just to check your callback. Don't worry about
;; that yet, just get ingest tests working so you can start factoring
;; out some of the duplicated logic and clean things up
;; that^ mocking will be invaluable for finally de-deplicating and
;; cleaning up api queries though (if you even want to do that w/
;; native query action going on)

;; Okay, in summary, get some sample data -> automated tests for
;; ingest -> so you can finally start up interestingness checker
