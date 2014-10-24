(ns adi.normalise.common.vector-test
  (:use midje.sweet)
  (:require [adi.normalise.base :as normalise]
            [adi.normalise.common.vector :refer :all]
            [adi.schema :as schema]
            [adi.test.examples :as examples]
            [adi.test.checkers :refer [raises-issue]]))