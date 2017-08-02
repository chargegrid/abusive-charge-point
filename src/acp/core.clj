(ns acp.core
  (:require [clojure.tools.logging :refer [infof]]
            [cli4clj.cli :refer :all]
            [clojure.string :as str]
            [acp.pool :as pool]
            [acp.persistence :as persistence]
            [clojure.tools.logging :as log])
  (:gen-class))

(defonce agents (atom []))

(defonce scheduler (atom nil))

(defn -main [& args]
  (if-let [[path] args] 
    (do (prn "Loading config from " path "...")
        (pool/add agents (persistence/load path))
        (pool/stats agents)
        (pool/run agents scheduler)
        (prn "Pool is now RUNNING.")))
  (start-cli {:cmds          {:start {:fn         (fn [] (pool/run agents scheduler))
                                      :short-info "Start a pool of charge boxes"}
                              :stop  {:fn         (fn [] (pool/stop scheduler))
                                      :short-info "Stop the current pool"}
                              :load  {:fn (fn [path] (pool/add agents
                                                               (persistence/load path)))}
                              :stats {:fn         #(pool/stats agents)
                                      :short-info "Show stats of running charge boxes"}
                              :add   {:fn         (fn [endpoint box-serial]
                                                    (pool/add agents
                                                              [(pool/empty-chargebox endpoint box-serial)]))
                                      :short-info "Add a single charge box to the pool (specify endpoint and box serial)"}
                              :reset {:fn         (fn [] (pool/reset agents))
                                      :short-info "Remove all charge boxes from the pool"}}
              :prompt-string "acp# "}))
