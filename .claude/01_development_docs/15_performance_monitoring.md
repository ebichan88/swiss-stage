# パフォーマンス監視・運用設計書

## 1. 基本方針

- 監視の目的は**大会当日の異常に即座に気づき、大会を止めないこと**
- コスト制約(月$3程度)の中で CloudWatch を中心に構成する
- 「大会モード」(当日の重点監視)と「平常モード」を分けて考える

---

## 2. 構造化ログ

- Spring Boot は logback + JSON エンコーダーで構造化ログを出力
- CloudWatch Logs へ転送(CloudWatch Agent)
- 保持期間: 30日(コスト対策)
- 必須フィールド: `timestamp`, `level`, `requestId`, `method`, `path`, `status`, `durationMs`, `errorCode`, `tournamentId`
- **個人情報(氏名・所属)はログ出力禁止**

### CloudWatch Logs Insights 定型クエリ(保存しておく)

```
# エラー一覧(直近1時間)
fields @timestamp, errorCode, path, requestId
| filter level = "ERROR" | sort @timestamp desc

# 遅いAPI Top10
fields path, durationMs
| filter durationMs > 1000 | sort durationMs desc | limit 10
```

---

## 3. メトリクスとアラーム

| アラーム | 条件 | 通知 |
|---------|------|------|
| EC2 CPU高負荷 | CPUUtilization > 80% が5分継続 | メール(SNS) |
| EC2 ステータス異常 | StatusCheckFailed >= 1 | メール |
| アプリ死活 | ALB HealthyHostCount < 1 | メール |
| 5xx多発 | ALB HTTPCode_Target_5XX_Count > 10/5分 | メール |
| DynamoDBスロットリング | ThrottledRequests > 0 | メール |
| メモリ逼迫 | mem_used_percent > 85%(CloudWatch Agent) | メール |

- ヘルスチェックエンドポイント: `GET /api/v1/health`(Spring Actuator。公開するのはhealthのみ)

---

## 4. フロントエンド計測

- MVP期は Lighthouse CI 相当の手動計測のみ:
  - リリース前に `npx lighthouse <共有ページURL> --preset=perf` を実行し、Performance 80以上を確認
  - バンドルサイズは `npx vite-bundle-visualizer` で確認(初期JS gzip 300KB以下)
- 商用アクセス解析・RUM(Real User Monitoring)はMVPでは導入しない(個人情報保護方針との整合を検討してから)

---

## 5. 大会当日の運用手順

### 前日まで

- [ ] デプロイ凍結(前日・当日はデプロイしない)
- [ ] CloudWatchアラームが有効なことを確認
- [ ] EC2再起動 + スモークチェック(メモリリセット)
- [ ] 手動運用へのフォールバック手順を印刷しておく(紙の対局カード様式)

### 当日

- CloudWatchダッシュボード(CPU・メモリ・5xx・レイテンシ)をタブで開いておく
- 障害時の一次対応: `sudo systemctl restart swiss-stage`(再起動30秒以内)
- 復旧不能時: 順位表を最後の正常表示からスプレッドシートへ転記し手動運用に切り替え

### 大会後

- Logs Insights でエラー・遅延を確認し、改善点を Issue 化する
