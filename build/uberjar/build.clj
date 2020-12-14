(ns uberjar.build
  (:require [hf.depstar.uberjar])
  (:import (javafx.application Platform)))

;; see https://github.com/cljfx/cljfx#aot-compilation-is-complicated

(defn run
  [& args]
  (try
    (apply hf.depstar.uberjar/run args)
    (finally
      (Platform/exit))))