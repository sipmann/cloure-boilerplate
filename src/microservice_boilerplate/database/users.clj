(ns microservice-boilerplate.database.users
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as sql.helpers]
            [microservice-boilerplate.adapters.users :as adapters.users]
            [microservice-boilerplate.schemas.db :as schemas.db]
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

(s/defn all-users :- [schemas.model/User]
  [db :- schemas.types/DatabaseComponent]
  (let [query (-> (sql.helpers/select :id :email :name :password_hash :role)
                  (sql.helpers/from :users)
                  (sql.helpers/order-by [:%lower.name :asc])
                  sql/format)]
    (mapv adapters.users/db->model (components.database/execute db query))))

(s/defn user-by-id :- (s/maybe schemas.model/User)
  [id :- s/Int
   db :- schemas.types/DatabaseComponent]
  (let [query (-> (sql.helpers/select :id :email :name :password_hash :role)
                  (sql.helpers/from :users)
                  (sql.helpers/where [:= :id id])
                  sql/format)]
    (some-> (components.database/execute db query)
            first
            adapters.users/db->model)))

(s/defn insert! :- schemas.model/User
  [new-user :- schemas.db/NewUser
   db :- schemas.types/DatabaseComponent]
  (let [query (-> (sql.helpers/insert-into :users)
                  (sql.helpers/values [new-user])
                  (sql.helpers/returning :id :email :name :password_hash :role)
                  sql/format)]
    (-> (components.database/execute db query)
        first
        adapters.users/db->model)))

(s/defn update! :- (s/maybe schemas.model/User)
  [id :- s/Int
   attrs :- schemas.db/NewUser
   db :- schemas.types/DatabaseComponent]
  (let [query (-> (sql.helpers/update :users)
                  (sql.helpers/set attrs)
                  (sql.helpers/where [:= :id id])
                  (sql.helpers/returning :id :email :name :password_hash :role)
                  sql/format)]
    (some-> (components.database/execute db query)
            first
            adapters.users/db->model)))

(s/defn delete!
  [id :- s/Int
   db :- schemas.types/DatabaseComponent]
  (components.database/execute
   db
   (-> (sql.helpers/delete-from :users)
       (sql.helpers/where [:= :id id])
       sql/format)))