import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sobre',
  standalone: true,
  imports: [CommonModule],
  // Referencia o HTML e o CSS em arquivos separados
  templateUrl: './sobre.component.html', 
  styleUrls: ['./sobre.component.css']
})
export class SobreComponent { 
  // Nenhuma lógica específica é necessária para este componente de página
}
