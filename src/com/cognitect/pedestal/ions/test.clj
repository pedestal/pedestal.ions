;; Copyright 2018 Cognitect, Inc.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

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
