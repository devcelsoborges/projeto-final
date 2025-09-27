// src/app/components/home/home.component.spec.ts

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent] // Importa o próprio componente por ser standalone
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  // Novo teste: Verificar se o título do componente é exibido
  it('should display the title in the header', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Encontre a Vaga Perfeita no BR-Jobs');
  });

  // Novo teste: Verificar se 4 vagas foram criadas
  it('should have 4 job listings', () => {
    expect(component.vagas.length).toBe(4);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.card-vaga').length).toBe(4);
  });
});