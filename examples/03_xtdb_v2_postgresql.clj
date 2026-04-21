;; examples/03_xtdb_v2_postgresql.clj
;; XTDB v2 + PostgreSQL バックエンド
;;
;; XTDB v2 の特徴：PostgreSQL をネイティブに使用できる
;; これにより、既存の PostgreSQL インフラと統合可能
;; クエリは SQL を使用（Datalog は legacy）
;;
;; 前提:
;;   PostgreSQL が localhost:5432 で起動していること
;;   (docker run --rm -e POSTGRES_PASSWORD=password -p 5432:5432 postgres:16)
;;
;; 実行方法:
;;   guix shell -m manifest.scm
;;   clj -i examples/03_xtdb_v2_postgresql.clj

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(println "\n==========================================")
(println "✨ XTDB v2 + PostgreSQL チュートリアル")
(println "==========================================\n")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 1. PostgreSQL接続設定
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "1️⃣  PostgreSQL 接続設定...")

;; XTDB v2 での PostgreSQL バックエンド設定
;; :topology キーで指定（v2の新形式）
(def pg-config
  {:xtdb/tx-log   {:xtdb.sql/connection-pool
                   {:xtdb.sql/dbtype "postgresql"
                    :xtdb.sql/dbname "xtdb_dev"
                    :xtdb.sql/host "localhost"
                    :xtdb.sql/port 5432
                    :xtdb.sql/user "postgres"
                    :xtdb.sql/password "password"}}
   :xtdb/document-store {:xtdb.sql/connection-pool
                         {:xtdb.sql/dbtype "postgresql"
                          :xtdb.sql/dbname "xtdb_dev"
                          :xtdb.sql/host "localhost"
                          :xtdb.sql/port 5432
                          :xtdb.sql/user "postgres"
                          :xtdb.sql/password "password"}}})

(println "✅ PostgreSQL 設定定義完了")
(println "   ├─ Host: localhost:5432")
(println "   ├─ Database: xtdb_dev")
(println "   └─ User: postgres\n")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; サンプルデータ定義
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(def sample-documents
  "チュートリアル用のサンプルドキュメント"
  [{:xt/id "INV-20240501-001"
    :type "Invoice"
    :supplier "ABC Trading Corp."
    :po_number "PO-2024-1001"
    :amount 50000.00
    :currency "USD"
    :status "Draft"
    :ship_destination "Tokyo, Japan"
    :issue_date #inst "2024-05-01T00:00:00Z"
    :created_at #inst "2024-05-01T10:30:00Z"}

   {:xt/id "PKG-20240502-001"
    :type "PackingList"
    :invoice_ref "INV-20240501-001"
    :shipped_date #inst "2024-05-02T00:00:00Z"
    :line_items [{:sku "WIDGET-A" :qty 500 :unit_price 50.0}
                 {:sku "GADGET-B" :qty 200 :unit_price 75.0}]}

   {:xt/id "BL-20240503-001"
    :type "BillOfLading"
    :invoice_ref "INV-20240501-001"
    :vessel "MV Trade Bridge"
    :container_numbers ["CONT-001" "CONT-002"]
    :eta #inst "2024-05-15T00:00:00Z"
    :port_of_lading "Port of Shanghai"
    :port_of_discharge "Port of Tokyo"}])

(def sample-queries
  "チュートリアル用のクエリ"
  [{:section 4
    :title "投入されたドキュメント"
    :sql "SELECT _id, type, amount, status FROM trade_documents"}

   {:section 6
    :title "修正後のインボイス"
    :sql "SELECT _id, amount, status FROM trade_documents WHERE _id = 'INV-20240501-001'"}])

(def corrections
  "修正適用用のドキュメント"
  [{:xt/id "INV-20240501-001"
    :type "Invoice"
    :supplier "ABC Trading Corp."
    :po_number "PO-2024-1001"
    :amount 52000.00              ; 修正後
    :currency "USD"
    :status "Draft"
    :ship_destination "Tokyo, Japan"
    :issue_date #inst "2024-05-01T00:00:00Z"
    :created_at #inst "2024-05-01T10:30:00Z"
    :correction_note "金額修正: 50000 -> 52000 USD"}])

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; チュートリアル操作関数
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(defn- run-postgresql-tutorial [node]
  ;; セクション 3: ドキュメント投入
  (println "3️⃣  トレードドキュメント投入（PostgreSQL に永続化）...")
  (doseq [{:keys [type] :as doc} sample-documents]
    (xt/execute-tx node [[:put-docs :trade_documents doc]])
    (let [type-ja (case type
                    "Invoice" "インボイス"
                    "PackingList" "パッキングリスト"
                    "BillOfLading" "B/L"
                    type)]
      (println (str "  ✅ " type-ja " 投入"))))
  (println "")

  ;; セクション 4: クエリ実行
  (println "4️⃣  SQL クエリ実行...")
  (let [query (first sample-queries)]
    (println (:title query))
    (doseq [doc (xt/q node (:sql query))]
      (println "  " doc)))
  (println "")

  ;; セクション 5: 修正適用
  (println "5️⃣  Bitemporal 操作: 金額修正...")
  (println "  シナリオ: 5/1 発行のINVで誤った金額（50000 -> 52000）\n")
  (doseq [doc corrections]
    (xt/execute-tx node [[:put-docs :trade_documents doc]]))
  (println "✅ 遡及修正完了（PostgreSQL に永続化）\n")

  ;; セクション 6: 修正結果確認
  (println "6️⃣  最新の INV 情報を確認...")
  (let [query (second sample-queries)]
    (println (:title query))
    (doseq [doc (xt/q node (:sql query))]
      (println "  " doc)))

  (println "\n" "=" 45)
  (println "✨ XTDB v2 + PostgreSQL チュートリアル完了")
  (println "=" 45)

  (println "\n📊 PostgreSQL バックエンド のメリット:")
  (println "  • トランザクションログの永続化")
  (println "  • 複数ノード間の分散トランザクション")
  (println "  • 既存 PostgreSQL インフラとの統合")
  (println "  • より強力な Bitemporal 機能")

  (println "\n💾 データ永続化:")
  (println "  ├─ PostgreSQL テーブル: xtdb_tx_log（トランザクションログ）")
  (println "  ├─ PostgreSQL テーブル: xtdb_docs（ドキュメント）")
  (println "  └─ XTDB インデックス: メモリキャッシュされた参照")

  (println "\n次のステップ:")
  (println "  1. src/xt_hledger/postgres.clj で Bitemporal 会計ロジック実装")
  (println "  2. hledger へのマッピング（PostgreSQL -> Journal形式）")
  (println "  3. 複式簿記の整合性チェック"))

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 2. XTDB v2 ノード起動（PostgreSQL バックエンド）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "2️⃣  XTDB v2 ノード起動中...")

;; ※ PostgreSQL に接続できない場合、例外が発生します
;; その場合は、Docker を使用して PostgreSQL を起動してください:
;;   docker run --rm -e POSTGRES_PASSWORD=password -p 5432:5432 postgres:16

(try
  (def node (xtn/start-node pg-config))
  (catch Exception e
    (println "❌ PostgreSQL 接続エラー:")
    (println "   " (.getMessage e))
    (println "\n修復方法:")
    (println "   1. PostgreSQL が起動しているか確認:")
    (println "      docker run --rm -e POSTGRES_PASSWORD=password -p 5432:5432 postgres:16")
    (println "   2. xtdb_dev データベースを作成:")
    (println "      psql -U postgres -c \"CREATE DATABASE xtdb_dev;\"")
    (println "   3. 再度チュートリアルを実行")))

;; ノード起動に成功した場合のみ、以降の操作を実行
(when (resolve 'node)
  (println "✅ XTDB v2 ノード（PostgreSQL バックエンド）起動成功\n")
  (run-postgresql-tutorial node))
