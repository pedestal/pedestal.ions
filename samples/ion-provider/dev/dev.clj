(ns dev
  (:require [clojure.test :refer [run-all-tests]]))

(comment :dev

         (require '[ion-provider.service-test])

         (run-all-tests #"ion-provider.service-test")

 )
