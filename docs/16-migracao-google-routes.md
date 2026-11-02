# Migração do planejamento para Google Routes

Data: 02/11/2026. Branch de continuidade: `feat/planejamento-rotas-visitante`.

Este é o registro da migração. A configuração local mudou após a validação
descrita aqui: consulte o [guia do time e o estado atualizado](17-guia-testes-time.md)
para os bloqueios atuais, resultados mais recentes do CI e roteiro de aceite.

## Decisão e resultado

O solicitante informou que a Routes API está ativada e pediu seu uso no aplicativo.
Isso altera a escolha de ORS do plano anterior. Google Maps SDK continua desenhando
o mapa Android; Google Routes passa a fornecer as candidatas de caminhada ao
backend. As regras RN01–RN04, RN06, RN07, RN13, RN17 e RN18 são preservadas na
avaliação piloto. RF03 continua parcialmente implementado: uma rota a pé do Google
não equivale a uma rota segura para cadeira de rodas ou outro perfil.

O backend não está publicado. A integração foi preparada para teste local no
emulador; não foi criada infraestrutura nem publicada uma API nesta alteração.

## Mudanças implementadas

- `POST /api/v1/rotas` usa Google Routes em modo `WALK`, pede alternativas e
  GeoJSON de alta qualidade. Mantém deduplicação, avaliação das evidências por
  perfil, ranking e limite de três candidatas exibidas.
- Origem e destino aceitam endereço ou coordenadas, inclusive combinados. Os
  endereços são resolvidos na própria chamada Routes; não é necessária uma chave
  ORS, Places ou Geocoding para esse fluxo. O app aceita até 200 caracteres por
  campo e orienta informar cidade e conferir o acesso/traçado resultante.
- O app envia os campos diretamente em **Consultar rota**. Foram retirados os
  botões da busca separada e sua lista de resultados. Não há autocomplete.
- Os clientes ORS/Pelias, sua configuração e seus testes específicos foram
  retirados. `POST /api/v1/destinos/busca` responde **410 Gone**, orientando a
  atualização dos clientes para enviar endereços a `/api/v1/rotas`.
- `/api/v1/rotas/base` preserva seu contrato de polyline codificada e a indicação
  de acessibilidade não avaliada. O transporte Google é compartilhado pelos dois
  endpoints, com máscaras de resposta adequadas a cada contrato.
- Avisos de caminhada do Google e de acessibilidade desconhecida são enviados
  no resultado e nos detalhes das rotas, e exibidos pelo Android. Erros externos
  não revelam a chave nem o corpo da resposta do provedor.
- `GOOGLE_ROUTES_API_KEY` é a configuração preferida do servidor.
  `GOOGLE_MAPS_API_KEY` é aceito para instalações anteriores quando a nova variável
  estiver ausente. Se houver uma variável nova vazia, preencha-a ou remova-a antes
  de usar o nome legado. A chave Android não é reutilizada no backend.
- O perfil Spring `local` escuta apenas em `127.0.0.1`. O Android debug permite
  HTTP somente para `10.0.2.2`, `127.0.0.1` e `localhost`. O manifesto de release
  não recebe essa exceção; o build release recusa uma URL HTTP. HTTPS continua
  validando certificados nas duas variantes.

São mantidos o orçamento de 8 segundos no backend e 9 no cliente, ausência de
retries automáticos, limite de consultas públicas e ausência de persistência
das rotas/perfis de visitante. Acesso a contas continua separado da consulta
pública. Não foram alteradas migrations nem a matriz de barreiras.

## Teste local no Android Studio

### 1. Preparar o backend

É necessário JDK 25 e um PostgreSQL/PostGIS preparado com as migrations V1–V6,
incluindo o catálogo dos três perfis. O login da aplicação deve herdar
`maonaroda_api`; use credenciais de migration separadas. Consulte
[banco e permissões](../apps/api/database/README.md).

Se ainda não houver banco, o ambiente descartável de
[PLANEJAMENTO.md](../apps/api/PLANEJAMENTO.md#verificações-reproduzíveis) pode ser
preparado com Docker e `tools/testar-postgis.ps1`. Esse script executa as migrations
e os testes; **não inicia o backend da aplicação**. Docker não estava disponível
nesta máquina durante esta verificação. Outra opção é preparar um banco de
desenvolvimento conforme o guia de banco. H2 e as respostas simuladas existem
somente nos testes automatizados.

Em `apps/api`, copie `.env.example` para `.env` somente se o arquivo local ainda
não existir e preencha no editor:

```dotenv
GOOGLE_ROUTES_API_KEY=CHAVE_DE_SERVIDOR_RESTRITA_A_ROUTES_API
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/SEU_BANCO
SPRING_DATASOURCE_USERNAME=SEU_LOGIN_DA_API
SPRING_DATASOURCE_PASSWORD=SUA_SENHA_LOCAL
```

Substitua porta/nome do banco/credenciais pelo ambiente preparado. O Compose de
testes usa porta **55432** e banco `maonaroda_test`; o exemplo acima não corresponde
automaticamente a ele. Para servidor remoto, configure TLS JDBC validando o
certificado. Não copie credenciais administrativas para o login da aplicação.

No Google Cloud, além de ativar Routes API, o projeto precisa de faturamento
configurado e de uma chave de **servidor** autorizada a usar Routes API. Restrinja
a chave ao IP público de saída do computador/servidor que fará a chamada.
Não coloque essa chave em `local.properties`, no APK, em commits ou em mensagens.

Inicie a API a partir de `apps/api`:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-25'
.\mvnw.cmd "-Dspring-boot.run.profiles=local" spring-boot:run
```

Mantenha esse terminal aberto. O `.env` é lido como propriedades (sem aspas,
`export` ou expansão); alterações exigem reiniciar a API. `spring-boot:run` não
aplica migrations. A porta padrão é 8080, alterável por `PORT`.

Antes de abrir o Android, confira em outro PowerShell:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8080/api/v1/perfis-mobilidade
```

A saúde deve estar `UP` e o catálogo deve retornar os três perfis. Um catálogo
com erro indica que banco/schema/login precisam ser corrigidos antes de tentar
rotas. O catálogo e a saúde não consultam o Google nem comprovam a chave Routes.

### 2. Conectar o aplicativo

Em `apps/android/local.properties`, preserve `sdk.dir` e sua chave Maps existente:

```properties
API_BASE_URL=http://10.0.2.2:8080
MAPS_API_KEY=SUA_CHAVE_RESTRITA_AO_ANDROID
```

Abra **apps/android** no Android Studio, sincronize e selecione a configuração
**app**, variante **debug**, no emulador. O projeto usa JDK 21 para Gradle e JDK 25
para a API; são configurações distintas. Não selecione `VisitorJourneyTest` para
usar o produto: essa configuração executa um teste automatizado e encerra a tela
ao terminar. O aplicativo de exemplo “Map with Marker” também é outro pacote.

Entre sem conta, expanda o planejamento e escolha o perfil. Como primeira consulta,
informe endereços completos, por exemplo Estação Carrão, São Paulo, SP e UNICID,
Rua Cesário Galeno, 448, São Paulo, SP. Toque em **Consultar rota**. Também são
aceitas coordenadas no formato `latitude, longitude`, com ponto decimal.

Uma consulta real pode gerar cobrança no Google. Confirme o acesso escolhido no
mapa, o resumo das barreiras e os avisos. Sem dataset revisado correspondente ao
traçado, a qualidade esperada é **DESCONHECIDA**, mesmo que a linha seja desenhada.
Não use esse teste como garantia de acessibilidade em campo.

Para aparelho Android conectado por USB, `adb reverse tcp:8080 tcp:8080` permite
usar `API_BASE_URL=http://127.0.0.1:8080` em debug. O IP da rede Wi-Fi não faz parte
da exceção HTTP. Quando a API for publicada, substitua a URL por HTTPS com
certificado válido e use o guia de [publicação](14-publicacao-e-validacao-remota.md).

### Diagnóstico

| Sintoma | Ação |
|---|---|
| Mapa aparece, perfis não carregam | Conferir backend iniciado, URL, porta e banco. A chave Maps não configura a API do aplicativo. |
| Endereço da API ausente/inválido | Preencher `API_BASE_URL` e recompilar/instalar a variante debug. |
| Serviço indisponível, HTTP 503 | Conferir `GOOGLE_ROUTES_API_KEY` e quota do Google; usar chave de servidor. |
| Falha do provedor, HTTP 502 | Conferir Routes ativado, faturamento, restrições da chave e validade dos locais. O corpo privado do Google não é devolvido ao app. |
| Tempo excedido, HTTP 504 | Conferir conexão/serviços e tentar novamente; não há retry automático pago. |
| Limite de consultas, HTTP 429 | Aguardar a janela de um minuto. |
| Nenhuma candidata | Testar locais distintos e conferir cobertura; o app não inventa rotas. |

## Verificação desta alteração

Registro em 02/11/2026:

- `mvnw verify`: 51 testes, **48 aprovados**, **3 PostGIS ignorados**, sem falhas.
  Os testes HTTP validam o planejamento nos três perfis, endereços/coordenadas,
  máscara e modo enviados ao Google, deduplicação, geometria inválida, falta de
  rota/chave, quota, timeout e ausência de vazamento de detalhes. Google e as
  evidências são simulados nesses testes; não houve chamada paga.
- Android: compilação debug e **18 testes unitários aprovados**; lint sem erros,
  com 94 avisos de recursos/layout, textos e configuração geral, cuja limpeza
  permanece pendente. Os testes de URL distinguem debug/release e rejeitam hosts
  HTTP externos e URLs ambíguas. O build com `API_BASE_URL` local passou.
- Release: tentativa com URL HTTP foi recusada pela regra esperada; geração do
  manifesto com URL HTTPS passou. Manifesto resultante mantém
  `usesCleartextTraffic=false`, sem a política de rede debug.
- Emulador Pixel 9 API 37.1: **3 testes instrumentados aprovados**, cobrindo barras
  do sistema, consulta/estado após recriação e rede HTTP local real com endereço
  acentuado e coordenadas. O teste de rede usa servidor apenas de teste dentro do
  emulador, sem dependência do backend ou de credenciais reais.
- A inspeção visual completa em temas/tamanhos e a validação de trajetos reais
  permanecem pendentes. Testes instrumentados não substituem essa revisão.

A versão debug foi instalada e sua `MainActivity` iniciou com sucesso no emulador,
usando a URL local fornecida como propriedade da compilação. `local.properties`
e as chaves locais foram preservados; o backend ainda precisa ser preparado e
iniciado para que o catálogo e as rotas funcionem.

O ambiente local não contém `.env` da API configurado. A migração foi compilada e
testada com simuladores, mas o novo fluxo **ainda não foi validado com uma chave
Routes e banco reais**. A consulta real registrada em 24/10 no guia antigo é do
endpoint `/rotas/base`; não comprova o planejamento avaliado desta alteração.

## Próximas entregas

1. Configurar banco e chave Routes localmente; executar uma consulta real no
   emulador e revisar pontos de acesso e avisos.
2. Revalidar o dataset piloto sobre as geometrias Google. A associação usa a
   assinatura exata do traçado/direção; não transferir evidências ORS por proximidade.
3. Revisar a matriz dos 30 pares e os dados de campo, qualidade, custo e latência.
4. Publicar um ambiente HTTPS com segredos, cotas e limites na borda, após revisão
   humana do PR. Cadastro/login, relatos remotos, upload de fotos e refinamento
   de desvios seguem fora desta migração.

## Referências

- [Google Compute Routes, campos e GeoJSON](https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes)
- [Waypoints e endereços](https://developers.google.com/maps/documentation/routes/specify_location)
- [Modo de viagem a pé e aviso beta](https://developers.google.com/maps/documentation/routes/reference/rest/v2/RouteTravelMode)
- [Chaves Google Routes](https://developers.google.com/maps/documentation/routes/get-api-key)
- [Configuração de segurança Android](https://developer.android.com/privacy-and-security/security-config)
- [Endereços de rede do emulador](https://developer.android.com/studio/run/emulator-networking-address)
- [Guia do planejamento e limites](../apps/api/PLANEJAMENTO.md)

Os registros 12–15 descrevem etapas anteriores e permanecem como histórico. Este
documento e os READMEs atualizados descrevem o provedor e o teste local atuais.
