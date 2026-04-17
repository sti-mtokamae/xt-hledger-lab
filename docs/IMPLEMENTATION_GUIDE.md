# IMPLEMENTATION_GUIDE.md

実装段階ガイド - XTDB v2 + hledger プロジェクト

---

## 🎯 現在の状態: 基盤完成 ✅

**2025-04-17 確認:**
- ✅ XTDB v2.1.0 動作確認
- ✅ PostgreSQL 16 稼働
- ✅ hledger 動作確認
- ✅ JVM オプション設定完了
- ✅ Maven 依存解決完了

---

## Phase 1: hledger 統合 (推奨・1-2時間)

### 目標
XTDB ドキュメント → hledger 複式仕訳への変換機能実装

### 実装ファイル
```
src/xt_hledger/core.clj
```

### 実装タスク

#### Task 1.1: ドキュメント → 仕訳エントリ変換

```clojure
(defn document->ledger-entry
  "XTDB ドキュメントを hledger 仕訳に変換"
  [doc]
  (case (:type doc)
    :invoice {:date (:date doc)
              :narration (str (:supplier doc) " Invoice")
              :postings [{:account "Assets:Receivable"
                         :amount (:amount doc)
                         :currency (:currency doc)}
                        {:account "Income:Sales"
                         :amount (- (:amount doc))
                         :currency (:currency doc)}]}
    
    :payment {:date (:date doc)
              :narration (str "Payment to " (:payee doc))
              :postings [{:account "Assets:Bank"
                         :amount (- (:amount doc))
                         :currency (:currency doc)}
                        {:account "Liabilities:Payable"
                         :amount (:amount doc)
                         :currency (:currency doc)}]}
    
    nil))

;; テスト
(document->ledger-entry
  {:type :invoice
   :date #inst "2025-04-01"
   :supplier "ACME Corp"
   :amount 10000.00
   :currency "JPY"})
```

#### Task 1.2: 複数ドキュメント → hledger journal 生成

```clojure
(defn documents->journal
  "複数ドキュメントから hledger journal を生成"
  [documents]
  (->> documents
       (map document->ledger-entry)
       (filter identity)
       (sort-by :date)
       format-as-journal))

(defn format-as-journal [entries]
  (str/join "\n\n"
    (map (fn [entry]
           (str (:date entry) " " (:narration entry) "\n"
                (str/join "\n"
                  (map (fn [{:keys [account amount currency]}]
                         (format "  %-40s %10.2f %s" 
                                 account amount currency))
                       (:postings entry)))))
         entries)))
```

#### Task 1.3: Bitemporal 修正の考慮

```clojure
(defn bitemporal-adjustment
  "遡及修正を二重仕訳で表現"
  [original-doc adjusted-doc]
  
  ;; 元の仕訳を取消
  (let [reversal (assoc (document->ledger-entry original-doc)
                       :narration (str (:narration ...) " - REVERSAL"))]
    
    ;; 修正後の仕訳を追加
    (let [corrected (document->ledger-entry adjusted-doc)]
      [reversal corrected])))
```

### テスト実行例

```bash
clj -A:xtdb

; REPL で
(require '[xt_hledger.core :as xt-h]
         '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(with-open [node (xtn/start-node)]
  ; ドキュメント投入
  (xt/execute-tx node
    [[:put-docs :invoices
      {:xt/id "INV-2025-001"
       :type :invoice
       :date #inst "2025-04-01"
       :supplier "Supplier A"
       :amount 100000.00
       :currency "JPY"}]])
  
  ; ドキュメント抽出
  (def all-docs
    (xt/q node "SELECT * FROM invoices"))
  
  ; hledger journal 生成
  (println (xt-h/documents->journal all-docs)))
```

### 検証ポイント
- [ ] 複式簿記の原則に違反していない（左=右）
- [ ] 複数通貨複合シナリオで動作
- [ ] 遡及修正時の二重仕訳が正確
- [ ] hledger で残高確認できる (`hledger -f <出力ファイル> balance`)

---

## Phase 2: PostgreSQL バックエンド (2-3時間)

### 目標
XTDB v2 PostgreSQL ネイティブ連携

### 実装手順

1. **PostgreSQL 接続設定**
```clojure
(def pg-config
  {:log   [:postgres {:host "localhost" :port 5432 :user "postgres" :password "password" :dbname "xtdb_dev"}]
   :storage [:postgres {:host "localhost" :port 5432 :user "postgres" :password "password" :dbname "xtdb_dev"}]})

(with-open [node (xtn/start-node pg-config)]
  ; 永続化されたノード
  (xt/execute-tx node [...]))
```

2. **Bitemporal クエリ**
```sql
SELECT *, _valid_from, _valid_to 
FROM invoices 
FOR VALID_TIME ALL
WHERE _modified_reason = 'tax-audit'
```

3. **監査ログ検証**
```clojure
(defn extract-audit-trail
  "すべての過去バージョンを抽出"
  [node entity-id]
  (xt/q node
    ["SELECT *, _valid_from, _system_time 
      FROM invoices 
      WHERE _id = ? 
      FOR VALID_TIME ALL 
      FOR SYSTEM_TIME ALL
      ORDER BY _system_time"
     entity-id]))
```

---

## Phase 3: Spring Boot 統合 (3-4時間)

### 目標
Web API から XTDB へのアクセス層実装

```clojure
; 将来: Clojure + Spring Boot Graal VM native image
```

---

## 📋 チェックリスト

### Phase 1 前提
- [x] XTDB v2 基本動作確認
- [x] hledger 動作確認
- [x] JVM オプション設定

### Phase 1 実装
- [ ] document->ledger-entry 実装
- [ ] documents->journal 実装
- [ ] Bitemporal 修正ロジック実装
- [ ] テストケース記述
- [ ] サンプル実行確認

### Phase 1 検証
- [ ] 複式簿記の整合性確認
- [ ] hledger balance コマンド成功
- [ ] 遡及修正が正確に二重仕訳される

---

## 🔧 デバッグ & トラブル対応

### XTDB ノード起動失敗
```bash
# JVM オプション確認
clj -A:xtdb -e "(println \"success\")"

# 依存ライブラリ確認
clj -A:xtdb -e "(require '[xtdb.node :as xtn]) (println \"loaded\")"
```

### hledger 検証
```bash
# journal ファイル生成後
hledger -f /tmp/generated.journal balance
hledger -f /tmp/generated.journal incomestatement
hledger -f /tmp/generated.journal balancesheet
```

### PostgreSQL 確認
```bash
docker exec xtdb-postgres psql -U postgres -d xtdb_dev -c "SELECT * FROM information_schema.tables WHERE table_schema = 'public';"
```

---

## 📚 参考資料

- **XTDB v2 Docs**: https://docs.xtdb.com/
- **XTDB v2 Clojure API**: https://docs.xtdb.com/drivers/clojure/
- **hledger Manual**: https://hledger.org/
- **複式簿記の基本**: https://en.wikipedia.org/wiki/Double-entry_bookkeeping

---

**Last Updated:** 2025-04-17  
**Status:** Phase 1 実装準備完了 🚀
