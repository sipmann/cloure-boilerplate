(ns microservice-boilerplate.routes
  (:require [io.pedestal.http.csrf :as csrf]
            [microservice-boilerplate.ports.http-in :as ports.http-in]
            [microservice-boilerplate.schemas.wire-in :as schemas.wire-in]
            [reitit.swagger :as swagger]
            [schema.core :as s]))

(def routes
  [["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "btc-wallet"
                            :description "small sample using the microservice-boilerplate"}}
           :handler (swagger/create-swagger-handler)}}]

   ["/home" {:get {:summary "home page"
                   :responses {200 {:body s/Str}
                               500 {:body s/Str}}
                   :handler ports.http-in/home}}]

   ["/login" {:interceptors [(into {} (csrf/anti-forgery))]
              :get {:summary "login page"
                    :responses {200 {:body s/Str}
                                500 {:body s/Str}}
                    :handler ports.http-in/login}
              :post {:summary "login page"
                     :parameters {:form schemas.wire-in/LoginForm}
                     :responses {200 {:body s/Str}
                                 500 {:body s/Str}}
                     :handler ports.http-in/do-login}}]

   ["/logout" {:get {:summary "logout page"
                     :responses {200 {:body s/Str}
                                 500 {:body s/Str}}
                     :handler ports.http-in/logout}}]

   ["/users"
    {:interceptors [(into {} (csrf/anti-forgery))]}

    ["" {:get {:summary "list all users alphabetically"
               :responses {200 {:body s/Str}
                           500 {:body s/Str}}
               :handler ports.http-in/list-users}
         :post {:summary "create a user"
                :parameters {:form schemas.wire-in/UserForm}
                :responses {200 {:body s/Str}
                            500 {:body s/Str}}
                :handler ports.http-in/create-user!}}]

    ["/new" {:conflicting true
             :get {:summary "new user form"
                   :responses {200 {:body s/Str}
                               500 {:body s/Str}}
                   :handler ports.http-in/new-user-page}}]

    ["/:id/edit" {:get {:summary "edit user form"
                        :parameters {:path {:id s/Int}}
                        :responses {200 {:body s/Str}
                                    404 {:body s/Str}
                                    500 {:body s/Str}}
                        :handler ports.http-in/edit-user-page}}]

    ["/:id" {:conflicting true
             :post {:summary "update a user"
                    :parameters {:path {:id s/Int}
                                 :form schemas.wire-in/UserForm}
                    :responses {404 {:body s/Str}
                                500 {:body s/Str}}
                    :handler ports.http-in/update-user!}}]

    ["/:id/delete" {:post {:summary "delete a user"
                           :parameters {:path {:id s/Int}}
                           :responses {500 {:body s/Str}}
                           :handler ports.http-in/delete-user!}}]]

   ["/wallet"
    {:swagger {:tags ["wallet"]}}

    ["/current-btc-usd"
     {:get {:summary "get current btc price in usd"
            :responses {200 {:body schemas.wire-in/BtcUsdPair}
                        500 {:body s/Str}}
            :handler ports.http-in/get-btc-usd-price}}]

    ["/history"
     {:get {:summary "get all wallet entries and current total"
            :responses {200 {:body schemas.wire-in/WalletHistory}
                        500 {:body s/Str}}
            :handler ports.http-in/get-history}}]
    ["/deposit"
     {:post {:summary "do a deposit in btc in the wallet"
             :parameters {:body schemas.wire-in/WalletDeposit}
             :responses {201 {:body schemas.wire-in/WalletEntry}
                         400 {:body s/Str}
                         500 {:body s/Str}}
             :handler ports.http-in/do-deposit!}}]

    ["/withdrawal"
     {:post {:summary "do a withdrawal in btc in the wallet if possible"
             :parameters {:body schemas.wire-in/WalletWithdrawal}
             :responses {201 {:body schemas.wire-in/WalletEntry}
                         400 {:body s/Str}
                         500 {:body s/Str}}
             :handler ports.http-in/do-withdrawal!}}]]])
