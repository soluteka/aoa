param(
    [string]$TargetHost = "127.0.0.1",
    [int]$Port          = 5140,
    [int]$Count         = 1,
    [int]$DelayMs       = 500
)

$events = @(
    "<27>{0} aix-prod-01 errpt: AA8AB241 {1} P H hdisk0 DISK OPERATION ERROR",
    "<27>{0} aix-prod-01 errpt: 9DBCFDEE {1} T S SYSPROC SOFTWARE PROGRAM ERROR",
    "<27>{0} aix-prod-01 errpt: F7DDA124 {1} P H mem0 MEMORY FAILURE PREDICTED",
    "<28>{0} aix-prod-01 errpt: 192AC071 {1} I O SYSPFS FILE SYSTEM RECOVERY",
    "<27>{0} aix-prod-01 errpt: BFE4C025 {1} P H ent0 ETHERNET DOWN",
    "<27>{0} aix-prod-01 errpt: 369D049B {1} T S SRC SUBSYSTEM CRASH"
)

function Send-SyslogMessage {
    param([string]$Message, [string]$DestHost, [int]$DestPort)

    $udpClient = New-Object System.Net.Sockets.UdpClient
    try {
        $bytes = [System.Text.Encoding]::ASCII.GetBytes($Message)
        [void]$udpClient.Send($bytes, $bytes.Length, $DestHost, $DestPort)
        Write-Host "[OK] Sent: $Message" -ForegroundColor Green
    }
    catch {
        Write-Host "[ERR] $_" -ForegroundColor Red
    }
    finally {
        $udpClient.Close()
    }
}

Write-Host "Sending $Count round(s) of errpt events to ${TargetHost}:${Port}..." -ForegroundColor Cyan

for ($i = 1; $i -le $Count; $i++) {
    foreach ($template in $events) {
        $timestamp  = (Get-Date).ToString("MMM dd HH:mm:ss")
        $errptStamp = (Get-Date).ToString("MMddHHmmyy")
        $message    = [string]::Format($template, $timestamp, $errptStamp)

        Send-SyslogMessage -Message $message -DestHost $TargetHost -DestPort $Port
        Start-Sleep -Milliseconds $DelayMs
    }
}

Write-Host ""
Write-Host "Simulation completed." -ForegroundColor Cyan