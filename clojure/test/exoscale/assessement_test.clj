(ns exoscale.assessement-test
  (:require [clojure.test :refer [deftest]]
            [exoscale.assessement :refer [update-state]]))

(def instances-state (atom {}))

(deftest test-operation-create
  (let [event { :payload {
                         :after {:organization_uuid "org1"
                                 :instance_uuid "inst1"
                                 :price_second 15
                                 :started true}
                         :op "c"}}]
    (reset! instances-state {})
    (update-state event)
  )
)
