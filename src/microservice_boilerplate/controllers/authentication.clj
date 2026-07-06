(ns microservice-boilerplate.controllers.authentication
  (:require [microservice-boilerplate.database.users :as db.users]
            [microservice-boilerplate.security.password :as security.password]
            [microservice-boilerplate.templates :as templates]
            [schema.core :as s]))

(s/defn login-page
  [{session :session :as request}]
  (if (:user session)
    {:status 302 :headers {"Location" "/home" "Content-Type" "text/plain"}}
    (templates/render request "authentication/login.html" {})))

(s/defn do-login!
  [{{{:keys [email password]} :form} :parameters
    {:keys [database]} :components}]
  (if-let [user (db.users/user-by-email email database)]
    (if (security.password/check-password password (:password-hash user))
      {:status 302
       :headers {"Location" "/home" "Content-Type" "text/plain"}
       :session {:user (dissoc user :password-hash)}}

      {:status 302
       :headers {"Location" "/login" "Content-Type" "text/plain"}
       :flash {:error "Email ou senha inválidos."}})

    {:status 302
     :headers {"Location" "/login" "Content-Type" "text/plain"}
     :flash {:error "Email ou senha inválidos."}}))

(s/defn logout!
  [_request]
  {:status 302
   :headers {"Location" "/login"}
   :session nil})