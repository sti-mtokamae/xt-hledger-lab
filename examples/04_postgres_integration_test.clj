;; examples/04_postgres_integration_test.clj
;; XTDB v2 統合テスト
;;
;; インメモリ XTDB ノードでの基本的な操作をテスト：
;; - ノード起動
;; - ドキュメント投入
;; - SQL クエリ実行
;; - ノード状態確認

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(println "\n" "=" 50)
(println "XTDB v2 統合テスト")
(println "=" 50)

(println "\n1️⃣  XTDB ノード起動（インメモリ版）...")

(try
  (def node (xtn/start-node {}))
  (println "✅ XTDB ノード起動成功")
  
  ;; ++++++++++++++++++++++++++++++++++
  ;; 3. トレード書類投入テスト
  ;; ++++++++++++++++++++++++++++++++++
  
  (println "\n3️⃣  トレード書類投入...")
  
  (xt/execute-tx node
    [[:put-docs :trade_docs
      {:xt/id "TEST-INV-001"
       :type "Invoice"
       :supplier "Test Supplier Corp."
       :amount 10000.00
       :currency "USD"
       :status "Draft"
       :created_at (java.util.Date.)}]])
  
  (println "✅ インボイス投入")
  
  ;; ++++++++++++++++++++++++++++++++++
  ;; 4. Datalog クエリテスト
  ;; ++++++++++++++++++++++++++++++++++
  
  (println "\n4️⃣  SQL クエリ実行...")
  
  (def results
    (xt/q node
      "SELECT _id, type, amount FROM trade_docs"))
  
  (println "✅ クエリ結果:" results)
  
  ;; ++++++++++++++++++++++++++++++++++
  ;; 5. ステータス確認
  ;; ++++++++++++++++++++++++++++++++++
  
  (println "\n5️⃣  XTDB ノード状態確認...")
  (let [status (xt/status node)]
    (println "✅ ノード情報:")
    (println "   - Version:" (:version status))
    (println "   - Index Version:" (:index-version status)))
  
  (println "\n" "=" 50)
  (println "✨ XTDB v2 統合テスト完了")
  (println "=" 50)
  
  (println "\n📝 テスト結果:")
  (println "  ✅ XTDB ノード起動")
  (println "  ✅ ドキュメント投入")
  (println "  ✅ SQL クエリ")
  (println "  ✅ ノード状態確認")
  
  (println "\n💡 次のステップ:")
  (println "  1. PostgreSQL バックエンド統合")
  (println "  2. Bitemporal 操作テスト")
  (println "  3. 複雑なクエリの実装")
  
  (catch Exception e
    (println "❌ XTDB テスト失敗: " (.getMessage e))
    (.printStackTrace e)))

(println "\n")
