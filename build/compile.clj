(ns compile
  (:require [clojure.java.io :as io]
            cljfx.fx)
  (:import [javafx.application Platform]))

(defn -main []
  (.mkdirs (io/file "classes"))
  (compile 'hn.core)
  (binding [*compile-files* true]
    (run! deref (vals cljfx.fx/keyword->lifecycle-delay)))
  (Platform/exit))