# concordia

JSON schema validation

## Installation

Leiningen coordinates:
```clj
[concordia "0.1.0-SNAPSHOT"]
```

## Usage

```clj
(ns com.example
  (:require [concordia.core :as c]))

(c/validate "/path/to/some/document.json"
            "http://www.acme.com/schema.json")
```


## Testing

`git clone https://github.com/mcohen01/concordia.git`

`lein midje`


### License

Copyright (C) 2013 Michael Cohen

Distributed under the Eclipse Public License, the same as Clojure.