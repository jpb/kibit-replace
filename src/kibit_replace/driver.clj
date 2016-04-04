(ns kibit-replace.driver
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [kibit.driver :refer [find-clojure-sources-in-dir]]
            [kibit.rules :refer [all-rules]]
            [kibit.check :as check]
            [kibit.reporters :as reporters]
            [rewrite-clj.zip :as z]
            [clojure.tools.cli :refer [cli]])
  (:import [java.io File]))

(def cli-specs [["-i" "--interactive"
                 "Run in interactive mode"
                 :flag true]])

(def ^:private default-args
  {:rules      all-rules
   :guard      check/unique-alt?
   :resolution :subform
   :init-ns    'user})

(def ^:private default-data-reader-binding
  (when (resolve '*default-data-reader-fn*)
    {(resolve '*default-data-reader-fn*) (fn [tag val] val)}))

(defn prompt
  "Create a yes/no prompt using the given message.
  From leiningen.ancient.console."
  [& msg]
  (let [msg (str (str/join msg) " [yes/no] ")]
    (locking *out*
      (loop [i 0]
        (when (= (mod i 4) 2)
          (println "*** please type in one of 'yes'/'y' or 'no'/'n' ***"))
        (print msg)
        (flush)
        (let [r (or (read-line) "")
              r (.toLowerCase ^String r)]
          (case r
            ("yes" "y") true
            ("no" "n")  false
            (recur (inc i))))))))

(defn prompt-check [{:keys [line expr alt]} filename]
  (prompt (with-out-str
            (println "Do you want to replace")
            (reporters/pprint-code expr)
            (println " with")
            (reporters/pprint-code alt)
            (printf "in %s:%s?" filename line))))

(defn find-and-replace-failed-checks
  "Recursivly call `check-fn` (a `check/check-reader`), replacing the form at
  `:line` and `:column` with the form in `:alt`, until no checks are returned."
  [bytes check-fn ignore-check-count interactive? filename]
  (let [checks (with-open [reader (io/reader bytes)]
                 (drop ignore-check-count (doall (check-fn reader))))]
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
          (if (or (and interactive? (prompt-check check filename))
                  true)
            (let [new-bytes (-> existing-form
                                (z/replace (vary-meta (:alt check)
                                                      dissoc
                                                      :line
                                                      :column))
                                z/root-string
                                .getBytes)]
              (recur new-bytes check-fn ignore-check-count interactive? filename))
            (recur bytes check-fn (inc ignore-check-count) interactive? filename))
          (throw (Exception. (str "Unable to find form for " check " in " code-forms))))))))

(defn check-file
  [source-file interactive? & kw-opts]
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
            replaced-bytes (find-and-replace-failed-checks source-bytes
                                                           check-fn
                                                           0
                                                           interactive?
                                                           source-file)]
        (String. replaced-bytes)))))

(defn run [source-paths rules & args]
  (let [[options file-args] (apply (partial cli args) cli-specs)
        source-files (mapcat (comp find-clojure-sources-in-dir io/file)
                             source-paths)]
    (doseq [file source-files]
      (spit file
            (check-file file (:interactive options) :rules (or rules all-rules))))))

(defn external-run
  [source-paths rules & args]
  (if (zero? (count (apply run source-paths rules args)))
    (System/exit 0)
    (System/exit 1)))
