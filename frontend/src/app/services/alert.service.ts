import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject, timer } from 'rxjs';

export interface FuelPriceAlert {
  id: string;
  fuelType: string;
  fuelLabel: string;
  fuelIcon: string;
  currentPrice: number;
  previousPrice: number;
  changePercent: number;
  direction: 'up' | 'down';
  severity: 'info' | 'warning' | 'critical';
  message: string;
  timestamp: number;
}

@Injectable({ providedIn: 'root' })
export class AlertService implements OnDestroy {

  private ws: WebSocket | null = null;
  private alertSubject = new Subject<FuelPriceAlert>();
  private connectionStatus = new BehaviorSubject<'connected' | 'disconnected' | 'connecting'>('disconnected');
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private alerts: FuelPriceAlert[] = [];
  private maxAlerts = 30;

  alerts$ = this.alertSubject.asObservable();
  connectionStatus$ = this.connectionStatus.asObservable();

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) return;

    this.connectionStatus.next('connecting');
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/alerts`;

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        this.connectionStatus.next('connected');
        this.reconnectAttempts = 0;
      };

      this.ws.onmessage = (event) => {
        try {
          const alert: FuelPriceAlert = JSON.parse(event.data);
          this.alerts.unshift(alert);
          if (this.alerts.length > this.maxAlerts) {
            this.alerts = this.alerts.slice(0, this.maxAlerts);
          }
          this.alertSubject.next(alert);
        } catch (e) {
          // ignore malformed alert
        }
      };

      this.ws.onclose = () => {
        this.connectionStatus.next('disconnected');
        this.scheduleReconnect();
      };

      this.ws.onerror = () => {
        this.ws?.close();
      };
    } catch (e) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return;
    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    setTimeout(() => this.connect(), delay);
  }

  getRecentAlerts(): FuelPriceAlert[] {
    return [...this.alerts];
  }

  disconnect() {
    this.ws?.close();
    this.ws = null;
  }

  ngOnDestroy() {
    this.disconnect();
  }
}
