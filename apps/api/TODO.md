# Pendências para preparar a API para testes

Atualização de 01/11/2026: o histórico de falhas abaixo é da base anterior.
A nova configuração de segurança, testes e pipeline está registrada em
[PLANEJAMENTO.md](PLANEJAMENTO.md) e [registro 13](../../docs/13-registro-implementacao-rotas.md).
Cadastro e integração real de identidade continuam pendentes; a execução local
de PostGIS depende do ambiente descrito no novo guia.

A API está pronta para testes isolados do controller, mas ainda precisa dos
itens abaixo para testes de ponta a ponta dos endpoints existentes de usuários
e rotas.

Na verificação mais recente, em 31/10/2026 com `mvnw verify`, foram executados
27 testes: 18 passaram e 9 falharam por receber HTTP 401 nos testes de integração.
O H2 está disponível somente nos testes; não configura o PostgreSQL da aplicação.
Os 15 testes isolados de controller e repository passaram. O controller substitui
o serviço e o principal JWT; o repository usa um schema mínimo em H2, sem executar
as migrations PostgreSQL/PostGIS nem validar a autenticação completa.

## P0 — Bloqueadores

- [ ] **Configurar o banco de dados.** Adicionar URL, usuário e senha do
  DataSource ao `.env.example`. `DATABASE_PASSWORD` isoladamente não configura
  o Spring JDBC. Preparar um banco de testes, executar as migrations e criar
  o login da aplicação com as permissões necessárias.
  **Critério de conclusão:** a API inicia e consegue consultar `app.usuarios`.
  **Progresso:** `.env.example` contém as três propriedades JDBC. Ainda faltam
  provisionamento e validação com o PostgreSQL do ambiente de testes.

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

- [x] **Corrigir a atualização do perfil de mobilidade.** O PATCH usa um DTO
  próprio e aceita `perfilMobilidadeUuid`. Omitir o campo ou enviar `null`
  preserva o perfil atual. UUID malformado, perfil inexistente ou inativo
  retorna HTTP 400; dados inválidos não alteram o registro.
  **Critério de conclusão:** atualizar nomes preserva o perfil; uma alteração
  explícita de perfil valida o UUID e verifica se o perfil está ativo.
  **Validação:** testes HTTP e JDBC cobrem preservação, troca de perfil,
  rejeição de perfis inválidos, isolamento entre usuários e conta em exclusão.

- [ ] **Preparar um ambiente repetível de testes de integração.** Os testes
  de integração ainda precisam de um DataSource PostgreSQL de teste e da
  preparação da autenticação.
  **Critério de conclusão:** `mvnw verify` passa usando um banco isolado para
  testes e com a segurança habilitada.
  **Progresso:** testes JDBC isolados em H2 adicionados. A suíte completa
  ainda não passa: falta configurar autenticação e testes com PostgreSQL/PostGIS.

## P1 — Contratos e cobertura funcional

- [ ] **Concluir a separação dos DTOs de entrada e saída.** O PATCH já possui
  um DTO próprio. O GET ainda preenche somente nome e sobrenome, apesar de
  expor um modelo maior com campos de identidade, estado da conta e credenciais.
  **Critério de conclusão:** os campos aceitos e retornados correspondem ao
  contrato documentado; o comportamento de campos omitidos no PATCH está definido.
  **Progresso:** PATCH separado em `AtualizarUsuarioRequest`; nome e sobrenome
  continuam obrigatórios. Revisão do DTO de saída e dos demais endpoints pendente.

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
