(defproject kibit-replace "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[jonase/kibit "0.1.2"]
                 [rewrite-clj "0.4.12"]]
  :profiles
  {:test
   {:resource-paths ["resources" "test-resources"]}}
  :eval-in-leiningen true)
