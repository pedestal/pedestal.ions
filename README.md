# pedestal.ions

[Datomic Ion](https://docs.datomic.com/cloud/ions/ions.html)
interceptor chain provider. To learn more about Ions, checkout the
[docs](https://docs.datomic.com/cloud/ions/ions.html).

## Parameters

Datomic Ions provides facilities for accessing system
parameters. Refer to the [Ion Parameters](https://docs.datomic.com/cloud/ions/ions-reference.html#ion-parameters)
docs for an overview. The Pedestal Ions provider makes these these
parameters available on the Pedestal Context through the following keys:

- `:io.pedestal.ions/app-info`      Contains the results of `(ion/get-app-info)`
- `:io.pedestal.ions/env-map`       Contains the results of `(ion/get-env)`
- `:io.pedestal.ions/params`        Contains the results of `(ion/get-params {:path path})`
                                    where `path` is calculated using :app-name and :env
                                    from `app-info` and `env-map`, respectively. Param names are keywordized."

## Usage

1. Declare the `io.pedestal.ions/ion-provider` chain provider in your service map.
1. Create a fn which constructs an ion handler fn from the service map.
1. Create your ion by ionizing  your handler.
1. Whitelist your ion in the `ion-config.edn` resource file.

### Example

```
;; src/my_ion/example.clj
;;
(ns my-ion.example
    (:require [io.pedestal.http :as http]
              [io.pedestal.http.route :as route]
              [io.pedestal.ions :as provider]
              [ion-provider.datomic]
              [ring.util.response :as ring-resp]
              [datomic.client.api :as d]
              [datomic.ion.lambda.api-gateway :as apig])

;; Routes elided
(def routes ...)

;;
;; 1. Declare the `com.cognitect.pedestal.ions/ion-provider` chain provider in your service map.
;;
(def service {:env :prod
              ::http/routes routes
              ::http/resource-path "/public"
              ::http/chain-provider provider/ion-provider})

;;
;; 2. Create a fn which constructs an ion handler fn from the service map.
;;
(defn handler
  "Ion handler"
  [service-map]
  (-> service-map
      http/default-interceptors
      http/create-provider))

;;
;; 3. Create your ion by ionizing your handler.
;;
(def app (apig/ionize (handler service)))

;; resources/ion-config.edn
;;
;;
;; 4. Whitelist your ion in the `ion-config.edn` resource file.
;;
{:allow    [my_ion.example/app]
 :lambdas  {:app {:fn my_ion.example/app :description "Pedestal Ions example"}}
 :app-name "my-ion-example"}
```

## Contributing

Thanks for your interest in contributing to pedestal.ions! This project is governed by the same contribution guidelines as dictated in the [Contributing to Pedestal](https://github.com/pedestal/pedestal/blob/master/CONTRIBUTING.md) document. Please have a look and sign a [Cognitect Contributor Agreement](https://secure.echosign.com/public/hostedForm?formid=8JU33Z7A7JX84U) before submitting a PR.

---

## License
Copyright 2018 Cognitect, Inc.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
which can be found in the file [epl-v10.html](epl-v10.html) at the root of this distribution.

By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
