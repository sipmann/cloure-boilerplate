(ns microservice-boilerplate.database.users
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as sql.helpers]
            [microservice-boilerplate.adapters.users :as adapters.users]
            [microservice-boilerplate.schemas.model :as schemas.model]
            [microservice-boilerplate.schemas.types :as schemas.types]
            [parenthesin.components.db.jdbc-hikari :as components.database]
            [schema.core :as s]))

(s/defn user-by-email :- (s/maybe schemas.model/User)
  [email :- s/Str
   db :- schemas.types/DatabaseComponent]
  (let [query (-> (sql.helpers/select :id :email :name :password_hash :role)
                  (sql.helpers/from :users)
                  (sql.helpers/where [:= :email email])
                  sql/format)]
    (some-> (components.database/execute db query)
            first
            adapters.users/db->model)))