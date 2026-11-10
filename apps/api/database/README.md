# Banco de dados e migrations

Implementação SQL inicial da **V1**, baseada em `docs/06-banco-de-dados.md`
e na matriz aprovada de `docs/02-regras-de-negocio.md`. A análise do provedor
usa a documentação oficial do Supabase; o projeto remoto não foi inspecionado.

## Provedor e dependências

| Componente | Decisão |
|---|---|
| Banco | PostgreSQL do Supabase, com PostGIS em `extensions`. |
| Identidade | `auth.users(id uuid)` existente, gerenciado pelo Supabase Auth. |
| Arquivos | Supabase Storage; SQL guarda apenas referências em `app.relato_fotos`. |
| Evolução do schema | Flyway Maven Plugin, executado como etapa de implantação. |
| Versões | Spring Boot 4.1.2 / JDK 25; o BOM fornece Flyway 12.4.0 e PostgreSQL JDBC 42.7.14. |

O `pom.xml` declara o plugin, `flyway-database-postgresql` e o driver PostgreSQL
no classpath do plugin. O módulo PostgreSQL é necessário além do Flyway core.
Não é necessário SDK Java do Supabase, Hibernate, `pgcrypto` ou `uuid-ossp` para
essas migrations: `gen_random_uuid()` é nativo das versões atuais do PostgreSQL.

As credenciais administrativas pertencem à etapa de migration. A aplicação usa
repositórios JDBC e driver PostgreSQL no classpath de execução, configurados por
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.
Use um login separado com as permissões de `maonaroda_api`.
`spring-boot:run` e `verify` não executam migrations. Para o planejamento atual
e o teste local, consulte [PLANEJAMENTO.md](../PLANEJAMENTO.md).

## Arquivos e ordem

Os SQLs estão em [`../src/main/resources/db/migration`](../src/main/resources/db/migration),
organizados em subpastas pela data de criação: [`25-10/`](../src/main/resources/db/migration/25-10)
(V1–V5), [`01-11/`](../src/main/resources/db/migration/01-11) (V6) e
[`04-11/`](../src/main/resources/db/migration/04-11) (V7). O Flyway
percorre as subpastas e ordena pela versão, não pela pasta. Esta é a fonte da
verdade do banco; [`database/migrations`](../../../database/migrations), na raiz
do repositório, é apenas a cópia de referência aplicada manualmente em produção
(ver [Produção existente](#produção-existente)).

| Migration | Conteúdo |
|---|---|
| `V1__preparar_schemas_e_postgis.sql` | Verifica Auth, habilita/confere PostGIS e cria `app`. |
| `V2__criar_tabelas_v1.sql` | Oito tabelas, PKs `bigserial`, UUIDs públicos, FKs, CHECKs e índices. |
| `V3__garantir_autoria_e_retencao.sql` | Timestamps, autor ativo no envio e desvinculação ao excluir conta. |
| `V4__carregar_matriz_de_barreiras.sql` | Três perfis, dez barreiras e as trinta classificações RN01/RN02. |
| `V5__configurar_permissoes_jdbc.sql` | Grupos de acesso da API e do worker de descarte. |
| `V6__adicionar_evidencias_do_piloto.sql` | Dataset piloto, trechos orientados e evidências de barreiras, sem carga fictícia. |
| `V7__habilitar_rls_e_politicas_jdbc.sql` | RLS em todas as tabelas de `app`, com políticas que espelham os grants JDBC. |

O histórico fica em `app_migrations.flyway_schema_history`, separado do domínio
e dos schemas gerenciados. Flyway administra somente esse schema de histórico;
os SQLs criam explicitamente os objetos `app`. O schema `app` deve estar ausente
na primeira aplicação. Um schema preexistente requer comparação com as migrations,
não um baseline automático. `clean` está desabilitado.

## Decisões físicas e revisão do modelo

- **Conta:** mantidos `nome`/`sobrenome` como `text` e a FK
  `perfil_mobilidade_id bigint`, conforme escolha do solicitante. Criptografia
  de campos continua pendente; esta estrutura não implementa RNF13/RNF14.
  E-mail e senha continuam exclusivamente no Auth.
- **Geografia:** PostGIS, SRID 4326, coordenadas em ordem longitude/latitude.
  `locais` aceita somente Point/ponto ou LineString/trecho válidos, não vazios
  e dentro dos limites geográficos. `relatos` usa `geography(Point,4326)`.
  A API deve validar latitude/longitude **antes** de converter para geography:
  o PostGIS pode normalizar coordenadas fora do intervalo durante a conversão.
- **Verificação:** `verificado_em` obrigatório, conforme a RN06 aprovada.
  Fontes sem data devem ser rejeitadas pelo importador, não receber `importado_em`
  como data de verificação. Condição `desconhecida`, dado antigo e ausência de
  observação continuam sendo situações distintas.
- **V1:** relatos só aceitam `nao_validado`; observações operacionais só aceitam
  dados públicos/OSM. `confirmado_em` e `relato_origem_id` ficam reservados e nulos.
  A tabela de confirmações e a elegibilidade de relatos exigem migrations V2.
- **Fotos:** bucket privado `relato-fotos`, JPEG/PNG, **5 MB = 5.000.000 bytes**.
  Posições de 1 a 3 com unicidade por relato garantem o máximo de três metadados,
  inclusive sob concorrência. A unicidade bucket/caminho evita referências
  duplicadas. O SQL não verifica o conteúdo dos arquivos nem remove EXIF.
- **Exclusão:** a FK Auth → usuário usa CASCADE; usuário → relatos usa SET NULL.
  Um trigger BEFORE DELETE no usuário preenche retenção e remove autoria antes
  da ação da FK, evitando o conflito entre SET NULL e o CHECK de autoria.
  Isso também funciona quando a exclusão é iniciada pelo Supabase Auth.
- **Prazo:** um mês de calendário em UTC, com limite superior no CHECK.
  Ex.: 31/01 → último dia de fevereiro, no mesmo horário UTC. A repetição da
  exclusão não renova prazos. O worker ainda precisa remover arquivos e registros
  dentro desse teto; uma rotina diária após o vencimento pode ultrapassá-lo.
- **Catálogos:** RESTRICT nas referências; desativar em vez de apagar itens usados.
  A matriz inicial é completa e o teste verifica sua cobertura. Ao acrescentar
  perfis/barreiras, incluir todas as classificações na mesma migration/transação;
  uma combinação ausente nunca significa `irrelevante`.
- **Identificador externo:** provedor e ID são preenchidos juntos, com índice
  não único, permitindo dividir uma entidade externa em vários trechos.
- **Índices:** GiST nas duas colunas espaciais, índices nas FKs não cobertas por
  unicidade, autor/data e índice parcial de descarte. Distâncias em metros devem
  usar geography ou projeção adequada; o GiST de geometry não cobre automaticamente
  uma consulta que converte essa coluna para geography.

O limite de caracteres da descrição permanece aberto; o banco exige conteúdo
não vazio. Também continuam na API: autorização pela identidade Auth, cota
atômica de cinco relatos/hora, sessões/revogação, prioridade de fontes, fluxo
de uploads, reconciliação e descarte. O lock de usuário no trigger serializa
envios com exclusões, mas não implementa a cota. Nenhuma rota ou escolha de
perfil de visitante é persistida por este esquema.

## Aplicar no Supabase

Na pasta `apps/api`, com JDK 25 configurado:

1. Obtenha host, porta e usuário em **Connect** no painel do projeto.
   Prefira conexão direta, porta 5432. Em rede somente IPv4, use **Session pooler**
   na porta 5432, com o host e usuário exatos do painel (`postgres.<project-ref>`).
   Transaction pooler/porta 6543 não é adequado para estas migrations.
2. Use o papel administrativo de migrations com permissão de criar schemas,
   extensão e papéis, além de referenciar `auth.users`. O `postgres` do projeto
   é uma opção para essa etapa, separado do futuro login JDBC da API.
3. Baixe o certificado raiz do projeto e configure TLS com validação de host.
   Exemplo PowerShell, substituindo os marcadores:

   ```powershell
   $env:FLYWAY_URL = 'jdbc:postgresql://db.PROJECT_REF.supabase.co:5432/postgres?sslmode=verify-full&sslrootcert=C:/git/mao-na-roda/apps/api/certs/supabase-prod-ca-2021.crt'
   $env:FLYWAY_USER = 'postgres'
   $credencial = Get-Credential -UserName $env:FLYWAY_USER -Message 'Senha PostgreSQL de migrations'
   $env:FLYWAY_PASSWORD = $credencial.GetNetworkCredential().Password
   .\mvnw.cmd flyway:info
   .\mvnw.cmd flyway:migrate
   .\mvnw.cmd flyway:validate
   ```

   Em Linux/macOS, exporte as mesmas variáveis e use `sh mvnw`.
   O plugin lê `FLYWAY_*` do ambiente; **não importa o `.env` do Spring Boot**.
   Senha do banco é diferente de chave publishable/anon/service_role do Supabase.
4. Mantenha `app`, `extensions` e `app_migrations` fora dos schemas expostos pela
   Data API. A migration revoga acesso a `app` de PUBLIC, anon, authenticated e
   service_role; não altera a configuração do painel.
5. Crie o bucket privado `relato-fotos` pelo painel/API do Storage com limite
   5.000.000 bytes e MIME `image/jpeg`/`image/png`. Operações em arquivos devem
   usar a API do Storage, não DML em `storage.objects`/`storage.buckets`.

Se PostGIS já estiver instalado em outro schema, V1 falha com uma mensagem
específica. Inspecione dependências e siga a documentação do provedor antes de
ajustar a instalação; as migrations não movem nem apagam a extensão existente.

Versione mudanças seguintes como `V8__...sql`, etc. Não edite migrations já
aplicadas: `validate` confere os checksums. O Flyway controla as transações;
os SQLs de migrations não contêm BEGIN/COMMIT próprios.

### Produção existente

O banco de produção foi criado executando manualmente, no SQL Editor do
Supabase, os scripts de [`database/migrations`](../../../database/migrations)
(`0001`–`0005`), equivalentes a V1–V6: `0002`–`0005` correspondem a V3–V6 e
`0001` reúne a V2 com as tabelas de catálogo. Por isso produção **não possui**
`app_migrations.flyway_schema_history`, e um `flyway:migrate` direto tentaria
recriar o schema `app` e falharia. Não executar esses scripts em ambientes novos
nem evoluí-los; mudanças entram somente pelo Flyway.

Antes do primeiro uso do Flyway em produção:

1. Compare o esquema de produção com V1–V6. Em especial, `0001` não inclui
   partes da V1: a verificação de `auth.users`, a conferência do PostGIS no
   schema `extensions` e os `ALTER DEFAULT PRIVILEGES` que revogam acesso de
   PUBLIC, anon, authenticated e service_role em `app`. Aplique manualmente o
   que faltar, registrando o que foi feito.
2. Com as variáveis `FLYWAY_*` configuradas como acima, registre o baseline na
   versão 6:

   ```powershell
   .\mvnw.cmd flyway:baseline "-Dflyway.baselineVersion=6" "-Dflyway.baselineDescription=producao aplicada manualmente"
   .\mvnw.cmd flyway:info
   .\mvnw.cmd flyway:validate
   ```

3. `flyway:info` deve mostrar V1–V6 abaixo do baseline e nenhuma pendente. A
   partir daí, use `flyway:migrate` normalmente para V7 em diante.

### Logins de execução

As migrations criam os grupos NOLOGIN `maonaroda_api` e `maonaroda_descarte`.
Provisione logins com senhas separadas por ambiente e conceda o grupo adequado:

```sql
GRANT maonaroda_api TO api_login;
GRANT maonaroda_descarte TO descarte_login;
```

Os logins devem herdar os grupos e não ter propriedade das tabelas, superuser
ou privilégios administrativos adicionais. A API lê o domínio, insere contas,
relatos/fotos e atualiza os campos editáveis da conta. A exclusão da identidade
passa pelo Auth. O worker lê/exclui relatos e fotos; só deve apagar as referências
após concluir a remoção dos objetos no Storage.

Todas as tabelas de `app` têm RLS ativo (V7). As políticas permissivas de V7
espelham os grants acima para `maonaroda_api` e `maonaroda_descarte`; sem elas,
os logins não leem nem gravam linhas. No Supabase de produção, o login da API é
`maonaroda_api_login` (no Session pooler: `maonaroda_api_login.<project-ref>`),
criado sem senha; defina-a no SQL Editor com `ALTER ROLE ... PASSWORD`.

O acesso privado JDBC usa grants, sem políticas `auth.uid()`: a identidade do
usuário não é propagada automaticamente pela conexão compartilhada. A API
deve autorizar cada operação e ocultar relatos com `descartar_em <= now()`.

### Conexão da API com o Supabase (TLS)

Configure somente no ambiente local/servidor, nunca no Git:

```dotenv
SPRING_PROFILES_ACTIVE=supabase
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/postgres
SPRING_DATASOURCE_USERNAME=LOGIN_RESTRITO
SPRING_DATASOURCE_PASSWORD=SENHA_LOCAL
SUPABASE_DB_CA_CERT=/caminho/absoluto/para/apps/api/certs/supabase-prod-ca-2021.crt
```

O perfil `application-supabase.properties` configura o Hikari, limites de conexão,
`sslmode=verify-full` e o certificado raiz. O certificado público (Supabase Root
2021 CA, válido até 2031-04-26) está em `apps/api/certs/`; use caminho absoluto,
pois o pgJDBC resolve `sslrootcert` relativo à pasta de execução. Se a Supabase
rotacionar a CA, baixe o novo arquivo em Database Settings → SSL Configuration.

Use a URL sem parâmetros SSL nem credenciais embutidas, que sobrepõem as
propriedades do DataSource, e não troque por `sslmode=require`, que não verifica
a identidade do servidor. Use host e usuário exatos da conexão direta ou do
Session pooler. Chaves anon/service_role não são senhas PostgreSQL, e o login da
aplicação não deve criar schemas nem aplicar migrations.

## Testar localmente

Use PostgreSQL com PostGIS instalado e um banco **descartável vazio**, por
exemplo `maonaroda_test`. `bootstrap-local.sql` cria apenas uma fixture do
contrato Auth e dos papéis Supabase; nunca executá-lo no projeto Supabase.
Configure autenticação local via `.pgpass`/variáveis de ambiente.

```powershell
psql -X -h 127.0.0.1 -p 5432 -U postgres -d maonaroda_test -f database/tests/bootstrap-local.sql
$env:FLYWAY_URL = 'jdbc:postgresql://127.0.0.1:5432/maonaroda_test'
$env:FLYWAY_USER = 'postgres'
# Defina FLYWAY_PASSWORD com a senha desse banco local.
.\mvnw.cmd flyway:migrate flyway:validate
psql -X -h 127.0.0.1 -p 5432 -U postgres -d maonaroda_test -f database/tests/integridade.sql
.\mvnw.cmd flyway:migrate
.\mvnw.cmd verify
```

O teste SQL cobre matriz, geometrias, autoria, referências, bloqueios V1,
limites de fotos, exclusão em cascata a partir do Auth, retenção e grants.
Usa dados fictícios em transação com ROLLBACK; sequências podem avançar.
A segunda execução de `migrate` deve informar que o schema está atualizado.
Isso não substitui a integração com os serviços reais de Auth/Storage.

Validação em 25/10/2026: PostgreSQL 18.6 / PostGIS 3.6.2 em instância local
isolada, JDK 25.0.3; cinco migrations aplicadas, `validate` e reaplicação sem
mudanças aprovados, teste SQL aprovado (incluindo os papéis restritos da API,
Auth simulado e descarte) e os 12 testes existentes da API aprovados.

### Persistência com login restrito

`database/testar-banco.ps1` (Docker e JDK 25) sobe um PostGIS temporário em
`127.0.0.1:55432`, aplica e valida as migrations, cria um login que herda
somente `maonaroda_api` e executa o teste de integridade e `UsuarioPostgresTests`
com esse login. Os cenários verificam isolamento entre usuários, payload SQL
gravado como texto, perfil inexistente, conta em exclusão e ausência de
privilégios administrativos do login. O script descarta o ambiente ao final, não
altera `.env` e não inicia a API HTTP. Sem `MNR_TEST_JDBC_URL`, esses testes são
ignorados, o que não conta como validação.

## Referências consultadas

- [Supabase: conexões, poolers e TLS](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Supabase: PostGIS](https://supabase.com/docs/guides/database/extensions/postgis)
- [Supabase: usuários e exclusão](https://supabase.com/docs/guides/auth/managing-user-data)
- [Supabase: papéis PostgreSQL](https://supabase.com/docs/guides/database/postgres/roles)
- [Supabase: schema do Storage](https://supabase.com/docs/guides/storage/schema/design)
- [Flyway: plugin Maven e configuração por ambiente](https://documentation.red-gate.com/flyway/reference/usage/maven-goal)
- [Spring Boot: inicialização de bancos e Flyway](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
