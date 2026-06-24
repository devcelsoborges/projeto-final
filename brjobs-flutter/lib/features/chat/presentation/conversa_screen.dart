import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/env.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/feedback.dart';
import '../../../shared/widgets/state_views.dart';
import '../../auth/application/auth_controller.dart';
import '../application/chat_providers.dart';
import '../domain/chat_message.dart';

class ConversaScreen extends ConsumerStatefulWidget {
  const ConversaScreen({
    required this.outroUsuarioId,
    this.nomeContato,
    super.key,
  });

  final int outroUsuarioId;
  final String? nomeContato;

  @override
  ConsumerState<ConversaScreen> createState() => _ConversaScreenState();
}

class _ConversaScreenState extends ConsumerState<ConversaScreen> {
  final _texto = TextEditingController();
  final _scrollController = ScrollController();
  bool _enviando = false;

  @override
  void dispose() {
    _texto.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    final texto = _texto.text.trim();
    if (texto.isEmpty || _enviando) return;
    setState(() => _enviando = true);
    try {
      await ref
          .read(conversaThreadProvider(widget.outroUsuarioId).notifier)
          .enviar(texto);
      _texto.clear();
      _scrollToBottom();
    } on ApiException catch (e) {
      if (mounted) showErrorSnack(context, e.message);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final titulo = (widget.nomeContato?.trim().isNotEmpty ?? false)
        ? widget.nomeContato!.trim()
        : 'Conversa';
    final usuarioId = ref.read(authControllerProvider).user?.id ?? -1;
    final mensagensAsync = ref.watch(conversaThreadProvider(widget.outroUsuarioId));

    // Rola para o fim sempre que novas mensagens chegam.
    ref.listen(conversaThreadProvider(widget.outroUsuarioId), (_, next) {
      if (next.hasValue) _scrollToBottom();
    });

    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      body: Column(
        children: [
          Expanded(
            child: mensagensAsync.when(
              loading: () => const LoadingView(),
              error: (e, _) => ErrorView(
                message: e is ApiException
                    ? e.message
                    : 'Falha ao carregar mensagens.',
                onRetry: () => ref
                    .read(conversaThreadProvider(widget.outroUsuarioId).notifier)
                    .recarregar(),
              ),
              data: (mensagens) {
                if (mensagens.isEmpty) {
                  return const EmptyView(
                    message: 'Nenhuma mensagem ainda.\nEnvie a primeira!',
                    icon: Icons.chat_bubble_outline,
                  );
                }
                final ordenadas = _ordenar(mensagens);
                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 12,
                  ),
                  itemCount: ordenadas.length,
                  itemBuilder: (_, i) {
                    final m = ordenadas[i];
                    return _Bolha(
                      mensagem: m,
                      souEu: m.remetenteId == usuarioId,
                    );
                  },
                );
              },
            ),
          ),
          _BarraEnvio(
            controller: _texto,
            enviando: _enviando,
            onEnviar: _enviar,
          ),
        ],
      ),
    );
  }

  /// Ordena cronologicamente (mais antigas no topo). O backend pode devolver
  /// as mais recentes primeiro.
  List<ChatMessage> _ordenar(List<ChatMessage> mensagens) {
    final copia = [...mensagens];
    copia.sort((a, b) {
      final da = a.criadoEm;
      final db = b.criadoEm;
      if (da == null && db == null) return a.id.compareTo(b.id);
      if (da == null) return -1;
      if (db == null) return 1;
      return da.compareTo(db);
    });
    return copia;
  }
}

class _Bolha extends StatelessWidget {
  const _Bolha({required this.mensagem, required this.souEu});

  final ChatMessage mensagem;
  final bool souEu;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final corFundo = souEu ? scheme.primary : scheme.surfaceContainerHighest;
    final corTexto = souEu ? scheme.onPrimary : scheme.onSurface;

    return Align(
      alignment: souEu ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        decoration: BoxDecoration(
          color: corFundo,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(souEu ? 16 : 4),
            bottomRight: Radius.circular(souEu ? 4 : 16),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(mensagem.conteudo, style: TextStyle(color: corTexto)),
            const SizedBox(height: 2),
            Text(
              Fmt.dataHora(mensagem.criadoEm),
              style: TextStyle(
                color: corTexto.withValues(alpha: 0.7),
                fontSize: 10,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BarraEnvio extends StatelessWidget {
  const _BarraEnvio({
    required this.controller,
    required this.enviando,
    required this.onEnviar,
  });

  final TextEditingController controller;
  final bool enviando;
  final VoidCallback onEnviar;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(12, 8, 8, 8),
        decoration: BoxDecoration(
          color: scheme.surface,
          border: Border(top: BorderSide(color: scheme.outlineVariant)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Expanded(
              child: TextField(
                controller: controller,
                maxLength: Env.chatMaxMessageLength,
                minLines: 1,
                maxLines: 5,
                textInputAction: TextInputAction.newline,
                textCapitalization: TextCapitalization.sentences,
                decoration: const InputDecoration(
                  hintText: 'Escreva uma mensagem...',
                  counterText: '',
                  border: OutlineInputBorder(),
                  isDense: true,
                ),
              ),
            ),
            const SizedBox(width: 8),
            IconButton.filled(
              onPressed: enviando ? null : onEnviar,
              icon: enviando
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2.5),
                    )
                  : const Icon(Icons.send),
            ),
          ],
        ),
      ),
    );
  }
}
