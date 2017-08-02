(ns acp.wamp)

(def msg-types
  {:CALL 2
   :CALLRESULT 3
   :CALLERROR 4})

(defn new-msg-id []
  (->> #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
       repeatedly
       (take 25)
       (apply str)))

(defn ok [msg-id payload]
  [(:CALLRESULT msg-types) msg-id payload])

(defn err [msg-id]
  [(:CALLERROR msg-types) msg-id])

(defn call [action payload]
  [(:CALL msg-types) (new-msg-id) action payload])