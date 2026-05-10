# ============================================================================
# E2E Agent + RAG 流程测试脚本
# ============================================================================
$BASE_URL = "http://localhost:8080/api"

Write-Host "===== 1. 登录 ====="
$loginResp = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"user1","password":"123456"}'
$token = $loginResp.data.token
$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "`n===== 2. Agent 对话 - 政策问答 ====="
$chatResp = Invoke-RestMethod -Uri "$BASE_URL/agent/chat" -Method Post -Headers $headers -ContentType "application/json" -Body '{
    "conversationId":"test-c1",
    "userInput":"签收超过7天还能退货退款吗？"
}'
$chatResp | ConvertTo-Json -Depth 3
$traceId = $chatResp.data.traceId

Write-Host "`n===== 3. 查询 Agent Trace ====="
Invoke-RestMethod -Uri "$BASE_URL/agent/traces/$traceId" -Headers $headers | ConvertTo-Json -Depth 3

Write-Host "`n===== 4. Agent 对话 - 售后资格判断 ====="
Invoke-RestMethod -Uri "$BASE_URL/agent/chat" -Method Post -Headers $headers -ContentType "application/json" -Body '{
    "conversationId":"test-c1",
    "userInput":"我的订单O202605010001能不能退货退款？",
    "orderNo":"O202605010001"
}' | ConvertTo-Json -Depth 3

Write-Host "`n===== 5. RAG 检索测试 ====="
Invoke-RestMethod -Uri "$BASE_URL/rag/search" -Method Post -Headers $headers -ContentType "application/json" -Body '{
    "query":"签收超过7天还能退货退款吗？",
    "topK":5,
    "filters":{"docType":"POLICY"}
}' | ConvertTo-Json -Depth 3

Write-Host "`n===== 6. RAG 评估 ====="
Invoke-RestMethod -Uri "$BASE_URL/rag/evaluate" -Method Post -Headers $headers | ConvertTo-Json -Depth 2

Write-Host "`n===== 7. 知识文档列表 ====="
Invoke-RestMethod -Uri "$BASE_URL/knowledge/docs" -Headers $headers | ConvertTo-Json -Depth 2

Write-Host "`n===== 8. Dashboard 总览 ====="
Invoke-RestMethod -Uri "$BASE_URL/dashboard/overview" -Headers $headers | ConvertTo-Json

Write-Host "`n===== E2E Agent/RAG 流程完成！====="
