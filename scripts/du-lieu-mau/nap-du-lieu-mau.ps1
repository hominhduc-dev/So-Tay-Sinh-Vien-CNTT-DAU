# Nạp dữ liệu mẫu vào hệ thống đang chạy (docker compose up: db + server ở cổng 9090).
#   1. TaoTaiLieuMau.java: tạo 3 PDF mẫu và tải lên qua TCP socket bằng tài khoản GV001.
#   2. du-lieu-mau.sql: thêm tài khoản, chủ đề, câu hỏi và lịch sử hội thoại.
# Chạy lại nhiều lần không sinh bản trùng.
#
#   powershell -File scripts\du-lieu-mau\nap-du-lieu-mau.ps1 [-ServerHost localhost] [-Port 9090]
param(
    [string]$ServerHost = 'localhost',
    [int]$Port = 9090
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$repo = Resolve-Path "$here\..\.."
$m2 = "$env:USERPROFILE\.m2\repository"

# JDK ≥ 21 (chạy tệp .java trực tiếp, không cần biên dịch)
$java = $null
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) { $java = "$env:JAVA_HOME\bin\java.exe" }
if (-not $java) {
    $jdk = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '(\d+)\.' -and [int]$Matches[1] -ge 21 } | Select-Object -First 1
    if ($jdk) { $java = "$($jdk.FullName)\bin\java.exe" }
}
if (-not $java) { throw 'Không tìm thấy JDK 21 trở lên. Đặt JAVA_HOME rồi chạy lại.' }

$jars = @(
    "org\apache\pdfbox\pdfbox\3.0.3\pdfbox-3.0.3.jar",
    "org\apache\pdfbox\pdfbox-io\3.0.3\pdfbox-io-3.0.3.jar",
    "org\apache\pdfbox\fontbox\3.0.3\fontbox-3.0.3.jar",
    "commons-logging\commons-logging\1.3.3\commons-logging-1.3.3.jar",
    "com\fasterxml\jackson\core\jackson-databind\2.17.2\jackson-databind-2.17.2.jar",
    "com\fasterxml\jackson\core\jackson-core\2.17.2\jackson-core-2.17.2.jar",
    "com\fasterxml\jackson\core\jackson-annotations\2.17.2\jackson-annotations-2.17.2.jar"
) | ForEach-Object { Join-Path $m2 $_ }
$missing = $jars | Where-Object { -not (Test-Path $_) }
if ($missing) { throw "Thiếu thư viện trong ~/.m2 — build server và client bằng Maven một lần trước:`n$($missing -join "`n")" }

[Console]::OutputEncoding = [Text.Encoding]::UTF8
$cp = $jars -join ';'
$out = Join-Path ([IO.Path]::GetTempPath()) 'tangthu-du-lieu-mau'
New-Item -ItemType Directory -Force $out | Out-Null

Write-Host '== Tải tài liệu mẫu lên server' -ForegroundColor Cyan
& (Join-Path (Split-Path $java) 'javac.exe') -encoding UTF-8 -nowarn -cp $cp -d $out "$here\TaoTaiLieuMau.java"
if ($LASTEXITCODE -ne 0) { throw 'Biên dịch TaoTaiLieuMau.java thất bại.' }
& $java '-Dstdout.encoding=UTF-8' -cp "$out;$cp" TaoTaiLieuMau $ServerHost $Port
if ($LASTEXITCODE -ne 0) { throw 'Tải tài liệu thất bại.' }

Write-Host '== Nạp tài khoản, chủ đề, câu hỏi, hội thoại' -ForegroundColor Cyan
Push-Location $repo
try {
    docker compose cp "$here\du-lieu-mau.sql" db:/tmp/du-lieu-mau.sql
    docker compose exec -T db psql -U tangthu -d tangthu -v ON_ERROR_STOP=1 -q -f /tmp/du-lieu-mau.sql
    if ($LASTEXITCODE -ne 0) { throw 'Nạp SQL thất bại.' }
} finally {
    Pop-Location
}
Write-Host 'Xong. Mật khẩu mọi tài khoản mẫu: Matkhau@123' -ForegroundColor Green
