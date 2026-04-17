;; src/xt_hledger/core.clj
;; 
;; XTDB と hledger の基本的な統合機能提供
;; - XTDB ノードの管理
;; - ドキュメント投入・照会
;; - hledger フォーマット変換

(ns xt-hledger.core
  (:require [xtdb.node :as xtn]
            [xtdb.api :as xt]))

;; ============================================
;; XTDB ノード管理
;; ============================================

(defn create-node
  "XTDB ノード（DBエンジン）を起動
   
   オプション:
   - :persist? : ディスクに永続化するか（デフォルト false）
   - :config   : カスタム設定マップ
   
   返り値: xtdb.api.Xtdb インスタンス（with-open で使用）
   
   例:
   (with-open [node (create-node)]
     (xt/execute-tx node [...])
     (xt/q node [...]))
  "
  [& {:keys [persist? config] :or {persist? false config {}}}]
  (let [final-config (if persist?
                       (merge config {:log [:local {:path ".xtdb/log"}]})
                       config)]
    (xtn/start-node final-config)))

;; ============================================
;; ドキュメント操作
;; ============================================

(defn put-document
  "XTDB にドキュメントを投入（同期実行）
   
   例:
   (put-document node :invoices
     {:xt/id \"INV-001\"
      :type :invoice
      :date #inst \"2025-04-01\"
      :supplier \"ACME Corp\"
      :amount 10000.00
      :currency \"JPY\"})
  "
  [node table doc]
  (xt/execute-tx node
    [[:put-docs table doc]]))

(defn put-documents
  "複数ドキュメントを一度に投入"
  [node table docs]
  (xt/execute-tx node
    [(into [:put-docs table] docs)]))

(defn update-document
  "ドキュメントを更新（上書き）
   
   内部的には put と同じだが、セマンティクスを明確化
  "
  [node table doc]
  (put-document node table doc))

(defn delete-document
  "ドキュメントを削除
   
   Bitemporal 対応: 遡及削除も可能
  "
  [node table id]
  (xt/execute-tx node
    [[:erase-docs table [id]]]))

(defn query-all
  "テーブル内のすべてのドキュメントを取得（SQL版）"
  [node table]
  (xt/q node
    (str "SELECT * FROM " table)))

(defn query-by-id
  "IDでドキュメントを取得"
  [node table id]
  (xt/q node
    [(str "SELECT * FROM " table " WHERE _id = ?") id]))

(defn query-by-type
  "type フィールドでフィルタ"
  [node table type-val]
  (xt/q node
    [(str "SELECT _id, type FROM " table " WHERE type = ?") type-val]))

;; ============================================
;; タイムトラベル（参考実装）
;; ============================================

(defn timetravel-query
  "特定の時点のデータを照会
   
   例:
   (timetravel-query node #inst \"2024-05-01\")
  "
  [node at-time table query-fields]
  (let [db (xt/db node at-time)]
    (xt/q db
      (list 'from table query-fields))))

;; ============================================
;; hledger フォーマット変換（スタブ）
;; ============================================

(defn document->ledger-entry
  "XTDB ドキュメントを hledger 複式仕訳に変換
   
   ドキュメントタイプに応じた仕訳パターン:
   - :invoice   → 売掛金 / 売上
   - :payment   → 銀行口座 / 買掛金
   - :expense   → 経費 / 銀行口座
   
   例:
   (document->ledger-entry
     {:type :invoice
      :date #inst \"2025-04-01\"
      :supplier \"ACME Corp\"
      :amount 10000.00
      :currency \"JPY\"})
  "
  [doc]
  (case (:type doc)
    :invoice
    {:date (:date doc)
     :narration (str "Invoice from " (:supplier doc))
     :postings [{:account "Assets:AccountsReceivable"
                :amount (:amount doc)
                :currency (:currency doc)}
               {:account "Revenue:Sales"
                :amount (- (:amount doc))
                :currency (:currency doc)}]}
    
    :payment
    {:date (:date doc)
     :narration (str "Payment to " (:payee doc))
     :postings [{:account "Assets:Bank"
                :amount (- (:amount doc))
                :currency (:currency doc)}
               {:account "Liabilities:AccountsPayable"
                :amount (:amount doc)
                :currency (:currency doc)}]}
    
    :expense
    {:date (:date doc)
     :narration (:description doc)
     :postings [{:account (:expense-account doc "Expenses:Other")
                :amount (:amount doc)
                :currency (:currency doc)}
               {:account "Assets:Bank"
                :amount (- (:amount doc))
                :currency (:currency doc)}]}
    
    nil))

(defn format-ledger-entry
  "hledger エントリマップをテキスト形式にフォーマット"
  [entry]
  (when entry
    (let [{:keys [date narration postings]} entry
          date-str (if (instance? java.util.Date date)
                     (subs (.toString date) 0 10)  ; YYYY-MM-DD format
                     (str date))]
      (str date-str " * " narration "\n"
           (apply str
             (map (fn [{:keys [account amount currency]}]
                    (format "  %-40s %12.2f %s\n"
                           account amount currency))
                  postings))))))

(defn documents->journal
  "複数のドキュメントから hledger ジャーナルテキストを生成
   
   例:
   (documents->journal
     [{:type :invoice :date #inst \"2025-04-01\" ...}
      {:type :payment :date #inst \"2025-04-05\" ...}])
  "
  [documents]
  (->> documents
       (map document->ledger-entry)
       (filter identity)
       (sort-by :date)
       (map format-ledger-entry)
       (apply str)))

;; ============================================
;; ユーティリティ
;; ============================================

(defn close-node
  "ノードをクローズ（クリーンアップ）"
  [node]
  (xt/close-node node))

;; 初期メッセージ
(println "✅ xt-hledger.core モジュール読み込み完了")
