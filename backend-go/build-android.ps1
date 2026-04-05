# Build the Go backend as Android-runnable binaries.
# Run from the backend-go directory: cd backend-go; .\build-android.ps1
#
# Requirements: Go 1.21+ (CGO_ENABLED=0, pure Go)

$ErrorActionPreference = "Stop"
$outDir = "..\app\src\main\jniLibs"

$env:CGO_ENABLED = "0"
$env:GOOS        = "linux"

# ── arm64-v8a (most modern devices) ──────────────────────────────────────────
$env:GOARCH = "arm64"
Remove-Item Env:GOARM -ErrorAction SilentlyContinue
Write-Host "Building arm64-v8a..."
go build -ldflags="-s -w" -o "$outDir\arm64-v8a\libbackend.so" .
Write-Host "  -> $outDir\arm64-v8a\libbackend.so"

# ── armeabi-v7a (older 32-bit devices) ───────────────────────────────────────
$env:GOARCH = "arm"
$env:GOARM  = "7"
Write-Host "Building armeabi-v7a..."
go build -ldflags="-s -w" -o "$outDir\armeabi-v7a\libbackend.so" .
Write-Host "  -> $outDir\armeabi-v7a\libbackend.so"

Write-Host ""
Write-Host "Done. Now build & install the APK in Android Studio."
