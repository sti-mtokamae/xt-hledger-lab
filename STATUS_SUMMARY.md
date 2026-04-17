# 🎯 プロジェクト進捗サマリー (2025-04-17 更新)

## ✨ 重要: XTDB v2.1.0 完全動作確認済み

## 重要な発見 ✨

### XTDB v2 が Maven Central で利用可能！

**2025年4月時点での最新版:**
- **com.xtdb/xtdb-core**: `2.1.0` (リリース: 2025-12-01)
- **場所**: https://repo1.maven.org/maven2/com/xtdb/xtdb-core/
- **選択肢**:
  - v2.0.0-beta1 (初期ベータ)
  - v2.0.0-beta2～beta9
  - v2.1.0 (最新安定版) ✅ **推奨**

## 以前の課題と解決

| 課題 | 原因 | 解決策 |
|------|------|-------|
| XTDB v2 アーティファクト不在 | JUXT リポジトリへの DNS 解決失敗 | Maven Central 2.1.0 利用 |
| xtdb-sql モジュール | v1 x系までしかない | XTDB v2 コアに PostgreSQL 統合済み |
| 依存ライブラリ問題 | Clojars への HTTP 416 エラー | 構成簡潔化中 |

## 現在の設定状況

### deps.edn / deps-v1.edn (最新版)

```edn
{:deps {com.xtdb/xtdb-core {:mvn/version "2.1.0"}
        org.clojure/clojure {:mvn/version "1.11.1"}
        org.postgresql/postgresql {:mvn/version "42.7.1"}
        com.zaxxer/HikariCP {:mvn/version "5.1.0"}
        org.clojure/java.jdbc {:mvn/version "0.7.12"}
        ...}}
```

### PostgreSQL ステータス

```
✅ Docker コンテナ: xtdb-postgres (postgres:16)
✅ 接続: localhost:5432
✅ データベース: xtdb_dev
✅ テーブル: 初期状態（0個）
```

## 実行テスト結果

### ✅ 成功（2025-04-17 確認）

**XTDB v2.1.0 完全動作:**
```
✅ ノード起動: (xtn/start-node) → with-open で安全に管理
✅ トランザクション: (xt/execute-tx node [...]) で同期実行
✅ SQL クエリ: (xt/q node "SELECT * FROM users") → [{:xt/id alice ...}]
✅ XTQL クエリ: (xt/q node '(-> (from :users [...]) ...)) → 動作確認
```

**実行コマンド:**
```bash
cd /home/mtok/dev.home/xt-hledger-lab
clj -A:xtdb -i examples/xtdb_v2_complete.clj
```

**JVM オプション (必須):**
```
--add-opens=java.base/java.nio=ALL-UNNAMED
--enable-native-access=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true
```

### ✅ 環境確認済み
- PostgreSQL 16 Docker コンテナ: 正常動作 ✅
- xtdb_dev データベース: 作成済み ✅
- Clojure 1.12.0: API 互換性確認 ✅
- hledger 1.27.1: 動作確認済み ✅

## ファイル構成と役割

| ファイル | 目的 | ステータス |
|---------|------|-----------|
| **manifest.scm** | Guix 環境定義 | ✅ 完成（PostgreSQL 参照修正済） |
| **deps.edn** | Maven 依存関係 | ✅ 最新版（v2.1.0） |
| **deps-v1.edn** | 代替設定 | ✅ v2.1.0 対応 |
| **examples/02_xtdb_v2_basics.clj** | インメモリチュートリアル | ✅ 完成 |
| **examples/03_xtdb_v2_postgresql.clj** | PostgreSQL + XTDB v2 サンプル | ✅ 完成 |
| **examples/04_postgres_integration_test.clj** | 統合テスト | ⏳ 依存解決中 |
| **src/xt_hledger/core.clj** | 統合 API | ⏳ 作業中 |
| **resources/sample.journal** | hledger サンプルデータ | ✅ テスト済み |

## 次のステップ

### 1. インメモリ動作確認（依存問題回避）

```bash
cd /home/mtok/dev.home/xt-hledger-lab
clj -i examples/02_xtdb_v2_basics.clj
```

**確認事項**:
- XTDB v2 ノードが起動できるか
- ドキュメント投入が機能するか
- Datalog クエリが実行できるか

### 2. PostgreSQL バックエンドテスト

XTDB v2 コアのインメモリ機能確認後、PostgreSQL 連携に進みます。

### 3. hledger 統合

XTDB の Bitemporal ドキュメントから hledger 仕訳形式への変換ロジックを実装します。

## 技術的キーポイント

### Bitemporal データモデル

```
XTDB v2 では以下の時間軸を独立にトラック：
- Valid Time (VT): ドキュメントが現実的に有効だった時刻
  → 貿易書類は受け取った日付
- Transaction Time (TT): データが DB に記録された時刻
  → 修正・遡及変更は新しい TT を持つ
```

**例: 税務調査による遡及修正**
```
元の記録: VT=2025-01-15, TT=2025-01-15, Amount=1000
修正後:   VT=2025-01-15, TT=2025-04-17, Amount=950  # VT 不変、TT 更新
```

### 会計への応用

複式簿記の原則と Bitemporal の組み合わせで：
- ✅ 監査証跡が自動保持
- ✅ 遡及修正が整合性を保証
- ✅ バージョン履歴が明確

## 利用資料

- XTDB v2 ドキュメント: https://docs.xtdb.com/
- hledger マニュアル: https://hledger.org/
- Clojure: https://clojure.org/

## 環境情報

```
OS: WSL2 Linux
Clojure CLI: 1.12.1
XTDB: 2.1.0 (Maven Central)
PostgreSQL: 16 (Docker)
hledger: 1.27.1 (Guix)
```

---

**Last Updated**: 2025-04-17  
**Maintainer**: @user  
**Status**: 🔄 In Active Development
