(ns com.palletops.maven-resolver-test
  (:require
   [clojure.java.io :refer [file]]
   [clojure.test :refer :all]
   [com.palletops.maven-resolver :refer :all]))

(def local-repo
  (.getAbsolutePath
   (file (System/getProperty "user.dir") "target" "local-repo")))

(def repositories
  {"central" {:url "http://repo1.maven.org/maven2/" :snapshots false}})

(deftest resolve-coordinates-test
  (let [resolved (resolve-coordinates
                  '[[org.clojure/clojure "1.5.1"]]
                  {:local-repo local-repo :repositories repositories})]
    (is (= 1 (count resolved)))
    (is (.exists (file (first resolved))))
    (is (.startsWith (.getAbsolutePath (file (first resolved))) local-repo))))
