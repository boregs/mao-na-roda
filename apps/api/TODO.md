# Pendências para preparar a API para testes

A API está pronta para testes isolados do controller, mas ainda precisa dos
itens abaixo para testes de ponta a ponta dos endpoints existentes de usuários
e rotas.

Na verificação realizada em 31/10/2026 com `mvnw verify`, foram executados
19 testes: 7 passaram e 12 apresentaram erros porque o Spring não conseguiu
inicializar o DataSource. Os testes isolados do controller substituem o serviço
e o principal JWT; eles não validam a persistência nem a autenticação completa.

## P0 — Bloqueadores

- [ ] **Configurar o banco de dados.** Adicionar URL, usuário e senha do
  DataSource ao `.env.example`. `DATABASE_PASSWORD` isoladamente não configura
  o Spring JDBC. Preparar um banco de testes, executar as migrations e criar
  o login da aplicação com as permissões necessárias.
  **Critério de conclusão:** a API inicia e consegue consultar `app.usuarios`.

- [ ] **Configurar a autenticação.** Definir explicitamente a cadeia de filtros
  de segurança, a verificação dos tokens JWT, as regras de acesso aos endpoints
  e a forma de obtenção dos tokens pelos responsáveis pelos testes.
  **Critério de conclusão:** tokens válidos permitem acesso; tokens ausentes,
  expirados ou inválidos retornam HTTP 401.

- [ ] **Implementar a criação de usuário.** O método `criar` retorna HTTP 501.
  Definir se ele cadastra uma conta no provedor de autenticação ou cria um
  perfil da aplicação para uma conta já autenticada. A exigência atual de JWT
  e o recebimento de e-mail/senha misturam esses dois fluxos.
  **Critério de conclusão:** a criação persiste o usuário, retorna HTTP 201
  e trata contas duplicadas de forma previsível.

- [ ] **Corrigir a atualização do perfil de mobilidade.** O controller sempre
  passa `null` como `profileUuid`, e o repository grava esse valor em
  `perfil_mobilidade_id`. Assim, atualizar o nome remove o perfil existente.
  **Critério de conclusão:** atualizar nomes preserva o perfil; uma alteração
  explícita de perfil valida o UUID e verifica se o perfil está ativo.

- [ ] **Preparar um ambiente repetível de testes de integração.** Os testes
  existentes não possuem configuração de DataSource de teste nem preparação
  da autenticação.
  **Critério de conclusão:** `mvnw verify` passa usando um banco isolado para
  testes e com a segurança habilitada.

## P1 — Contratos e cobertura funcional

- [ ] **Separar os DTOs de entrada e saída.** O PATCH recebe `UsuarioModel`,
  incluindo identidade, estado da conta, e-mail e senha, embora a operação não
  utilize esses campos. O GET preenche somente nome e sobrenome, apesar de
  expor um modelo maior.
  **Critério de conclusão:** os campos aceitos e retornados correspondem ao
  contrato documentado; o comportamento de campos omitidos no PATCH está definido.

- [ ] **Definir o comportamento da exclusão de conta.** O DELETE apenas altera
  o estado da conta. Definir se o escopo dos testes cobre essa transição ou a
  exclusão completa, além do comportamento de chamadas repetidas e do acesso
  após o início da exclusão.
  **Critério de conclusão:** os testes verificam a transição de estado,
  a resposta HTTP e o acesso posterior conforme o contrato definido.

- [ ] **Testar persistência real e autorização.** Adicionar testes de criação,
  contas duplicadas, usuários inexistentes ou inativos, preservação do perfil
  de mobilidade e falhas do banco de dados.
  **Critério de conclusão:** dois usuários de teste conseguem acessar e alterar
  somente os próprios dados, e os cenários de persistência estão cobertos.

- [ ] **Completar as instruções de teste.** Documentar obtenção de tokens,
  usuários e perfis de exemplo, autorização no Swagger, requisições de exemplo,
  respostas esperadas e procedimentos para reinicializar o banco de testes.
  **Critério de conclusão:** outra pessoa consegue executar o fluxo de testes
  a partir de um clone novo do repositório.

## Antes de disponibilizar um ambiente remoto de testes

- [ ] Configurar HTTPS.
- [ ] Configurar cotas e controles de acesso para a integração com Google Maps.
- [ ] Configurar CORS caso os testes utilizem um frontend no navegador.

## Ordem sugerida

1. Banco de dados.
2. Autenticação.
3. Criação de usuário e correção da atualização de perfil.
4. Testes de integração.

No Windows, execute os comandos Maven com `.\mvnw.cmd`; no Linux/macOS,
use `sh mvnw`, sempre a partir da pasta `apps/api`.
