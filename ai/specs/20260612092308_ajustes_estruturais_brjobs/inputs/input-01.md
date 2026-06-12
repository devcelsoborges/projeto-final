<!-- Fonte: conteudo colado em 2026-06-12 -->
<!-- Coletado durante o discovery de 20260612092308_ajustes_estruturais_brjobs -->

# Ajustes estruturais no BRJobs

1. Notificações só aparecem quando clica no botão, por exemplo, se eu recebo uma mensagem, ela só vai me notificar se eu clicar no botão de notificação.
2. Login deixará de ter campos obrigatórios, exigindo nome completo, cpf e telefone. Login também deverá ser o mesmo tanto para login social quanto para login normal. Quando tentar fazer login deverá identificar se já existe um login com aquele email. Cuidado com o cadastro social que não aceita alguns campos, então diferencie o payload.
3. Campo prestador/contratante será legado. Isso ficará especificado ao publicar, se é contratação ou prestação.
4. Campos de endereço serão opcionais, porém o card de publicar serviço pedirá endereço obrigatoriamente. No card das publicações deve aparecer "a X quilômetros de você", baseado na localização real, podendo utilizar localização atual para buscar a localização do usuário ou fornecida por ele.
5. Responsividade na tela de chat e no dropdown das notificações está errada.
6. Para cadastro agora, só serão obrigatórios os campos: nome, email, senha e confirmação de senha. Para a senha, deixe o informativo fixo abaixo do input dizendo o que é preciso para cadastrar a senha e só exiba uma mensagem de erro em vermelho se algum dos requisitos para senha não forem atendidos.
7. Ainda na tela de cadastro, remova todos esses campos e deixe-os somente na tela de editar perfil:

- Dados Pessoais
- Tipo de Usuário * (será legado)
- Data de Nascimento *
- Gênero *
- CEP *
- Rua *
- Número *
- Complemento
- Bairro *
- Cidade *
- UF *
- Informações Profissionais (Opcional)
- Função/Cargo Desejado
- Especialidades/Habilidades
- Resumo da Experiência Profissional
- Arquivos
- Foto de Perfil

Mantenha apenas um card com:

## Dados de Acesso

- Nome Completo *
- E-mail *
- Senha *
- Confirme a Senha *
