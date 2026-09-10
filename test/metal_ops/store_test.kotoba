(ns metal-ops.store-test
  (:require [clojure.test :refer [deftest is testing]]
            [metal-ops.store :as store]))

(deftest mem-store-plant-lookup
  (testing "plants can be registered and looked up"
    (let [s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          p (store/plant s :plant-1)]
      (is (= "Plant 1" (:name p)))
      (is (:verified? p)))))

(deftest mem-store-commit-record
  (testing "records can be committed and retrieved"
    (let [s (store/mem-store {:plant-1 {:name "Plant 1" :verified? true}})
          record {:plant-id :plant-1 :op :log-furnace-reading :payload {:temp 1200}}]
      (store/commit-record! s record)
      (is (= 1 (count (store/records s))))
      (is (= record (first (store/records s)))))))

(deftest mem-store-append-ledger
  (testing "ledger entries can be appended and retrieved"
    (let [s (store/mem-store {})
          entry {:disposition :commit :record {:plant-id :p1 :op :log}}]
      (store/append-ledger! s entry)
      (is (= 1 (count (store/ledger s))))
      (is (= entry (first (store/ledger s)))))))

(deftest commit-record-requires-plant-id
  (testing "commit-record! throws if plant-id is missing"
    (let [s (store/mem-store {})]
      (is (thrown? #?(:clj Exception :cljs js/Error)
                   (store/commit-record! s {:op :log}))))))
