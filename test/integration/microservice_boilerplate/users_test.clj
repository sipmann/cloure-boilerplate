(ns integration.microservice-boilerplate.users-test
  (:require [clojure.test :as clojure.test]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [honey.sql.helpers :as sql.helpers]
            [integration.microservice-boilerplate.util :as util]
            [microservice-boilerplate.database.users :as db.users]
            [parenthesin.components.config.aero :as components.config]
            [parenthesin.components.db.jdbc-hikari :as components.database]
            [schema.test :as schema.test]
            [state-flow.api :as state-flow.api :refer [defflow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]
            [state-flow.core :refer [flow]]
            [state-flow.state :as state]))

(clojure.test/use-fixtures :once schema.test/validate-schemas)

(defn- create-and-start-components! []
  (component/start-system
   (component/system-map
    :config (components.config/new-config)
    :database (component/using (components.database/new-database)
                               [:config]))))

(defn- insert-user! [database name email]
  (components.database/execute
   database
   (-> (sql.helpers/insert-into :users)
       (sql.helpers/values [{:name name
                             :email email
                             :password_hash "hash"
                             :role "user"}])
       sql/format)))

(defflow flow-integration-users-test
  {:init (util/start-system! create-and-start-components!)
   :cleanup util/stop-system!
   :fail-fast? true}
  (flow "lists all users ordered alphabetically by name, case-insensitively"
    [database (state-flow.api/get-state :database)]

    (state/invoke #(insert-user! database "Zoe" "zoe@example.com"))
    (state/invoke #(insert-user! database "ana" "ana@example.com"))
    (state/invoke #(insert-user! database "Bruno" "bruno@example.com"))

    (flow "returns users sorted by name regardless of case (seeded admin user comes first)"
      (match? [{:name "Admin"}
               {:name "ana"}
               {:name "Bruno"}
               {:name "Zoe"}]
              (db.users/all-users database)))))
