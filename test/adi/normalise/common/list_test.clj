(ns adi.normalise.common.list-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.common.list :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))