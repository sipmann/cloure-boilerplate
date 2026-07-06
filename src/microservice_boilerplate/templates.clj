(ns microservice-boilerplate.templates
  (:require [clojure.java.io :as io]
            [io.pedestal.http.csrf :as csrf]
            [selmer.parser :as parser]))

(parser/set-resource-path! (io/resource "templates"))

;FIXME: Temp fix for dev environment
(parser/cache-off!)

(defn- csrf-field-html [token]
  (str "<input id=\"__anti-forgery-token\" name=\"__anti-forgery-token\" type=\"hidden\" value=\"" token "\" />"))

(parser/add-tag! :csrf-field (fn [_ context-map] (csrf-field-html (:csrf-token context-map))))

(defn render
  [request template params]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file template (assoc params
                                                :current-user (get-in request [:session :user])
                                                :csrf-token (csrf/anti-forgery-token request)
                                                :flash (:flash request)))})