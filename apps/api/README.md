# API Mão na Roda

Primeira etapa da issue #7: base Spring Boot com verificação de saúde,
OpenAPI/Swagger e testes HTTP. Requer JDK 25; o Maven Wrapper baixa o Maven
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

## Verificar e empacotar

```powershell
.\mvnw.cmd verify
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Os testes iniciam a API em porta aleatória, verificam saúde, documentação,
interface Swagger e ausência de exposição do endpoint de variáveis de ambiente.
No Linux/macOS, use `sh mvnw verify` ou `sh mvnw spring-boot:run`.

## Limites desta entrega

A saúde indica apenas que o processo está funcionando. Não verifica Google Maps
ou Supabase. Ainda não há endpoints de contas, relatos ou rotas, autenticação,
persistência ou integração com o aplicativo. A issue #7 não está concluída.

Google Maps foi escolhido por Felipe para a integração. A próxima etapa deve
validar Routes API e sua compatibilidade com as regras de barreiras e desvios;
uma rota-base do Google não é automaticamente uma rota acessível.
Não são necessárias credenciais para executar esta base. Não coloque chaves no
código: a configuração da integração será definida na etapa correspondente.

Referências: `docs/05-tech-stack.md`, RN01–RN04, RN12, RNF07, RNF10 e RNF15.
