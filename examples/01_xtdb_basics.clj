;; examples/01_xtdb_basics.clj
;; XTDB v2 基本操作のイントロダクション
;;
;; 実行方法:
;;   guix shell -m manifest.scm
;;   clj -i examples/01_xtdb_basics.clj
;;
;; または REPL から:
;;   (load-file "examples/01_xtdb_basics.clj")

(require '[xtdb.api :as xt])

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 1. ノード（DBエンジン）の起動
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n=== XTDB v2 基本操作ガイド ===\n")

(println "1️⃣  ノードを起動中...")
(def node (xt/start-node {}))
(println "✅ ノード起動完了")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 2. データの投入（Put）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n2️⃣  ドキュメント投入（貿易書類の例）...")

;; 貿易書類をドキュメントとして投入
(xt/submit-tx node
  [[:put :documents
    {:xt/id "INV-001"
     :type "Invoice"
     :party_to "ABC Trading Ltd."
     :amount 1500.00
     :currency "USD"
     :status "Draft"
     :issue_date #inst "2024-05-01"}]])

(println "✅ インボイス INV-001 を投入")

;; 複数のドキュメントを一度に投入
(xt/submit-tx node
  [[:put :documents
    {:xt/id "PKG-001"
     :type "PackingList"
     :invoice_ref "INV-001"
     :items [{:description "Widget A" :qty 100}
             {:description "Gadget B" :qty 50}]
     :status "Ready"}]
   [:put :documents
    {:xt/id "BL-001"
     :type "BillOfLading"
     :invoice_ref "INV-001"
     :vessel "MV Trade Ship"
     :eta #inst "2024-05-15"}]])

(println "✅ パッキングリスト PKG-001 と B/L BL-001 を投入")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 3. 基本的なクエリ（Datalog）
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n3️⃣  クエリ実行: すべてのドキュメントを取得...")

;; XTDB v2 では from構文でテーブルを指定
(def all-docs
  (xt/q node
    '(from :documents [xt/id type status])))

(println "クエリ結果:")
(doseq [doc all-docs]
  (println "  " doc))

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 4. Unification（単一化）による結合クエリ
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n4️⃣  グラフクエリ（関連ドキュメントの取得）...")
(println "  -> INV-001 に関連するすべてのドキュメント")

(def related-docs
  (xt/q node
    '(from :documents [xt/id type]
      (where (or (= xt/id "INV-001")
                 (= :invoice_ref "INV-001"))))))

(println "関連ドキュメント:")
(doseq [doc related-docs]
  (println "  " doc))

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 5. データの更新
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n5️⃣  Confirmed ステータスへの更新...")

(xt/submit-tx node
  [[:put :documents
    {:xt/id "INV-001"
     :type "Invoice"
     :party_to "ABC Trading Ltd."
     :amount 1500.00
     :currency "USD"
     :status "Confirmed"    ; ← ステータス変更
     :issue_date #inst "2024-05-01"
     :confirmed_date #inst "2024-05-10"}]])

(println "✅ INV-001 のステータスを Confirmed に更新")

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 6. タイムトラベル：過去のデータを取得
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n6️⃣  タイムトラベル: 過去の状態を確認...")
(println "  -> 2024-05-05（初期投入直後）時点での INV-001 の status")

;; ノード作成時の特定時点のデータを切り出す
(def db-past (xt/db node #inst "2024-05-05"))

(def past-status
  (xt/q db-past
    '(from :documents [xt/id status]
      (where (= xt/id "INV-001")))))

(println "過去のステータス:")
(doseq [doc past-status]
  (println "  " doc))

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 7. Bitemporal の真価：遡及修正
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n7️⃣  Bitemporal: 遡及修正（金額修正）...")
(println "  -> 2024-05-01 時点での金額を 1500 USD から 1600 USD へ（システム時間は現在）")

;; 有効時刻（Valid Time）を指定して、過去のデータを修正
; Note: :xt/valid-from は、そのドキュメントの「実世界有効日」を指定
(xt/submit-tx node
  [[:put :documents
    {:xt/id "INV-001"
     :type "Invoice"
     :party_to "ABC Trading Ltd."
     :amount 1600.00      ; 修正後の金額
     :currency "USD"
     :status "Confirmed"
     :issue_date #inst "2024-05-01"
     :confirmed_date #inst "2024-05-10"
     :correction_note "金額を修正しました"}
    {:xt/valid-from #inst "2024-05-01"}]])

(println "✅ 遡及修正完了")

;; 最新の値を確認
(def latest-docs
  (xt/q node
    '(from :documents [xt/id amount status]
      (where (= xt/id "INV-001")))))

(println "\n最新の状態:")
(doseq [doc latest-docs]
  (println "  " doc))

;; +++++++++++++++++++++++++++++++++++++++++++++++++++++
;; 8. 整理とシャットダウン
;; +++++++++++++++++++++++++++++++++++++++++++++++++++++

(println "\n✨ 基本操作のチュートリアルが完了しました")
(println "\nこれで理解した内容:")
(println "  • ドキュメント投入と取得")
(println "  • Datalog クエリと Unification")
(println "  • タイムトラベルクエリ")
(println "  • Bitemporal な遡及修正")

(println "\n次のステップ: examples/02_timavel.clj へ")

;; ノードのクローズは任意
;; (xt/close-node node)

nil
