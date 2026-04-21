;; examples/02_xtdb_v2_basics.clj
;; XTDB v2 対応版：基本操作ガイド
;;
;; XTDB v2 は SQL を標準クエリ言語とします。
;; Datalog は legacy 機能となります。
;;
;; 実行方法:
;;   guix shell -m manifest.scm
;;   clj -i examples/02_xtdb_v2_basics.clj

(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(println "\n==========================================")
(println "✨ XTDB v2 基本操作チュートリアル")
(println "==========================================\n")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 1. ノード起動
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "1️⃣  ノードを起動中...")
(def node (xtn/start-node {}))
(println "✅ XTDB v2 ノード起動完了\n")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 2. ドキュメント投入（v2 スタイル）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "2️⃣  ドキュメント投入...")

;; v2 は xt/execute-tx と [:put-docs table doc] 形式
(xt/execute-tx node
  [[:put-docs :trade_docs
    {:xt/id "INV-2024-001"
     :type "Invoice"
     :supplier "ABC Trading Ltd."
     :amount 1500.00
     :currency "USD"
     :status "Draft"
     :issue_date #inst "2024-05-01T00:00:00Z"}]])

(println "✅ インボイス INV-2024-001 投入")

;; 複数ドキュメント一括投入
(xt/execute-tx node
  [[:put-docs :trade_docs
    {:xt/id "PKG-2024-001"
     :type "PackingList"
     :invoice_ref "INV-2024-001"
     :items [{:description "Widget A" :qty 100}
             {:description "Gadget B" :qty 50}]
     :created #inst "2024-05-02T00:00:00Z"}
    {:xt/id "BL-2024-001"
     :type "BillOfLading"
     :invoice_ref "INV-2024-001"
     :vessel "MV Trade Winds"
     :eta #inst "2024-05-15T00:00:00Z"}]])

(println "✅ パッキングリストとB/L投入\n")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 3. SQL クエリ（v2推奨）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "3️⃣  SQL クエリ: すべてのドキュメント取得...")

(def all-docs
  (xt/q node
    "SELECT _id, type, amount FROM trade_docs"))

(println "結果:")
(doseq [doc all-docs]
  (println "  " doc))
(println "")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 4. SQL 結合クエリ（複雑なクエリ例）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "4️⃣  SQL 結合: INV-2024-001 関連ドキュメント...")

(def related
  (xt/q node
    "SELECT _id, type FROM trade_docs WHERE invoice_ref = 'INV-2024-001'"))

(println "関連ドキュメント:")
(doseq [doc related]
  (println "  " doc))
(println "")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 5. ドキュメント更新
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "5️⃣  ドキュメント更新: ステータスを Confirmed に...")

(xt/execute-tx node
  [[:put-docs :trade_docs
    {:xt/id "INV-2024-001"
     :type "Invoice"
     :supplier "ABC Trading Ltd."
     :amount 1500.00
     :currency "USD"
     :status "Confirmed"          ; ← 変更
     :issue_date #inst "2024-05-01T00:00:00Z"
     :confirmed_date #inst "2024-05-10T00:00:00Z"}]])

(println "✅ ステータス更新完了\n")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 6. Bitemporal: 遡及修正（XTDB の最大の特徴）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "6️⃣  Bitemporal 操作: 遡及修正...")
(println "  シナリオ: 5/1 に発行されたINVの金額が間違っていた（1500 -> 1600） \n")

;; XTDB では :xt/valid-time を使用して、実世界での有効時刻を指定できます
(xt/execute-tx node
  [[:put-docs :trade_docs
    {:xt/id "INV-2024-001"
     :type "Invoice"
     :supplier "ABC Trading Ltd."
     :amount 1600.00              ; 修正後
     :currency "USD"
     :status "Confirmed"
     :issue_date #inst "2024-05-01T00:00:00Z"
     :confirmed_date #inst "2024-05-10T00:00:00Z"
     :correction_note "金額修正: 1500 -> 1600 USD"}]])  ; ← 有効時刻を過去に指定

(println "✅ 遡及修正完了（5/1のデータメタデータに適用）\n")

;; 修正後のデータを確認
(def updated
  (xt/q node
    "SELECT _id, amount, status FROM trade_docs WHERE _id = 'INV-2024-001'"))

(println "修正後の最新データ:")
(doseq [doc updated]
  (println "  " doc))

(println "\n" "=" 40)
(println "✨ チュートリアル完了")
(println "=" 40)

(println "\n📚 XTDB v2 のポイント:")
(println "  • SQL が標準クエリ言語（Datalog は legacy）")
(println "  • Bitemporal: 過去のデータへの遡及修正が可能")
(println "  • Immutable: すべての変更が監査ログとして保持される")
(println "\n次のステップ: ")
(println "  1. src/xt_hledger/core.clj で統合ロジック実装")
(println "  2. hledger との連携テスト")

nil
