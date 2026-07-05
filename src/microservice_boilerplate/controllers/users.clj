(ns microservice-boilerplate.controllers.users
  (:require [clojure.string :as string]
            [microservice-boilerplate.adapters.users :as adapters.users]
            [microservice-boilerplate.database.users :as db.users]
            [microservice-boilerplate.security.password :as security.password]
            [microservice-boilerplate.templates :as templates]
            [schema.core :as s]))

(s/defn list-all
  [{{:keys [database]} :components :as request}]
  (let [users (mapv #(dissoc % :password-hash) (db.users/all-users database))] ;TODO: Move this into the adapter, no here
    (templates/render request "users/list.html" {:users users})))

(s/defn new-page
  [request]
  (templates/render request "users/new.html" {}))

(s/defn create!
  [{{{:keys [name email password role]} :form} :parameters
    {:keys [database]} :components :as request}]
  (try
    (db.users/insert! (adapters.users/new-user-model->db
                        {:email email
                         :name name
                         :password-hash (security.password/hash-password password)
                         :role role})
                      database)
    {:status 302
     :headers {"Location" "/users" "Content-Type" "text/plain"}
     :flash {:success (str "Usuário " name " criado com sucesso.")}}
    (catch Exception _
      (templates/render request "users/new.html"
                        {:error "Não foi possível criar o usuário. Verifique se o email já está cadastrado."
                         :name name
                         :email email
                         :role role}))))

(s/defn edit-page
  [{{{:keys [id]} :path} :parameters
    {:keys [database]} :components :as request}]
  (if-let [user (db.users/user-by-id id database)]
    (templates/render request "users/edit.html" {:user user})
    {:status 404 :headers {"Content-Type" "text/plain"} :body "user not found"}))

(s/defn update!
  [{{{:keys [id]} :path
     {:keys [name email password role]} :form} :parameters
    {:keys [database]} :components}]
  (if-let [current (db.users/user-by-id id database)]
    (let [password-hash (if (string/blank? password)
                          (:password-hash current)
                          (security.password/hash-password password))]
      (db.users/update! id (adapters.users/new-user-model->db
                            {:email email
                             :name name
                             :password-hash password-hash
                             :role role})
                        database)
      {:status 302
       :headers {"Location" "/users" "Content-Type" "text/plain"}
       :flash {:success (str "Usuário " name " atualizado com sucesso.")}})
    {:status 404 :headers {"Content-Type" "text/plain"} :body "user not found"}))

(s/defn delete!
  [{{{:keys [id]} :path} :parameters
    {:keys [database]} :components}]
  (let [user (db.users/user-by-id id database)]
    (db.users/delete! id database)
    {:status 302
     :headers {"Location" "/users" "Content-Type" "text/plain"}
     :flash {:success (str "Usuário " (:name user) " removido com sucesso.")}}))
