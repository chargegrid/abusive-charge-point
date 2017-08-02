(ns acp.persistence
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [acp.pool :as pool]))

;;(spit "/tmp/test.json" (json/write-str {:key1 "val1" :key2 "val2"}))

(defn read [path]
  (-> path
      io/resource
      slurp
      (json/read-str :key-fn keyword)))

(defn config-to-agent [endpoint {:keys [serial connectors]}]
  (pool/empty-chargebox endpoint serial connectors))

(defn load [path]
  (let [{:keys [endpoint charge-boxes]} (read path)]
    (map #(config-to-agent endpoint %) charge-boxes)))
