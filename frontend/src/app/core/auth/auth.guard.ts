import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs/operators';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Pas de restauration en cours : décision synchrone immédiate
  if (!authService.isRestoringSession()) {
    if (authService.isAuthenticated()) return true;
    router.navigate(['/auth/login']);
    return false;
  }

  // Restauration en cours : attendre qu'elle se termine avant de décider
  return toObservable(authService.isRestoringSession).pipe(
    filter(restoring => !restoring),
    take(1),
    map(() => {
      if (authService.isAuthenticated()) return true;
      router.navigate(['/auth/login']);
      return false;
    })
  );
};
