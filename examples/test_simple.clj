;; examples/test_simple.clj
;; シンプルなアーティファクト読み込み確認

(println "Starting XTDB v2.1.0 artifact test...")
(println "Loading com.xtdb/xtdb-core:2.1.0...")

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(println "✅ XTDB v2.1.0 loaded successfully!")

;; インメモリノード作成
(println "\nCreating in-memory node...")
(def node (xtn/start-node {}))
(println "✅ Node started!")

;; ノード情報
(def status (xt/status node))
(println "\n📊 Node Status:")
(println "   Version:" (:version status))
(println "   Index Version:" (:index-version status))

;; トランザクション送信テスト
(println "\n📝 Submitting test document...")
(xt/execute-tx node
               [[:put-docs :person
                 {:xt/id "alice"
                  :name "Alice"
                  :age 30}]])

(println "✅ Document submitted!")

;; クエリテスト
(println "\n🔍 Running query...")
(def results
  (xt/q node
        "SELECT name, age FROM person"))

(println "✅ Query results:" results)

(println "\n" "=" 50)
(println "🎉 All basic tests passed!")
(println "=" 50)

;; (System/exit 0)
