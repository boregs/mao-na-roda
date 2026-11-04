# Diagnóstico e próximas entregas do aplicativo

Data: 01/11/2026. Status: análise técnica e plano de execução; nenhuma funcionalidade implementada nesta revisão.

## 1. Escopo e referência da análise

Solicitação: analisar como dar seguimento ao aplicativo e documentar o trabalho.

A referência principal é `origin/ci/implantacao-api-aws`, commit
`5189a2a752a5133002e7b861e15afb8d7d767e5a`, consultado no GitHub nesta sessão.
A árvore de trabalho permanece em `feat/7-base-api-java`; os arquivos da branch
AWS foram lidos diretamente pelo Git, sem trocar a branch nem mesclar alterações.
Este documento foi criado na árvore de trabalho atual e ainda não foi commitado.

Também foram considerados as regras RN, os requisitos RF/RNF, o guia de
contribuição e o arquivo externo `11-plano-implementacao-mapas-rotas.md`.
O arquivo externo é uma proposta, não comprovação de funcionalidades existentes.
As decisões abaixo são recomendações e não alteram automaticamente regras aprovadas.

## 2. Diagnóstico baseado no código

| Área | Evidência na branch AWS | Consequência |
|---|---|---|
| Rota-base | `apps/api/src/main/java/com/maonaroda/api/controller/RotasController.java` e `service/GoogleRoutesService.java` | Existe `POST /api/v1/rotas/base`, com rota pedestre Google e aviso de ausência de avaliação de acessibilidade. Pode ser reutilizado para validar a integração. |
| Testes de rotas | `apps/api/src/test/java/com/maonaroda/api/rotas/RotasIntegrationTests.java` | Já simulam Google localmente; não exigem chave real para os cenários simulados. Não comprovam funcionamento no ambiente publicado. |
| Segurança | Dependência OAuth2 resource server e uso de `Jwt` em `UsuarioController`; nenhuma classe própria de configuração de segurança encontrada na árvore Java | Falta tornar explícitos os endpoints públicos, as operações protegidas e a validação de tokens. Não resolver os 401 desabilitando a segurança globalmente. |
| Cadastro | `UsuarioController.criar` retorna HTTP 501 | Tela de cadastro não representa criação funcional de conta. |
| Perfil no servidor | `AtualizarUsuarioRequest`, `UsuarioService`, `UsuarioRepository` e testes novos | Já há atualização com validação de UUID; reaproveitar em vez de reescrever. |
| Leitura do perfil | `UsuarioRepository.find` seleciona apenas nome e sobrenome e o GET retorna `UsuarioModel` | Contrato de saída ainda não entrega um perfil completo; usar DTO explícito e testar os campos necessários ao app. |
| Banco | Migrations V1–V5, PostGIS, perfis, matriz, locais, observações, relatos e fotos | Há estrutura inicial, não os serviços completos de evidências ou relatos. |
| Data de evidência | V2 exige `verificado_em NOT NULL` e distingue `importado_em` | Fonte sem data real não pode ser importada como verificada; preparar staging ou uma evolução explícita para data desconhecida. |
| Android | `HomeFragment.showSearch` avisa que busca e cálculo não estão conectados | Formulário de origem/destino é reaproveitável como interface, mas não consulta a API. |
| Mapa e rede Android | Sem Maps SDK/cliente HTTP nas dependências; manifesto sem permissões de rede/localização | Falta a integração de rede e do mapa. Localização pode ser adiada usando origem manual. |
| Relatos Android | `BarrierReportStore` grava arquivo privado local; `BarrierReport` contém categoria, descrição, data e URI da foto, sem coordenadas | São registros locais, não relatos georreferenciados enviados. Não há agrupamento espacial nem sincronização. |
| Privacidade local | Relatos em arquivo texto e `android:allowBackup="true"` | Revisar proteção e política de backup antes de aceitar dados reais; não presumir conformidade com RNF19. |
| Perfis Android | `AccessibilityProfileStore` persiste seleção múltipla, incluindo visual/auditivo, e migra a preferência antiga | Não corresponde diretamente aos três perfis da matriz V1. Separar preferências visuais de perfil de rota e revisar a coleta de tipo de deficiência frente à RN15. |
| AWS | `.github/workflows/api-beanstalk.yml`, `apps/api/deploy/Procfile`, `docs/08-implantacao-aws.md` | Há preparação de publicação; não comprova recursos provisionados, deploy executado ou ambiente saudável. |
| CI e banco | Workflow executa Maven verify, mas não prepara serviço PostgreSQL/PostGIS nem executa migrations explicitamente | É preciso adicionar uma etapa reproduzível antes de tratar o pipeline como validação do banco. |
| Java API | `java.version=25` e plugin com `source/target=15` | Configuração contraditória na branch AWS; corrigir e verificar compilação. A branch local atual não contém esse mesmo conflito. |
| ORS e domínio espacial | Nenhum cliente ORS, importador OSM, associador ou ordenador encontrado na árvore Java | O núcleo do plano externo permanece pendente. |

O `apps/api/TODO.md` da branch registra, em 31/10/2026, 27 testes com 9 falhas
HTTP 401. Trata-se de resultado histórico documentado, não de execução nesta análise.

## 3. Decisão sobre a próxima entrega

Recomenda-se entregar primeiro uma consulta de visitante integrada:
origem manual + destino manual → API → rota-base → desenho no mapa → distância,
duração e aviso de acessibilidade não avaliada.

Essa entrega valida transporte, contrato, estados e geometria usando código já
existente. Não conclui RF03/RF04 nem substitui o piloto com evidências. A consulta
Google existente é uma referência explicitamente escolhida; quando ORS estiver
integrado, sua indisponibilidade não deve acionar Google silenciosamente.

Não é necessário esperar cadastro, fotos ou importação OSM ampla para validar
essa jornada. A API precisa, antes, separar acesso público de operações protegidas.

## 4. Entregas em ordem e critérios de conclusão

| ID | Trabalho | Dependências | Critério de conclusão |
|---|---|---|---|
| P01 | Consolidar uma base de trabalho a partir da branch AWS e reconciliar catálogos | Comparar histórico, scripts e migrations das branches | Uma única fonte de verdade para perfis/matriz; sem aplicar seeds ou migrations duplicadas. Preservar migrations já aplicadas e evoluir por novas versões. **Atualização (PR #32):** Flyway em `apps/api` é a fonte da verdade; `database/migrations` ficou como cópia de referência do que foi aplicado manualmente em produção e o seed com códigos antigos foi removido. |
| P02 | Corrigir configuração Java e preparar PostgreSQL/PostGIS de testes | P01 | API compila com Java 25; banco isolado nasce vazio, executa V1–V5 e permite operações com o papel da aplicação. |
| P03 | Definir regras públicas e JWT | P02; configuração do emissor para teste real | Rota-base e health acessíveis conforme contrato; `/usuarios/me` sem token retorna 401; tokens inválidos são recusados; usuário não acessa dados de outra pessoa. Testes locais usam tokens/decodificador de teste sem alterar segurança de produção. |
| P04 | Completar testes no CI | P02–P03 | Maven verify passa com PostgreSQL/PostGIS; testes simulados de Google cobrem sucesso, entrada inválida e falhas. Publicação depende da suíte completa. |
| P05 | Ligar origem/destino manual à API Android | Contrato estável de P03; URL HTTPS do ambiente para integração real | Carregamento, resposta vazia, erro, ausência de conexão e cancelamento funcionam. Consultas anteriores não sobrescrevem a atual. Nenhuma chave de servidor no APK. |
| P06 | Exibir rota-base no mapa | P05; chave Maps SDK restrita ao aplicativo | Polilinha é convertida corretamente, mapa acomoda painel, distância/duração e aviso são legíveis; não inicia navegação. Entrada manual funciona sem localização. |
| P07 | Corrigir ciclo de vida e privacidade do visitante | P05–P06 | Rota/perfil ficam em memória, sobrevivem à rotação conforme ViewModel e são descartados ao encerrar a sessão; não aparecem em preferências, logs ou backup. Preferências visuais são tratadas separadamente. |
| P08 | Definir contratos e corpus do piloto acessível | P01; escolha da área | Estados documentado/desconhecido/bloqueado separados; GeoJSON `[longitude, latitude]`; 30 pares origem/destino com três perfis e casos críticos. |
| P09 | Testar ORS e preparar evidências piloto | P08; chave para teste externo e revisão manual dos dados | Cliente com testes simulados, geometria normalizada, limites medidos; proveniência e data desconhecida preservadas; sem inferir acessibilidade de tags ausentes. |
| P10 | Associar evidências e avaliar rotas | P09 | Casos de calçadas opostas, vias paralelas, níveis e entradas passam; ambiguidades ficam desconhecidas. Matriz e dataset usam uma versão consistente por consulta. |
| P11 | Integrar ranking e detalhes no Android | P10 | Até três candidatas, cobertura e impedimentos explícitos; zero barreiras com dados ausentes não vira selo acessível; jornada completa testada no piloto. |
| P12 | Completar conta e relatos georreferenciados | Segurança, contrato de identidade e localização consentida | Cadastro funcional, envio com JWT e localização; sem foto obrigatório; limites, autorização e estado não validado testados. |
| P13 | Fotos, grupos e desvios | P12 e jornada de rotas validada | Fotos validadas/removidas conforme retenção; grupos com âncora fixa e limite 3 m; desvio solicitado explicitamente; falha mantém rota anterior. |
| P14 | Ingestão OSM ampla, carga e homologação | Piloto espacial aprovado | Importação idempotente/versionada fora do HTTP; custo/latência medidos; testes em campo nos três perfis; publicação e recuperação verificadas. |

P01–P04 formam a próxima entrega técnica recomendada. P05–P07 formam a primeira
entrega integrada para demonstração. P08 pode ser preparado em paralelo como
documentação e coleta manual, sem depender da disponibilidade da AWS.

## 5. Decisões de domínio que precisam ficar explícitas

- RN01/RN04: priorizar acessibilidade; uma rota com impedimento nunca recebe selo de acessível.
- RN06/RN07: data de importação não é data de verificação. Definir apresentação de data desconhecida sem inventar validade. A regra aprovada exige data; o tratamento de fontes sem data precisa ser documentado como complemento/proposta, não alteração silenciosa.
- RN09/RN12: relatos não validados são avisos; não alteram automaticamente classificação ou rota. Três contribuições da mesma pessoa não são três validações. Confirmações são V2.
- RN18: perfil de mobilidade de visitante não deve ser persistido. Auditar `AccessibilityProfileStore` e onboarding para separar mobilidade de preferências visuais.
- Perfis: o catálogo (Flyway V4) usa `cadeirante`, `muleta_ou_outro_auxilio` e `mobilidade_reduzida`; a interface usa outras chaves e permite múltiplas seleções. Definir mapeamento explícito e um perfil de rota V1, sem transformar seleção visual/auditiva em classificação que a matriz não suporta. RN15 proíbe coletar tipo de deficiência; registrar e corrigir a divergência da interface.
- RNF03: alvo de três segundos continua sendo o requisito de experiência; orçamento proposto de API 8 s/Android 9 s/UX 10 s é limite de espera, não prova de cumprimento do alvo. Medir proporção de consultas atendidas e cenários de refinamento.
- Ranking: definir em exemplos o comportamento de uma candidata documentada com impedimento frente a uma candidata desconhecida. O plano externo prioriza documentadas; isso precisa de avaliação de produto e comunicação clara antes de ser cristalizado em código.
- Cadastro: escolher o responsável pela identidade e o significado de `POST /usuarios/me`; não implementar um segundo armazenamento de senha sem necessidade. O modelo usa `auth_user_id` e a documentação cita Supabase, mas a validação real do emissor ainda precisa ser demonstrada.

## 6. O que pode avançar agora e dependências externas

Pode avançar sem segredos reais: correção do build, contratos/fixtures, cliente
Android contra servidor simulado, testes de segurança com identidade de teste,
PostGIS local quando o runtime estiver disponível, parser de geometria, estados
de tela, ciclo de vida, revisão do armazenamento e especificação do piloto.

Para comprovar a integração externa são necessários: URL HTTPS da API, configuração
do banco com TLS, emissor e chaves públicas JWT, chave Google de servidor, chave
Maps SDK restrita e chave ORS conforme o experimento. Registrar somente nomes das
variáveis e instruções; valores secretos ficam fora do código e deste documento.

Área piloto, pontos revisados e avaliação em campo não podem ser substituídos por
rotas simuladas. A análise não pressupõe acesso a credenciais ou recursos AWS.

## 7. Verificação por entrega

API: testes HTTP de acesso público/protegido, contratos de erro, isolamento entre
usuários, timeouts e provedor simulado; integração JDBC e migrations em PostGIS
real. H2 é útil para testes isolados, mas não substitui PostGIS.

Android: build, lint e testes de ViewModel/repositório; no dispositivo/emulador,
origem manual, rotação, resposta atrasada, ausência de rede, tema claro/escuro,
texto ampliado e TalkBack. Localização, quando adicionada, exige casos de negação
e revogação e alternativa manual preservada.

Piloto: comparar associação com revisão humana; registrar lacunas e falhas por
perfil, latência e custo incluindo refinamentos. Se houver associação ao caminho
errado, reduzir a área/rever o modelo antes de ampliar a ingestão.

## 8. Registro desta análise

| Etapa | Trabalho realizado | Resultado |
|---|---|---|
| 1 | Consultadas branches locais/remotas e obtida a referência AWS | Branch correta identificada; leitura sem checkout. |
| 2 | Comparadas árvores da branch atual e AWS | AWS contém avanços de interface, usuário e publicação, mas não o núcleo de rotas acessíveis. |
| 3 | Inspecionados build, manifesto, HomeFragment e armazenamento de relatos | Confirmados formulário desconectado, ausência de SDK de mapa/rede e relatos locais sem coordenadas. |
| 4 | Inspecionados controller/repository de usuários, testes, migrations e workflow | Confirmados cadastro 501, saída incompleta, conflito Java e CI sem preparação PostGIS. |
| 5 | Localizados e lidos `database/migrations/0001_catalogos.sql` e `database/seeds/0001_catalogos.sql` em `feat/schema-catalogos` | A outra branch cria as mesmas três tabelas de catálogo e carrega a matriz por scripts próprios. Há sobreposição com Flyway da AWS, não uma entrega independente para aplicar por cima. Comparação exaustiva de constraints/dados e reconciliação permanecem em P01. Posteriormente, o PR #32 levou esses scripts à main como cópia de referência de produção (equivalentes a V1–V6) e removeu `database/seeds/0001_catalogos.sql`. |
| 6 | Relacionados achados às RN/RNF e ao plano externo | Definida sequência P01–P14 e critérios verificáveis. |
| 7 | Inspecionado `AccessibilityProfileStore` | Confirmadas persistência de perfis e seleção visual/auditiva; adicionadas divergências de RN18, RN15 e matriz V1 ao diagnóstico. |
| 8 | Revisado o documento e consultado `git status --short` | Somente o novo Markdown foi acrescentado nesta tarefa; arquivo Gradle preexistente preservado. |

Limites: não foram executados build, testes, deploy, chamadas pagas, aplicativo
ou inspeção visual nesta revisão. Os resultados de execução citados são históricos
e atribuídos ao TODO da branch. As referências remotas de outras branches não
foram atualizadas nesta etapa; a referência AWS foi obtida na sessão.

Alterações desta tarefa: apenas este documento. O arquivo não rastreado
`apps/android/gradle/gradle-daemon-jvm.properties` já existia e foi preservado.

## 9. Como manter o registro nas próximas implementações

Para cada Pxx, adicionar data, commit de base, arquivos alterados, decisões,
comandos de verificação e resultado real. Usar estados: pendente, em andamento,
implementado sem validação externa, validado localmente e homologado.
Não marcar uma entrega concluída apenas por existir tela, tabela ou dependência.
Falhas devem registrar cenário, resultado observado e ação seguinte, sem segredos.

Todas as entregas P01–P14 permaneciam pendentes ao término desta análise inicial.
O trabalho implementado posteriormente está registrado em
[13-registro-implementacao-rotas.md](13-registro-implementacao-rotas.md).
