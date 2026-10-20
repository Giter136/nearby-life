# ---------------------------------------------------------------------------
# 本机构建 + 推送到镜像仓库（TCR / 任意 registry）
#
#   .\deploy\release.ps1 -Registry ccr.ccs.tencentyun.com/<命名空间> -Tag v1.0.0
#
# 推送前需先登录（用户名是腾讯云账号 ID，密码是 TCR 个人版的仓库密码）：
#   docker login ccr.ccs.tencentyun.com --username=1000xxxxxxx
# ---------------------------------------------------------------------------
param(
    [Parameter(Mandatory = $true)]
    [string]$Registry,          # 例：ccr.ccs.tencentyun.com/my-namespace

    [string]$Tag = "latest"     # 建议用版本号或时间戳，方便回滚
)

# 注意：不能用 Stop。PowerShell 5.1 下 docker 把进度写到 stderr，
# 开着 Stop 会把正常输出当成 NativeCommandError 抛出来（假失败）。
$ErrorActionPreference = "Continue"

if ($Registry -notmatch '/') {
    throw "Registry 需要包含命名空间，例如 ccr.ccs.tencentyun.com/my-namespace"
}

Write-Host "==> 目标: $Registry/hmdp-backend:$Tag 与 $Registry/hmdp-web:$Tag" -ForegroundColor Cyan

# compose 通过这两个变量决定镜像名
$env:IMAGE_REGISTRY = $Registry
$env:TAG = $Tag

Write-Host "`n==> [1/2] 构建" -ForegroundColor Cyan
docker compose build
if ($LASTEXITCODE -ne 0) { throw "构建失败" }

Write-Host "`n==> [2/2] 推送" -ForegroundColor Cyan
docker compose push
if ($LASTEXITCODE -ne 0) { throw "推送失败（常见原因：未 docker login，或命名空间/仓库名不存在）" }

Write-Host "`n完成。服务器上执行：" -ForegroundColor Green
Write-Host "  cd /opt/hmdp-compose"
Write-Host "  # 确认 .env 里 IMAGE_REGISTRY / TAG 与本次一致"
Write-Host "  docker compose pull && docker compose up -d --no-build"

# 显式 exit 0：否则 PowerShell 会因最后一条原生命令写过 stderr 而返回 1
exit 0
