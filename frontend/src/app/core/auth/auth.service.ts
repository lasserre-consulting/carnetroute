import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { AuthTokens, LoginRequest, RegisterRequest, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _currentUser = signal<User | null>(null);
  private readonly _isLoading = signal(false);
  private readonly _isRestoringSession = signal(false);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null || this._isRestoringSession());
  readonly isLoading = this._isLoading.asReadonly();
  readonly isRestoringSession = this._isRestoringSession.asReadonly();

  constructor() {
    const token = localStorage.getItem('accessToken');
    if (token) {
      this._isRestoringSession.set(true);
      this.loadProfile().subscribe({
        next: () => { this._isRestoringSession.set(false); },
        error: (err: HttpErrorResponse) => {
          this._isRestoringSession.set(false);
          if (err.status === 401 || err.status === 404) {
            this.clearSession();
          }
        }
      });
    }
  }

  register(request: RegisterRequest) {
    this._isLoading.set(true);
    return this.http.post<AuthTokens>('/api/auth/register', request).pipe(
      tap(tokens => { this.saveTokens(tokens); this._isLoading.set(false); })
    );
  }

  login(request: LoginRequest) {
    this._isLoading.set(true);
    return this.http.post<AuthTokens>('/api/auth/login', request).pipe(
      tap(tokens => { this.saveTokens(tokens); this._isLoading.set(false); })
    );
  }

  loadProfile() {
    return this.http.get<User>('/api/auth/me').pipe(
      tap(user => this._currentUser.set(user))
    );
  }

  logout() {
    this.clearSession();
    this.router.navigate(['/auth/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  private saveTokens(tokens: AuthTokens) {
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    this.loadProfile().subscribe();
  }

  private clearSession() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this._currentUser.set(null);
  }
}
