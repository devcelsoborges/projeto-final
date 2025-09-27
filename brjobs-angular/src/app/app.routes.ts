// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
export const routes: Routes = [
  // Rota raiz (URL: /)
  { path: '', component: HomeComponent },
  
  // Rota de login (URL: /login)
  { path: 'login', component: LoginComponent },
  
  // Opcional: Rota curinga para páginas não encontradas (404)
  { path: '**', redirectTo: '' }
];
