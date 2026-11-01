# Mão na Roda — aplicativo Android

Aplicativo Android nativo em Java, com layouts e estilos XML, AndroidX e Material Components. A migração da implementação anterior em Flutter está em andamento.

## Estado atual

- Onboarding, tela de autenticação com validação local e entrada sem conta.
- Planejamento de visitante com origem/destino manuais ou seleção de locais pela API.
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

São exemplos a substituir localmente. Não versionar o arquivo. A URL exige HTTPS
com certificado válido; o app não permite HTTP nem ignora certificados inválidos.
A chave Maps deve ser restrita ao applicationId e ao certificado de assinatura
usado no teste. Chaves ORS, geocoder e Google Routes são somente do servidor.
Também é possível fornecer `-PAPI_BASE_URL=...` e `-PMAPS_API_KEY=...` ao Gradle.

Sem chave Maps, o painel continua disponível e informa que o mapa não foi
configurado. Sem URL/API disponível, uma consulta mostra indisponibilidade.
O catálogo vem de `GET /api/v1/perfis-mobilidade`; selecionar um dos três perfis
antes de consultar. Buscar endereço exige selecionar o resultado; coordenadas
manuais usam `latitude, longitude`, com ponto decimal. Os limites/estados da API
estão em [planejamento](../api/PLANEJAMENTO.md).

## Compilação e verificações

Abra `apps/android` no Android Studio ou use o Gradle Wrapper deste diretório.
Configure o SDK localmente; `local.properties` não deve ser versionado.
Use **JDK 21 para executar o Gradle/lint**. O código Android continua com nível
Java 17 e desugaring para API 24+. A API Spring usa JDK 25 separadamente.

No Windows, com um JDK compatível configurado em `JAVA_HOME`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Para testes instrumentados, conecte um emulador ou dispositivo e execute `:app:connectedDebugAndroidTest`. As verificações visuais dependem de um dispositivo ou emulador disponível.

Os testes locais cobrem cancelamento, respostas atrasadas, preservação da rota
após falha, GeoJSON e limpeza de preferências legadas. O teste instrumentado de
visitante usa um repositório de teste; não faz chamadas pagas nem substitui a
homologação com Maps/ORS reais. Ver o [registro de implementação](../../docs/13-registro-implementacao-rotas.md).
