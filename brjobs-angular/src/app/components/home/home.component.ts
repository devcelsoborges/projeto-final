// src/app/components/home/home.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  titulo = 'Encontre a Vaga Perfeita no BR-Jobs';
  
  // Dados simulados de vagas (Mock Data)
  vagas = [
    { id: 1, titulo: 'Desenvolvedor Frontend (Angular)', empresa: 'TechSolutions', local: 'Remoto', nivel: 'Sênior' },
    { id: 2, titulo: 'Analista de Dados', empresa: 'DataCorp', local: 'São Paulo, SP', nivel: 'Pleno' },
    { id: 3, titulo: 'DevOps Engineer', empresa: 'CloudFlex', local: 'Rio de Janeiro, RJ', nivel: 'Pleno' },
    { id: 4, titulo: 'Designer UX/UI', empresa: 'Creative Minds', local: 'Remoto', nivel: 'Júnior' }
  ];
}