(ns concordia.document
  (:require [concordia.schema :as s]))

(defn- error-msg
  [data-type json-node]
  (str "Invalid data for required "
       (name data-type)
       " type: " json-node))

(defn- assert-array
  [json-node schema-node]
  (if-not (sequential? json-node)
    [(str "Element[" (-> schema-node :name keyword)
          "] is not an 'array' type")]))

(defn ref-schema [schema-node]
  (if (contains? schema-node :$ref)
      (-> schema-node :$ref s/schema-at)
      schema-node))

(defn resolve-field-schema
  [fields field]
  (conj fields 
        (if (and (contains? field :$ref)
                 (not (contains? field :name)))
            (:fields (ref-schema field))
            field)))

(defn extended-schema-fields
  [schema-node]
  (->> (:fields schema-node)
       (reduce resolve-field-schema [])
       flatten))
  
(defmulti validate-data
  (fn [json-node schema-node]
    (-> schema-node :type keyword)))

(defn validate-object-field
  [json-node]
  (fn [errors element]
    (let [schema (ref-schema element)
          field  (keyword (or (:name element) (:name schema)))
          data   (json-node field)]
      (conj errors
            (if (and (nil? data)
                     (not (true? (:optional element))))
                (str "Missing field: " (if field (name field)))
                (validate-data data schema))))))

(defmethod validate-data :object
  [json-node schema-node]
  (conj []
    (if-not (map? json-node)
      [(str "Data is not of required object type: " json-node)])
    (reduce
      (validate-object-field json-node)
      []
      (extended-schema-fields schema-node))))

(defn validate-const-type
  [json-node schema-node]
  (if (contains? schema-node :constType)
      (reduce
        (fn [m e]
          (->> schema-node
               :constType
               ref-schema
               (validate-data e)
               (conj m)))
        []
        json-node)))

(defn validate-const-length
  [json-node schema-node]
  (let [counter (atom 0)
        types   (:constLength schema-node)]
    (if (contains? schema-node :constLength)
        (if (not= (count types)
                  (count json-node))
            [(str "More elements than allowed: " json-node)]
            (reduce
              (fn [m e]
                (swap! counter inc)
                (->> (dec @counter)
                     (nth types)
                     ref-schema
                     (validate-data e)
                     (conj m)))
              []
              json-node)))))

(defmethod validate-data :array
  [json-node schema-node]
  (apply conj []
    (assert-array json-node schema-node)
    (validate-const-type json-node schema-node)
    (validate-const-length json-node schema-node)))

(defmacro if-not-optional
  [json-node schema-node & body]
  `(if-not (and (nil? ~json-node)
                (true? (:optional ~schema-node)))
     (do ~@body)))

(defmethod validate-data :boolean
  [json-node schema-node]
  (if-not-optional json-node schema-node
    (if-not (or (true? json-node)
                (false? json-node))
        (error-msg :boolean json-node))))

(defmethod validate-data :number
  [json-node schema-node]
  (if-not-optional json-node schema-node
    (if-not (number? json-node)
        (error-msg :number json-node))))

(defmethod validate-data :string
  [json-node schema-node]
  (if-not-optional json-node schema-node
    (if-not (string? json-node)
      (error-msg :string json-node))))