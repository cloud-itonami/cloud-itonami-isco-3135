(ns metal-ops.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [metal-ops.governor :as gov]
            [metal-ops.store :as store]))

(deftest hard-violations-unregistered-plant
  (testing "unregistered plant triggers hard violation"
    (let [proposal {:op :log-furnace-reading :effect :propose}
          request {:plant-id :nonexistent}
          s (store/mem-store {})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))
      (is (not (:ok? verdict)))
      (is (seq (:violations verdict))))))

(deftest hard-violations-unverified-plant
  (testing "unverified plant triggers hard violation"
    (let [proposal {:op :log-furnace-reading :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? false}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))
      (is (not (:ok? verdict))))))

(deftest hard-violations-non-propose-effect
  (testing "non-:propose effect triggers hard violation"
    (let [proposal {:op :log-furnace-reading :effect :commit}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))
      (is (not (:ok? verdict))))))

(deftest hard-violations-forbidden-ops
  (testing "furnace-power-on is forbidden"
    (let [proposal {:op :furnace-power-on :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "furnace-power-off is forbidden"
    (let [proposal {:op :furnace-power-off :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "smelting-temperature-control is forbidden"
    (let [proposal {:op :smelting-temperature-control :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "alloy-composition-control is forbidden"
    (let [proposal {:op :alloy-composition-control :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "emergency-shutdown is forbidden"
    (let [proposal {:op :emergency-shutdown :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict)))))

(deftest escalation-anomalous-reading
  (testing "flag-anomalous-reading always escalates"
    (let [proposal {:op :flag-anomalous-reading :effect :propose :confidence 0.95}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (:escalate? verdict))
      (is (not (:ok? verdict))))))

(deftest escalation-temperature-deviation
  (testing "flag-temperature-deviation always escalates"
    (let [proposal {:op :flag-temperature-deviation :effect :propose :confidence 0.95}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (:escalate? verdict))
      (is (not (:ok? verdict))))))

(deftest escalation-low-confidence
  (testing "low confidence triggers escalation"
    (let [proposal {:op :log-furnace-reading :effect :propose :confidence 0.5}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (:escalate? verdict))
      (is (not (:ok? verdict))))))

(deftest ok-proposal
  (testing "valid proposal with high confidence passes"
    (let [proposal {:op :log-furnace-reading :effect :propose :confidence 0.9}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (not (:escalate? verdict)))
      (is (:ok? verdict)))))
