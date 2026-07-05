(ns unit.microservice-boilerplate.adapters.users-test
  (:require [clojure.test :refer [use-fixtures]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as properties]
            [microservice-boilerplate.adapters.users :as adapters.users]
            [microservice-boilerplate.schemas.db :as schemas.db]
            [microservice-boilerplate.schemas.model :as schemas.model]
            [schema-generators.generators :as g]
            [schema.core :as s]
            [schema.test :as schema.test]))

(use-fixtures :once schema.test/validate-schemas)

(defspec db->model-test 50
  (properties/for-all [user-db (g/generator schemas.db/User)]
                      (s/validate schemas.model/User (adapters.users/db->model user-db))))

(defspec model->db-test 50
  (properties/for-all [user-model (g/generator schemas.model/User)]
                      (s/validate schemas.db/User (adapters.users/model->db user-model))))

(defspec model->db->model-round-trip-test 50
  (properties/for-all [user-model (g/generator schemas.model/User)]
                      (= user-model
                         (adapters.users/db->model (adapters.users/model->db user-model)))))

(defspec db->model->db-round-trip-test 50
  (properties/for-all [user-db (g/generator schemas.db/User)]
                      (= user-db
                         (adapters.users/model->db (adapters.users/db->model user-db)))))

(defspec new-user-model->db-test 50
  (properties/for-all [new-user-model (g/generator schemas.model/NewUser)]
                      (s/validate schemas.db/NewUser (adapters.users/new-user-model->db new-user-model))))
