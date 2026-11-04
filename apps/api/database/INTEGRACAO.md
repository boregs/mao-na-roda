# Integração JDBC e verificação de segurança — tarefa #12

**Status:** preparação implementada; execução com PostgreSQL/PostGIS e homologação
no Supabase pendentes. Este documento não atesta ausência de vulnerabilidades.

## Escopo

A API já usa JDBC e consultas parametrizadas em `UsuarioRepository`. A tarefa #12
acrescenta uma configuração explícita para o ambiente Supabase, um banco local
repetível e testes usando um login restrito, sem refazer o acesso JDBC existente.

O código de base foi importado do ZIP `feat/7-base-api-java`, sem histórico Git.
A branch local `feat/12-integracao-banco-api` parte da base local anterior à #17;
esta entrega não inclui o adaptador Storage nem depende do PR da #17. Antes de
abrir um PR, alinhar a base da API com a equipe para evitar importações duplicadas.

## Testes disponíveis agora, sem Docker ou Supabase

Com JDK 25 em `JAVA_HOME`, na pasta `apps/api`:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=UsuarioRepositoryTests,UsuarioControllerTests" test
```

Os testes de repository usam H2; os de controller substituem serviço e principal
JWT. Eles verificam contratos locais, preservação do perfil, isolamento por
identificador e erros sanitizados. Um teste adicional armazena payload com sintaxe
SQL como texto, verificando que não altera estrutura ou registros de outro usuário.
Isso não comprova o comportamento do PostgreSQL, suas permissões nem a autenticação HTTP.

## Teste real com PostgreSQL/PostGIS local

Pré-requisitos: Docker Desktop iniciado em modo Linux, Compose v2, acesso às
imagens/dependências e JDK 25 configurado. Não é necessário instalar `psql` no Windows.
Nenhuma credencial Supabase é necessária.

Na pasta `apps/api`:

```powershell
.\database\testar-banco.ps1
```

Se a política local bloquear scripts, consultar a política da equipe antes de
alterá-la. O script verifica pré-requisitos antes de iniciar o ambiente.

O script:

1. Gera senhas aleatórias temporárias e nome exclusivo para o projeto Compose.
2. Inicia `postgis/postgis:17-3.5`, acessível apenas em `127.0.0.1:55432`.
3. Cria `maonaroda_jdbc_test` a partir de `template0`, para que PostGIS seja instalado
   em `extensions` pelas migrations, em vez de ser herdado em `public` da imagem.
4. Prepara a fixture local de Auth e aplica/valida as migrations Flyway.
5. Cria login `maonaroda_jdbc_test_api`, sem poderes administrativos, herdando
   somente `maonaroda_api`.
6. Executa o teste SQL de integridade existente e os testes JDBC/controller.
7. Reaplica as migrations para verificar ausência de alterações e valida checksums.
8. Encerra somente o projeto Docker criado na execução e restaura as variáveis de
   ambiente anteriores. Os dados do banco ficam em tmpfs e são descartados.

A porta 55432 precisa estar livre. O script não altera `.env`, não aceita banco
remoto como parâmetro e não remove outros projetos Docker. Ele não inicia a API
HTTP completa; testa a camada de persistência real pelo serviço/repository.

### Verificações PostgreSQL

`UsuarioPostgresTests` usa conexão administrativa somente para fixture e limpeza.
As chamadas do repository usam conexão separada com o login restrito. Os cinco
cenários verificam:

- Consulta e atualização de uma identidade sem alterar a outra.
- Payload SQL persistido como texto, sem executar seus comandos.
- Perfil inexistente recusado sem alteração do usuário.
- Conta em exclusão bloqueada para atualização.
- Login sem superuser, criação de banco/papéis ou bypass RLS; sem propriedade das
  tabelas, sem DELETE de usuários, alteração de identidade, DDL em `app` ou leitura de Auth.

A classe é ignorada quando `MNR_TEST_JDBC_URL` não está definida. Quando definida,
aceita somente a URL local fixa do harness. Um teste ignorado não conta como validação
real aprovada. Resultados ficam em `target/surefire-reports/`.

## Resultado local em 02/11/2026

A API e as cinco classes de teste compilaram com JDK 25. A seleção de testes
executou 16 testes aprovados (nove de controller e sete de repository em H2).
Os cinco cenários PostgreSQL foram ignorados por falta do ambiente local.
A sintaxe do script PowerShell foi validada; sua execução detectou corretamente
Docker ausente antes de iniciar qualquer banco. Compose, migrations e testes com
login PostgreSQL restrito ainda não foram executados nesta máquina.

## Configurar o Supabase posteriormente

1. Aplicar as migrations com credencial administrativa separada, conforme
   [README.md](README.md). Não rodar `bootstrap-local.sql` no Supabase.
2. Provisionar login PostgreSQL da aplicação com herança de `maonaroda_api` e sem
   propriedade de tabelas ou privilégios administrativos. Não usar `postgres` na API.
3. Manter `app` fora dos schemas expostos pela Data API. Grants JDBC não propagam
   automaticamente identidade Auth; a API precisa autorizar cada chamada.
4. Obter host, porta e certificado CA no ambiente da equipe. Configurar somente
   no ambiente local/servidor, nunca no Git:

```dotenv
SPRING_PROFILES_ACTIVE=supabase
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/postgres
SPRING_DATASOURCE_USERNAME=LOGIN_RESTRITO
SPRING_DATASOURCE_PASSWORD=SENHA_LOCAL
SUPABASE_DB_CA_CERT=C:/git/mao-na-roda/apps/api/certs/supabase-prod-ca-2021.crt
```

O certificado raiz público da Supabase (Supabase Root 2021 CA, válido até
2031-04-26) está versionado em `apps/api/certs/supabase-prod-ca-2021.crt`. Use o
caminho absoluto do seu clone em `SUPABASE_DB_CA_CERT`, pois o pgJDBC resolve
`sslrootcert` relativo ao diretório de execução. Se a Supabase rotacionar a CA,
baixe o novo arquivo em Database Settings → SSL Configuration e substitua-o.

O perfil `application-supabase.properties` configura Hikari, limites de conexão,
`sslmode=verify-full` e certificado raiz. Use URL sem parâmetros SSL conflitantes e
sem credenciais embutidas: propriedades da URL JDBC podem sobrepor propriedades do
DataSource. Não substituir por `sslmode=require`, que não verifica identidade do
servidor. Proteja também os arquivos de configuração e o mecanismo de implantação.
O perfil precisa ser ativado explicitamente; ele não altera o modo padrão de execução.

Use host/usuário exatos da conexão direta ou Session pooler. As instruções de
migrations já descrevem o uso do pooler; não usar a conexão de aplicação para criar
schemas ou aplicar migrations. Chave anon/service_role não é senha PostgreSQL.

## Limites e trabalho restante

- Executar o harness numa máquina com Docker e registrar os resultados reais.
- Homologar TLS, certificado e privilégios no Supabase de desenvolvimento.
- Configurar autenticação JWT real e testar autorização HTTP de ponta a ponta.
  Os testes de controller atuais usam principal substituído e não validam assinatura,
  expiração, audience ou revogação de tokens.
- A credencial JDBC compartilhada pode ler outros usuários; o filtro por identidade
  é responsabilidade da API, não uma garantia de isolamento por RLS.
- Criação de usuário ainda retorna 501; exclusão apenas inicia o estado de exclusão.
  Esta tarefa não implementa esses fluxos completos.
- Criptografia de campos sensíveis e gestão de chaves permanecem pendentes.
- Os cenários não são auditoria completa de vulnerabilidades. Revise novos endpoints,
  logs, implantação, privilégios herdados e credenciais em cada ambiente.
- Conferir escopo e critérios de aceite com a equipe; a issue estava sem critérios.

## Referências

- [pgJDBC — SSL e verificação de identidade](https://jdbc.postgresql.org/documentation/ssl/)
- [Docker Compose — serviços e publicação de portas](https://docs.docker.com/reference/compose-file/services/)
- [Imagem PostGIS — inicialização das extensões](https://github.com/postgis/docker-postgis/blob/master/initdb-postgis.sh)
- [Requisitos do projeto](../../../docs/03-requisitos.md)

Consultadas em 02/11/2026.
