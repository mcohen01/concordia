(ns concordia.core
  (:require [concordia.document :as d]
            [concordia.schema :as s]
            [cheshire.core :as json])
  (:import java.io.File))

(defn validate-node
  [json-node schema-node]
  (filter 
    not-empty
    (flatten
      (apply conj [] (s/validate-schema schema-node)
                     (d/validate-data json-node schema-node)))))

(defn validate
  [document schema]
  (let [errors (try
                 (validate-node
                   (json/parse-string (slurp document) true)
                   (json/parse-string (slurp schema) true))
                 (catch Exception e
                   (.getMessage e)))]
    {:valid? (empty? errors)
     :errors errors}))