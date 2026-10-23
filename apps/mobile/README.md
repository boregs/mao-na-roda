# Aplicativo Mão na Roda

Aplicativo Flutter para Android que ajuda pessoas com mobilidade reduzida a consultar rotas considerando condições de acessibilidade e barreiras relatadas.

## Requisitos

- Flutter stable 3.47.7 (Dart 3.13.5)
- Android Studio com os plugins Flutter e Dart
- Android SDK com suporte a API 24 ou superior
- Um emulador Android ou dispositivo físico com depuração USB habilitada

O projeto deve ser aberto em `apps/mobile/`, e não na raiz do repositório, para que Android Studio e VS Code reconheçam corretamente o projeto Flutter.

## Configuração

1. Instale o Flutter e adicione `flutter/bin` ao `PATH`.
2. Abra o Android Studio e confirme o Android SDK, Platform-Tools e um emulador no Device Manager.
3. Aceite as licenças com `flutter doctor --android-licenses`.
4. Execute `flutter doctor -v` e corrija pendências do Android toolchain.
5. Na pasta deste projeto, execute:

```powershell
flutter pub get
```

Não versionamos caminhos locais do SDK, arquivos de configuração do usuário ou segredos.

## Android Studio

Instale os plugins **Flutter** e **Dart**, selecione o SDK do Flutter em **Settings > Languages & Frameworks > Flutter** e abra esta pasta pelo menu **Open**. Selecione um dispositivo Android, abra `lib/main.dart` e pressione **Run**.

## Visual Studio Code

Instale as extensões **Dart-Code.dart-code** e **Dart-Code.flutter**. Abra a raiz do repositório ou `apps/mobile/`; a configuração em `.vscode/launch.json` define o diretório correto. Pressione **F5** para iniciar e use o hot reload após salvar arquivos Dart.

## Comandos

Execute os comandos a partir de `apps/mobile/`:

```powershell
flutter run
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk --debug
```

O aplicativo suporta Android API 24 ou superior. A interface inicial usa Material 3, suporta escala de texto do Android e deve ser validada com TalkBack antes de cada entrega relevante.
