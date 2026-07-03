(ns microservice-boilerplate.adapters.users
  (:require [microservice-boilerplate.schemas.db :as schemas.db]
            [microservice-boilerplate.schemas.model :as schemas.model]
            [schema.core :as s]))

(s/defn db->model :- schemas.model/User
  [{:users/keys [id email name password_hash role]} :- schemas.db/User]
  {:id id
   :email email
   :name name
   :password-hash password_hash
   :role role})

(s/defn model->db :- schemas.db/User
  [{:keys [id email name password-hash role]} :- schemas.model/User]
  {:users/id id
   :users/email email
   :users/name name
   :users/password_hash password-hash
   :users/role role})
