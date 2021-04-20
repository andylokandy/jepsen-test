(ns jepsen.tikv.util
  "Utilities")

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defn num-suffix
  [node]
  (let [len (count node)]
    (parse-long (subs node (- len 1) len))))
