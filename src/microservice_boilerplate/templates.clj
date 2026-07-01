(ns microservice-boilerplate.templates
  (:require [clojure.java.io :as io]
            [selmer.parser :as parser]))

(parser/set-resource-path! (io/resource "templates"))

(defn render
  [template params]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (parser/render-file template params)})