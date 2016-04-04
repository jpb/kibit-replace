(ns kibit-replace.driver-test
  (:require [kibit-replace.driver :as driver]
            [clojure.java.io :as io]
            [kibit.rules :as rules]
            [clojure.test :refer :all]))

(deftest check-file-test
  (is (= (slurp (io/resource "test_case.clj.result"))
         (driver/check-file (io/resource "test_case.clj")
                            false
                            :rules rules/all-rules))))
