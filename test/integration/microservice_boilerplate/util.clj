(ns integration.microservice-boilerplate.util
  (:require [com.stuartsierra.component :as component]
            [parenthesin.helpers.logs :as logs]
            [parenthesin.helpers.migrations :as migrations]
            [pg-embedded-clj.core :as pg-emb]))

(defn- db-port
  "Porta do Postgres embutido dos testes - le a mesma env var DB-PORT que
  config.edn usa pro :database :port, pra embutido e app-sob-teste
  conversarem na mesma porta. Permite rodar os testes de integracao mesmo
  com um Postgres nativo local ocupando a 5432 (ver bb.edn: task `test`
  seta DB-PORT=5433 via :extra-env)."
  []
  (if-let [port (System/getenv "DB-PORT")]
    (Long/parseLong port)
    5432))

(defn start-system!
  [system-start-fn]
  (fn []
    (logs/setup :info :auto)
    (pg-emb/init-pg {:port (db-port)})
    (migrations/migrate (migrations/configuration-with-db))
    (system-start-fn)))

(defn stop-system!
  [system]
  (component/stop-system system)
  (pg-emb/halt-pg!))
