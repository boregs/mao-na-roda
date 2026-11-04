# Registro da implementação de mapas e rotas

Data: 01/11/2026. Branch: `feat/planejamento-rotas-visitante`.
Base: `origin/main` atualizada e fast-forward para a base AWS `5189a2a`.
Referências: RN01–RN07, RN12–RN14, RN17–RN18, RF01–RF08, RF13 e RNF03–RNF04.

## Escopo autorizado

Implementar base/contratos, consulta e avaliação piloto, e jornada Android descritas
no diagnóstico 12 e no plano externo 11. Cadastro, upload de fotos, importação OSM
ampla e agrupamento de relatos permanecem posteriores à jornada de visitante.
Nenhuma publicação ou chamada paga é necessária para os testes simulados.

## Registro de decisões e execução

- Lido CONTRIBUTING.md: branch a partir da main, commits pequenos em português,
  testes e revisão antes de merge. Main preservada. Base AWS incorporada por
  fast-forward, sem reimplementar os avanços de interface e perfil existentes.
- Configuração Gradle não rastreada preservada antes da troca de branch em uma
  cópia temporária `mao-na-roda-gradle-daemon-jvm-20261101.properties`; a main já
  contém esse arquivo. Documento 12 preservado.
- Executada suíte da base: 27 testes, 9 falhas HTTP 401, confirmando o TODO histórico.
- Removidos source/target 15; Java 25 permanece definido pelo projeto.
- Adicionados contratos de coordenadas WGS84, planejamento, cobertura, barreiras e
  estados. Catálogo usa as tabelas Flyway existentes; scripts da branch de catálogos
  não são aplicados por cima das migrations.
- Reconciliação confirmou divergência de códigos entre scripts antigos e Flyway:
  a API e o Android usam `cadeirante`, `muleta_ou_outro_auxilio` e
  `mobilidade_reduzida`, da V4. O tipo correto é `calcada_danificada_irregular`.
  O diagnóstico 12 descreve os nomes antigos como achado da outra branch; não são
  os códigos do contrato implementado. Não renomear catálogos já migrados.
- Atualização posterior (PR #32): os scripts da branch de catálogos foram levados
  à main em `database/migrations` como cópia de referência do que foi aplicado
  manualmente em produção (equivalentes a V1–V6). O seed com os códigos antigos
  foi removido. O Flyway, com a V4, continua sendo o contrato do catálogo.
- Segurança: rotas/consulta de destinos/catálogo públicos; usuários e relatos
  protegidos; JWT de emissor/audiência configuráveis. Sem configuração, tokens são
  recusados. Sem sessão, credenciais no APK ou localização em logs próprios.
- Adicionados ORS Directions e geocoder como configurações independentes, timeouts,
  limite de candidatas, deduplicação, concorrência e limite local de consultas.
- Migração V6 cria datasets e evidências piloto sem alterar V1–V5. Dados sem data
  real preservam motivo explícito; nunca são tratados como verificados.
- Associação piloto usa assinatura SHA-256 da geometria exata e orientada vinculada
  por revisão humana a trechos. Mudança de geometria exige nova revisão. Isso é
  deliberadamente restrito: não implementa um matcher geral nem prova qualidade em
  campo. Lado/nível/travessia/entrada devem ser revisados antes da publicação.
- Cobertura documentada exige as dez condições da matriz conhecidas, com data de
  verificação recente. Cobertura parcial e desconhecida não recebem garantia de
  acessibilidade. Impedimentos conhecidos são exibidos mesmo quando a cobertura é
  insuficiente. Intervalos sobrepostos não somam cobertura duas vezes.
- CI passa a preparar banco PostGIS descartável, executar Flyway e integridade
  antes da suíte. Bootstrap de auth é apenas fixture do contrato externo Supabase.

## Jornada Android implementada

- Home com SupportMapFragment, painel expansível, origem/destino manuais e busca
  explícita de locais. O usuário escolhe um resultado antes de consultar; o envio
  por botão evita buscas automáticas a cada tecla, portanto não requer debounce.
- Perfil selecionado do catálogo da API, até três rotas com distância/duração,
  cobertura, impedimentos e detalhes com fonte, referência, data ou motivo de
  verificação desconhecida. Seleção destaca a polilinha; não inicia navegação.
- ViewModel mantém rota, entradas e perfil em memória nas mudanças de configuração.
  Campos sensíveis não salvam estado de View. Nenhum SavedStateHandle, cache de
  rotas ou preferência de mobilidade foi adicionado.
- Preferências visuais foram separadas da mobilidade; opções de deficiência visual
  e auditiva foram removidas da coleta. Preferências de mobilidade legadas são
  removidas na abertura. Configurações visuais existentes foram preservadas.
- HTTP exige HTTPS válido; sem redirecionamento nem certificado ignorado. Há
  cancelamento, timeout de nove segundos e descarte de respostas atrasadas. Falha
  numa nova tentativa com os mesmos parâmetros preserva a rota anterior.
- Localização é obtida uma vez, somente após ação e consentimento. Negação,
  indisponibilidade, edição manual ou saída da tela interrompem a obtenção e
  preservam a opção manual. Nenhuma atualização contínua é iniciada.
- Maps usa chave Android restrita, obtida de configuração local não versionada.
  ORS/Google Routes não entram no APK. Sem configuração, o app apresenta estados
  reais de indisponibilidade e não usa rotas fictícias.
- Backup do aplicativo desativado nesta entrega. Anotações de barreiras continuam
  locais; sua proteção criptográfica e envio remoto permanecem pendentes. Esta
  entrega não homologa o armazenamento dessas anotações para conteúdo sensível.
- Corrigida a substituição do arquivo de anotações no Windows com preservação da
  versão anterior e recuperação de interrupção entre renomeações. O teste existente
  de histórico passou a funcionar; a correção fica em commit próprio.
- Gradle/lint Android passaram a usar JDK 21; nível de linguagem Android permanece
  Java 17 com desugaring. O lint do AGP atual falhava com JDK 25. A API continua em
  JDK 25. JDK de teste baixado em `.local-tools/`, ignorado pelo Git.

## Verificações concluídas nesta sessão

| Verificação | Resultado observado |
|---|---|
| Base AWS: Maven test | 27 testes, 9 falhas HTTP 401 antes das correções |
| API final: `mvnw.cmd --batch-mode --no-transfer-progress verify` | BUILD SUCCESS; 52 testes reportados, 49 executados sem falha e 3 PostGIS ignorados por falta de ambiente |
| JWT com assinatura RSA real nos testes | Aceita assinatura/emissor/audiência/prazo corretos; recusa assinatura divergente, expirado, emissor/audiência errados, prazo ausente e duração maior que 15 min |
| Clientes de provedores e avaliação | ORS/geocoder simulados, falhas/ausência de caminho, interrupção; Google preservado; ordem de coordenadas, ranking, deduplicação, cobertura sobreposta, contexto divergente, orçamento monotônico e limite de concorrência testados |
| Android: `assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest` | BUILD SUCCESSFUL; 13 testes locais sem falhas; APK e APK de testes gerados; lint sem erros e com avisos de apresentação/recursos |
| `adb devices` | Nenhum dispositivo conectado; testes instrumentados e avaliação visual não executados |
| `git diff --check` | Sem erros de whitespace; Git informa conversão normal LF/CRLF em arquivos Windows |

Os três testes PostGIS cobrem catálogo/migrations/permissões, publicação atômica
com rollback e consulta HTTP pública nos três perfis com ORS simulado. Foram
compilados, mas não executados localmente. Compose, script PowerShell e CI foram
preparados para executá-los; não há resultado de CI remoto nesta sessão.

## Estado das entregas e próximos gates

| Parte | Estado |
|---|---|
| Base Java, contratos, catálogo público e segurança explícita | Implementados e validados localmente nos cenários sem PostGIS |
| Cliente ORS/geocoder, normalização, deduplicação e ranking | Implementados e testados com provedores simulados |
| Proveniência, dataset e carga administrativa atômica | Código e migration implementados; execução PostGIS pendente |
| Associação espacial | Piloto de vínculo manual por geometria exata implementado; matcher geral e eficácia em campo não validados |
| Android mapa, consulta, seleção e detalhes | Implementados, compilados e testados localmente; integração visual e real pendentes |
| Corpus de 30 pares | Roteiro criado com cenários e seis casos reservados; coordenadas e revisão de campo ainda pendentes |
| Refinamento por exclusões | Não implementado nesta entrega; apenas uma chamada ORS, sem retries ou fallback automático |
| OSM amplo, relatos remotos, grupos, fotos e desvios | Mantidos como etapas posteriores ao gate de visitante, conforme escopo |

Antes de homologar: executar os testes reais PostGIS, configurar URL HTTPS/chaves,
testar identidade real, escolher/revisar a área, publicar evidências e testar o
aplicativo em dispositivo. Avaliar temas, tela pequena, texto ampliado, teclado,
TalkBack e negação/revogação de localização. Medir meta de três segundos e custo
com rede/provedores reais; orçamento de oito segundos não é benchmark atingido.

Não houve deploy, push, merge na main ou chamada paga aos provedores nesta sessão.
Os workflows foram preparados; sua execução remota ainda deve ser acompanhada.

## Commits de implementação

| Commit | Alteração |
|---|---|
| `1dae82a` | Remove configuração conflitante Java 15 da API |
| `98d9a83` | Adiciona planejamento público, ORS, evidências piloto, publicação administrativa e testes |
| `4a44868` | Prepara migrations/integração PostGIS no CI e ambiente local |
| `542839b` | Corrige substituição de anotações locais no Windows |
| `bd9b6ea` | Integra mapa, planejamento e privacidade do visitante no Android |
| `384bccd` | Adiciona verificação contínua Android |

A documentação e o corpus são registrados em commit próprio de documentação.
Todos os commits são locais nesta sessão; revisão humana continua necessária
antes de integrar na main, conforme CONTRIBUTING.md.

## Arquivos de operação

- [Contrato e execução da API](../apps/api/PLANEJAMENTO.md).
- [Carga administrativa do piloto](../apps/api/database/piloto/README.md).
- [Configuração e execução Android](../apps/android/README.md).
- [Corpus de campo](piloto/README.md) e [30 pares pendentes](piloto/corpus.csv).
- [Script PostGIS](../apps/api/tools/testar-postgis.ps1) e [Compose](../apps/api/compose.test.yml).

## Fontes técnicas consultadas

- [ORS Directions e GeoJSON](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/requests-and-return-types).
- [ORS Geocoder hospedado](https://giscience.github.io/openrouteservice/api-reference/endpoints/geocoder/).
- [Google Maps SDK para Android](https://developers.google.com/maps/documentation/android-sdk/start).
