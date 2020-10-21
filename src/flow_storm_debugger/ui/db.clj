(ns flow-storm-debugger.ui.db
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]))

(defonce *state
  (atom (fx/create-context {:flows {}
                            :selected-flow-id nil                            
                            :connected-clients 0}
                           cache/lru-cache-factory)))
