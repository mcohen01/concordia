(ns concordia.schema-test
  (:require [concordia.schema :refer :all]
            [midje.sweet :refer :all]))

(fact "schema assertions"

  (assert-attr {:type "object"} :type) => nil
  (assert-attr {:name "object"} :type) => "Missing type attribute"

  (assert-type-one-of {:type "object"} :object :array) => nil
  (assert-type-one-of {:type "boolean"} :object :array) => string?

  (filter not-empty
          (validate-schema {:type "string" :name "anything"})) => empty?

  (filter not-empty
          (validate-schema {:type "number" :name "anything"})) => empty?

  (filter not-empty
          (validate-schema {:type "boolean" :name "anything"})) => empty?

  (filter not-empty
          (validate-schema {:type "object" :fields []})) => empty?

  (validate-schema {:type "object" :fields {}}) => not-empty
  
  (filter not-empty
          (flatten 
            (validate-schema {:type "object"
                              :fields [
                                {:type "string"
                                 :name "nexted"}
                                {:type "boolean"
                                 :name "another-nested"}]}))) => empty?

  (validate-schema {:type "object"
                    :fields [
                      {:type "string"
                       :name "nexted"}
                      {:type "boolean"
                       :name "another-nested"}
                      {:type "object"
                       :fields [
                         {:type "FOOBARFOOBARFOOBAR"
                          :name "nexted"}]}]}) => not-empty)