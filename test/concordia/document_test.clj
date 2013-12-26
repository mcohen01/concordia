(ns concordia.document-test
  (:require [concordia.core :as c]
            [concordia.document :refer :all]
            [midje.sweet :refer :all]))

(fact "document assertions"

  (filter not-empty
          (flatten
            (validate-data {:first_name "Michael"}
                           {:type "object"
                            :fields [{:name "first_name"
                                      :type "string"}]}))) => empty?

  (first
    (filter not-empty
            (validate-data {:first_name 42}
                           {:type "object"
                            :fields [{:name "first_name"
                                      :type "string"}]}))) => 
      ["Invalid data for required string type: 42"]

  (filter not-empty
          (flatten
            (validate-data {:first_name 42
                          :last_name "Cohen"
                          :gender "male"}
                         {:type "object"
                          :fields [{:name "first_name"
                                    :type "string"}
                                   {:name "last_name"
                                    :type "string"}
                                   {:name "gender"
                                    :type "string"}]}))) => 
      ["Invalid data for required string type: 42"]

  (filter not-empty
          (flatten
            (c/validate-node {:first_name 42
                              :last_name "Cohen"
                              :gender "male"}
                             {:type "object"
                              :fields [{:name "first_name"
                                        :type "string"}
                                       {:name "last_name"
                                        :type "string"}
                                       {:name "gender"
                                        :type "string"}]}))) => 
      ["Invalid data for required string type: 42"])