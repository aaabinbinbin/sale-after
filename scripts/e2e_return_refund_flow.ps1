# ============================================================================
# E2E 退货退款完整流程测试脚本
# ============================================================================
$BASE_URL = "http://localhost:8080/api"

Write-Host "===== 1. 用户登录 ====="
$loginResp = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"user1","password":"123456"}'
$token = $loginResp.data.token
$headers = @{ "Authorization" = "Bearer $token" }
Write-Host "Token: $token"

Write-Host "`n===== 2. 查询订单列表 ====="
Invoke-RestMethod -Uri "$BASE_URL/orders?pageNum=1&pageSize=5" -Headers $headers | ConvertTo-Json -Depth 3

Write-Host "`n===== 3. 查询订单详情 O202605010001 ====="
Invoke-RestMethod -Uri "$BASE_URL/orders/O202605010001" -Headers $headers | ConvertTo-Json -Depth 4

Write-Host "`n===== 4. 创建退货退款售后申请 ====="
$idempotencyKey = [guid]::NewGuid().ToString()
$createResp = Invoke-RestMethod -Uri "$BASE_URL/after-sales/applications" -Method Post -Headers @{
    "Authorization" = "Bearer $token"
    "Idempotency-Key" = $idempotencyKey
} -ContentType "application/json" -Body '{
    "orderNo":"O202605010001",
    "afterSalesType":"RETURN_REFUND",
    "reasonCode":"QUALITY_PROBLEM",
    "reasonText":"商品有明显破损",
    "items":[{"orderItemId":10001,"applyQuantity":1,"applyAmount":199.00}],
    "remark":"已拍照上传凭证"
}'
$afterSalesNo = $createResp.data.afterSalesNo
Write-Host "售后单号: $afterSalesNo"

Write-Host "`n===== 5. 查询售后详情 ====="
Invoke-RestMethod -Uri "$BASE_URL/after-sales/$afterSalesNo" -Headers $headers | ConvertTo-Json -Depth 4

Write-Host "`n===== 6. 登录客服 ====="
$csResp = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"cs_agent1","password":"123456"}'
$csToken = $csResp.data.token
$csHeaders = @{ "Authorization" = "Bearer $csToken" }
Write-Host "客服 Token: $csToken"

Write-Host "`n===== 7. 售后审核通过 ====="
Invoke-RestMethod -Uri "$BASE_URL/after-sales/$afterSalesNo/review/approve" -Method Post -Headers $csHeaders -ContentType "application/json" -Body '{
    "approvedAmount":199.00,
    "approvedItems":[{"orderItemId":10001,"approvedQuantity":1,"approvedAmount":199.00}],
    "reviewRemark":"符合售后政策，审核通过",
    "version":0
}' | ConvertTo-Json

Write-Host "`n===== 8. 用户填写退货物流 ====="
Invoke-RestMethod -Uri "$BASE_URL/after-sales/$afterSalesNo/return/shipment" -Method Post -Headers $headers -ContentType "application/json" -Body '{
    "logisticsCompany":"顺丰速运",
    "logisticsNo":"SF1234567890"
}'

Write-Host "`n===== 9. 客服确认收货 ====="
Invoke-RestMethod -Uri "$BASE_URL/after-sales/$afterSalesNo/return/receive" -Method Post -Headers $csHeaders -ContentType "application/json" -Body '{
    "receiverRemark":"商品已收到，符合退货要求",
    "version":1
}'

Write-Host "`n===== 10. 执行退款 ====="
$refundKey = [guid]::NewGuid().ToString()
Invoke-RestMethod -Uri "$BASE_URL/after-sales/$afterSalesNo/refund/execute" -Method Post -Headers @{
    "Authorization" = "Bearer $csToken"
    "Idempotency-Key" = $refundKey
} -ContentType "application/json" -Body '{"refundAmount":199.00,"version":2}' | ConvertTo-Json

Write-Host "`n===== 11. 查询操作日志 ====="
Invoke-RestMethod -Uri "$BASE_URL/after-sales/$afterSalesNo" -Headers $headers | Select-Object -ExpandProperty data | Select-Object -ExpandProperty operationLogs | ConvertTo-Json

Write-Host "`n===== E2E 退货退款流程完成！====="
