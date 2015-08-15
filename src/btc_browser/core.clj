(ns btc-browser.core
  (:refer-clojure :exclude [vector])
  (:require [clj-tuple :refer [vector]]
            [btc-browser.protocol :refer :all]
            [clojure.core.async :as a]))

(def ^:dynamic *buffer-size* 100)

(defn split-pipe
  "pipe all items from `from` to `to`, using a mult to avoid
   destructively reading from `from`"
  [from to]
  (-> from
      (a/mult)
      (a/tap to)))

(defn start-connections-lookup [api address]
  (let [publish-results (a/chan *buffer-size*)
        lookup-more (a/chan *buffer-size*)]
    (split-pipe publish-results lookup-more)
    ;; (sent-async api address publish-results)
    ;; (received-async api address publish-results)
    {:input lookup-more :output publish-results
     :futures
     (-> (loop [[_ connections] (a/<!! lookup-more)]
           (when-not (nil? connections)
             (doseq [:let [more-addresses (map :address connections)]
                     address more-addresses]
;;              (sent-async api address publish-results)
               (received-async api address publish-results))
             ;; TODO
             ;; going to need some smooth cycle detection if you're
             ;; going to branch in both directions. That should
             ;; probably exist on the api implementation level, could
             ;; be fancy and roll it into some kind of caching
             (recur (a/<!! lookup-more))))
         (future)
         (vector))}))

(defmacro ^:private for' [& body]
  `(doall (for ~@body)))

(defn start-connections-save [storage threads raw-incoming]
  (let [to-save (a/chan *buffer-size*)
        output (a/chan *buffer-size*)]
    (split-pipe raw-incoming to-save)
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

(defn start-ingest [api storage {:keys [save-threads]}]
  (let [{incoming :output :as lookup} (start-connections-lookup api "")
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
