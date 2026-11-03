# Análise de aplicativos e mapeamento de fluxos – Mão na Roda

> **Versão:** 0.1 · **Data:** 31/10/2026  
> **Tarefa relacionada:** #9 — Mapear funcionalidades com base em outros aplicativos de rotas  
> **Produto:** V1 · **Status:** Proposto para revisão da equipe  
> **Documentos relacionados:** [Regras de negócio](02-regras-de-negocio.md) · [Requisitos](03-requisitos.md) · [Casos de uso](04-casos-de-uso.md) · [Tech stack](05-tech-stack.md)

## 1. Objetivo e método

Mapear funcionalidades de aplicativos de mapas e os fluxos de ação relevantes para o Mão na Roda. O resultado orienta o desenho das telas e a divisão das próximas tarefas, preservando os requisitos já aprovados.

A pesquisa utiliza documentação pública dos aplicativos e inspeção do código Android deste repositório. As funcionalidades das referências são separadas das propostas para o Mão na Roda. Não foram realizados testes de uso nesses aplicativos, entrevistas ou inspeção de sequências de telas no Mobbin; portanto, este documento não constitui avaliação visual nem teste de usabilidade.

A descrição visível da issue sugere o Mobbin, mas a captura fornecida termina no título “Escopo”. Eventuais critérios adicionais dessa seção precisam ser conferidos antes de encerrar a issue. A referência da issue a Flutter está desatualizada em relação à implementação atual em Android nativo com Java e XML.

## 2. Aplicativos de referência

| Referência | Funcionalidade documentada | Aplicação proposta no Mão na Roda | Limite da comparação |
|---|---|---|---|
| Google Maps | A documentação de acessibilidade descreve a escolha de rotas acessíveis para cadeira de rodas no transporte público. | Permitir selecionar critérios antes de consultar a rota e deixar claro o alcance da informação apresentada. | A opção de transporte público não comprova acessibilidade de todo percurso a pé e não cobre todos os perfis do projeto. |
| Apple Maps | O usuário pode selecionar um ponto ou endereço, solicitar direções e consultar um percurso a pé. | Organizar a sequência destino → consulta → apresentação do trajeto. | Recursos e disponibilidade variam por região; não são requisitos automáticos para a V1. |
| Wheelmap | O mapa permite encontrar locais com informação de acessibilidade; a FAQ diferencia locais com informação desconhecida. | Distinguir falta de informação de acessibilidade conhecida e permitir examinar detalhes de locais. | A acessibilidade de um estabelecimento não garante a acessibilidade do caminho até ele. |

Fontes: [Google Maps — acessibilidade](https://support.google.com/maps/answer/6396990?co=GENIE.Platform%3DDesktop&hl=en), [Apple Maps — direções a pé](https://support.apple.com/en-gb/guide/iphone/iph8eae299d1/ios), [Wheelmap — mapa](https://wheelmap.org/) e [Wheelmap — FAQ](https://news.wheelmap.org/en/FAQ/). Consultadas em 31/10/2026.

O [Mobbin](https://mobbin.com/) é uma referência complementar para uma futura revisão visual de busca, detalhes de destino e apresentação de rotas. Nenhuma tela específica desse catálogo é tratada como evidência nesta análise.

## 3. Mapeamento de funcionalidades da V1

As decisões abaixo são propostas de aplicação das referências aos requisitos existentes. Não representam novas regras aprovadas.

| Funcionalidade | Ação da pessoa | Resposta esperada | Rastreabilidade |
|---|---|---|---|
| Entrada sem conta | Escolhe continuar como visitante. | Libera a consulta; não persiste rota nem perfil de mobilidade. | RF13; RN17, RN18; UC08 |
| Mapa e localização | Abre o mapa e escolhe usar sua posição. | Solicita localização quando necessária; trata recusa sem bloquear a origem manual. | RF01; RN14; UC01 |
| Busca de destino | Digita um local e seleciona um resultado. | Apresenta resultados e permite confirmar o destino. | RF02; UC02 |
| Perfil de mobilidade | Seleciona um perfil antes da consulta. | Usa a matriz de barreiras correspondente, mantendo o perfil de visitante apenas em memória. | RF08; RN01, RN02, RN18; UC03 |
| Consulta de rota | Confirma origem, destino e perfil. | Apresenta percurso, distância, tempo e resumo de barreiras, priorizando acessibilidade. | RF03, RF04; RN01–RN04; UC04 |
| Qualidade da informação | Abre os detalhes do percurso. | Mostra origem e última verificação dos dados, sinaliza desatualização e informa que a rota é uma sugestão. | RF05, RF06; RN06, RN07, RN13; UC04 |
| Consulta de relatos | Abre um possível obstáculo. | Exibe descrição e evidências disponíveis, distinguindo relato não validado de informação verificada. | RF14; RN10, RN11 |
| Alternativa por relato | Seleciona relatos e pede um desvio. | Busca alternativa para os trechos associados, sem alterar automaticamente a rota padrão. | RF07; RN12; UC05 |
| Cadastro e login | Preenche os dados e confirma. | Valida os campos e comunica o resultado real da autenticação. | RF10, RF11; RN15; UC06, UC07 |
| Envio de relato | Informa descrição e localização; adiciona fotos opcionais. | Exige autenticação, aceita até três fotos e registra o relato como não validado. | RF09, RF18; RN08, RN11; UC09 |
| Exclusão da conta | Solicita e confirma a exclusão. | Remove os dados pessoais e aplica o processo de retenção e descarte dos relatos e fotos. | RF12; RN16; UC10 |

## 4. Fluxos de ação propostos

### 4.1 Consultar rota sem conta — UC01 a UC04 e UC08

**Ator:** visitante. **Entrada:** aplicativo aberto. **Resultado:** rota e limitações apresentadas, sem persistir rota ou perfil.

1. Escolher “Continuar sem conta” na apresentação inicial.
2. Acessar a busca e informar um destino.
3. Selecionar o resultado e confirmar a origem manualmente ou escolher usar a localização atual.
4. Se usar localização, responder à solicitação de permissão.
5. Escolher o perfil de mobilidade e confirmar a consulta.
6. Examinar a rota, distância, tempo e barreiras.
7. Consultar fonte, última verificação e aviso sobre as limitações dos dados.

**Alternativas:** se a permissão for negada, permitir origem manual; se não houver destino correspondente, permitir editar a busca; se não houver rota sem barreiras impeditivas, informar isso e apresentar a opção com menos barreiras, destacando as restantes, conforme RN03. Dados ausentes não devem ser interpretados como ausência de barreiras.

### 4.2 Solicitar alternativa por relato — UC05

**Ator:** visitante ou pessoa autenticada. **Entrada:** rota com relatos associados. **Resultado:** alternativa apresentada ou indisponibilidade explicada.

1. Abrir um relato associado ao percurso.
2. Examinar descrição, localização e fotos disponíveis.
3. Identificar o aviso de que o relato não foi validado.
4. Selecionar o relato que deseja evitar e solicitar uma alternativa.
5. Examinar a nova rota e suas barreiras antes de decidir.

**Alternativas:** se não existir alternativa, manter acesso à rota anterior e explicar o resultado. Em falha de consulta, permitir nova tentativa. A simples existência de um relato não dispara desvio automático na V1.

### 4.3 Enviar relato — UC07 e UC09

**Ator:** pessoa autenticada. **Entrada:** problema identificado em um local. **Resultado:** relato registrado como não validado.

1. Acessar a ação de relatar um problema.
2. Se estiver sem conta, acessar o login ou cadastro antes do envio.
3. Selecionar ou confirmar a localização do problema.
4. Preencher uma descrição.
5. Opcionalmente, anexar até três fotos.
6. Revisar e confirmar o envio.
7. Receber confirmação e visualizar o estado “não validado”.

**Alternativas:** indicar campos obrigatórios ausentes; rejeitar anexos inválidos e permitir correção. Tratar sessão expirada, limite de envios e falha de rede sem apresentar sucesso indevido. Fotos devem respeitar formatos e tamanho do RNF20; presença de foto não valida o relato.

### 4.4 Criar conta ou entrar — UC06 e UC07

**Ator:** visitante. **Resultado:** sessão autenticada ou erro compreensível.

1. Escolher criar conta ou entrar.
2. No cadastro, preencher nome, sobrenome, e-mail e senha; no login, preencher e-mail e senha.
3. Confirmar e acompanhar o estado de processamento.
4. Corrigir os campos indicados ou prosseguir após confirmação do serviço.

**Alternativas:** permitir voltar e continuar sem conta; tratar credenciais inválidas, indisponibilidade, bloqueio por tentativas e expiração de sessão conforme os requisitos. Alternar cadastro e login não deve acumular telas repetidas na navegação.

### 4.5 Excluir conta — UC10

**Ator:** pessoa autenticada. **Resultado:** conta excluída e sessão encerrada.

1. Acessar as opções da conta e escolher exclusão.
2. Ler os efeitos sobre dados pessoais, relatos e fotos.
3. Confirmar ou cancelar a solicitação.
4. Receber o resultado real da operação.

**Alternativas:** em falha, explicar o estado e permitir tratamento sem indicar exclusão concluída. Após a exclusão, os relatos retidos são apresentados como enviados por “usuário excluído” e descartados dentro do prazo da RN16. Os detalhes operacionais pendentes estão registrados no documento de banco de dados.

## 5. Estados e acessibilidade

| Situação | Comportamento esperado |
|---|---|
| Consulta em andamento | Mostrar carregamento; após três segundos, mantê-lo; após dez segundos sem resposta, informar falha e permitir nova tentativa, conforme RNF03. |
| Busca sem resultados | Explicar a ausência e permitir alterar o texto. |
| Localização recusada ou indisponível | Oferecer origem manual e não insistir em coleta sem consentimento. |
| Informação desconhecida ou antiga | Distinguir dado ausente, dado desatualizado e barreira conhecida; apresentar fonte e data quando disponíveis. |
| Função ainda não implementada | Informar a indisponibilidade, sem simular resultado real. |
| Texto ampliado ou tela pequena | Permitir crescimento e rolagem, sem reduzir a fonte para acomodar conteúdo. |
| Leitor de tela e interação | Fornecer rótulos significativos, ordem de foco lógica e alternativa a gestos. |
| Temas claro e escuro | Verificar contraste e não depender apenas de cor para informar barreiras ou validação. |

Aplicar os critérios do RNF01, incluindo alvos de toque de pelo menos 48 × 48 dp e contraste mínimo previsto nos requisitos. A verificação desses critérios exige testes posteriores; este documento não atesta conformidade das telas.

## 6. Relação com o código atual

| Área | Estado observado no repositório | Trabalho seguinte |
|---|---|---|
| Entrada e apresentação | Onboarding e entrada sem conta implementados. | Validar navegação de volta e conclusão da apresentação. |
| Autenticação | Formulários com validação local e aviso de indisponibilidade. | Integrar ao serviço real, com estados de sessão e erro. |
| Mobilidade | Seleção disponível; perfil salvo em SharedPreferences mesmo sem conta. | Corrigir a persistência do visitante para atender RN18. |
| Tela principal | Painel expansível e avisos de funções indisponíveis. | Integrar mapa e busca preservando a hierarquia e acessibilidade. |
| Rotas e relatos | Fluxos centrais ainda não implementados. | Definir contratos da API e entregar consulta e envio com estados reais. |
| Conta | Aviso de autenticação indisponível. | Implementar gerenciamento e exclusão após integração. |

A implementação atual está em `apps/android/`. A análise não altera código nem pressupõe que a API, o banco ou um provedor de mapas já estejam integrados.

## 7. Limites da V1 e próximos passos

Confirmações de relatos por usuários pertencem à V2 (UC11). Navegação por voz, realidade aumentada, mapas offline, trânsito em tempo real e histórico persistido para visitantes não são adicionados ao escopo por esta comparação. Qualquer nova funcionalidade exige avaliação da equipe.

Próximos passos sugeridos:

1. Conferir o restante do escopo e os critérios de aceite da issue #9.
2. Revisar esta proposta com a equipe e registrar decisões ou ajustes.
3. Se exigido no escopo completo, complementar com capturas de telas, identificando aplicativo, plataforma, data e fonte de cada imagem.
4. Transformar os fluxos aprovados em protótipos e tarefas de implementação vinculadas aos RF, RN e UC.
5. Validar os protótipos com pessoas com mobilidade reduzida antes de tratar as escolhas como comprovadas em uso.

## 8. Histórico

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 31/10/2026 | Pesquisa documental inicial, comparação de referências e proposta de funcionalidades e fluxos para revisão. |
