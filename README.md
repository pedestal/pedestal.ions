# pedestal.ions ![CI](https://github.com/pedestal/pedestal.ions/workflows/CI/badge.svg)

[Datomic Ion](https://docs.datomic.com/cloud/ions/ions.html)
interceptor chain provider. To learn more about Ions, checkout the
[docs](https://docs.datomic.com/cloud/ions/ions.html).

## Getting the latest release

Clojure [tools.deps.alpha](https://github.com/clojure/tools.deps.alpha):

```
{io.pedestal/pedestal.ions {:git/url "https://github.com/pedestal/pedestal.ions.git"
                            :sha "56070e360295f3359a6300a2d27858e0a125188b" 
                            :tag "0.1.3"}}
```

## Caveats

Going async either by returning a channel instead of a Context map or
response body is currently not supported.

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
;; 1. Declare the `io.pedestal.ions/ion-provider` chain provider in your service map.
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

Check out the [Pedestal Ions Sample](https://github.com/pedestal/pedestal-ions-sample) project for
a fully functional sample and [example](https://github.com/pedestal/pedestal-ions-sample/blob/master/deps.edn)
`deps.edn` configuration.

## Parameters

Datomic Ions provides facilities for accessing system
parameters. Refer to the [Ion Parameters](https://docs.datomic.com/cloud/ions/ions-reference.html#ion-parameters)
docs for an overview. The `io.pedestal.ions` namespace provides an interceptor fn, `datomic-params-interceptor`,
which makes these these parameters available on the Pedestal Context. Include this interceptor in your commons interceptor collection
to enable parameter support. If included, this interceptor makes parameters available on the Context through the following keys:

- `:io.pedestal.ions/app-info`      Contains the results of `(ion/get-app-info)`
- `:io.pedestal.ions/env-map`       Contains the results of `(ion/get-env)`
- `:io.pedestal.ions/params`        Only present if the `:get-params?` option is provided.
                                    Contains the results of `(ion/get-params {:path path})`
                                    where `path` is calculated using :app-name and :env
                                    from `app-info` and `env-map`, respectively. Param names are keywordized."

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
