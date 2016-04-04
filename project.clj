(defproject jpb/kibit-replace "0.1.0"
  :description "A Leiningen plugin to automatically apply kibit suggestions"
  :url "https://github.com/jpb/kibit-replace"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[jonase/kibit "0.1.2"]
                 [rewrite-clj "0.4.12"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :profiles
  {:test
   {:resource-paths ["resources" "test-resources"]}}
  :eval-in-leiningen true)
