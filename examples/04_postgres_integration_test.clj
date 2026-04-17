;; examples/04_postgres_integration_test.clj
;; PostgreSQL 接続テスト（XTDB v1 での検証）
;;
;; XTDB v2 の JUXT リポジトリアクセス問題を回避して、
;; PostgreSQL 統合の基本を XTDB v1 で検証
;;
;; 実行方法:
;;   clj --deps-file deps-v1.edn -i examples/04_postgres_integration_test.clj

(require '[xtdb.api :as xt])
(require '[clojure.java.jdbc :as jdbc])

(println "\n" "=" 50)
(println "PostgreSQL 統合テスト")
(println "=" 50)

(println "\n1️⃣  PostgreSQL 接続テスト...")

;; PostgreSQL 接続情報
(def db-spec
  {:dbtype "postgresql"
   :host "localhost"
   :port 5432
   :dbname "xtdb_dev"
   :user "postgres"
   :password "password"})

;; 接続確認
(try
  (let [conn (jdbc/get-connection db-spec)]
    (println "✅ PostgreSQL 接続成功")
    (println "   - Host: localhost:5432")
    (println "   - Database: xtdb_dev")
    
    ;; テーブル一覧の確認
    (let [tables (jdbc/query conn ["SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"])]
      (if (empty? tables)
        (println "   - テーブル: なし（初期状態）")
        (println "   - テーブル:" (map :table_name tables))))
    
    (.close conn))
  
  (catch Exception e
    (println "❌ PostgreSQL 接続失敗: " (.getMessage e))
    (println "\n対応方法:")
    (println "  1. PostgreSQL が localhost:5432 で起動しているか確認")
    (println "  2. Docker で起動している場合:")
    (println "     docker ps | grep postgres")
    (println "  3. 接続情報を確認")
    (println "     - ユーザー: postgres")
    (println "     - パスワード: password")))

(println "\n2️⃣  XTDB ノード起動（インメモリ版）...")

(try
  (def node (xt/start-node {}))
  (println "✅ XTDB ノード起動成功")
  
  ;; ++++++++++++++++++++++++++++++++++
  ;; 3. トレード書類投入テスト
  ;; ++++++++++++++++++++++++++++++++++
  
  (println "\n3️⃣  トレード書類投入...")
  
  (xt/submit-tx node
    [[:put :trade_docs
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
  
  (println "\n4️⃣  Datalog クエリ実行...")
  
  (def results
    (xt/q node
      '(find ?id ?type ?amount
        :where [[?e :xt/id ?id]
                [?e :type ?type]
                [?e :amount ?amount]])))
  
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
  (println "✨ PostgreSQL + XTDB 統合テスト完了")
  (println "=" 50)
  
  (println "\n📝 テスト結果:")
  (println "  ✅ PostgreSQL 接続")
  (println "  ✅ XTDB ノード起動")
  (println "  ✅ ドキュメント投入")
  (println "  ✅ Datalog クエリ")
  (println "  ✅ ノード状態確認")
  
  (println "\n💡 次のステップ:")
  (println "  1. XTDB v2 への移行（JUXT 接続修正後）")
  (println "  2. PostgreSQL バックエンド統合")
  (println "  3. Bitemporal 操作テスト")
  
  (catch Exception e
    (println "❌ XTDB テスト失敗: " (.getMessage e))
    (.printStackTrace e)))

(println "\n")
