(ns microservice-boilerplate.security.password
  (:import [org.mindrot.jbcrypt BCrypt]))

(defn hash-password
  [password]
  (BCrypt/hashpw password (BCrypt/gensalt 12)))

(defn check-password
  [password hash]
  (BCrypt/checkpw password hash))