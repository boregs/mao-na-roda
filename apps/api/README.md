# API Mão na Roda

Etapa inicial da issue #7: Spring Boot com verificação de saúde,
OpenAPI/Swagger, consulta de rota-base a pé via Google e testes HTTP.
Requer JDK 25; o Maven Wrapper baixa o Maven
automaticamente no primeiro uso, que precisa de internet.

## Executar no Windows

Na pasta `apps/api`, execute no terminal do Android Studio ou PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Configure `JAVA_HOME` para o JDK 25 se o terminal não reconhecer o Java.
O JDK usado pelo Flutter/Gradle pode ser diferente do usado nesta API.

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
versionado. O arquivo é opcional para iniciar a aplicação; consultar rotas exige
a chave. Habilite Routes API e restrinja a chave do backend a essa API e ao IP
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

```powershell
.\mvnw.cmd verify
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Os testes iniciam a API em porta aleatória, verificam saúde, documentação,
interface Swagger e ausência de exposição do endpoint de variáveis de ambiente.
Os testes de rotas usam um servidor local simulado com chave fictícia: validam
contrato HTTP, modo WALK, campos solicitados, entradas inválidas, erros, timeout
e proteção de detalhes. Não chamam Google nem consomem cota.
No Linux/macOS, use `sh mvnw verify` ou `sh mvnw spring-boot:run`.

## Limites desta entrega

A saúde indica apenas que o processo está funcionando. Não verifica Google Maps
ou Supabase. Ainda não há endpoints de contas ou relatos, autenticação,
persistência ou integração com o aplicativo. A issue #7 não está concluída.

Google Maps foi escolhido por Felipe para a integração. Falta validar a
compatibilidade com as regras de barreiras e desvios;
uma rota-base do Google não é automaticamente uma rota acessível.
Não são necessárias credenciais para executar esta base. Configure a chave
local no `.env` para consultar rotas; não coloque chaves no código.

Referências: `docs/05-tech-stack.md`, RN01–RN04, RN12, RNF07, RNF10 e RNF15.
