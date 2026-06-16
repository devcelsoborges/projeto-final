import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';

export interface TelemetryPayload {
  [key: string]: string | number | boolean | null | undefined;
}

@Injectable({
  providedIn: 'root'
})
export class UxTelemetryService {
  private readonly enabled = environment.uxTelemetryEnabled ?? true;

  logEvent(eventName: string, payload: TelemetryPayload = {}): void {
    if (!this.enabled) {
      return;
    }

    // Placeholder for a future analytics provider.
    void eventName;
    void payload;
  }
}
