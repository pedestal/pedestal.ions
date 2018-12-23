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

(ns io.pedestal.ions-test
  (:require [clojure.test :refer :all]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.ions :as ions]
            [io.pedestal.ions.test :as ions.test]
            [datomic.ion]
            [cheshire.core :as json])
  (:import (java.io ByteArrayInputStream)))

;; Test app
(defn about
  [_]
  {:status 200
   :body   (format "Clojure %s" (clojure-version))})

(defn home
  [_]
  {:status 200
   :body  "Hello World!"})

(defn echo
  [request]
  {:status 200
   :body (:json-params request)})

(def common-interceptors [(body-params/body-params) http/json-body])

(def routes #{["/" :get (conj common-interceptors `home)]
              ["/about" :get (conj common-interceptors `about)]
              ["/echo" :post (conj common-interceptors `echo)]})

(defn service
  []
  (-> {::http/routes routes ::http/chain-provider ions/ion-provider}
      http/default-interceptors
      http/create-provider))

;; Tests
(deftest home-page-test
  (is (= (:body (ions.test/response-for (service) :get "/"))
         "Hello World!"))
  (is (=
       (:headers (ions.test/response-for (service) :get "/"))
       {"Content-Type" "text/plain"
        "Strict-Transport-Security" "max-age=31536000; includeSubdomains"
        "X-Frame-Options" "DENY"
        "X-Content-Type-Options" "nosniff"
        "X-XSS-Protection" "1; mode=block"
        "X-Download-Options" "noopen"
        "X-Permitted-Cross-Domain-Policies" "none"
        "Content-Security-Policy" "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;"})))

(deftest about-page-test
  (is "Clojure 1.9"
      (:body (ions.test/response-for (service) :get "/about")))
  (is (=
       (:headers (ions.test/response-for (service) :get "/about"))
       {"Content-Type"                      "text/plain"
        "Strict-Transport-Security"         "max-age=31536000; includeSubdomains"
        "X-Frame-Options"                   "DENY"
        "X-Content-Type-Options"            "nosniff"
        "X-XSS-Protection"                  "1; mode=block"
        "X-Download-Options"                "noopen"
        "X-Permitted-Cross-Domain-Policies" "none"
        "Content-Security-Policy"           "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;"})))

(defn- post-echo
  [body]
  (ions.test/response-for (service)
                          :post "/echo"
                          :headers {"content-type" "application/json"}
                          :body body))

(deftest post-test
  (let [expected     {:foo "bar"}
        json-payload (json/encode expected)]
    ;; user are here instead
    (testing "string body"
      (let [result (post-echo json-payload)]
        (is (= 200 (:status result)))
        (is (= expected
               (-> result
                   :body
                   slurp
                   (json/parse-string keyword))))))

    (testing "stream body"
      (let [result (post-echo (ByteArrayInputStream. (.getBytes json-payload)))]
        (is (= 200 (:status result)))
        (is (= expected
               (-> result
                   :body
                   slurp
                   (json/parse-string keyword))))))))

(deftest params-test
  (let [param-key   "a-param"
        param-value "a-value"
        expected    {::ions/app-info {:app-name "params-test"}
                     ::ions/env-map  {:env :unit-test}
                     ::ions/params   {(keyword param-key) param-value}}]
    (with-redefs [datomic.ion/get-app-info (constantly (::ions/app-info expected))
                  datomic.ion/get-env (constantly (::ions/env-map expected))
                  datomic.ion/get-params (constantly {param-key param-value})]
      (is (= expected (chain/execute {} [(ions/datomic-params-interceptor)])))
      (is (= (dissoc expected ::ions/params) (chain/execute {} [(ions/datomic-params-interceptor {:get-params? false})]))))))
