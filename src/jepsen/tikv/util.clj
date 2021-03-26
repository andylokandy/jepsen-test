(ns jepsen.tikv.util
  "Utilities")

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))
