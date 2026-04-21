;; examples/xtdb_v2_api_exploration.clj
;; XTDB v2.1.0 API 探索

(println "XTDB v2 API Exploration")
(println "========================\n")

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(println "📚 Available XTDB API functions:")
(doseq [sym (sort (filter #(not (.startsWith (name %) "_"))
                          (map first (ns-publics 'xtdb.api))))]
  (println "  -" sym))

(println "\n📦 Available XTDB API functions (checked):")
(println "✅ Use: (require '[xtdb.api :as xt])")
(println "✅ Then: (xt/execute-tx node [...]) or (xt/q node [...])")

(println "\n🧪 実際のテスト:")
(try
  (let [node (xtn/start-node {})]
    (println "✅ ノード起動成功")
    
    ;; ドキュメント投入
    (xt/execute-tx node
      [[:put-docs :test_docs
        {:xt/id "test-1"
         :value "Hello XTDB v2"}]])
    (println "✅ ドキュメント投入成功")
    
    ;; クエリ実行
    (let [result (xt/q node "SELECT * FROM test_docs")]
      (println "✅ クエリ結果:" result)))
  
  (catch Exception e
    (println "❌ エラー:" (.getMessage e))
    (.printStackTrace e)))
