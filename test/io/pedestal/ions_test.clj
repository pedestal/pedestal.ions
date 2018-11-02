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
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.ions :as ions]
            [io.pedestal.ions.test :as ions.test]))

;; Test app
(defn about
  [request]
  {:status 200
   :body   (format "Clojure %s" (clojure-version))})

(defn home
  [request]
  {:status 200
   :body  "Hello World!"})

(def common-interceptors [(body-params/body-params) http/json-body])

(def routes #{["/" :get (conj common-interceptors `home)]
              ["/about" :get (conj common-interceptors `about)]})

(def service (-> {::http/routes routes ::http/chain-provider ions/ion-provider}
                 http/default-interceptors
                 http/create-provider))

;; Tests
(deftest home-page-test
  (is (= (:body (ions.test/response-for service :get "/"))
         "Hello World!"))
  (is (=
       (:headers (ions.test/response-for service :get "/"))
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
      (:body (ions.test/response-for service :get "/about")))
  (is (=
       (:headers (ions.test/response-for service :get "/about"))
       {"Content-Type" "text/plain"
        "Strict-Transport-Security" "max-age=31536000; includeSubdomains"
        "X-Frame-Options" "DENY"
        "X-Content-Type-Options" "nosniff"
        "X-XSS-Protection" "1; mode=block"
        "X-Download-Options" "noopen"
        "X-Permitted-Cross-Domain-Policies" "none"
        "Content-Security-Policy" "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;"})))
