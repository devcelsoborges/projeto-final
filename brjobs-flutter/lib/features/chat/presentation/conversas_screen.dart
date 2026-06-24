import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/config/env.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/router/app_router.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/state_views.dart';
import '../application/chat_providers.dart';
import '../domain/conversa.dart';

class ConversasScreen extends ConsumerStatefulWidget {
  const ConversasScreen({super.key});

  @override
  ConsumerState<ConversasScreen> createState() => _ConversasScreenState();
}

class _ConversasScreenState extends ConsumerState<ConversasScreen> {
  @override
  Widget build(BuildContext context) {
    final conversasAsync = ref.watch(conversasControllerProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Conversas')),
      body: conversasAsync.when(
        loading: () => const LoadingView(),
        error: (e, _) => ErrorView(
          message: e is ApiException ? e.message : 'Falha ao carregar conversas.',
          onRetry: () =>
              ref.read(conversasControllerProvider.notifier).recarregar(),
        ),
        data: (conversas) {
          if (conversas.isEmpty) {
            return const EmptyView(
              message:
                  'Nenhuma conversa ainda.\nEntre em contato a partir de uma publicação.',
              icon: Icons.forum_outlined,
            );
          }
          return RefreshIndicator(
            onRefresh: () =>
                ref.read(conversasControllerProvider.notifier).recarregar(),
            child: ListView.separated(
              itemCount: conversas.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (_, i) => _ConversaTile(conversa: conversas[i]),
            ),
          );
        },
      ),
    );
  }
}

class _ConversaTile extends StatelessWidget {
  const _ConversaTile({required this.conversa});

  final Conversa conversa;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final nome = (conversa.contatoNome?.trim().isNotEmpty ?? false)
        ? conversa.contatoNome!.trim()
        : 'Contato';
    final iniciais = _iniciais(nome);
    final temNaoLidas = conversa.naoLidas > 0;

    return ListTile(
      leading: CircleAvatar(
        backgroundColor: scheme.primaryContainer,
        foregroundColor: scheme.onPrimaryContainer,
        child: Text(iniciais),
      ),
      title: Text(
        nome,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontWeight: temNaoLidas ? FontWeight.bold : FontWeight.w500,
        ),
      ),
      subtitle: Text(
        conversa.ultimaMensagem?.trim().isNotEmpty == true
            ? conversa.ultimaMensagem!.trim()
            : 'Sem mensagens',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          color: temNaoLidas
              ? scheme.onSurface
              : scheme.onSurfaceVariant,
        ),
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            Fmt.relativo(conversa.ultimaMensagemEm),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: temNaoLidas ? scheme.primary : scheme.onSurfaceVariant,
                ),
          ),
          const SizedBox(height: 4),
          if (temNaoLidas)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
              decoration: BoxDecoration(
                color: scheme.primary,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                _badge(conversa.naoLidas),
                style: TextStyle(
                  color: scheme.onPrimary,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            )
          else
            const SizedBox(height: 18),
        ],
      ),
      onTap: () {
        final nomeParam = Uri.encodeComponent(conversa.contatoNome ?? '');
        context.push('${Routes.conversa(conversa.contatoId)}?nome=$nomeParam');
      },
    );
  }

  String _badge(int n) =>
      n > Env.chatHeaderBadgeMax ? '${Env.chatHeaderBadgeMax}+' : '$n';

  String _iniciais(String nome) {
    final partes = nome.trim().split(RegExp(r'\s+'));
    if (partes.isEmpty || partes.first.isEmpty) return '?';
    if (partes.length == 1) return partes.first[0].toUpperCase();
    return (partes.first[0] + partes.last[0]).toUpperCase();
  }
}
