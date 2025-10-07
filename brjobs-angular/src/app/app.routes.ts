import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
// import { SobreComponent } from './components/sobre/sobre.component';
//import { ContatoComponent } from './components/contato/contato.component';
//import { AcessibilidadeComponent } from './components/acessibilidade/acessibilidade.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'cadastro', component: RegisterComponent },
  // { path: 'sobre', component: SobreComponent },
  // { path: 'contato', component: ContatoComponent },
  // { path: 'acessibilidade', component: AcessibilidadeComponent },
  { path: '**', redirectTo: '' }
];
