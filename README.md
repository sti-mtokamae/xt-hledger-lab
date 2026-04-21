# xt-hledger-lab

**XTDBとhledgerの学習・検証プロジェクト**

XTDB v2（次世代二時系データベース）とhledger（プレインテキスト会計ツール）を実験的に組み合わせ、以下を探求します：

- **XTDB v2** の Bitemporal（二時系）特性による遡及修正・過去データ管理
- **Datalog/XTQL** によるグラフクエリ
- **hledger** の Plain Text Accounting 思想と複式簿記
- 貿易ドキュメント管理と計数管理（会計）の統合

## 目的

1. XTDBの基本操作習得：ドキュメント投入、タイムトラベル、グラフクエリ
2. hledger との連携：XTDB由来の仕訳データをPTA形式で生成・検証
3. 将来的な貿易実務適用に向けた設計検証（計数の遡及修正、複式簿記の保証）

## 前提条件

- **JDK 21+** (Apache Arrow 要件)
- **WSL 2** (Linux環境)
- **Git** (バージョン管理)
- **Docker** (PostgreSQL コンテナ実行用 - オプション)

### JVM オプション（必須）

XTDB v2 は Apache Arrow のメモリ操作に JVM オプションが必須です：

```bash
--add-opens=java.base/java.nio=ALL-UNNAMED
--enable-native-access=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true
```

設定済み: `deps.edn` の `:xtdb` alias で自動適用

## ファイル構成

| ファイル | 説明 |
|---------|------|
| **manifest.scm** | Guix環境定義（OpenJDK, Clojure CLI, hledger, PostgreSQL） |
| **deps.edn** | Clojure依存関係（XTDB v2 + PostgreSQL + カスタム Maven リポジトリ） |
| **resources/sample.journal** | hledger サンプルデータ（貿易書類を想定） |
| **examples/02_xtdb_v2_basics.clj** | XTDB v2 インメモリ版チュートリアル |
| **examples/03_xtdb_v2_postgresql.clj** | XTDB v2 + PostgreSQL チュートリアル ✨ **推奨** |
| **src/xt_hledger/core.clj** | XTDB + hledger 統合ロジック |
| **scripts/setup-postgres-for-xtdb.sh** | PostgreSQL 初期化スクリプト（Phase 2） |

## 重要な更新 ✨

**2025年4月時点**：XTDB v2.1.0 が Maven Central で公開されています。
- ✅ `com.xtdb/xtdb-core:2.1.0` - 入手可能
- ✅ PostgreSQL サポートはコアに統合
- ✅ JUXT カスタムリポジトリは不要

## 実装フェーズ

| フェーズ | 機能 | 状態 | コミット |
|---------|------|------|---------|
| **Phase 1** | XTDB + hledger 統合（ジャーナル生成） | ✅ 完了 | `56bb5f6` |
| **Phase 2-persistent** | ファイルベース永続化（.xtdb/log） | ✅ 完了 | `07f2193` |
| **Phase 2.5** | PostgreSQL バックアップ同期 | ✅ 完了 | `d48f5d0` |
| **Phase 3** | Spring Boot 統合 | ⏳ 計画中 | - |

### Phase ごとの使用例

```clojure
; Phase 1: インメモリ（揮発性）
(with-open [node (core/create-node :persist? false)]
  (core/demo-phase1 node))

; Phase 2-persistent: ファイル永続化（デフォルト）
(with-open [node (core/create-node :db-path "./my-db")]
  (core/demo-phase2-persistent node))

; Phase 2.5: ファイル永続化 + PostgreSQL バックアップ
(with-open [node (core/create-node :db-path "./my-db")]
  (core/demo-phase2-persistent node)
  (core/sync-documents-to-postgres node :password "password"))
```

## セットアップ

### 0. PostgreSQL 準備（オプション - Phase 2.5 で使用）

#### オプション A: Docker で PostgreSQL 起動（推奨）

```bash
# PostgreSQL 16 コンテナ起動
docker run --rm -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  --name xtdb-postgres \
  postgres:16

# 別ターミナルで xtdb_dev データベース作成
docker exec xtdb-postgres psql -U postgres -c "CREATE DATABASE xtdb_dev;"
```

#### オプション B: ローカル PostgreSQL を使用

```bash
# ローカル PostgreSQL 起動
# macOS:
brew services start postgresql

# Linux (systemd):
sudo systemctl start postgresql

# xtdb_dev データベース作成
createdb -U postgres xtdb_dev
```

### 1. 環境の作成

```bash
# このリポジトリをクローン
git clone https://github.com/<your-org>/xt-hledger-lab.git
cd xt-hledger-lab

# Guixで依存環境を構築（初回はhledger等をビルド）
guix shell -m manifest.scm
```

### 2. Clojure REPL の起動

```bash
# 基本的なREPL
rlwrap clj -A:xtdb

# XTDB v2 完全テスト（推奨）✨
clj -A:xtdb -i examples/xtdb_v2_complete.clj

# XTDB v2 インメモリ基本編
clj -A:xtdb -i examples/02_xtdb_v2_basics.clj
```

### 3. hledger の動作確認

```bash
# バージョン確認
hledger --version

# サンプルジャーナルでレポート出力
hledger -f resources/sample.journal balance
```

### 補足：XTDB v2 について

**✅ 動作確認済み (2025-04-17)**
- **XTDB v2.1.0** Maven Central で完全公開
- PostgreSQL ネイティブサポート統合
- Clojure 1.12.0 推奨
- JVM オプション必須（deps.edn `-A:xtdb` で自動適用）

**基本 API:**
```clojure
(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])

(with-open [node (xtn/start-node)]
  (xt/execute-tx node [[:put-docs :users {:xt/id "alice" :age 30}]])
  (xt/q node "SELECT * FROM users"))
```

## ディレクトリ構成

```
xt-hledger-lab/
├── src/                      # Clojureソースコード
│   └── xt_hledger/
│       ├── core.clj         # XTDB基本操作
│       ├── pta_gen.clj      # hledger形式への変換
│       └── queries.clj      # Datalog/SQLクエリ集
├── examples/                 # 実行例・チュートリアル
│   ├── 01_xtdb_basics.clj   # XTDB入門
│   ├── 02_timavel.clj      # タイムトラベルクエリ
│   └── 03_pta_integration.clj
├── resources/                # 静的リソース
│   ├── sample.journal        # hledgerサンプルデータ
│   └── trade_doc_sample.json # 貿易書類サンプル
├── test/                     # テスト
├── docs/                     # ドキュメント
│   ├── XTDB_NOTES.md        # XTDB学習ノート
│   └── DESIGN.md            # アーキテクチャ設計
├── manifest.scm             # Guix環境定義
├── deps.edn                 # Clojure依存関係
└── README.md                # このファイル
```

## 学習ロードマップ

### Phase 1: XTDB v2 の基本 ✅ **完了**
- ✅ XTDB ノード管理
- ✅ ドキュメント投入・照会
- ✅ Bitemporal なデータ管理
- ✅ hledger ジャーナル生成
- **実装ファイル**: `src/xt_hledger/core.clj` (Phase 1 関連関数)

### Phase 2: ディスク永続化 ✅ **完了**
- ✅ ファイルベース永続化（.xtdb/log）
- ✅ プロセス再起動後のデータ保持確認
- ✅ Bitemporal 履歴の永続化
- **実装ファイル**: `src/xt_hledger/core.clj` (Phase 2-persistent 関連)

### Phase 2.5: PostgreSQL バックアップ同期 ✅ **完了**
- ✅ JDBC 経由の PostgreSQL 接続
- ✅ xtdb_backup テーブル自動作成
- ✅ ドキュメント同期機能
- ✅ 修正履歴の同期
- **実装ファイル**: `src/xt_hledger/core.clj` (Phase 2.5 関数群)

### Phase 3: Spring Boot 統合 ⏳ **計画中**
- [ ] Spring Boot プロジェクト構築
- [ ] REST API エンドポイント
- [ ] Spring Data JPA による PostgreSQL 管理
- [ ] 会計伝票の CRUD 操作

### Phase 4: 統合検証・最適化 ⏳ **計画中**
- [ ] パフォーマンス検証
- [ ] マルチテーブル操作
- [ ] トランザクション管理

## クイックスタート

### 1. 環境初期化

```bash
cd xt-hledger-lab
guix shell -m manifest.scm    # Guix環境構築
```

### 2. PostgreSQL を起動（オプション - Phase 2.5 使用時）

```bash
# Docker を使用する場合
docker run --rm -d -e POSTGRES_PASSWORD=password \
  -p 5432:5432 --name xtdb-postgres \
  postgres:16 postgres -c wal_level=logical

# データベース作成
docker exec xtdb-postgres psql -U postgres -c "CREATE DATABASE xtdb_dev;"
```

### 3. XTDB REPL を起動

```bash
# Calva jack-in (VS Code + Calva 推奨)
# Cmd+Shift+P → "Calva: Start a Project REPL and Connect"

# または CLI REPL
clj -A:xtdb
```

### 4. Phase デモを実行

```clojure
; Phase 1: インメモリデモ（基本）
(require '[xt-hledger.core :as core])
(with-open [node (core/create-node :persist? false)]
  (core/demo-phase1 node))

; Phase 2: ファイル永続化デモ
(with-open [node (core/create-node :db-path "./my-db")]
  (core/demo-phase2-persistent node))

; Phase 2.5: PostgreSQL バックアップ同期デモ
(with-open [node (core/create-node :db-path "./my-db")]
  (core/demo-phase2-persistent node)
  (core/demo-phase2-postgres-backup node))
```

### 5. hledger でテスト

```bash
hledger -f resources/sample.journal balance
hledger -f resources/sample.journal is      # Income Statement
hledger -f resources/sample.journal bs      # Balance Sheet
```

### API リファレンス（Phase 2 以降）

```clojure
;; ノード作成（ファイル永続化）
(create-node :db-path "./my-ledger-db")

;; ドキュメント投入
(put-documents node :documents [{:xt/id "DOC-001" :amount 100000.0 ...}])

;; PostgreSQL 同期
(sync-documents-to-postgres node :password "password")
```

### XTDB v2 コア API

```clojure
;; ドキュメント投入
(xt/execute-tx node [[:put-docs :table {:xt/id "id" :field "value"}]])

;; クエリ実行
(xt/q node "SELECT * FROM table WHERE id = 'id'")

;; トランザクション実行
(def node (xt/start-node pg-config))

;; ドキュメント投入（PostgreSQL に永続化）
(xt/submit-tx node
  {:xtdb.api/tx-ops
   [{:xtdb.api/op :put
     :xtdb.api/table :documents
     :xt/id "INV-001"
     :amount 1500.00
     :currency "USD"
     :xt/valid-time #inst "2024-05-01T00:00:00Z"}]})

;; Bitemporal: 遡及修正
(xt/submit-tx node
  {:xtdb.api/tx-ops
   [{:xtdb.api/op :put
     :xtdb.api/table :documents
     :xt/id "INV-001"
     :amount 1600.00        ; 修正
     :currency "USD"
     :xt/valid-time #inst "2024-05-01T00:00:00Z"}]})
```

## プロジェクト構成

```
xt-hledger-lab/
├── README.md                           # このファイル
├── docs/                               # ドキュメント
│   ├── SETUP.md                        # セットアップガイド（JVM オプション、トラブルシューティング）
│   ├── STATUS_SUMMARY.md               # プロジェクト状態と成果物まとめ
│   └── IMPLEMENTATION_GUIDE.md         # Phase 1-3 実装ロードマップ
├── src/xt_hledger/                     # メイン Clojure ソースコード
│   └── core.clj                        # XTDB v2 + hledger 統合 API
├── examples/                           # 実行例・チュートリアル
│   ├── 02_xtdb_v2_basics.clj           # インメモリ XTDB v2 基础
│   ├── 03_xtdb_v2_postgresql.clj       # PostgreSQL バックエンド例
│   └── xtdb_v2_complete.clj            # 完全な動作例（✅ 動作確認済）
├── resources/                          # リソース
│   └── sample.journal                  # サンプル hledger ジャーナル
├── test/                               # テスト
├── deps.edn                            # 主要 Clojure 依存定義（✅ 動作確認版）
├── deps-v1.edn                         # バックアップ依存定義
├── manifest.scm                        # Guix 環境定義
└── .gitignore                          # Git 無視設定
```

### ドキュメント読み方

1. **セットアップ中:** [docs/SETUP.md](docs/SETUP.md) を参照（環境構築の手順とトラブルシューティング）
2. **現在の状態確認:** [docs/STATUS_SUMMARY.md](docs/STATUS_SUMMARY.md) を参照（XTDB v2 動作確認状況）
3. **実装を始める:** [docs/IMPLEMENTATION_GUIDE.md](docs/IMPLEMENTATION_GUIDE.md) を参照（Phase 1-3 具体的タスク分解）

## リソース

- **XTDB 公式ドキュメント**: https://docs.xtdb.com/
- **XTDB v2 ブログ**: https://www.xtdb.com/blog
- **hledger 公式**: https://hledger.org/
- **Plain Text Accounting**: https://plaintextaccounting.org/

## ライセンス

MIT License

## 貢献

学習中のプロジェクトですが、フィードバックや提案は大歓迎です。
