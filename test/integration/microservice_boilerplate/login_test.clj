(ns integration.microservice-boilerplate.login-test
  (:require [clojure.string :as string]
            [clojure.test :as clojure.test]
            [com.stuartsierra.component :as component]
            [integration.microservice-boilerplate.util :as util]
            [matcher-combinators.matchers :as matchers]
            [microservice-boilerplate.router :as router]
            [microservice-boilerplate.routes :as routes]
            [parenthesin.components.config.aero :as components.config]
            [parenthesin.components.db.jdbc-hikari :as components.database]
            [parenthesin.components.http.clj-http :as components.http]
            [parenthesin.components.server.reitit-pedestal-jetty :as components.webserver]
            [parenthesin.helpers.state-flow.server.pedestal :as state-flow.server]
            [schema.test :as schema.test]
            [state-flow.api :refer [defflow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]
            [state-flow.core :as state-flow :refer [flow]]))

(clojure.test/use-fixtures :once schema.test/validate-schemas)

(defn- create-and-start-components! []
  (component/start-system
   (component/system-map
    :config (components.config/new-config)
    :http (components.http/new-http-mock {})
    :router (component/using (router/new-router routes/routes)
                             [:config])
    :database (component/using (components.database/new-database)
                               [:config])
    :webserver (component/using (components.webserver/new-webserver)
                                [:config :http :router :database]))))

;; the session cookie identifies the client to the server; without echoing it
;; back, each request would start a brand new (anonymous) session
(defn- session-cookie [response]
  (some-> (get-in response [:headers "Set-Cookie"])
          first
          (string/split #";")
          first))

;; the anti-forgery token is only ever exposed inside the rendered login page
;; html, so it has to be scraped from the hidden form field
(defn- csrf-token [response]
  (some-> (:body response)
          (->> (re-find #"name=\"__anti-forgery-token\" type=\"hidden\" value=\"([^\"]+)\""))
          second))

(defn- form-post [uri cookie token body]
  (state-flow.server/request! {:method :post
                               :uri    uri
                               :headers (cond-> {"Content-Type" "application/x-www-form-urlencoded"}
                                          cookie (assoc "Cookie" cookie)
                                          token (assoc "x-csrf-token" token))
                               :body   body}))

(defflow flow-integration-login-test
  {:init (util/start-system! create-and-start-components!)
   :cleanup util/stop-system!
   :fail-fast? true}
  (flow "should authenticate users through the login form"

    (flow "should show the login page to a guest"
      (match? {:status 200
               :body #"Login"}
              (state-flow.server/request! {:method :get
                                           :uri    "/login"})))

    (flow "shouldn't accept a login post without a valid csrf token"
      (match? {:status 403
               :body #"Invalid anti-forgery token"}
              (form-post "/login" nil nil {:email "admin@admin.com"
                                           :password "admin"})))

    (flow "shouldn't authenticate a user with wrong credentials"
      [login-page (state-flow.server/request! {:method :get :uri "/login"})
       :let [cookie (session-cookie login-page)
             token (csrf-token login-page)]
       failed-login-response (form-post "/login" cookie token {:email "admin@admin.com"
                                                               :password "wrong-password"})
       login-page-with-flash (state-flow.server/request! {:method :get
                                                          :uri "/login"
                                                          :headers {"Cookie" cookie}})]
      (match? (matchers/embeds {:status 302 :headers {"Location" "/login"}})
              failed-login-response)

      (match? {:status 200 :body #"Email ou senha inválidos"}
              login-page-with-flash))

    (flow "should authenticate a user with the right credentials"
      [login-page (state-flow.server/request! {:method :get :uri "/login"})
       :let [cookie (session-cookie login-page)
             token (csrf-token login-page)]
       login-response (form-post "/login" cookie token {:email "admin@admin.com"
                                                        :password "admin"})]
      (match? (matchers/embeds {:status 302
                                :headers {"Location" "/home"}})
              login-response)

      (flow "should redirect an already authenticated user away from the login page"
        (match? (matchers/embeds {:status 302
                                  :headers {"Location" "/home"}})
                (state-flow.server/request! {:method :get
                                             :uri    "/login"
                                             :headers {"Cookie" cookie}}))))))
