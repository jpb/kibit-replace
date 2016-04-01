(ns test-case)

;; A comment

(defn bar []
  (if false
    ;; Another comment
    (do 2
        1)))


(defn baz
  "Doc string"
  [z b]
  (-> (if false (do b)) str)
  (if z 1 2))
