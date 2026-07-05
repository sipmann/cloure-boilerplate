(ns microservice-boilerplate.schemas.model
  (:require [schema.core :as s]))

(s/defschema User
  {:id s/Int
   :email s/Str
   :name s/Str
   :password-hash s/Str
   :role s/Str})

(s/defschema NewUser
  {:email s/Str
   :name s/Str
   :password-hash s/Str
   :role s/Str})
