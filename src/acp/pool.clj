(ns acp.pool
  (:require [acp.charge-box :as cp]
            [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [clojure.pprint :refer [print-table]]))

(defn empty-chargebox
  ([endpoint box-serial] (empty-chargebox endpoint box-serial 5))
  ([endpoint box-serial connectors]
   (agent {:box-serial   box-serial
           :endpoint     endpoint
           :inbox        []
           :active-calls {}
           :sessions     []
           :connectors   connectors})))

(defn add [agents charge-boxes]
  (swap! agents into charge-boxes))

(defn step [agents]
  (doseq [agent @agents]
    (send-off agent cp/run! agent)))

(defn run [agents scheduler]
  (step agents)
  (reset! scheduler (chime-at [(-> 3 t/seconds t/from-now)]
                              (fn [_] (run agents scheduler)))))

(defn stats [agents]
  (let [online-fn #(if % "online" "offline")]
    (->> @agents
         (map #(-> %
                   deref
                   (update :sessions count)
                   (update :inbox count)
                   (update :active-calls count)
                   (update :socket online-fn)))
         (print-table [:box-serial :connectors :socket :inbox :active-calls :sessions]))))

(defn disconnect [agents]
  (doseq [agent @agents]
    (send-off agent cp/disconnect!)))

(defn stop [scheduler]
  (swap! scheduler #(if % (%))))

(defn reset [agents]
  (disconnect agents)
  (reset! agents []))
