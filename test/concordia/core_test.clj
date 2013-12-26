(ns concordia.core-test
  (:require [concordia.core     :as c]
            [concordia.schema   :as s]
            [org.httpkit.server :as http]
            [cheshire.core      :as json]
            [midje.sweet :refer :all])
  (:import java.io.File))

(defn handler [req]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (slurp "test/fixtures/data/reference/referenced.json")})

;; need this to test $ref type schema resolution
(future (http/run-server handler {:port 60000}))

(fact "sample schemas are valid"
  
  (let [root-dir (clojure.java.io/file "test/fixtures/definition")
        conforms (File. root-dir "conforms")
        valid    (File. root-dir "valid")
        invalid  (File. root-dir "invalid")]
    (doseq [valid-sample (apply conj (seq (.listFiles valid))
                                     (seq (.listFiles conforms)))]
      (println "testing valid schema" valid-sample)
      (let [plain  (slurp valid-sample)
            schema (json/parse-string plain true)]
        (->> (s/validate-schema schema)
             flatten
             (filter not-empty)) => empty?))))

(fact "sample schemas are invalid"
    
  (let [root-dir (clojure.java.io/file "test/fixtures/definition")
        invalid  (File. root-dir "invalid")]
    (doseq [invalid-sample (flatten (map #(seq (.listFiles %)) (.listFiles invalid)))]
      (println "testing invalid schema" invalid-sample)
      (let [plain  (slurp invalid-sample)
            schema (json/parse-string plain true)]
        (->> (s/validate-schema schema)
             flatten
             (filter not-empty)) => not-empty))))

(fact "sample documents validate against sample schemas"

  (let [root-dir (clojure.java.io/file "test/fixtures/data")]
    (doseq [file (.listFiles root-dir)]
      (let [schema  (File. file "definition.json")
            valid   (File. file "valid")
            invalid (File. file "invalid")]
        (doseq [valid-sample (.listFiles valid)]
          (println "testing valid document" valid-sample)
          (:valid? (c/validate valid-sample schema)) => true)
        (doseq [invalid-sample (.listFiles invalid)]
          (println "testing invalid document " invalid-sample)
          (:valid? (c/validate invalid-sample schema)) => false)))))