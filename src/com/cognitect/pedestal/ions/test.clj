(ns com.cognitect.pedestal.ions.test
  (:require  [clojure.test :as t]))

(defn response-for
  "Like pedestal.service's `response-for` but expects `service` to be a handler
  created by `com.cognitect.pedestal.ions/ion-provider`."
  [service verb url & options]
  (let [{:keys [server-port
                server-name
                remote-addr
                scheme
                headers]
         :or   {server-port 0
                server-name "localhost"
                remote-addr "127.0.0.1"
                scheme      :http
                headers     {}}} options]
    (service {:server-port    server-port
              :server-name    server-name
              :remote-addr    remote-addr
              :uri            url
              :scheme         scheme
              :request-method verb
              :headers        headers})))
