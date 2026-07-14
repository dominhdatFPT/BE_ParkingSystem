# Test Validation - Xe Vao / Xe Ra
# =================================
# YEU CAU: Dang nhap truoc de lay JWT token, thay the `YOUR_JWT_TOKEN` o duoi
# Chay trong PowerShell hoac Command Prompt

$BASE = "http://localhost:8080"
$TOKEN = "YOUR_JWT_TOKEN"
$AUTH = "Bearer $TOKEN"

Write-Host "=== PARKING ENTRY - VALIDATION TESTS ===" -ForegroundColor Cyan

# --- CHECK: Hop le ---
Write-Host "`n[1] POST /parking-entry/check - HOP LE" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/check" -Method POST `
  -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
  -Body '{"licensePlate":"51A-123.45","vehicleType":"CAR"}' | ConvertTo-Json -Depth 5

# --- CHECK: Blank licensePlate ---
Write-Host "`n[2] POST /parking-entry/check - BLANK licensePlate (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{"licensePlate":"","vehicleType":"CAR"}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# --- CHECK: Null licensePlate ---
Write-Host "`n[3] POST /parking-entry/check - NULL licensePlate (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{"vehicleType":"CAR"}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# --- CHECK: licensePlate qua dai ---
Write-Host "`n[4] POST /parking-entry/check - licensePlate > 20 ky tu (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{"licensePlate":"AAAA111111111111111111111","vehicleType":"CAR"}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# --- CHECK: Empty body ---
Write-Host "`n[5] POST /parking-entry/check - EMPTY BODY (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# --- CONFIRM: Hop le ---
Write-Host "`n[6] POST /parking-entry/confirm - HOP LE" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/confirm" -Method POST `
  -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
  -Body '{"licensePlate":"51A-123.45","vehicleType":"CAR","visitorCardCode":"CAR001"}' | ConvertTo-Json -Depth 5

# --- CONFIRM: Blank licensePlate ---
Write-Host "`n[7] POST /parking-entry/confirm - BLANK licensePlate (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-entry/confirm" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{"licensePlate":"","vehicleType":"CAR"}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

Write-Host "`n=== PARKING EXIT - VALIDATION TESTS ===" -ForegroundColor Cyan

# --- CHECK: Hop le ---
Write-Host "`n[8] POST /parking-exit/check - HOP LE" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE/api/v1/parking-exit/check" -Method POST `
  -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
  -Body '{"licensePlate":"51A-123.45"}' | ConvertTo-Json -Depth 5

# --- CHECK: Blank licensePlate ---
Write-Host "`n[9] POST /parking-exit/check - BLANK licensePlate (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-exit/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{"licensePlate":""}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# --- CHECK: Null licensePlate ---
Write-Host "`n[10] POST /parking-exit/check - NULL licensePlate (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-exit/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# --- CHECK: licensePlate qua dai ---
Write-Host "`n[11] POST /parking-exit/check - licensePlate > 20 ky tu (expect 400)" -ForegroundColor Yellow
try {
  Invoke-RestMethod -Uri "$BASE/api/v1/parking-exit/check" -Method POST `
    -Headers @{Authorization=$AUTH; "Content-Type"="application/json"} `
    -Body '{"licensePlate":"AAAA111111111111111111111"}' | ConvertTo-Json -Depth 5
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

Write-Host "`n=== DONE ===" -ForegroundColor Cyan
