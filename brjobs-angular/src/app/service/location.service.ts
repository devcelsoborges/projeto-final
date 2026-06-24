import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject, Observable, of } from "rxjs";
import { tap } from "rxjs/operators";
import { environment } from "../environments/environment";

export interface UserLocation {
  lat: number;
  lng: number;
  source: "browser" | "manual";
}

export interface GeocodeResponse {
  lat: number;
  lng: number;
  source: string;
  precision?: string;
}

@Injectable({
  providedIn: "root"
})
export class LocationService {
  private readonly geocodeUrl = `${environment.apiUrl}/api/v1/geocode`;
  private readonly storageKey = "brjobs_user_location";
  private locationSubject = new BehaviorSubject<UserLocation | null>(this.readStoredLocation());

  readonly location$ = this.locationSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get currentLocation(): UserLocation | null {
    return this.locationSubject.value;
  }

  requestBrowserLocation(): Promise<UserLocation> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("Geolocalização não está disponível neste navegador."));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location: UserLocation = {
            lat: position.coords.latitude,
            lng: position.coords.longitude,
            source: "browser"
          };
          this.setLocation(location);
          resolve(location);
        },
        (err) => {
          let msg = "Não foi possível obter sua localização.";
          if (err.code === err.PERMISSION_DENIED) {
            msg = "Permissão de localização negada. Habilite o acesso no navegador (ícone de cadeado na barra de endereço) e tente novamente.";
          } else if (err.code === err.TIMEOUT) {
            msg = "Tempo esgotado ao obter sua localização. Tente novamente.";
          } else if (err.code === err.POSITION_UNAVAILABLE) {
            msg = "Localização indisponível no momento. Tente novamente em instantes.";
          }
          reject(new Error(msg));
        },
        {
          enableHighAccuracy: false,
          timeout: 10000,
          maximumAge: 300000
        }
      );
    });
  }

  geocodeManualAddress(payload: {
    endereco: string;
    cidade?: string;
    estado?: string;
    cep?: string;
  }): Observable<GeocodeResponse> {
    if (!payload.endereco?.trim()) {
      return of(null as unknown as GeocodeResponse);
    }

    return this.http.post<GeocodeResponse>(this.geocodeUrl, payload).pipe(
      tap((response) => {
        if (response?.lat != null && response?.lng != null) {
          this.setLocation({
            lat: response.lat,
            lng: response.lng,
            source: "manual"
          });
        }
      })
    );
  }

  setLocation(location: UserLocation): void {
    this.locationSubject.next(location);
    localStorage.setItem(this.storageKey, JSON.stringify(location));
  }

  clearLocation(): void {
    this.locationSubject.next(null);
    localStorage.removeItem(this.storageKey);
  }

  private readStoredLocation(): UserLocation | null {
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw) as UserLocation;
      if (Number.isFinite(parsed.lat) && Number.isFinite(parsed.lng)) {
        return parsed;
      }
      return null;
    } catch {
      return null;
    }
  }
}
