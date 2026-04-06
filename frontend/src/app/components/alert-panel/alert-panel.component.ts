import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AlertService, FuelPriceAlert } from '../../services/alert.service';

@Component({
  selector: 'app-alert-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card alert-card">
      <div class="alert-header">
        <span class="card-title" style="margin-bottom:0">🔔 Alertes prix carburant</span>
        <div class="connection-status">
          <span class="status-dot" [class]="connectionStatus"></span>
          <span class="status-label mono">
            {{ connectionStatus === 'connected' ? 'Live' : connectionStatus === 'connecting' ? '...' : 'Hors ligne' }}
          </span>
        </div>
      </div>

      <div *ngIf="alerts.length === 0" class="empty-alerts">
        <span class="empty-icon">📊</span>
        <span class="empty-text">En attente d'alertes Kafka...</span>
      </div>

      <div class="alert-list" *ngIf="alerts.length > 0">
        <div *ngFor="let alert of alerts; trackBy: trackAlert"
          class="alert-item"
          [class.animate-in]="alert === latestAlert"
          [class.severity-info]="alert.severity === 'info'"
          [class.severity-warning]="alert.severity === 'warning'"
          [class.severity-critical]="alert.severity === 'critical'"
        >
          <div class="alert-icon-col">
            <span class="alert-direction" [class.up]="alert.direction === 'up'" [class.down]="alert.direction === 'down'">
              {{ alert.direction === 'up' ? '▲' : '▼' }}
            </span>
          </div>
          <div class="alert-content">
            <div class="alert-main">
              <span class="fuel-icon">{{ alert.fuelIcon }}</span>
              <span class="fuel-label">{{ alert.fuelLabel }}</span>
              <span class="price-value" [style.color]="getPriceColor(alert)">
                {{ alert.currentPrice.toFixed(3) }}€
              </span>
            </div>
            <div class="alert-meta">
              <span class="change-badge" [style.background]="getChangeBg(alert)" [style.color]="getChangeColor(alert)">
                {{ alert.direction === 'up' ? '+' : '' }}{{ alert.changePercent.toFixed(2) }}%
              </span>
              <span class="severity-badge" [class]="'sev-' + alert.severity">{{ alert.severity }}</span>
              <span class="alert-time mono">{{ formatTime(alert.timestamp) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="kafka-footer mono">
        <span>⚡ Kafka topic: carnetroute.fuel.alerts</span>
        <span>{{ alerts.length }} événement{{ alerts.length > 1 ? 's' : '' }}</span>
      </div>
    </div>
  `,
  styles: [`
    .alert-card { max-height: 400px; display: flex; flex-direction: column; }

    .alert-header {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 12px;
    }

    .connection-status { display: flex; align-items: center; gap: 6px; }
    .status-dot {
      width: 8px; height: 8px; border-radius: 50%;
    }
    .status-dot.connected { background: #22C55E; box-shadow: 0 0 6px #22C55E; }
    .status-dot.connecting { background: #EAB308; animation: blink 1s infinite; }
    .status-dot.disconnected { background: #64748B; }
    .status-label { font-size: 10px; color: #94A3B8; }

    @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

    .empty-alerts {
      display: flex; align-items: center; gap: 8px;
      padding: 20px; justify-content: center; color: #64748B; font-size: 13px;
    }
    .empty-icon { font-size: 20px; }

    .alert-list {
      flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px;
      max-height: 280px;
    }

    .alert-item {
      display: flex; gap: 10px; padding: 10px; border-radius: 10px;
      background: #0F172A; border: 1px solid #1E293B;
      transition: all 0.3s;
    }
    .alert-item.animate-in {
      animation: slideIn 0.4s ease;
    }
    .alert-item.severity-warning { border-color: rgba(234,179,8,0.3); }
    .alert-item.severity-critical { border-color: rgba(239,68,68,0.3); background: rgba(239,68,68,0.05); }

    @keyframes slideIn {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .alert-icon-col { display: flex; align-items: center; }
    .alert-direction {
      font-size: 14px; font-weight: 700;
    }
    .alert-direction.up { color: #EF4444; }
    .alert-direction.down { color: #22C55E; }

    .alert-content { flex: 1; min-width: 0; }
    .alert-main {
      display: flex; align-items: center; gap: 6px;
      margin-bottom: 4px;
    }
    .fuel-icon { font-size: 14px; }
    .fuel-label { font-size: 13px; font-weight: 500; }
    .price-value { font-size: 14px; font-weight: 700; margin-left: auto; font-family: 'Space Grotesk', sans-serif; }

    .alert-meta { display: flex; align-items: center; gap: 6px; }
    .change-badge {
      font-size: 10px; padding: 2px 6px; border-radius: 4px;
      font-family: 'JetBrains Mono', monospace; font-weight: 600;
    }
    .severity-badge {
      font-size: 9px; padding: 2px 6px; border-radius: 4px;
      text-transform: uppercase; font-family: 'JetBrains Mono', monospace;
      font-weight: 600; letter-spacing: 0.05em;
    }
    .sev-info { background: rgba(6,182,212,0.15); color: #06B6D4; }
    .sev-warning { background: rgba(234,179,8,0.15); color: #EAB308; }
    .sev-critical { background: rgba(239,68,68,0.15); color: #EF4444; }
    .alert-time { font-size: 10px; color: #475569; margin-left: auto; }

    .kafka-footer {
      display: flex; justify-content: space-between; margin-top: 10px;
      padding-top: 8px; border-top: 1px solid #1E293B;
      font-size: 9px; color: #475569;
    }
  `]
})
export class AlertPanelComponent implements OnInit, OnDestroy {
  alerts: FuelPriceAlert[] = [];
  latestAlert: FuelPriceAlert | null = null;
  connectionStatus: 'connected' | 'disconnected' | 'connecting' = 'disconnected';

  private alertSub?: Subscription;
  private statusSub?: Subscription;

  constructor(private alertService: AlertService) {}

  ngOnInit() {
    this.alertService.connect();

    this.alertSub = this.alertService.alerts$.subscribe(alert => {
      this.alerts.unshift(alert);
      this.latestAlert = alert;
      if (this.alerts.length > 30) {
        this.alerts = this.alerts.slice(0, 30);
      }
      // Reset animation flag
      setTimeout(() => { this.latestAlert = null; }, 500);
    });

    this.statusSub = this.alertService.connectionStatus$.subscribe(status => {
      this.connectionStatus = status;
    });
  }

  ngOnDestroy() {
    this.alertSub?.unsubscribe();
    this.statusSub?.unsubscribe();
    this.alertService.disconnect();
  }

  trackAlert(index: number, alert: FuelPriceAlert): string {
    return alert.id;
  }

  getPriceColor(alert: FuelPriceAlert): string {
    return alert.direction === 'up' ? '#EF4444' : '#22C55E';
  }

  getChangeBg(alert: FuelPriceAlert): string {
    return alert.direction === 'up' ? 'rgba(239,68,68,0.12)' : 'rgba(34,197,94,0.12)';
  }

  getChangeColor(alert: FuelPriceAlert): string {
    return alert.direction === 'up' ? '#EF4444' : '#22C55E';
  }

  formatTime(timestamp: number): string {
    const d = new Date(timestamp);
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }
}
