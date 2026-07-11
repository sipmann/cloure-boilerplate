(ns microservice-boilerplate.ports.http-in.pages
  (:require [microservice-boilerplate.controllers.authentication :as controllers.authentication]
            [microservice-boilerplate.controllers.users :as controllers.users]
            [microservice-boilerplate.templates :as templates]))

(defn home
  [request]
  (templates/render request "home.html" {}))

(defn login
  [request]
  (controllers.authentication/login-page request))

(defn do-login
  [request]
  (controllers.authentication/do-login! request))

(defn logout
  [request]
  (controllers.authentication/logout! request))

(defn list-users
  [request]
  (controllers.users/list-all request))

(defn new-user-page
  [request]
  (controllers.users/new-page request))

(defn create-user!
  [request]
  (controllers.users/create! request))

(defn edit-user-page
  [request]
  (controllers.users/edit-page request))

(defn update-user!
  [request]
  (controllers.users/update! request))

(defn delete-user!
  [request]
  (controllers.users/delete! request))
