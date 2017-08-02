(defproject acp "0.1.0-SNAPSHOT"
  :description "Charge box pool simulator"
  :url "http://www.42amps.com"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring-server "0.4.0"]
                 [reagent "0.5.1"
                  :exclusions [org.clojure/tools.reader]]
                 [reagent-forms "0.5.13"]
                 [reagent-utils "0.1.7"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [environ "1.0.1"]
                 [stylefruits/gniazdo "0.4.1"]
                 [org.clojure/data.json "0.2.6"]
                 [jarohen/chime "0.1.9"]
                 [clj-time "0.11.0"]
                 [cli4clj "1.0.0"]
                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]]

  :plugins [[lein-environ "1.0.1"]]

  :main acp.core)
