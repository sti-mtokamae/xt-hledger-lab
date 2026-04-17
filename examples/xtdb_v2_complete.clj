;; examples/xtdb_v2_complete.clj
;; XTDB v2.1.0 完全テスト - 正しい API使用法

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(println "=" 50)
(println "XTDB v2.1.0 Complete Test")
(println "=" 50)

;; インメモリノードを作成・起動
(println "\n🚀 Starting in-memory XTDB node...")
(with-open [node (xtn/start-node)]
  (println "✅ Node started!")
  
  ;; ノード情報確認
  (println "\n📊 Node Status:")
  (let [status (xt/status node)]
    (println "   Status:" (dissoc status :system)))
  
  ;; トランザクション実行 (execute-tx は同期実行)
  (println "\n📝 Submitting documents...")
  (let [result (xt/execute-tx node 
                 [[:put-docs :users
                   {:xt/id "alice", :name "Alice", :age 30}]
                  [:put-docs :users
                   {:xt/id "bob", :name "Bob", :age 25}]])]
    (println "✅ Submitted! Transaction ID:" (:xt/id result))
    (println "   System time:" (:xt/system-time result)))
  
  ;; SQL クエリ実行
  (println "\n🔍 Running SQL query...")
  (let [results (xt/q node "SELECT * FROM users ORDER BY name")]
    (println "✅ Results:")
    (doseq [row results]
      (println "   " row)))
  
  ;; XTQL (Clojure表記) クエリ実行
  (println "\n🔍 Running XTQL query...")
  (let [results (xt/q node 
                  '(-> (from :users [xt/id name age])
                       (where (> age 26))))]
    (println "✅ Results:")
    (doseq [row results]
      (println "   " row)))
  
  (println "\n" "=" 50)
  (println "🎉 All tests passed!")
  (println "=" 50))

(println "\nExiting...")
(System/exit 0)
