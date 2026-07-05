(ns microservice-boilerplate.schemas.wire-in
  (:require [schema.core :as s]))

(s/defschema WalletDeposit
  {:btc s/Num})

(s/defschema WalletWithdrawal
  {:btc s/Num})

(s/defschema WalletEntry
  {:id s/Uuid
   :btc-amount s/Num
   :usd-amount-at s/Num
   :created-at s/Inst})

(s/defschema WalletHistory
  {:entries [WalletEntry]
   :total-btc s/Num
   :total-current-usd s/Num})

(s/defschema BtcUsdPair
  {:btc-amount s/Num
   :usd-amount s/Num})

(s/defschema LoginForm
  {:email s/Str
   :password s/Str})

(s/defschema UserForm
  {:name s/Str
   :email s/Str
   :password s/Str
   :role s/Str})
