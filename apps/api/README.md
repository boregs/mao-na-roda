# API Mão na Roda

Etapa inicial da issue #7: Spring Boot com verificação de saúde,
OpenAPI/Swagger, consulta de rota-base a pé via Google e testes HTTP.
O projeto usa Spring Boot 4.1.2 e compila para Java 25. O Maven Wrapper
fornece o Maven 3.9.11 automaticamente; não é necessário instalar Maven separado.

## Preparar o ambiente pela primeira vez

1. Clone o repositório e abra um terminal na raiz dele.
2. Instale um **JDK 25** completo, incluindo `javac`. JDK 26 também foi
   validado para compilar este projeto; uma instalação apenas de JRE não basta.
3. Configure `JAVA_HOME` para a pasta do JDK, sem o sufixo `bin`, e coloque
   a pasta `bin` desse JDK no `PATH`.
4. Entre na pasta da API: `cd apps/api`. Os comandos abaixo partem dessa pasta.

No PowerShell, adapte o caminho à sua instalação para configurar a sessão atual:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\mvnw.cmd --version
.\mvnw.cmd compile
```

Os três comandos de versão devem apontar para o JDK escolhido. Para persistir
no Windows, abra **Editar as variáveis de ambiente da sua conta**, crie
`JAVA_HOME` com a pasta do JDK e adicione `%JAVA_HOME%\bin` ao `Path`.
Reabra o terminal e a IDE depois de salvar. Se `java -version` continuar
mostrando outra versão, confira `Get-Command java` e a ordem das entradas do PATH.
O JDK usado pelo Flutter/Gradle pode ser diferente do usado nesta API.

No Linux/macOS, adapte o caminho e use o wrapper pelo shell:

```sh
export JAVA_HOME=/caminho/para/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
cd apps/api # execute apenas se ainda estiver na raiz do repositório
java -version
javac -version
sh mvnw --version
sh mvnw compile
```

No primeiro uso, o wrapper e o Maven precisam de internet para baixar a
distribuição, os plugins e as dependências para `~/.m2`. Aguarde a mensagem
`BUILD SUCCESS`. A compilação não exige conexão com banco nem chave do Google;
ela ainda não inicia a aplicação nem executa testes.

## Importar no IntelliJ IDEA ou Android Studio

1. Abra a pasta do repositório. Clique com o botão direito em `apps/api/pom.xml`
   e escolha **Add as Maven Project** (ou abra esse POM como projeto).
2. Em **Project Structure**, selecione JDK 25 ou 26 como **Project SDK**.
3. Em **Settings > Build, Execution, Deployment > Build Tools > Maven**,
   selecione **Use Maven Wrapper** como Maven home. Configure o JDK do
   **Importing** e do **Runner** para o mesmo JDK do projeto.
4. Na janela Maven, clique em **Reload All Maven Projects** e aguarde a importação.
5. Para executar pela IDE, use a classe `MaoNaRodaApiApplication` e configure
   o **Working directory** como a pasta `apps/api`, onde o `.env` será procurado.

Os starters declarados no POM fornecem Spring MVC, JDBC, validação e segurança
OAuth2/JWT. O Maven importa as dependências transitivas automaticamente; não
adicione JARs manualmente para corrigir imports vermelhos.

## Configuração necessária para iniciar a versão atual

Copie o arquivo de exemplo antes de preencher os valores locais:

```powershell
Copy-Item .env.example .env
```

No Linux/macOS, use `cp .env.example .env`. Não sobrescreva um `.env` já preenchido.

A versão atual contém `UsuarioRepository`, que depende de `JdbcTemplate`.
Configure um PostgreSQL acessível no `.env`, usando valores do seu ambiente:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/NOME_DO_BANCO
SPRING_DATASOURCE_USERNAME=USUARIO_DA_API
SPRING_DATASOURCE_PASSWORD=SENHA_DO_USUARIO_DA_API
```

`DATABASE_PASSWORD`, caso exista em um `.env` antigo, não configura o DataSource
do Spring automaticamente. Use as propriedades acima para a aplicação. As credenciais
administrativas de migration são separadas: consulte
[database/README.md](database/README.md) antes de aplicar o schema e as permissões.
`spring-boot:run` e `verify` não executam migrations.

O projeto também inclui Spring Security e endpoints de usuário que recebem
`Jwt`, mas ainda não declara uma configuração própria de segurança/JWT.
Não há um fluxo de autenticação completo documentado nesta versão; a política
padrão pode bloquear as chamadas de saúde, Swagger e rotas. Configurar o banco
e compilar não garante que esses endpoints estejam prontos para uso.

## Executar no Windows

Na pasta `apps/api`, execute no terminal do Android Studio ou PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Antes de executar, conclua a configuração do banco descrita acima.
As respostas abaixo descrevem o contrato esperado; dependem também da
configuração de segurança e da disponibilidade dos serviços.

- Saúde: http://localhost:8080/actuator/health — retorna status `UP` e os grupos de saúde disponíveis, sem detalhes internos.
- Swagger: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Use Ctrl+C para encerrar. A variável de ambiente `PORT` permite trocar a porta.
No emulador Android padrão, o computador é acessível por `10.0.2.2`.
HTTP é destinado ao desenvolvimento local; serviços publicados devem usar HTTPS (RNF15).

## Chave local do Google Maps

Na pasta `apps/api`, copie `.env.example` para `.env` e preencha:

```dotenv
GOOGLE_MAPS_API_KEY=COLE_SUA_CHAVE_AQUI
```

O Spring Boot importa esse arquivo como propriedades, sem biblioteca adicional.
Use `NOME=valor`, sem aspas, sem `export` e sem expansão de variáveis.
O caminho é relativo à pasta de execução: inicie a API em `apps/api`, inclusive
ao executar o JAR ou configurar a IDE. Reinicie a API após editar o arquivo.
Variáveis de ambiente do terminal têm prioridade sobre os valores do `.env`.

O `.env` é ignorado pelo Git; somente `.env.example`, sem credenciais, deve ser
versionado. A chave do Google é opcional para iniciar a aplicação; consultar
rotas exige a chave. Habilite Routes API e restrinja a chave do backend a essa API e ao IP
público de saída do servidor. Não use a chave Android.

## Consultar rota-base a pé

No Swagger, abra `POST /api/v1/rotas/base`, selecione **Try it out** e **Execute**.
O exemplo padrão é Metrô Carrão → UNICID Tatuapé:

```json
{
  "origem": {"endereco": "Estação de Metrô Carrão, São Paulo, SP, Brasil"},
  "destino": {"endereco": "UNICID, Rua Cesário Galeno, 448, Tatuapé, São Paulo, SP, Brasil"}
}
```

Cada ponto aceita um endereço não vazio de até 500 caracteres OU latitude e
longitude numéricas. Não misture os formatos no mesmo ponto. Exemplo de ponto:
`{"latitude": -23.5505, "longitude": -46.6333}`.
Endereços são resolvidos pelo próprio Google Routes: confira no mapa se as
entradas escolhidas correspondem à saída da estação e ao acesso desejado do
campus. A consulta não garante uma entrada específica nem uma entrada acessível.

A resposta inclui `distanciaMetros`, `duracaoSegundos` e `polylineCodificada`
(encoded polyline do Google para desenhar no mapa), além de `provedor`, `modo`,
`acessibilidadeAvaliada: false` e `aviso`. O cliente deve exibir o aviso:
rotas a pé estão em beta e podem omitir calçadas ou caminhos de pedestres.
Não há armazenamento de consultas ou registro de endereços/chaves em logs da aplicação.

Erros retornam Problem Details: 400 para entrada inválida, 404 sem rota,
502 para falha/resposta inválida do provedor, 503 para chave ausente ou cota
esgotada e 504 para tempo excedido. Corpos de erro do Google não são repassados.
O timeout de resposta é 8 segundos e o de conexão é 3 segundos. Não há retries
automáticos. `google.routes.url` e `google.routes.timeout` são configurações
internas usadas também pelos testes; não são parâmetros do endpoint.

Cada execução real pode consumir cota e gerar cobrança. Este endpoint ainda é
um protótipo local sem autenticação ou limite de chamadas: antes de publicá-lo,
defina proteção contra abuso e quotas. Ele não conclui RF03 nem a issue #7.

Validação real em 24/10/2026: a consulta do exemplo retornou HTTP 200, 566 metros,
456 segundos e polyline preenchida. Esses valores podem mudar. As entradas e
o traçado ainda não foram inspecionados no mapa, e não houve avaliação de barreiras.

Referências: [Compute Routes](https://developers.google.com/maps/documentation/routes/compute_route_directions),
[localizações](https://developers.google.com/maps/documentation/routes/specify_location),
[modo a pé](https://developers.google.com/maps/documentation/routes/reference/rest/v2/RouteTravelMode)
e [endereço UNICID](https://www.unicid.edu.br/a-unicid/nossas-unidades/).

## Banco de dados e migrations

As migrations V1 do PostgreSQL/Supabase estão em `src/main/resources/db/migration`.
O Flyway Maven Plugin aplica o schema `app`, PostGIS, a matriz de barreiras e as
permissões JDBC. Consulte [database/README.md](database/README.md) para a análise
do modelo, dependências, configuração das credenciais e comandos de aplicação/teste.

## Verificar e empacotar

Validação em 31/10/2026: `compile` passou com Maven 3.9.11 e JDK 26,
gerando bytecode Java 25. Os 15 testes isolados de controller/repository passam.
O repository usa um schema mínimo em H2, disponível apenas no escopo de testes;
isso não valida as migrations PostgreSQL/PostGIS nem configura o banco real.
Na suíte completa, `verify` executou 27 testes, com 18 aprovados e 9 falhas por
HTTP 401. A configuração de segurança e os testes de integração com PostgreSQL
continuam pendentes; veja [TODO.md](TODO.md).

Para executar somente os testes isolados, sem credenciais externas:

```powershell
.\mvnw.cmd "-Dtest=UsuarioControllerTests,UsuarioRepositoryTests" test
```

### Atualizar nome e perfil de mobilidade

O contrato de `PATCH /api/v1/usuarios/me` usa nome e sobrenome obrigatórios
(não vazios, com até 100 caracteres) e um UUID de perfil opcional:

```json
{
  "nome": "Ana",
  "sobrenome": "Silva",
  "perfilMobilidadeUuid": "00000000-0000-0000-0000-000000000001"
}
```

Use o UUID de um perfil ativo de `app.perfis_mobilidade`. Omitir
`perfilMobilidadeUuid` ou enviar `null` preserva o perfil existente; este
contrato não permite removê-lo. O endpoint retorna 204 após atualizar, 400
para dados/perfil inválidos e 404 para usuário ausente ou fora do estado ativo.
O acesso exige autenticação, cuja configuração completa ainda está pendente.

### Executar a verificação completa e o JAR

```powershell
.\mvnw.cmd verify
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Execute o JAR somente após `verify` concluir com `BUILD SUCCESS`.
Os testes foram escritos para iniciar a API em porta aleatória e verificar saúde, documentação,
interface Swagger e ausência de exposição do endpoint de variáveis de ambiente.
Os testes de rotas usam um servidor local simulado com chave fictícia: validam
contrato HTTP, modo WALK, campos solicitados, entradas inválidas, erros, timeout
e proteção de detalhes. Não chamam Google nem consomem cota.
No Linux/macOS, use `sh mvnw verify` ou `sh mvnw spring-boot:run`.

## Limites desta entrega

A saúde depende dos indicadores habilitados; com JDBC, também pode refletir
o estado do banco. Não valida Google Maps nem o fluxo de autenticação.
Existem endpoints e repositório de usuário em desenvolvimento, mas a integração
de segurança/JWT ainda precisa ser concluída e validada. A issue #7 não está concluída.

Google Maps foi escolhido por Felipe para a integração. Falta validar a
compatibilidade com as regras de barreiras e desvios;
uma rota-base do Google não é automaticamente uma rota acessível.
Configure as credenciais do banco para executar a aplicação e a chave local
no `.env` para consultar rotas; não coloque chaves no código.

## Solução de problemas

| Sintoma | Verificação / ação |
|---|---|
| `JAVA_HOME` inválido ou Java 8 em uso | Aponte para um JDK 25/26 completo, reabra terminal/IDE e confira `java`, `javac` e `mvnw --version`. |
| `release version 25 not supported` | O Maven está usando um JDK antigo; confira a versão Java exibida pelo wrapper. |
| Imports `org.springframework` vermelhos | Importe `apps/api/pom.xml` como Maven, recarregue os projetos e compare com `mvnw compile`. |
| `No implementation for ArtifactHandlerManager was bound` | Confirme **Use Maven Wrapper** e Maven 3.9.11. Essa falha foi observada na importação da IDE com Maven 3.10. |
| Falha ao baixar dependências | Confira internet, proxy e permissões de escrita em `~/.m2`; tente `mvnw -U compile` após corrigir a causa. |
| `Failed to configure a DataSource` | Configure `SPRING_DATASOURCE_URL`, usuário e senha; a API usa PostgreSQL, sem banco embarcado. |
| HTTP 401/403 ao abrir endpoints | Confira a configuração Spring Security/JWT; a integração de autenticação ainda está em desenvolvimento. |
| Porta 8080 ocupada | Defina `$env:PORT = '8081'` no PowerShell antes de executar. No Linux/macOS: `PORT=8081 sh mvnw spring-boot:run`. |

Nos exemplos da tabela, use `.\mvnw.cmd` no Windows e `sh mvnw` no Linux/macOS.

Referências: `docs/05-tech-stack.md`, RN01–RN04, RN12, RNF07, RNF10 e RNF15.

## Supabase Storage

O adaptador interno de fotos e os testes sem credenciais estão descritos em
[STORAGE.md](STORAGE.md). A homologação real e a integração aos relatos estão pendentes.
