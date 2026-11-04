# Mão na Roda — aplicativo Android

Aplicativo Android nativo em Java, com layouts e estilos XML, AndroidX e Material Components. A migração da implementação anterior em Flutter está em andamento.

## Estado atual

- Onboarding, tela de autenticação com validação local e entrada sem conta.
- Planejamento de visitante com endereços completos ou coordenadas, resolvidos pelo Google Routes no backend.
- Maps SDK, até três candidatas, cobertura de evidências e detalhes de barreiras.
- Perfil de mobilidade e rota somente em memória; preferências visuais podem ser salvas.
- Localização obtida uma vez após ação e consentimento, com alternativa manual.
- Anotações de barreiras continuam locais, sem envio nem georreferenciamento. Contas não estão conectadas.
- Mapa e rede exigem configuração abaixo; ausência de chave/serviço é informada, sem dados simulados no aplicativo.

## Configurar mapa e API

Em `apps/android/local.properties`, preservando `sdk.dir`, adicionar:

```properties
API_BASE_URL=https://seu-ambiente-de-testes.example
MAPS_API_KEY=chave-android-restrita
```

São exemplos a substituir localmente. Não versionar o arquivo. Serviços remotos
exigem HTTPS com certificado válido. Em debug, HTTP é permitido somente para
`10.0.2.2`, `127.0.0.1` e `localhost`; veja o teste local abaixo.
A chave Maps deve ser restrita ao applicationId e ao certificado de assinatura
usado no teste. A chave Google Routes é separada e fica somente no servidor,
em `apps/api/.env` como `GOOGLE_ROUTES_API_KEY`. ORS não é mais necessário.
Também é possível fornecer `-PAPI_BASE_URL=...` e `-PMAPS_API_KEY=...` ao Gradle.

Sem chave Maps, o painel continua disponível e informa que o mapa não foi
configurado. Sem URL/API disponível, uma consulta mostra indisponibilidade.
O catálogo vem de `GET /api/v1/perfis-mobilidade`; selecionar um dos três perfis
antes de consultar. Informe endereços completos com cidade diretamente nos campos
Origem e Destino; não há busca separada/autocomplete. Confira o trajeto retornado.
Coordenadas manuais usam `latitude, longitude`, com ponto decimal. Os limites/estados da API
estão em [planejamento](../api/PLANEJAMENTO.md).

### Testar sem publicar a API

Prepare o banco, a chave de servidor e inicie o backend conforme o
[passo a passo local](../../docs/16-migracao-google-routes.md#teste-local-no-android-studio).
No emulador padrão do Android Studio, configure:

```properties
API_BASE_URL=http://10.0.2.2:8080
```

Preserve `sdk.dir` e `MAPS_API_KEY`, sincronize o Gradle e execute a configuração
**app**, variante **debug**. `localhost` dentro do emulador aponta para o próprio
emulador; `10.0.2.2` alcança o computador. Em um aparelho USB, pode-se usar
`adb reverse tcp:8080 tcp:8080` com `API_BASE_URL=http://127.0.0.1:8080` em debug.
O perfil `local` da API escuta apenas em `127.0.0.1` no computador.

O manifesto debug restringe a exceção HTTP aos três hosts locais; certificados
HTTPS continuam validados. O build release recusa `API_BASE_URL` HTTP e não inclui
a configuração de rede debug. Para release, substitua a URL local por uma HTTPS.

## Compilação e verificações

Abra `apps/android` no Android Studio ou use o Gradle Wrapper deste diretório.
Configure o SDK localmente; `local.properties` não deve ser versionado.
Use **JDK 21 para executar o Gradle/lint**. O código Android continua com nível
Java 17 e desugaring para API 24+. A API Spring usa JDK 25 separadamente.

Se o Java 21 não for encontrado, `gradle/gradle-daemon-jvm.properties` fornece URLs
para o Gradle baixar Eclipse Temurin 21 automaticamente. A primeira sincronização
pode precisar de internet e levar mais tempo. Não é necessário trocar o Java da
API nem gravar caminhos particulares no repositório.

Se o Studio informar `No defined toolchain download url`, atualize a branch e
clique em **Try Again** para repetir a sincronização. Em ambientes sem acesso ao
download, instale JDK 21 e configure sua descoberta pelo Gradle antes de sincronizar.

No Windows, com um JDK compatível configurado em `JAVA_HOME`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Para testes instrumentados, conecte um emulador ou dispositivo e execute `:app:connectedDebugAndroidTest`. As verificações visuais dependem de um dispositivo ou emulador disponível.

Os testes locais cobrem endereços/coordenadas, política de URL, cancelamento,
respostas atrasadas, preservação da rota após falha, GeoJSON, avisos e limpeza
de preferências legadas. O teste instrumentado de visitante usa um repositório
de teste; o de rede usa um servidor HTTP dentro do emulador. Não fazem chamadas
pagas nem substituem a homologação com Maps/Routes reais. Ver o
[registro da migração](../../docs/16-migracao-google-routes.md).

## Acompanhar um percurso

Abra os detalhes da candidata escolhida e toque em **Iniciar percurso**. Autorize a localização precisa e habilite o GPS. O mapa, quando configurado, mostra a posição durante a sessão. Os controles permitem recentralizar, pausar, retomar e encerrar. Sair da tela interrompe a coleta. A retomada é explícita e depende de a sessão ainda estar em memória.

A API de planejamento retorna `etapas` em cada candidata, com instrução, distância e geometria GeoJSON. Se o provedor ou uma versão anterior da API não fornecer etapas, o app orienta a seguir o trajeto no mapa. A distância restante é uma estimativa proporcional ao comprimento da geometria. Os avisos usam os intervalos de evidência da rota e permanecem deduplicados na sessão. Ao tocar em **Buscar outra rota**, a posição atual é enviada como origem à API de planejamento. A nova consulta usa o mesmo perfil, e a rota atual permanece até o aceite da substituta. A sessão e as leituras de GPS ficam em memória e não são registradas em histórico.

Os parâmetros iniciais ficam em `mapa/Percurso.java` e no controlador de localização. Ajuste após homologação em campo.

| Parâmetro | Regra inicial |
| --- | --- |
| GPS | Solicitação a cada 2 s, precisão de até 20 m e leitura de até 8 s. Após 10 s sem leitura recente, o app volta a aguardar sinal. |
| Progresso | Usa a janela próxima ao último progresso. Exige distância lateral de até 15 m, limitada também pela precisão mais 5 m. |
| Etapa | Duas leituras consecutivas além do limite da etapa, descontada a incerteza da leitura. |
| Evidência | Alerta até 40 m antes do trecho associado, uma vez por evidência na sessão. |
| Desvio | Três leituras consecutivas fora de 30 m mais a incerteza, em relação à janela acompanhada. |
| Chegada | Três leituras consecutivas nos últimos 15 m e a até 15 m do destino, somada a incerteza. |

Para homologar, simule um percurso com duas etapas, uma barreira e um desvio sustentado no emulador. Repita em via paralela e com precisão ruim, pause e coloque o app em segundo plano, e confira que a retomada exige ação. Simule falha no replanejamento e confira que a rota original continua disponível. Em dispositivo, confira GPS, Maps e Routes reais, fonte ampliada e TalkBack. Os testes automatizados cobrem o vínculo de etapas à candidata, progresso, GPS impreciso, via paralela, deduplicação, desvio e chegada.

A validação local passou na compilação, no lint e nos testes unitários do Android, incluindo os três testes de percurso. O cenário instrumentado de consulta e rotação do visitante passou no emulador Pixel 9. A suíte padrão da API passou com Google Routes simulado. Os oito testes dependentes de PostgreSQL/PostGIS foram ignorados pela configuração local.
