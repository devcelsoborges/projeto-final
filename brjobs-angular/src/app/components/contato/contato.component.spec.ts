import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContatoComponent } from './contato.component';

describe('ContatoComponent', () => {
  let component: ContatoComponent;
  let fixture: ComponentFixture<ContatoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContatoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ContatoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate form fields', () => {
    const form = component.form;
    expect(form.get('nome')?.valid).toBeFalsy();
    expect(form.get('email')?.valid).toBeFalsy();
    expect(form.get('assunto')?.valid).toBeFalsy();
    expect(form.get('mensagem')?.valid).toBeFalsy();
  });

  it('should enable submit button when form is valid', () => {
    component.form.patchValue({
      nome: 'João Silva',
      email: 'joao@example.com',
      assunto: 'Teste de contato',
      mensagem: 'Esta é uma mensagem de teste para validar o formulário',
    });

    expect(component.form.valid).toBeTruthy();
  });

  it('should reset form after submission', () => {
    component.form.patchValue({
      nome: 'João Silva',
      email: 'joao@example.com',
      assunto: 'Teste de contato',
      mensagem: 'Esta é uma mensagem de teste para validar o formulário',
    });

    component.enviarFormulario();
    expect(component.form.get('nome')?.value).toBeNull();
    expect(component.enviado).toBeTruthy();
  });
});
