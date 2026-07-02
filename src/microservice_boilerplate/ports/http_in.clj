(ns microservice-boilerplate.ports.http-in
  (:require [microservice-boilerplate.adapters :as adapters]
            [microservice-boilerplate.controllers :as controllers]
            [microservice-boilerplate.templates :as templates]
            [parenthesin.helpers.logs :as logs]))

(defn get-btc-usd-price
  [{components :components}]
  {:status 200
   :body {:btc-amount 1
          :usd-amount (controllers/get-btc-usd-price components)}})

(defn get-history
  [{components :components}]
  (let [{:keys [entries usd-price]} (controllers/get-wallet components)]
    {:status 200
     :body (adapters/->wallet-history usd-price entries)}))

(defn do-deposit!
  [{{{:keys [btc]} :body} :parameters
    components :components}]
  (if (pos? btc)
    {:status 201
     :body (-> btc
               (controllers/do-deposit! components)
               adapters/db->wire-in)}
    {:status 400
     :body "btc deposit amount can't be negative."}))

(defn do-withdrawal!
  [{{{:keys [btc]} :body} :parameters
    components :components}]
  (if (neg? btc)
    (if-let [withdrawal (controllers/do-withdrawal! btc components)]
      {:status 201
       :body (adapters/db->wire-in withdrawal)}
      {:status 400
       :body "withdrawal amount bigger than the total in the wallet."})
    {:status 400
     :body "btc withdrawal amount can't be positive."}))

(defn home
  [request]
  (templates/render request "home.html" {}))

(defn login
  [{session :session :as request}]
  (logs/log :info :login :page-access)
  (if (:user session)
    {:status 302 :headers {"Location" "/home" "Content-Type" "text/plain"}}
    (templates/render request "authentication/login.html" {})))

(defn do-login
  [{{{:keys [email password]} :form} :parameters}]
  (if (and (= email "admin@admin.com") (= password "admin"))
    {:status 302
     :headers {"Location" "/home" "Content-Type" "text/plain"}
     :session {:user {:email email
                      :name "Admin"}}}
    {:status 401
     :body "invalid email or password"}))