(ns acp.ocpp
  (:require [acp.wamp :as wamp]
            [clojure.data.json :as json]
            [gniazdo.core :as ws]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn timestamp []
  "Returns ISO 8601 timestamp as string"
  (f/unparse (f/formatters :date-time-no-ms) (t/now)))

(defn- send! [socket action data]
  (let [[_ key :as payload] (wamp/call action data)]
    (log/info "Sending OCPP action" action "with payload" (pr-str payload))
    (ws/send-msg socket (json/write-str payload))
    {key {:timestamp (timestamp)
          :action    action
          :data      data}}))

(defn- send-result! [socket key data]
  (println "Sending OCPP call-result")
  (ws/send-msg socket (json/write-str (wamp/ok key data))))

;; OCPP CALL RESULT

(defn remote-start-transaction-response! [socket key]
  (send-result! socket key
                {:status "Accepted"}))

(defn remote-stop-transaction-response! [socket key]
  (send-result! socket key
                {:status "Accepted"}))

;; OCPP CALL

(defn heartbeat! [socket]
  (send! socket "Heartbeat" {}))

(defn start-transaction! [socket connector idTag]
  (send! socket "StartTransaction"
         {:connectorId connector
          :idTag       idTag
          :timestamp   (timestamp)
          :meterStart  (rand-int 500)}))

(defn stop-transaction! [socket tr-id]
  (send! socket "StopTransaction"
         {:transactionId tr-id
          :timestamp     (timestamp)
          :meterStop     (rand-int 1000)}))

(defn meter-values! [socket tr-id]
  (send! socket "MeterValues"
         {:connectorId   (+ (rand-int 5) 1)
          :transactionId tr-id
          :values        [{
                           :timestamp (timestamp)
                           :values    [{:value 100 :unit "Wh"}]}]}))

(defn boot-notification! [socket]
  (send! socket "BootNotification"
         {:chargePointVendor "CWIN"
          :chargePointModel  "Machinery 1.0 (SIMULATOR)"}))

(defn status-notification! [socket]
  (send! socket "StatusNotification"
         {:connectorId (+ (rand-int 5) 1)
          :status      (rand-nth ["Available" "Occupied" "Faulted"
                                  "Unavailable" "Reserved"])
          :errorCode   "NoError"}))
