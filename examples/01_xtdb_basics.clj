;; examples/01_xtdb_basics.clj
;; XTDB v2.1.0 基本操作のイントロダクション
;;
;; 実行方法:
;;   guix shell -m manifest.scm
;;   clj -A:xtdb -i examples/01_xtdb_basics.clj
;;
;; または REPL から:
;;   (load-file "examples/01_xtdb_basics.clj")

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; リソース管理: with-open でノードをラップ（推奨）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(with-open [node (xtn/start-node {})]
  
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 1. ノード（DBエンジン）の起動
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n=== XTDB v2.1.0 基本操作ガイド ===\n")

  (println "1️⃣  ノードを起動中...")
  (println "✅ ノード起動完了")

  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 2. データの投入（Put）
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n2️⃣  ドキュメント投入（貿易書類の例）...")

  ;; v2.1.0 API: [[:put-docs :table {...}]] ベクトル形式
  (xt/execute-tx node
    [[:put-docs :documents
      {:xt/id "INV-001"
       :type "Invoice"
       :party_to "ABC Trading Ltd."
       :amount 1500.00
       :currency "USD"
       :status "Draft"
       :issue_date #inst "2024-05-01T00:00:00Z"}]])

  (println "✅ インボイス INV-001 を投入")

  ;; 複数のドキュメントを一度に投入
  (xt/execute-tx node
    [[:put-docs :documents
      {:xt/id "PKG-001"
       :type "PackingList"
       :invoice_ref "INV-001"
       :items [{:description "Widget A" :qty 100}
               {:description "Gadget B" :qty 50}]
       :status "Ready"}]
     [:put-docs :documents
      {:xt/id "BL-001"
       :type "BillOfLading"
       :invoice_ref "INV-001"
       :vessel "MV Trade Ship"
       :eta #inst "2024-05-15T00:00:00Z"}]])

  (println "✅ パッキングリスト PKG-001 と B/L BL-001 を投入")

  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 3. 基本的なクエリ（SQL）
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n3️⃣  クエリ実行: すべてのドキュメントを取得...")

  ;; SQL クエリ（SELECT）
  (let [all-docs (xt/q node "SELECT * FROM documents")]
    (println "クエリ結果:")
    (doseq [doc all-docs]
      (println "  " doc)))

  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 4. グラフクエリ（SQL で外部キー検索）
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n4️⃣  グラフクエリ（SQL）...")
  (println "  -> INV-001 に関連するすべてのドキュメント")

  (let [related-docs (xt/q node "SELECT * FROM documents WHERE _id IN ('PKG-001', 'BL-001')")]
    (println "関連ドキュメント:")
    (doseq [doc related-docs]
      (println "  " doc)))

  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 5. データの更新（PUT で同じ ID を上書き）
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n5️⃣  ステータスを Confirmed に更新...")

  (xt/execute-tx node
    [[:put-docs :documents
      {:xt/id "INV-001"
       :type "Invoice"
       :party_to "ABC Trading Ltd."
       :amount 1500.00
       :currency "USD"
       :status "Confirmed"
       :issue_date #inst "2024-05-01T00:00:00Z"
       :confirmed_date #inst "2024-05-10T00:00:00Z"}]])

  (println "✅ INV-001 のステータスを Confirmed に更新")

  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 6. 金額修正（同じ ID でデータを更新）
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n6️⃣  金額修正（1500 USD -> 1600 USD）...")

  (xt/execute-tx node
    [[:put-docs :documents
      {:xt/id "INV-001"
       :type "Invoice"
       :party_to "ABC Trading Ltd."
       :amount 1600.00
       :currency "USD"
       :status "Confirmed"
       :issue_date #inst "2024-05-01T00:00:00Z"
       :confirmed_date #inst "2024-05-10T00:00:00Z"
       :correction_note "金額を修正しました（1500 -> 1600）"}]])

  (println "✅ 金額修正完了")

  ;; 修正後のデータを確認
  (let [updated-amount (xt/q node "SELECT xt_id, amount FROM documents WHERE _id = 'INV-001'")]
    (println "修正後の金額:")
    (doseq [row updated-amount]
      (println "  " row)))

  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
  ;; 7. 最終サマリ
  ;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

  (println "\n" "=" 50)
  (println "✨ XTDB v2.1.0 チュートリアル完了")
  (println "=" 50)

  (println "\n📚 XTDB v2 のポイント:")
  (println "  • ノード起動: (xtn/start-node {})")
  (println "  • ドキュメント投入: (xt/execute-tx node [[:put-docs :table {...}]])")
  (println "  • データ更新: 同じ ID で PUT すると自動的に上書き")
  (println "  • SQL クエリ: (xt/q node \"SELECT ...\")")
  (println "  • リソース管理: with-open でノードの自動クローズ")

  (println "\n次のステップ:")
  (println "  1. src/xt_hledger/core.clj で hledger 統合を実装")
  (println "  2. Phase 1: ドキュメント -> hledger ジャーナル変換")
  (println "  3. Phase 2: PostgreSQL バックエンド導入")
  
  nil)  ; with-open ブロック終了
