import '../../../shared/utils/json_utils.dart';
import 'tipo_usuario.dart';

/// Espelha `UsuarioDTO` do backend.
class Usuario {
  const Usuario({
    required this.id,
    this.tipoUsuario,
    required this.nome,
    required this.email,
    this.telefone,
    this.endereco,
    this.cep,
    this.rua,
    this.bairro,
    this.cidade,
    this.estado,
    this.numero,
    this.complemento,
    this.bio,
    this.cpf,
    this.genero,
    this.dataNascimento,
    this.dataCadastro,
    this.ativo = true,
    this.fotoPerfilUrl,
  });

  final int id;
  final TipoUsuario? tipoUsuario;
  final String nome;
  final String email;
  final String? telefone;
  final String? endereco;
  final String? cep;
  final String? rua;
  final String? bairro;
  final String? cidade;
  final String? estado;
  final String? numero;
  final String? complemento;
  final String? bio;
  final String? cpf;
  final String? genero;
  final DateTime? dataNascimento;
  final DateTime? dataCadastro;
  final bool ativo;

  /// Não faz parte do `UsuarioDTO`, mas o login social devolve `fotoPerfil`.
  final String? fotoPerfilUrl;

  bool get isPrestador => tipoUsuario?.isPrestador ?? false;

  factory Usuario.fromJson(Map<String, dynamic> json) {
    return Usuario(
      id: asInt(json['id']) ?? 0,
      tipoUsuario: TipoUsuario.fromWire(json['tipoUsuario']?.toString()),
      nome: json['nome']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      telefone: json['telefone']?.toString(),
      endereco: json['endereco']?.toString(),
      cep: json['cep']?.toString(),
      rua: json['rua']?.toString(),
      bairro: json['bairro']?.toString(),
      cidade: json['cidade']?.toString(),
      estado: json['estado']?.toString(),
      numero: json['numero']?.toString(),
      complemento: json['complemento']?.toString(),
      bio: json['bio']?.toString(),
      cpf: json['cpf']?.toString(),
      genero: json['genero']?.toString(),
      dataNascimento: asDate(json['dataNascimento']),
      dataCadastro: asDate(json['dataCadastro']),
      ativo: asBool(json['ativo'], fallback: true),
      fotoPerfilUrl:
          json['fotoPerfil']?.toString() ?? json['fotoPerfilUrl']?.toString(),
    );
  }

  Map<String, dynamic> toJson() => pruneNulls({
        'id': id,
        'tipoUsuario': tipoUsuario?.wire,
        'nome': nome,
        'email': email,
        'telefone': telefone,
        'endereco': endereco,
        'cep': cep,
        'rua': rua,
        'bairro': bairro,
        'cidade': cidade,
        'estado': estado,
        'numero': numero,
        'complemento': complemento,
        'bio': bio,
        'cpf': cpf,
        'genero': genero,
        'dataNascimento': toLocalDate(dataNascimento),
        'ativo': ativo,
      });

  Usuario copyWith({
    String? nome,
    String? telefone,
    String? endereco,
    String? cep,
    String? rua,
    String? bairro,
    String? cidade,
    String? estado,
    String? numero,
    String? complemento,
    String? bio,
    String? genero,
    DateTime? dataNascimento,
    String? fotoPerfilUrl,
  }) {
    return Usuario(
      id: id,
      tipoUsuario: tipoUsuario,
      nome: nome ?? this.nome,
      email: email,
      telefone: telefone ?? this.telefone,
      endereco: endereco ?? this.endereco,
      cep: cep ?? this.cep,
      rua: rua ?? this.rua,
      bairro: bairro ?? this.bairro,
      cidade: cidade ?? this.cidade,
      estado: estado ?? this.estado,
      numero: numero ?? this.numero,
      complemento: complemento ?? this.complemento,
      bio: bio ?? this.bio,
      cpf: cpf,
      genero: genero ?? this.genero,
      dataNascimento: dataNascimento ?? this.dataNascimento,
      dataCadastro: dataCadastro,
      ativo: ativo,
      fotoPerfilUrl: fotoPerfilUrl ?? this.fotoPerfilUrl,
    );
  }
}
