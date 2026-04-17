# SETUP.md

## クイック構成手順

### 前提
- Docker がインストールされていること
- WSL / Linux 環境
- インターネット接続（初回ダウンロード時）

---

## Step 1: PostgreSQL 起動

```bash
# ターミナルA: PostgreSQL コンテナ起動
docker run --rm \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=xtdb_dev \
  -p 5432:5432 \
  --name xtdb-postgres \
  postgres:16

# 起動確認
docker ps | grep postgres
```

**セッション時間目安**: 永続（Ctrl+C で停止）

---

## Step 2: 開発環境起動

```bash
# ターミナルB: プロジェクトディレクトリへ
cd /home/mtok/dev.home/xt-hledger-lab

# Guix 統合開発環境起動
guix shell -m manifest.scm
```

**このコマンド後に利用可能**:
- `clj`: Clojure CLI
- `rlwrap`: REPL ラッパー
- `hledger`: Plain Text Accounting
- `psql`: PostgreSQL クライアント

---

## Step 3: テスト実行

### A) hledger テスト（即座に分かる）

```bash
# Linux/macOS
hledger -f resources/sample.journal balance

# 出力例
#          300,000 JPY  Assets:Bank Account
#          -150,000 JPY  Equity:Opening Balance
#            ...
```

### B) XTDB v2 完全テスト（推奨）✨

```bash
# JVM オプション付きで実行（alias 自動適用）
clj -A:xtdb -i examples/xtdb_v2_complete.clj
```

**実行内容**:
✅ ノード起動 (with-open で安全管理)
✅ トランザクション実行 (execute-tx)
✅ SQL クエリ実行
✅ XTQL (Clojure 表記) クエリ実行

**初回実行**:
- Maven Central から XTDB v2.1.0 をダウンロード（5〜10 分）
- 2回目以後は非常に高速

### C) PostgreSQL 連携テスト（今後）

```bash
# PostgreSQL コンテナ起動済みの場合
clj -A:xtdb -i examples/03_xtdb_v2_postgresql.clj
```

---

## トラブルシューティング

### JVM オプション不足エラー

```
UnsupportedOperationException: ...java.nio...
IllegalAccessException: ...
```

**解決**: JVM オプションを付与
```bash
# ❌ 間違い (オプションなし)
clj -i examples/xtdb_v2_complete.clj

# ✅ 正しい (alias で自動適用)
clj -A:xtdb -i examples/xtdb_v2_complete.clj
```

### Maven 依存解決エラー

```
Error building classpath. Could not find artifact ...
```

**確認手順**:
1. インターネット接続確認
2. Maven キャッシュをクリア:
   ```bash
   rm -rf ~/.m2/repository
   ```
3. 再実行

### PostgreSQL コンテナの再起動

```bash
# 既存コンテナ停止
docker stop xtdb-postgres

# 再起動
docker run --rm -d \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=xtdb_dev \
  -p 5432:5432 \
  --name xtdb-postgres \
  postgres:16
```

### Clojure REPL で XTDB を試す

```bash
clj -A:xtdb

# REPL 内で
(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(with-open [node (xtn/start-node)]
  (println "Node started!")
  (xt/status node))
```

```bash
# 古いコンテナを削除
docker stop xtdb-postgres || true
docker rm xtdb-postgres || true

# 再度起動
docker run --rm -d \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=xtdb_dev \
  -p 5432:5432 \
  --name xtdb-postgres \
  postgres:16
```

---

## ファイル構成

```
xt-hledger-lab/
├── README.md                 # メインドキュメント
├── SETUP.md                  # このファイル
├── manifest.scm              # Guix 環境定義
├── deps.edn                  # Clojure 依存関係（XTDB v2）
├── deps-v1.edn               # XTDB v1 バージョン（フォールバック）
│
├── examples/
│   ├── 02_xtdb_v2_basics.clj           （インメモリ版）
│   └── 03_xtdb_v2_postgresql.clj       ✨ 推奨版（PostgreSQL連携）
│
├── src/xt_hledger/
│   └── core.clj              # 統合ロジック基盤
│
├── resources/
│   └── sample.journal        # hledger サンプル（複式簿記テスト）
│
├── docs/                     # 設計ドキュメント
├── test/                     # テスト
└── .gitignore
```

---

## 次のステップ

1. **Bitemporal 会計ロジック実装**
   - `src/xt_hledger/postgres.clj` で PostgreSQL-backed Bitemporal 操作

2. **hledger マッピング**
   - XTDB ドキュメント → Journal 形式への変換

3. **複式簿記整合性チェック**
   - 貸借対照表の自動検証

4. **GitHub へ公開**
   - リポジトリ初期化・Push

---

## リファレンス

- **XTDB v2 Docs**: https://docs.xtdb.com/v2/
- **hledger Manual**: https://hledger.org/
- **Plain Text Accounting**: https://plaintextaccounting.org/
