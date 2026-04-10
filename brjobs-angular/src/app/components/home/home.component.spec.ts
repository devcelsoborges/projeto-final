// src/app/components/home/home.component.spec.ts

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { HomeComponent } from './home.component';
import { PublicacaoServicoService } from '../../service/publicacao-servico.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    const publicacaoServiceMock = {
      listar: jasmine.createSpy('listar').and.returnValue(of([]))
    };

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: PublicacaoServicoService, useValue: publicacaoServiceMock }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should display the title in the header', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Encontre prestação e contratação de serviços no BR-Jobs');
  });

  it('should render search controls including filter and buscar button', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.search-bar input')).toBeTruthy();
    expect(compiled.querySelector('.search-bar select')).toBeTruthy();
    expect(compiled.querySelector('.search-bar button')?.textContent).toContain('Buscar');
  });
});