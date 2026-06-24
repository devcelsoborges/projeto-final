import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/router/app_router.dart';
import '../../../shared/utils/validators.dart';
import '../../../shared/widgets/feedback.dart';
import '../data/publicacao_repository.dart';
import '../domain/publicacao.dart';

class PublicarScreen extends ConsumerStatefulWidget {
  const PublicarScreen({super.key});

  @override
  ConsumerState<PublicarScreen> createState() => _PublicarScreenState();
}

class _PublicarScreenState extends ConsumerState<PublicarScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titulo = TextEditingController();
  final _descricao = TextEditingController();
  final _categoria = TextEditingController();
  final _endereco = TextEditingController();
  final _cep = TextEditingController();
  final _cidade = TextEditingController();
  final _estado = TextEditingController();
  final _preco = TextEditingController();
  final _orcamentoMin = TextEditingController();
  final _orcamentoMax = TextEditingController();

  String _tipo = 'PRESTACAO';
  bool _enviando = false;

  bool get _isPrestacao => _tipo == 'PRESTACAO';

  @override
  void dispose() {
    _titulo.dispose();
    _descricao.dispose();
    _categoria.dispose();
    _endereco.dispose();
    _cep.dispose();
    _cidade.dispose();
    _estado.dispose();
    _preco.dispose();
    _orcamentoMin.dispose();
    _orcamentoMax.dispose();
    super.dispose();
  }

  double? _parseMoeda(String texto) {
    final limpo = texto.trim().replaceAll('.', '').replaceAll(',', '.');
    if (limpo.isEmpty) return null;
    return double.tryParse(limpo);
  }

  String? _vazioParaNull(String texto) {
    final t = texto.trim();
    return t.isEmpty ? null : t;
  }

  Future<void> _publicar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _enviando = true);

    // Geocode (latitude/longitude) e destaque pago via Stripe são fase 2;
    // por isso enviamos coordenadas nulas aqui.
    final dto = CriarPublicacao(
      tipoPublicacao: _tipo,
      titulo: _titulo.text.trim(),
      descricao: _descricao.text.trim(),
      categoria: _categoria.text.trim(),
      enderecoPublicacao: _vazioParaNull(_endereco.text),
      cepPublicacao: _vazioParaNull(_cep.text),
      cidadePublicacao: _vazioParaNull(_cidade.text),
      estadoPublicacao: _vazioParaNull(_estado.text),
      preco: _isPrestacao ? _parseMoeda(_preco.text) : null,
      orcamentoMin: _isPrestacao ? null : _parseMoeda(_orcamentoMin.text),
      orcamentoMax: _isPrestacao ? null : _parseMoeda(_orcamentoMax.text),
    );

    try {
      await ref.read(publicacaoRepositoryProvider).criar(dto);
      if (!mounted) return;
      showSuccessSnack(context, 'Publicação criada com sucesso!');
      context.go(Routes.minhasPublicacoes);
    } on ApiException catch (e) {
      if (mounted) showErrorSnack(context, e.message);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nova publicação')),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                'Tipo de publicação',
                style: Theme.of(context).textTheme.labelLarge,
              ),
              const SizedBox(height: 8),
              SegmentedButton<String>(
                showSelectedIcon: false,
                segments: const [
                  ButtonSegment(value: 'PRESTACAO', label: Text('Prestação')),
                  ButtonSegment(value: 'CONTRATACAO', label: Text('Contratação')),
                ],
                selected: {_tipo},
                onSelectionChanged: _enviando
                    ? null
                    : (sel) => setState(() => _tipo = sel.first),
              ),
              const SizedBox(height: 8),
              Text(
                _isPrestacao
                    ? 'Você está oferecendo um serviço.'
                    : 'Você está procurando contratar um serviço.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 20),
              TextFormField(
                controller: _titulo,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: 'Título *',
                  prefixIcon: Icon(Icons.title),
                ),
                validator: (v) => Validators.obrigatorio(v, campo: 'O título'),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descricao,
                minLines: 3,
                maxLines: 6,
                textInputAction: TextInputAction.newline,
                decoration: const InputDecoration(
                  labelText: 'Descrição *',
                  alignLabelWithHint: true,
                  prefixIcon: Icon(Icons.notes),
                ),
                validator: (v) =>
                    Validators.obrigatorio(v, campo: 'A descrição'),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _categoria,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: 'Categoria *',
                  prefixIcon: Icon(Icons.category_outlined),
                ),
                validator: (v) =>
                    Validators.obrigatorio(v, campo: 'A categoria'),
              ),
              const SizedBox(height: 24),
              Text(
                'Localização',
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _endereco,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: 'Endereço',
                  prefixIcon: Icon(Icons.home_outlined),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    flex: 2,
                    child: TextFormField(
                      controller: _cep,
                      keyboardType: TextInputType.number,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                        labelText: 'CEP',
                        prefixIcon: Icon(Icons.local_post_office_outlined),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _estado,
                      textCapitalization: TextCapitalization.characters,
                      textInputAction: TextInputAction.next,
                      maxLength: 2,
                      decoration: const InputDecoration(
                        labelText: 'UF',
                        counterText: '',
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _cidade,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: 'Cidade',
                  prefixIcon: Icon(Icons.location_city_outlined),
                ),
              ),
              const SizedBox(height: 24),
              Text(
                _isPrestacao ? 'Preço do serviço' : 'Faixa de orçamento',
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 12),
              if (_isPrestacao)
                TextFormField(
                  controller: _preco,
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  textInputAction: TextInputAction.done,
                  decoration: const InputDecoration(
                    labelText: 'Preço (R\$)',
                    prefixIcon: Icon(Icons.attach_money),
                    hintText: 'Ex.: 150,00',
                  ),
                )
              else
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _orcamentoMin,
                        keyboardType:
                            const TextInputType.numberWithOptions(decimal: true),
                        textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(
                          labelText: 'Mínimo (R\$)',
                          prefixIcon: Icon(Icons.attach_money),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextFormField(
                        controller: _orcamentoMax,
                        keyboardType:
                            const TextInputType.numberWithOptions(decimal: true),
                        textInputAction: TextInputAction.done,
                        decoration: const InputDecoration(
                          labelText: 'Máximo (R\$)',
                        ),
                      ),
                    ),
                  ],
                ),
              const SizedBox(height: 32),
              FilledButton(
                onPressed: _enviando ? null : _publicar,
                child: _enviando
                    ? const SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(strokeWidth: 2.5),
                      )
                    : const Text('Publicar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
