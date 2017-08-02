(ns acp.charge-box
  (:require [clojure.data.json :as json]
            [gniazdo.core :as ws]
            [acp.ocpp :as ocpp]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:import (java.net ConnectException)))

(defn receive! [agent payload]
  "Add the received message to the inbox of the charge box"
  (send agent #(update % :inbox conj payload)))

(defn disconnected! [agent]
  "When the connection drops, nillify socket attr of state"
  (send agent #(dissoc % :socket)))

(defn connect! [endpoint agent box-serial]
  "Connect to the central-system, return nil when connection cannot be established"
  (try
    (ws/connect
      (str endpoint "/" box-serial)
      :subprotocols ["ocpp1.5"]
      :on-receive #(receive! agent (json/read-str % :key-fn keyword))
      :on-close (fn [& _] (disconnected! agent)))
    (catch ConnectException e
      (log/error "Failed to connect with error" e))))

(defn disconnect! [state]
  (log/info "disconnecting charge box with state" state)
  (if-let [socket (:socket state)]
    (ws/close socket))
  (dissoc state :socket))

(defn random-ocpp-msg! [socket sessions connectors]
  (log/info "Number of transactions:" (count sessions))
  (if (empty? sessions)
    (case (rand-int 5)
      0 (ocpp/boot-notification! socket)
      1 (ocpp/start-transaction! socket (+ (rand-int connectors) 1)"6E8E19F")
      2 (ocpp/status-notification! socket)
      (ocpp/heartbeat! socket))
    (let [[tr-id] sessions]
      (case (rand-int 2)
        0 (ocpp/stop-transaction! socket tr-id)
        (ocpp/meter-values! socket tr-id)))))

;; TODO handle error case
(defn handle-result-or-error [[msg-type key data] {:keys [active-calls] :as state}]
  (if-let [{action :action req-data :data} (get active-calls key)]
    (let [new-state (update state :active-calls dissoc key)]
      (log/info "Received reply on" action "with data" (pr-str data) "key" key)
      (case action
        "StartTransaction" (update new-state :sessions conj
                                   (:transactionId data))
        "StopTransaction" (update new-state :sessions
                                  (fn [li] (filterv #(not (= (:transactionId req-data) %)) li)))
        (do (log/warn "Received reply on" action "but no action is implemented")
            new-state)))
    (log/error "Received reply on call with id " key ", but thats not an active rpc")))



(defn handle-call [socket [_ key action {:keys [connectorId idTag transactionId]}] state]
  (case action
    "RemoteStartTransaction" (do (ocpp/remote-start-transaction-response! socket key)
                                 (update state :active-calls
                                         #(into % (ocpp/start-transaction! socket connectorId idTag))))
    "RemoteStopTransaction" (do (ocpp/remote-stop-transaction-response! socket key)
                                (update state :active-calls
                                        #(into % (ocpp/stop-transaction! socket transactionId))))))


(defn run! [{:keys [endpoint box-serial socket inbox sessions connectors] :as state} agent]
  "Runs a charge box one 'tick', connects when needed, returns the new state"
  (cond
    ;; connect
    (nil? socket) (assoc state :socket (connect! endpoint agent box-serial))
    ;; disconnect at random
    (= (rand-int 50) 0) (disconnect! state)
    ;; send a random OCPP msg when inbox is empty or at random
    (or (= (rand-int 5) 0)
        (empty? inbox)) (update state :active-calls
                                #(into % (random-ocpp-msg! socket sessions connectors)))
    :else
    ;; inspect the first incoming message and handle it
    (let [[[type & _ :as msg] & tail] inbox
          new-state (assoc state :inbox (or tail []))]
      (if (= (:CALL acp.wamp/msg-types) type)
        (handle-call socket msg new-state)
        (handle-result-or-error msg new-state)))))
