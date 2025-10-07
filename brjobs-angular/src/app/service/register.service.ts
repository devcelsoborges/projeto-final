import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RegisterService {
  private baseUrl = 'http://localhost:8080/api/usuarios'; // endpoint Spring Boot

  constructor(private http: HttpClient) {}

  cadastrarUsuario(formData: FormData): Observable<any> {
    return this.http.post(this.baseUrl, formData);
  }
}
