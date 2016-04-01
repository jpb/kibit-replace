(ns kibit-replace.driver
  (:require [clojure.java.io :as io]
            [kibit.driver :refer [find-clojure-sources-in-dir]]
            [kibit.rules :refer [all-rules]]
            [kibit.check :as check]
            [kibit.reporters :as reporters]
            [rewrite-clj.zip :as z]
            [clojure.tools.cli :refer [cli]])
  (:import [java.io File]))

(def cli-specs [])

(def ^:private default-args
  {:rules      all-rules
   :guard      check/unique-alt?
   :resolution :subform
   :init-ns    'user})

(def ^:private default-data-reader-binding
  (when (resolve '*default-data-reader-fn*)
    {(resolve '*default-data-reader-fn*) (fn [tag val] val)}))

(defn find-and-replace-failed-checks
  "Recursivly call `check-fn` (a `check/check-reader`), replacing the form at
  `:line` and `:column` with the form in `:alt`, until no checks are returned."
  [bytes check-fn]
  (let [checks (with-open [reader (io/reader bytes)]
                 (check-fn reader))]
    (if (empty? checks)
      bytes
      ;; else
      (let [check (first checks)
            code-forms (z/of-string (slurp bytes))
            existing-form (z/find-depth-first code-forms
                                              (fn -find-node [form]
                                                (= (select-keys (meta (z/node form))
                                                                [:row :col])
                                                   {:row (:line check)
                                                    :col (:column check)})))]
        (if existing-form
          (let [new-bytes (-> existing-form
                              (z/replace (:alt check))
                              z/root-string
                              .getBytes)]
            (recur new-bytes check-fn))
          (throw (Exception. (str "Unable to find form for " check " in " code-forms))))))))

(defn check-file
  [source-file & kw-opts]
  (let [{:keys [rules guard resolution init-ns]
         :as options}
        (merge default-args
               (apply hash-map kw-opts))]
    (with-bindings default-data-reader-binding
      (let [check-fn (fn -check-reader [reader]
                       (check/check-reader reader
                                           :rules rules
                                           :guard guard
                                           :resolution resolution
                                           :init-ns init-ns))
            source-bytes (.getBytes (slurp source-file))
            replaced-bytes (find-and-replace-failed-checks source-bytes check-fn)]
        (String. replaced-bytes)))))

(defn run [source-paths rules & args]
  (let [[options file-args usage-text] (apply (partial cli args) cli-specs)
        source-files (mapcat (comp find-clojure-sources-in-dir io/file)
                             source-paths)]
    (doseq [file source-files]
      (println (check-file file :rules (or rules all-rules))))))
