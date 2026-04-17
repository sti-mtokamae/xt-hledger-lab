;; examples/xtdb_v2_api_exploration.clj
;; XTDB v2.1.0 API 探索

(println "XTDB v2 API Exploration")
(println "========================\n")

(require '[xtdb.api :as xt])

(println "📚 Available XTDB API functions:")
(doseq [sym (sort (filter #(not (.startsWith (name %) "_"))
                          (map first (ns-publics 'xtdb.api))))]
  (println "  -" sym))

(println "\n📦 Available XTDB API functions (checked):")
(println "✅ Use: (require '[xtdb.api :as xt])")
(println "✅ Then: (xt/execute-tx node [...]) or (xt/q node [...])")

(System/exit 0)
