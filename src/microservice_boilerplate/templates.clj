(ns microservice-boilerplate.templates
  (:require [clojure.java.io :as io]
            [selmer.parser :as parser]
            [io.pedestal.http.csrf :as csrf]))

(parser/set-resource-path! (io/resource "templates"))

(defn- csrf-field-html [token]
  (str "<input id=\"__anti-forgery-token\" name=\"__anti-forgery-token\" type=\"hidden\" value=\"" token "\" />"))

(parser/add-tag! :csrf-field (fn [_ context-map] (csrf-field-html (:csrf-token context-map))))

(defn render
  [request template params]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file template (assoc params
                                                  :current-user (get-in request [:session :user])
                                                  :csrf-token (csrf/anti-forgery-token request)))})