(ns microservice-boilerplate.schemas.db
  (:require [schema.core :as s]))

(def wallet {:wallet/id s/Uuid
             :wallet/btc_amount s/Num
             :wallet/usd_amount_at s/Num
             :wallet/created_at s/Inst})

(s/defschema WalletTransaction
  (select-keys wallet [:wallet/id
                       :wallet/btc_amount
                       :wallet/usd_amount_at]))

(s/defschema WalletEntry
  (select-keys wallet [:wallet/id
                       :wallet/btc_amount
                       :wallet/usd_amount_at
                       :wallet/created_at]))

(def users {:users/id s/Int
            :users/email s/Str
            :users/name s/Str
            :users/password_hash s/Str
            :users/role s/Str})

(s/defschema User
  (select-keys users [:users/id
                      :users/email
                      :users/name
                      :users/password_hash
                      :users/role]))

(s/defschema NewUser
  (select-keys users [:users/email
                      :users/name
                      :users/password_hash
                      :users/role]))
