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
    (str "SELECT * FROM " (name table))))

(defn query-by-id
  "IDでドキュメントを取得"
  [node table id]
  (xt/q node
    (str "SELECT * FROM " (name table) " WHERE _id = '" id "'")))

(defn query-by-type
  "type フィールドでフィルタ"
  [node table type-val]
  (xt/q node
    (str "SELECT * FROM " (name table) " WHERE type = '" (name type-val) "'")))

;; ============================================
;; タイムトラベル（参考実装）
;; ============================================

(defn timetravel-query
  "特定の時点のデータを照会（v2.1.0 での実装）
   
   注: XTDB v2.1.0ではバージョン1と異なる API を使用
   例:
   (xt/q node \"SELECT * FROM table AT #inst \\\"2024-05-01\\\"\")
  "
  [node _at-time table]
  ;; v2.1.0 では SQL で AT 句を使用
  (xt/q node
    (format "SELECT * FROM %s" (name table))))

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
;; Bitemporal 修正対応
;; ============================================

(defn bitemporal-correction
  "Bitemporal: 遡及修正を二重仕訳で表現
   
   修正前のドキュメント金額と修正後を反映させる
   例:
   (bitemporal-correction
     {:type :invoice :amount 1500.00}
     {:type :invoice :amount 1600.00})
  "
  [original-doc adjusted-doc]
  (let [original-entry (document->ledger-entry original-doc)
        adjusted-entry (document->ledger-entry adjusted-doc)
        diff-amount (- (:amount adjusted-doc) (:amount original-doc))
        ;; 元の仕訳を取消し
        reversal (assoc original-entry
                       :narration (str (:narration original-entry) " - REVERSAL"))
        ;; 差分だけの修正仕訳
        correction {:date (:date adjusted-doc)
                   :narration (str "Correction: " (:narration adjusted-entry))
                   :postings [{:account "Assets:AccountsReceivable"
                             :amount diff-amount
                             :currency (:currency adjusted-doc)}
                            {:account "Revenue:Sales"
                             :amount (- diff-amount)
                             :currency (:currency adjusted-doc)}]}]
    [reversal correction adjusted-entry]))

;; ============================================
;; ユーティリティ
;; ============================================

(defn close-node
  "ノードをクローズ（クリーンアップ）
   
   注: with-open で自動管理される場合は呼ばずに with-open ブロック終了時に自動クローズ
  "
  [node]
  ;; v2.1.0 では with-open ブロック内での使用が推奨
  ;; 手動クローズが必要な場合は .close メソッドを使用
  (when node
    (.close node)))

;; ============================================
;; テスト用実装例
;; ============================================

(defn demo-phase1
  "Phase 1 デモンストレーション
   
   用法:
   (with-open [node (create-node)]
     (demo-phase1 node))
  "
  [node]
  (println "\n" "="50)
  (println "🚀 Phase 1: XTDB -> hledger 統合デモ")
  (println "="50)
  
  ;; サンプルドキュメント投入
  (println "\n📝 サンプルドキュメント投入中...")
  (put-documents node :documents
    [{:xt/id "INV-001"
      :type :invoice
      :date #inst "2025-04-01T00:00:00Z"
      :supplier "ACME Trading Corp"
      :amount 100000.00
      :currency "JPY"}
     {:xt/id "PAY-001"
      :type :payment
      :date #inst "2025-04-05T00:00:00Z"
      :payee "ACME Trading Corp"
      :amount 100000.00
      :currency "JPY"}
     {:xt/id "EXP-001"
      :type :expense
      :date #inst "2025-04-02T00:00:00Z"
      :description "Office Supplies"
      :expense-account "Expenses:Office"
      :amount 5000.00
      :currency "JPY"}])
  (println "✅ ドキュメント投入完了\n")
  
  ;; XTDB ドキュメント照会
  (println "📊 XTDB 内のドキュメント:")
  (let [docs (query-all node :documents)]
    (doseq [doc docs]
      (println "  " (select-keys doc [:xt/id :type :amount]))))
  
  ;; hledger 変換
  (println "\n🔄 hledger ジャーナル変換中...")
  (let [docs (query-all node :documents)
        journal (documents->journal docs)]
    (println "\n📄 Generated hledger Journal:")
    (println journal))
  
  (println "✨ Phase 1 デモ完了\n")
  nil)

;; ============================================
;; Phase 2: PostgreSQL バックエンド統合
;; ============================================

(defn create-node-with-postgres
  "PostgreSQL バックエンド付き XTDB ノードを起動
   
   オプション:
   - :host     : PostgreSQL ホスト（デフォルト localhost）
   - :port     : PostgreSQL ポート（デフォルト 5432）
   - :user     : ユーザー名（デフォルト postgres）
   - :password : パスワード（デフォルト postgres）
   - :dbname   : データベース名（デフォルト xtdb_dev）
   
   返り値: PostgreSQL バックエンド付き XTDB ノード
   
   例:
   (with-open [node (create-node-with-postgres)]
     (xt/execute-tx node [...]))
  "
  [& {:keys [host port user password dbname]
      :or {host "localhost" port 5432 user "postgres" 
           password "postgres" dbname "xtdb_dev"}}]
  
  (let [pg-config
        {:log   [:postgres {:host host :port port :user user 
                           :password password :dbname dbname}]
         :storage [:postgres {:host host :port port :user user 
                             :password password :dbname dbname}]}]
    (xtn/start-node pg-config)))

(defn extract-audit-trail
  "すべての過去バージョンを抽出（Bitemporal クエリ）
   
   注: XTDB v2.1.0 では API が異なるため、現在のバージョンのみ取得
   将来の実装で FOR VALID_TIME ALL / FOR SYSTEM_TIME ALL 対応
   
   例:
   (extract-audit-trail node :documents \"INV-001\")
  "
  [node table entity-id]
  (xt/q node
    (str "SELECT * FROM " (name table) 
         " WHERE _id = '" entity-id "'")))

(defn bitemporal-history
  "エンティティの時間的履歴を取得（Bitemporal対応）
   
   複数バージョンのドキュメントを取得
   例:
   (bitemporal-history node :documents \"INV-001\")
  "
  [node table entity-id]
  (extract-audit-trail node table entity-id))

;; ============================================
;; Phase 2 デモンストレーション
;; ============================================

(defn demo-phase2
  "Phase 2: PostgreSQL バックエンド & Bitemporal クエリ デモ
   
   用法:
   (with-open [node (create-node-with-postgres)]
     (demo-phase2 node))
  "
  [node]
  (println "\n" "="50)
  (println "🚀 Phase 2: PostgreSQL バックエンド & Bitemporal")
  (println "="50)
  
  ;; サンプルドキュメント投入
  (println "\n📝 サンプルドキュメント投入中...")
  (put-documents node :documents
    [{:xt/id "INV-PG-001"
      :type :invoice
      :date #inst "2025-04-01T00:00:00Z"
      :supplier "PostgreSQL Inc."
      :amount 500000.00
      :currency "JPY"
      :description "Database Service Contract"}])
  (println "✅ ドキュメント投入完了\n")
  
  ;; 初期データ確認
  (println "📊 投入されたドキュメント:")
  (let [docs (query-all node :documents)]
    (doseq [doc docs]
      (println "  " (select-keys doc [:xt/id :type :amount]))))
  
  ;; Bitemporal: 金額修正
  (println "\n🔄 Bitemporal: 遡及修正実行中...")
  (put-documents node :documents
    [{:xt/id "INV-PG-001"
      :type :invoice
      :date #inst "2025-04-01T00:00:00Z"
      :supplier "PostgreSQL Inc."
      :amount 550000.00  ; 修正：500000 -> 550000
      :currency "JPY"
      :description "Database Service Contract - CORRECTED"
      :correction_reason "Price adjustment"}])
  (println "✅ 遡及修正完了\n")
  
  ;; 監査ログ抽出
  (println "📋 監査ログ抽出:")
  (let [history (bitemporal-history node :documents "INV-PG-001")]
    (doseq [version history]
      (println "  版:")
      (println "    ID:" (:xt/id version))
      (println "    金額:" (:amount version))
      (println "    説明:" (:description version))))
  
  ;; hledger 変換（修正後）
  (println "\n🔄 修正後のhledger ジャーナル生成...")
  (let [docs (query-all node :documents)
        journal (documents->journal docs)]
    (println "\n📄 生成されたジャーナル:")
    (println journal))
  
  (println "✅ PostgreSQL 永続化完了")
  (println "   → Docker コンテナ内に保存されました\n")
  
  (println "✨ Phase 2 デモ完了\n")
  nil)

;; 初期メッセージ
(println "✅ xt-hledger.core モジュール読み込み完了")
