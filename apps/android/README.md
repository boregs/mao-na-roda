# Mão na Roda — aplicativo Android

Aplicativo Android nativo em Java, com layouts e estilos XML, AndroidX e Material Components. A migração da implementação anterior em Flutter está em andamento.

## Estado atual

- Onboarding, tela de autenticação com validação local e entrada sem conta.
- Tela inicial com painel expansível e preferência local de mobilidade.
- Mapa, busca de destinos, rotas, relatos e autenticação ainda não estão conectados aos serviços. A interface informa essas limitações.

## Compilação e verificações

Abra `apps/android` no Android Studio ou use o Gradle Wrapper deste diretório. Configure o Android SDK localmente no Android Studio; `local.properties` não deve ser versionado. O projeto usa Java 17 para compilação.

No Windows, com um JDK compatível configurado em `JAVA_HOME`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Para testes instrumentados, conecte um emulador ou dispositivo e execute `:app:connectedDebugAndroidTest`. As verificações visuais dependem de um dispositivo ou emulador disponível.
