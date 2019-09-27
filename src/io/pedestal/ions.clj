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

(ns io.pedestal.ions
  (:require [clojure.java.io :as io]
            [io.pedestal.log :as log]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [datomic.ion]
            [ring.util.response :as ring-response])
  (:import (java.io OutputStream PipedInputStream PipedOutputStream)))

(defprotocol IonizeBody
  (default-content-type [body] "Get default HTTP content-type for `body`.")
  (ionize [body] "Transforms body to a value type supported by Datomic Ions."))

(extend-protocol IonizeBody

  (class (byte-array 0))
  (default-content-type [_] "application/octet-stream")
  (ionize [byte-array]
    (io/input-stream byte-array))

  String
  (default-content-type [_] "text/plain")
  (ionize [string] string)

  clojure.lang.IPersistentCollection
  (default-content-type [_] "application/edn")
  (ionize [o] (pr-str o))

  clojure.lang.Fn
  (default-content-type [_] nil)
  (ionize [f]
    (let [i (PipedInputStream.)
          o (PipedOutputStream. i)]
      (future
        (try
          (f o)
          (catch Exception e
            (log/error :msg "Failure ionizing function. Unable to create streaming response."))
          (finally
            (.close ^OutputStream o))))
      i))

  java.io.File
  (default-content-type [_] "application/octet-stream")
  (ionize [file]
    ;; should be able to return the file...
    (io/input-stream file))

  java.io.InputStream
  (default-content-type [_] "application/octet-stream")
  (ionize [input-stream] input-stream)

  nil
  (default-content-type [_] nil)
  ;; An ion compatible response body is a string or inputstream.
  (ionize [_] (io/input-stream (byte-array 0))))

(def
  terminator-injector
  (interceptor/interceptor {:name ::terminator-injector
                            :enter (fn [ctx]
                                     (chain/terminate-when ctx #(ring-response/response? (:response %))))}))

(defn- assoc-error-response
  [ctx message]
  (log/info :msg "sending error" :message message)
  (assoc ctx :response {:status 500 :body message}))

(defn- set-default-content-type
  [{:keys [headers body] :or {headers {}} :as resp}]
  (let [content-type (headers "Content-Type")]
    (update-in resp [:headers] merge {"Content-Type" (or content-type
                                                       (default-content-type body))})))
(defn- ionize-body
  [resp]
  (update resp :body ionize))

(def ring-response
  (interceptor/interceptor {:name ::ring-response
                            :leave (fn [ctx]
                                     (if (:response ctx)
                                       (update ctx :response (comp ionize-body set-default-content-type))
                                       (assoc-error-response ctx "Internal server error: no response.")))
                            :error (fn [ctx ex]
                                     (log/error :msg "error response triggered"
                                       :exception ex
                                       :context ctx)
                                     (assoc-error-response ctx "Internal server error: exception."))}))

(defn- add-content-type
  [req]
  (if-let [ctype (get-in req [:headers "content-type"])]
    (assoc req :content-type ctype)
    req))

(defn- add-content-length
  [req]
  (if-let [clength (get-in req [:headers "content-length"])]
    (assoc req :content-length clength)
    req))

(defn- get-or-fail
  [m k]
  (or (get m k) (throw (ex-info (format "Key %s not found." k) {:key k
                                                                :map m}))))

(def ^:private prepare-params
  "Given opts, constructs parameter map containing Datomic Ion params info.
  The only option supported is `:get-params?`. Refer to the `datomic-params-interceptor`
  documentation for details."
  (memoize (fn [opts]
             (let [app-info    (datomic.ion/get-app-info)
                   env-map     (datomic.ion/get-env)
                   app         (get-or-fail app-info :app-name)
                   env         (get-or-fail env-map :env)
                   params-path (format "/datomic-shared/%s/%s/" (name env) app)]
               (cond-> {::app-info app-info
                        ::env-map env-map}

                 (:get-params? opts)
                 (assoc ::params
                        (reduce-kv #(assoc %1 (keyword %2) %3)
                                   {}
                                   (datomic.ion/get-params {:path params-path}))))))))

(defn datomic-params-interceptor
  "Given opts, constructs an interceptor which assoc's Datomic Ion parameters to
  the context map.

  opts:

  - :get-params?  If true, then parameters will be retrieved from AWS
                  Systems Manager Parameter Store using the path \"datomic-shared/{env}/{app-name}\".
                  Defaults to `true`.

  The param info is available via the following keys;
  - :io.pedestal.ions/app-info      Contains the results of `(ion/get-app-info)`
  - :io.pedestal.ions/env-map       Contains the results of `(ion/get-env)`
  - :io.pedestal.ions/params        Contains the results of `(ion/get-params {:path path})
                                    where `path` is calculated using :app-name and :env,
                                    from app-info and env-map, respectively.
                                    Param names are keywordized."
  ([]
   (datomic-params-interceptor {:get-params? true}))
  ([opts]
   (interceptor/interceptor
    {:name  ::datomic-params-interceptor
     :enter (fn [ctx]
              (merge ctx (prepare-params opts)))})))

(defn ion-provider
  "Given a service map, returns a handler function which consumes ring requests
  and returns ring responses suitable for Datomic Ion consumption."
  [service-map]
  (let [interceptors    (into [terminator-injector ring-response]
                              (:io.pedestal.http/interceptors service-map))]
    (fn [{:keys [uri] :as request}]
      (let [initial-context  {:request (-> request
                                           (assoc :path-info uri)
                                           add-content-type
                                           add-content-length)}
            response-context (chain/execute initial-context interceptors)]
        (:response response-context)))))
