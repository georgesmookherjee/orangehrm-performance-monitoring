# Diagnostic Avancé cAdvisor - Pourquoi kind_ardinghelli n'est pas détecté
# Usage: .\Advanced-cAdvisor-Debug.ps1

Write-Host "🔍 DIAGNOSTIC AVANCE CADVISOR" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

Write-Host "`n1. VERIFICATION DU SOCKET DOCKER" -ForegroundColor Blue
Write-Host "--------------------------------" -ForegroundColor Blue

# Vérifier que le socket Docker est bien monté
Write-Host "🔌 Vérification du socket Docker dans cAdvisor:"
try {
    $socketTest = docker exec orangehrm-cadvisor ls -la /var/run/docker.sock 2>$null
    if ($socketTest) {
        Write-Host "✅ Socket Docker accessible: $socketTest" -ForegroundColor Green
    } else {
        Write-Host "❌ Socket Docker NON accessible" -ForegroundColor Red
        Write-Host "⚠️ PROBLÈME CRITIQUE: cAdvisor ne peut pas accéder au socket Docker" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Erreur lors du test socket: $_" -ForegroundColor Red
}

Write-Host "`n2. TEST COMMUNICATION DOCKER DEPUIS CADVISOR" -ForegroundColor Blue
Write-Host "--------------------------------------------" -ForegroundColor Blue

# Tester si cAdvisor peut voir les conteneurs Docker
Write-Host "🐳 Test: cAdvisor peut-il lister les conteneurs Docker ?"
try {
    $dockerPS = docker exec orangehrm-cadvisor docker ps --format "{{.Names}}" 2>$null
    if ($dockerPS) {
        Write-Host "✅ cAdvisor peut communiquer avec Docker:" -ForegroundColor Green
        $dockerPS | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }

        # Chercher spécifiquement kind_ardinghelli
        if ($dockerPS -contains "kind_ardinghelli") {
            Write-Host "✅ kind_ardinghelli visible depuis cAdvisor" -ForegroundColor Green
        } else {
            Write-Host "❌ kind_ardinghelli NON visible depuis cAdvisor" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ cAdvisor NE PEUT PAS communiquer avec Docker" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Erreur lors du test Docker: $_" -ForegroundColor Red
}

Write-Host "`n3. ANALYSE DES LOGS CADVISOR" -ForegroundColor Blue
Write-Host "----------------------------" -ForegroundColor Blue

Write-Host "📋 Logs cAdvisor (30 dernières lignes):"
try {
    $logs = docker logs orangehrm-cadvisor --tail 30 2>$null
    if ($logs) {
        $logs | ForEach-Object {
            if ($_ -match "error|Error|ERROR|failed|Failed|FAILED") {
                Write-Host "  🔴 $_" -ForegroundColor Red
            } elseif ($_ -match "warning|Warning|WARN") {
                Write-Host "  🟡 $_" -ForegroundColor Yellow
            } else {
                Write-Host "  $_" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "❌ Impossible de récupérer les logs" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Erreur lors de la récupération des logs: $_" -ForegroundColor Red
}

Write-Host "`n4. VERIFICATION METRIQUES CADVISOR BRUTES" -ForegroundColor Blue
Write-Host "----------------------------------------" -ForegroundColor Blue

Write-Host "📊 Test direct de l'endpoint cAdvisor:"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/metrics" -TimeoutSec 10 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        # Compter toutes les métriques container_*
        $allContainerMetrics = $response.Content -split "`n" | Where-Object { $_ -match "^container_" }
        $totalMetrics = $allContainerMetrics.Count
        Write-Host "📈 Total métriques conteneur: $totalMetrics" -ForegroundColor Cyan

        if ($totalMetrics -eq 0) {
            Write-Host "❌ AUCUNE métrique de conteneur détectée!" -ForegroundColor Red
            Write-Host "⚠️ cAdvisor fonctionne mais ne voit aucun conteneur" -ForegroundColor Yellow
        } else {
            # Extraire les noms de conteneurs uniques
            $containerNames = $response.Content | Select-String 'name="([^"]+)"' | ForEach-Object { $_.Matches[0].Groups[1].Value } | Sort-Object -Unique
            Write-Host "📋 Conteneurs détectés par cAdvisor:" -ForegroundColor Cyan
            $containerNames | ForEach-Object {
                if ($_ -match "kind") {
                    Write-Host "  ✅ $_" -ForegroundColor Green
                } else {
                    Write-Host "  - $_" -ForegroundColor White
                }
            }

            # Chercher spécifiquement kind_ardinghelli
            $kindFound = $containerNames | Where-Object { $_ -match "kind_ardinghelli" }
            if ($kindFound) {
                Write-Host "🎯 TROUVÉ: $kindFound" -ForegroundColor Green
            } else {
                Write-Host "❌ kind_ardinghelli ABSENT des métriques cAdvisor" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "❌ cAdvisor non accessible (HTTP $($response.StatusCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Erreur lors du test endpoint: $_" -ForegroundColor Red
}

Write-Host "`n5. VERIFICATION RESEAUX DOCKER" -ForegroundColor Blue
Write-Host "------------------------------" -ForegroundColor Blue

Write-Host "🌐 Réseaux des conteneurs:"
try {
    # Vérifier le réseau du conteneur OrangeHRM
    $orangeHrmNetwork = docker inspect kind_ardinghelli --format='{{range $net, $conf := .NetworkSettings.Networks}}{{$net}} {{end}}' 2>$null
    Write-Host "🎯 OrangeHRM sur réseau(x): $orangeHrmNetwork" -ForegroundColor Cyan

    # Vérifier le réseau du conteneur cAdvisor
    $cAdvisorNetwork = docker inspect orangehrm-cadvisor --format='{{range $net, $conf := .NetworkSettings.Networks}}{{$net}} {{end}}' 2>$null
    Write-Host "👁️ cAdvisor sur réseau(x): $cAdvisorNetwork" -ForegroundColor Cyan

    # Vérifier s'ils sont sur le même réseau
    if ($orangeHrmNetwork -and $cAdvisorNetwork) {
        $commonNetworks = $orangeHrmNetwork.Split(' ') | Where-Object { $cAdvisorNetwork.Split(' ') -contains $_ }
        if ($commonNetworks) {
            Write-Host "✅ Réseaux communs: $($commonNetworks -join ', ')" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Aucun réseau commun détecté" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ Erreur lors de la vérification réseau: $_" -ForegroundColor Red
}

Write-Host "`n6. CONFIGURATION DETAILLEE CADVISOR" -ForegroundColor Blue
Write-Host "----------------------------------" -ForegroundColor Blue

Write-Host "⚙️ Configuration complète du conteneur cAdvisor:"
try {
    $cAdvisorConfig = docker inspect orangehrm-cadvisor --format='
Image: {{.Config.Image}}
Mounts: {{range .Mounts}}
  {{.Source}} -> {{.Destination}} ({{.Mode}}){{end}}
Command: {{.Config.Cmd}}
Args: {{.Args}}
Privileged: {{.HostConfig.Privileged}}
PID Mode: {{.HostConfig.PidMode}}
Network Mode: {{.HostConfig.NetworkMode}}
' 2>$null
    Write-Host $cAdvisorConfig -ForegroundColor Gray
} catch {
    Write-Host "❌ Erreur lors de l'inspection: $_" -ForegroundColor Red
}

Write-Host "`n7. DIAGNOSTIC FINAL" -ForegroundColor Blue
Write-Host "------------------" -ForegroundColor Blue

Write-Host "🎯 RÉSUMÉ DU PROBLÈME:" -ForegroundColor Cyan

Write-Host "`n🔍 Points à vérifier:" -ForegroundColor Yellow
Write-Host "1. ✅ cAdvisor accessible sur :8080" -ForegroundColor White
Write-Host "2. ❓ Socket Docker monté et accessible ?" -ForegroundColor White
Write-Host "3. ❓ cAdvisor peut communiquer avec Docker ?" -ForegroundColor White
Write-Host "4. ❓ kind_ardinghelli visible depuis cAdvisor ?" -ForegroundColor White
Write-Host "5. ❓ Métriques de conteneurs générées ?" -ForegroundColor White

Write-Host "`n🔧 SOLUTIONS POSSIBLES:" -ForegroundColor Yellow
Write-Host "A. Recréer cAdvisor avec configuration Windows-compatible" -ForegroundColor White
Write-Host "B. Utiliser cAdvisor en mode 'host' network" -ForegroundColor White
Write-Host "C. Vérifier les permissions du socket Docker" -ForegroundColor White
Write-Host "D. Essayer une version différente de cAdvisor" -ForegroundColor White

Write-Host "`n⚡ PROCHAINE ÉTAPE RECOMMANDÉE:" -ForegroundColor Cyan
Write-Host "Recréer cAdvisor avec une configuration Windows-optimisée" -ForegroundColor White

Write-Host "`n✅ Diagnostic avancé terminé!" -ForegroundColor Green