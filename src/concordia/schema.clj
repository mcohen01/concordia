(ns concordia.schema
  (:require [cheshire.core :as json]))

(def types #{:boolean :number :string :object :array :unnamed-type})

(defn schema-at* [url]
  (json/parse-string (slurp url) true))

(def schema-at (memoize schema-at*))

(defn assert-doc-string
  [schema-node]
  (if (and (contains? schema-node :doc)
           (not (string? (:doc schema-node))))
      (str "'doc' attribute must be string: " (:doc schema-node))))

(defn assert-type-decl
  [schema-node]
  (if-not (some types [(keyword (:type schema-node))])
    [(str "Invalid type declaration:" schema-node)]))

(defn assert-attr
  [schema-node attr & [not-null]]
  (if-not (or (contains? schema-node attr)
              (and (= :type attr) (contains? schema-node :$ref)))
    (str "Missing " (name attr) " attribute")
    (if (and not-null 
             (nil? (attr schema-node)))
        (str "Invalid null value for attribute: " (name attr)))))

(defn assert-optional-boolean
  [schema-node]
  (if-let [opt (:optional schema-node)]
    (if-not (or (true? opt) (false? opt))
        [(str "Invalid 'optional' attribute: " opt)])))

(defn assert-type-and-name
  [schema-node]
  (conj []
    (assert-attr schema-node :type)
    (assert-attr schema-node :name true)
    (assert-optional-boolean schema-node)))

(defn assert-array-attrs
  [schema-node]
  (if-not (or (contains? schema-node :constLength)
              (contains? schema-node :constType))
    ["Invalid schema: constLength or constType 
      attribute required for array type"]))

(defn assert-fields-array
  [schema-node]
  (if (contains? schema-node :fields)
      (if-not (sequential? (:fields schema-node))
              (str "'fields' attribute must be array of datatypes"))))

(defn assert-type-one-of
  [schema-node & type-names]
  (let [req-type (:type schema-node)]
    (if (not-any? (apply hash-set type-names)
                  (map keyword [req-type]))
        (str "Invalid schema: required type " (name req-type)
             " but found " (:type schema-node)))))

(defn assert-no-duplicate-fields
  [schema-node]
  (let [fields (:fields schema-node)]
    (if (sequential? fields)
        (if-not (= (count fields)
                   (count (frequencies fields)))
          [(str "Duplicate names found in schema definition: "
                schema-node)]))))

(defmulti validate-schema
  (fn [schema-node]
    (if (or (not (nil? (assert-type-decl schema-node)))
            (and (sequential? schema-node)
                 (empty? schema-node)))
      :default)
    (-> schema-node :type keyword)))

(defn validate-subschema
  [errors element]
  (if (or (not (map? element))
          (empty? element))
    [(str "Invalid schema node definition: " element)]
    (if (empty? (filter not-empty (vals element)))
        [(str "Schema node must contain fields: " element)]
        (conj errors
              (validate-schema
                (assoc element :type :unnamed-type))))))

(defn validate-const-length
  [schema-node]
  (when-let [types (:constLength schema-node)]
    (if (or (not (sequential? types))
            (empty? types))
      [(str "Invalid constLength attribute: " types)]
      (reduce validate-subschema [] types))))

(defn validate-const-type
  [schema-node]
  (if (contains? schema-node :constType)
    (let [type (:constType schema-node)]
      (if (or (not (map? type))
              (nil? type)
              (empty? type)
              (and (nil? (:type type))
                   (nil? (:$ref type))))
          [(str "Invalid constType attribute: " type)]
          [(validate-schema (assoc type :type :unnamed-type))]))))

(defmethod validate-schema :object
  [schema-node]
  (apply conj []
    (assert-attr schema-node :type)
    (assert-attr schema-node :fields)
    (assert-optional-boolean schema-node)
    (assert-fields-array schema-node)
    (assert-doc-string schema-node)
    (assert-no-duplicate-fields schema-node)
    (if (sequential? (:fields schema-node))
        (reduce
          (fn [m e]
            (conj m (validate-schema e)))
          []
          (:fields schema-node)))))

(defmethod validate-schema :array
  [schema-node]
  (apply conj []
    (assert-attr schema-node :type)
    (assert-array-attrs schema-node)
    (validate-const-length schema-node)
    (validate-const-type schema-node)))

(defmethod validate-schema :boolean
  [schema-node]
  (assert-type-and-name schema-node))

(defmethod validate-schema :number
  [schema-node]
  (assert-type-and-name schema-node))

(defmethod validate-schema :string
  [schema-node]
  (assert-type-and-name schema-node))

(defmethod validate-schema :unnamed-type
  [schema-node]
  (if (or (= "object" (:type schema-node))
          (= "array" (:type schema-node)))
      (validate-schema (assoc schema-node :name "_")))
  (if-not (map? schema-node)
    [(str "Invalid nested type definition: " schema-node)]
    (if (contains? schema-node :$ref)
        (validate-schema (assoc (schema-at (:$ref schema-node))
                                :type :object))
        (if-not (contains? schema-node :type)
          ["Empty type declaration not allowed"]
          (assert-type-decl schema-node)))))

(defmethod validate-schema :default
  [schema-node]
  [(str "Invalid schema: " schema-node)])