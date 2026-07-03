(ns microservice-boilerplate.controllers.authentication
  (:require [schema.core :as s]
            [microservice-boilerplate.database.users :as db.users]
            [microservice-boilerplate.security.password :as security.password]
            [microservice-boilerplate.templates :as templates]))

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

      {:status 401 ;TODO: Use flash and redirect to login page with error message
       :body "invalid email or password"})

    {:status 401
     :body "invalid email or password"}))