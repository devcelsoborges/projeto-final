import '../../../shared/utils/json_utils.dart';

/// Payload de cadastro de contratante (`CadastroContratanteDTO`).
class CadastroContratante {
  CadastroContratante({
    required this.nome,
    required this.email,
    required this.senha,
    required this.confirmacaoSenha,
    this.telefone,
    this.dataNascimento,
    this.cpf,
    this.genero,
    this.endereco,
    this.cep,
    this.rua,
    this.bairro,
    this.cidade,
    this.estado,
    this.numero,
    this.complemento,
    this.bio,
  });

  final String nome;
  final String email;
  final String senha;
  final String confirmacaoSenha;
  final String? telefone;
  final DateTime? dataNascimento;
  final String? cpf;
  final String? genero;
  final String? endereco;
  final String? cep;
  final String? rua;
  final String? bairro;
  final String? cidade;
  final String? estado;
  final String? numero;
  final String? complemento;
  final String? bio;

  Map<String, dynamic> toJson() => pruneNulls({
        'nome': nome,
        'email': email,
        'senha': senha,
        'confirmacaoSenha': confirmacaoSenha,
        'telefone': telefone,
        'dataNascimento': toLocalDate(dataNascimento),
        'cpf': cpf,
        'genero': genero,
        'endereco': endereco,
        'cep': cep,
        'rua': rua,
        'bairro': bairro,
        'cidade': cidade,
        'estado': estado,
        'numero': numero,
        'complemento': complemento,
        'bio': bio,
      });
}

/// Payload de cadastro de prestador (`CadastroPrestadorDTO`).
class CadastroPrestador {
  CadastroPrestador({
    required this.base,
    this.funcao,
    this.experienciaProfissional,
    this.especialidades,
  });

  final CadastroContratante base;
  final String? funcao;
  final String? experienciaProfissional;
  final String? especialidades;

  Map<String, dynamic> toJson() => pruneNulls({
        ...base.toJson(),
        'funcao': funcao,
        'experienciaProfissional': experienciaProfissional,
        'especialidades': especialidades,
      });
}
