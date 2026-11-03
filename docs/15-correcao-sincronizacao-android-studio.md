# Correção da sincronização no Android Studio

Data: 02/11/2026. Branch: `feat/planejamento-rotas-visitante`.

## Problema observado

O Android Studio exibiu `Gradle project sync failed` ao abrir o projeto. O log da
IDE confirmou `ToolchainDownloadException`: Java 21 não foi encontrado e faltava
`toolchainUrl` para Windows x86_64. O arquivo de critérios havia ficado somente
com `toolchainVersion=21` na mudança de Java 25 para Java 21.

Os testes anteriores passaram porque o terminal apontava explicitamente para a
instalação local de Java 21. O CI também fornece Java 21. Esse cenário não cobria
o Studio iniciado com Java 25 sem uma instalação de Java 21 detectável.

## Correção

Restauradas URLs de provisionamento em `gradle-daemon-jvm.properties` para
Temurin 21 em Windows x86_64, Linux x86_64/AArch64 e macOS x86_64/AArch64. O
Gradle continua exigindo Java 21 para Android e pode baixá-lo se necessário.
O código Android permanece no nível Java 17; a API permanece em Java 25.
Nenhum caminho particular de instalação foi versionado.

As URLs usam o canal de manutenção da versão 21 do Adoptium: atualizações de
correção podem mudar a versão pontual baixada, preservando a exigência de Java 21.
A primeira sincronização exige acesso ao provedor e pode demorar pelo download.

## Verificação

- Bootstrap executado com `JAVA_HOME` apontando para o Java 25 do Android Studio:
  `gradlew.bat --no-daemon help -Porg.gradle.java.installations.auto-detect=false`.
  Resultado: `BUILD SUCCESSFUL`, com Temurin 21.0.13 provisionado no cache de JDKs
  do Gradle. Não foi necessário usar o caminho particular de Java 21 em JAVA_HOME.
- Executados `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` e
  `testDebugUnitTest` a partir do mesmo Java 25. Resultado: `BUILD SUCCESSFUL`;
  78 tarefas verificadas, 1 executada e 77 atualizadas em cache. Os testes locais
  anteriores não foram forçados a executar novamente, pois o código não mudou.
- Sincronização acionada na janela do projeto pelo atalho `Ctrl+Shift+O`.
  O log do Android Studio confirmou `onSuccess(RESOLVE_PROJECT:2)`,
  `onImportFinished` e `Gradle sync finished in 27 s 519 ms` em 02/11/2026.
- A conferência da sincronização foi feita pelo log da IDE. A captura visual foi
  bloqueada por revisão automática por risco de exibir conteúdo de outra janela;
  não foram feitas novas capturas. Nenhuma tela do aplicativo foi validada
  visualmente nesta correção.

O próximo passo para o teste de visitante é iniciar um emulador ou conectar um
celular e executar `VisitorJourneyTest`. Esse teste usa respostas simuladas e não
exige ORS nem uma API publicada. A consulta real do aplicativo mantém as
dependências de configuração descritas no README.

## Referência

[Gradle: critérios e provisionamento da JVM do daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:daemon_jvm_criteria).
