import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

export interface ViaCepResponse {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  localidade: string;
  uf: string;
  erro?: boolean;
}

@Injectable({
  providedIn: "root"
})
export class CepService {
  constructor(private readonly http: HttpClient) {}

  consultarCep(cep: string): Observable<ViaCepResponse> {
    const cepLimpo = cep.replace(/\D/g, "");
    return this.http.get<ViaCepResponse>(`https://viacep.com.br/ws/${cepLimpo}/json/`);
  }
}
