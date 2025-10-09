import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule],
  // Referencia o HTML e o CSS em arquivos separados
  templateUrl: './about.component.html', 
  styleUrls: ['./about.component.css']
})
export class SobreComponent { 
  // Nenhuma lógica específica é necessária para este componente de página
}
