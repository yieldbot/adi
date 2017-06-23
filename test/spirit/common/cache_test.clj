(ns spirit.common.cache-test
  (:use hara.test)
  (:require [spirit.common.cache :refer :all])
  (:refer-clojure :exclude [get set keys count]))