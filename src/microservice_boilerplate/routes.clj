(ns microservice-boilerplate.routes
  (:require [microservice-boilerplate.routes.api :as routes.api]
            [microservice-boilerplate.routes.pages :as routes.pages]))

(def routes
  (into routes.pages/routes routes.api/routes))
