import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { SobreComponent } from './components/about/sobre.component';
import { ContatoComponent } from './components/contato/contato.component';
import { ProfileComponent } from './components/profile/profile.component';
import { PerfilPublicoComponent } from './components/perfil-publico/perfil-publico.component';
import { PublicacoesComponent } from './components/publicacoes/publicacoes.component';
import { PublicacaoDetalheComponent } from './components/publicacoes/publicacao-detalhe.component';
import { ChatComponent } from './components/chat/chat.component';
import { ForgotPasswordComponent } from './components/forgot-password.component/forgot-password.component';
//import { AcessibilidadeComponent } from './components/acessibilidade/acessibilidade.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'perfil/:usuarioId', component: PerfilPublicoComponent },
  { path: 'publicacoes', component: PublicacoesComponent },
  { path: 'publicacoes/:id', component: PublicacaoDetalheComponent },
  { path: 'chat', component: ChatComponent },
  { path: 'sobre', component: SobreComponent },
  { path: 'contato', component: ContatoComponent },
  // { path: 'acessibilidade', component: AcessibilidadeComponent },
  //{ path: '**', redirectTo: '' }
];
