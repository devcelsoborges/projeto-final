// src/app/components/login/login.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // ⬅️ IMPORTANTE para usar ngModel

@Component({
  selector: 'app-login',
  standalone: true,
  // Adiciona FormsModule para formulários de template-driven
  imports: [CommonModule, FormsModule], 
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  
  // Objeto para armazenar os dados do formulário
  credenciais = {
    email: '',
    senha: ''
  };

  /**
   * Método chamado quando o formulário é submetido
   */
  onSubmit() {
    console.log('Tentativa de Login:', this.credenciais);
    
    // Simulação básica de validação (apenas para demonstração)
    if (this.credenciais.email && this.credenciais.senha.length >= 6) {
      alert('Login bem-sucedido (simulado)!');
      // Em uma aplicação real, aqui você chamaria um Service de autenticação
    } else {
      alert('Por favor, preencha o email e a senha (mínimo 6 caracteres).');
    }
  }
}