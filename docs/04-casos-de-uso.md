# Casos de Uso – Mão na Roda

> **Versão do documento:** 1.0 <br>
> **Data:** 13/10/2026 <br>
> **Documentos relacionados:** [Visão de negócio](01-visao-de-negocio.md) · [Regras de negócio](02-regras-de-negocio.md) · [Requisitos](03-requisitos.md) · [Tech stack](05-tech-stack.md)

---

## 1. Introdução

Este documento descreve as interações entre os atores e o sistema **Mão na Roda**. Cada caso de uso (UC) detalha um objetivo do usuário, o fluxo esperado e as situações que exigem tratamento alternativo.

Os casos de uso complementam os requisitos: os requisitos definem **o que** o sistema deve oferecer; os casos de uso mostram **como** a pessoa chega a esse resultado.

---

## 2. Convenções

### 2.1 Identificação

| Prefixo | Significado | Formato | Exemplo |
|---|---|---|
| `UC` | Caso de uso | `UC` + número com 2 dígitos | `UC01` |

Os identificadores são fixos. Se um caso de uso deixar de existir, seu número não deve ser reutilizado; ele deve ser marcado como `Cancelado`.

### 2.2 Versão e status

| Campo | Valores possíveis |
|---|---|
| **Versão** | `V1` (escopo inicial) · `V2` (evolução prevista) |
| **Status** | `Proposto` · `Aprovado` · `Em desenvolvimento` · `Implementado` · `Cancelado` |

---

## 3. Atores

| Ator | Descrição |
|---|---|
| **Usuário visitante** | Pessoa que utiliza o aplicativo sem criar conta ou fazer login. Pode consultar mapa, destinos, rotas e selecionar o perfil de mobilidade. |
| **Usuário autenticado** | Pessoa que criou uma conta e entrou no aplicativo. Além das ações do visitante, pode enviar relatos e administrar a própria conta. |
| **Serviço de mapas e rotas** | Serviço externo que fornece mapa, busca de locais, geocodificação e informações necessárias para compor trajetos. |
| **Serviço de localização do dispositivo** | Recurso do dispositivo que fornece a localização atual após autorização da pessoa usuária. |

---

## 4. Catálogo de casos de uso

| ID | Caso de uso | Ator principal | Versão | Status | Requisitos relacionados |
|---|---|---|---|---|---|
| UC01 | Visualizar mapa e localização atual | Usuário visitante | V1 | Aprovado | RF01 |
| UC02 | Buscar destino | Usuário visitante | V1 | Aprovado | RF02 |
| UC03 | Selecionar perfil de mobilidade | Usuário visitante | V1 | Aprovado | RF08 |
| UC04 | Consultar rota acessível | Usuário visitante | V1 | Aprovado | RF03, RF04, RF05, RF06 |
| UC05 | Solicitar alternativa para relatos não validados | Usuário visitante | V1 | Aprovado | RF07 |
| UC06 | Criar conta | Usuário visitante | V1 | Aprovado | RF10 |
| UC07 | Fazer login | Usuário visitante | V1 | Aprovado | RF11 |
| UC08 | Usar o aplicativo sem conta | Usuário visitante | V1 | Aprovado | RF13 |
| UC09 | Enviar relato de problema de acessibilidade | Usuário autenticado | V1 | Aprovado | RF09, RF14, RF18 |
| UC10 | Excluir conta | Usuário autenticado | V1 | Aprovado | RF12 |
| UC11 | Confirmar relato de outro usuário | Usuário autenticado | V2 | Aprovado | RF15, RF16, RF17 |

---

## 5. Detalhamento dos casos de uso

### UC01 — Visualizar mapa e localização atual

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que a pessoa visualize o mapa e sua posição atual. |
| **Ator principal** | Usuário visitante. |
| **Atores de apoio** | Serviço de localização do dispositivo; serviço de mapas e rotas. |
| **Pré-condições** | O aplicativo está aberto e possui conexão com a internet. |
| **Gatilho** | A pessoa acessa a tela inicial do aplicativo. |
| **Relacionado** | RF01, RN14, RNF04, RNF05, RNF15. |

**Fluxo principal**

1. O sistema solicita a permissão de localização, caso ela ainda não tenha sido definida.
2. A pessoa autoriza o uso da localização.
3. O sistema obtém a localização atual do dispositivo.
4. O sistema exibe o mapa centralizado na localização obtida.

**Fluxos alternativos**

- **A1 — Permissão negada ou revogada:** o sistema mantém o mapa disponível, informa que a localização não foi obtida e permite que a pessoa busque ou informe um destino manualmente.
- **A2 — Sem conexão:** o sistema informa que não foi possível carregar o mapa ou atualizar a localização.

**Pós-condições**

- O mapa está disponível para consulta; quando autorizada, a localização atual é exibida.

---

### UC02 — Buscar destino

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que a pessoa informe o local para o qual deseja ir. |
| **Ator principal** | Usuário visitante. |
| **Ator de apoio** | Serviço de mapas e rotas. |
| **Pré-condições** | O mapa está disponível. |
| **Gatilho** | A pessoa seleciona a ação de buscar destino. |
| **Relacionado** | RF02, RNF02, RNF05, RNF15. |

**Fluxo principal**

1. A pessoa informa o destino por texto.
2. O sistema consulta o serviço de mapas e rotas.
3. O sistema apresenta os locais compatíveis encontrados.
4. A pessoa escolhe um dos resultados.
5. O sistema registra o destino selecionado para a consulta de rota.

**Fluxos alternativos**

- **A1 — Nenhum resultado encontrado:** o sistema informa que não encontrou o destino e permite uma nova busca.
- **A2 — Falha de conexão:** o sistema informa que a busca não pôde ser concluída.

**Pós-condições**

- Um destino foi selecionado ou a pessoa permaneceu na busca para tentar novamente.

---

### UC03 — Selecionar perfil de mobilidade

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que a pessoa escolha o perfil que definirá os critérios de acessibilidade da rota. |
| **Ator principal** | Usuário visitante. |
| **Pré-condições** | O aplicativo está aberto. |
| **Gatilho** | A pessoa acessa as preferências de rota ou inicia uma consulta de rota sem perfil definido. |
| **Relacionado** | RF08, RN01, RN02, RNF01, RNF02. |

**Fluxo principal**

1. O sistema apresenta os perfis de mobilidade disponíveis.
2. A pessoa escolhe um perfil.
3. O sistema confirma o perfil escolhido.
4. O sistema utiliza o perfil na próxima consulta de rota.

**Pós-condições**

- O perfil escolhido fica disponível para o cálculo da rota durante o uso atual do aplicativo.

---

### UC04 — Consultar rota acessível

| Campo | Descrição |
|---|---|
| **Objetivo** | Apresentar uma rota adequada ao perfil de mobilidade escolhido, com informações para apoiar a decisão da pessoa. |
| **Ator principal** | Usuário visitante. |
| **Atores de apoio** | Serviço de localização do dispositivo; serviço de mapas e rotas. |
| **Pré-condições** | Há uma origem disponível ou informada, um destino selecionado e um perfil de mobilidade escolhido. |
| **Gatilho** | A pessoa seleciona a ação de visualizar rotas. |
| **Relacionado** | RF03, RF04, RF05, RF06, RN01–RN07, RNF01, RNF03, RNF04, RNF05, RNF15. |

**Fluxo principal**

1. O sistema recebe a origem, o destino e o perfil de mobilidade.
2. O sistema identifica as barreiras impeditivas e de atenção conforme o perfil escolhido.
3. O sistema calcula as opções de rota, priorizando acessibilidade antes de tempo e distância.
4. O sistema apresenta a melhor rota disponível no mapa.
5. O sistema exibe distância, tempo estimado, resumo das barreiras e a data da última verificação dos trechos.
6. O sistema sinaliza informações desatualizadas, quando houver.
7. O sistema informa que a rota é uma sugestão baseada nos dados disponíveis.

**Fluxos alternativos**

- **A1 — Não existe rota sem barreiras impeditivas:** o sistema informa essa condição e apresenta a rota com menos barreiras impeditivas, destacando-as.
- **A2 — Há relatos não validados no percurso:** o sistema mostra os relatos como possíveis obstáculos e disponibiliza a ação descrita no UC05.
- **A3 — Não foi possível obter a localização:** o sistema solicita que a pessoa informe ou escolha uma origem no mapa.
- **A4 — Resposta demorada:** após 3 segundos sem resultado, o sistema mantém um indicador de carregamento.
- **A5 — Falha de conexão, serviço externo ou tempo excedido:** após 10 segundos sem resposta, o sistema informa que não foi possível calcular a rota e permite nova tentativa.

**Pós-condições**

- A pessoa recebeu uma rota e informações sobre suas condições de acessibilidade, ou foi informada sobre a impossibilidade de calculá-la.

---

### UC05 — Solicitar alternativa para relatos não validados

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que a pessoa tente evitar trechos associados a relatos não validados que considere relevantes. |
| **Ator principal** | Usuário visitante. |
| **Pré-condições** | Uma rota foi apresentada e contém ao menos um relato não validado. |
| **Gatilho** | A pessoa seleciona um ou mais relatos e solicita uma rota alternativa. |
| **Relacionado** | RF07, RN10, RN12, RNF05, RNF15. |

**Fluxo principal**

1. O sistema exibe os relatos não validados associados à rota.
2. A pessoa escolhe quais relatos deseja evitar.
3. O sistema calcula uma nova rota, evitando os trechos associados aos relatos selecionados quando possível.
4. O sistema apresenta a alternativa e suas informações de acessibilidade.

**Fluxos alternativos**

- **A1 — Não há rota alternativa:** o sistema informa que não encontrou uma alternativa que evite os trechos selecionados e mantém a rota original disponível.

**Pós-condições**

- Uma rota alternativa foi apresentada ou a rota original permanece disponível com o aviso correspondente.

---

### UC06 — Criar conta

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que a pessoa crie uma conta para utilizar funções associadas à identidade do usuário. |
| **Ator principal** | Usuário visitante. |
| **Pré-condições** | A pessoa ainda não está autenticada. |
| **Gatilho** | A pessoa seleciona a ação de criar conta. |
| **Relacionado** | RF10, RN15, RNF08–RNF10, RNF12, RNF15–RNF18. |

**Fluxo principal**

1. O sistema apresenta o formulário de cadastro.
2. A pessoa informa nome, sobrenome, e-mail e senha.
3. O sistema valida os dados recebidos.
4. O sistema cria a conta e inicia a sessão da pessoa.

**Fluxos alternativos**

- **A1 — Dados inválidos ou incompletos:** o sistema aponta os campos que precisam de correção.
- **A2 — Conta já existente:** o sistema informa a condição e oferece o acesso pela tela de login.

**Pós-condições**

- Uma conta foi criada e a pessoa está autenticada, ou os erros de cadastro foram informados.

---

### UC07 — Fazer login

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que uma pessoa com conta acesse as funções autenticadas do aplicativo. |
| **Ator principal** | Usuário visitante. |
| **Pré-condições** | A pessoa possui uma conta. |
| **Gatilho** | A pessoa seleciona a ação de entrar. |
| **Relacionado** | RF11, RN15, RNF08–RNF10, RNF15–RNF18. |

**Fluxo principal**

1. O sistema apresenta os campos de acesso.
2. A pessoa informa suas credenciais.
3. O sistema valida as credenciais.
4. O sistema inicia uma sessão segura e identifica a pessoa como usuária autenticada.

**Fluxos alternativos**

- **A1 — Credenciais inválidas:** o sistema informa que não foi possível realizar o acesso, sem expor qual dado está incorreto.
- **A2 — Limite de tentativas atingido:** o sistema aplica a proteção definida e informa quando uma nova tentativa poderá ser feita.
- **A3 — Sessão expirada:** o sistema pode mostrar a última conta usada como identificação visual, mas solicita novo login para continuar ações protegidas.

**Pós-condições**

- A pessoa está autenticada ou recebeu a informação necessária para tentar novamente.

---

### UC08 — Usar o aplicativo sem conta

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir a consulta de rotas sem obrigar a criação de conta. |
| **Ator principal** | Usuário visitante. |
| **Pré-condições** | O aplicativo está aberto e a pessoa não está autenticada. |
| **Gatilho** | A pessoa decide continuar sem criar conta ou encerrar a sessão. |
| **Relacionado** | RF13, RN17, RN18, RNF04, RNF13, RNF15, RNF19. |

**Fluxo principal**

1. A pessoa escolhe continuar sem conta.
2. O sistema disponibiliza mapa, busca, perfil de mobilidade e consulta de rotas.
3. O sistema não armazena o histórico de rotas nem o perfil escolhido de forma associada à pessoa.

**Pós-condições**

- A pessoa pode usar as funcionalidades permitidas a visitantes.

---

### UC09 — Enviar relato de problema de acessibilidade

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que uma pessoa comunique um problema de acessibilidade encontrado em um local ou trecho. |
| **Ator principal** | Usuário autenticado. |
| **Pré-condições** | A pessoa está autenticada e possui uma localização para associar ao relato. |
| **Gatilho** | A pessoa seleciona a ação de relatar problema. |
| **Relacionado** | RF09, RF14, RF18, RN08, RN10, RN11, RNF13, RNF15, RNF17–RNF21. |

**Fluxo principal**

1. O sistema apresenta o formulário de relato.
2. A pessoa informa uma descrição do problema e confirma ou indica sua localização.
3. Opcionalmente, a pessoa anexa até três fotos.
4. O sistema valida os dados e o arquivo enviado, quando houver foto.
5. O sistema registra o relato como não validado.
6. O sistema informa que o relato foi enviado e indica se há evidência visual complementar.

**Fluxos alternativos**

- **A1 — Descrição ou localização ausente:** o sistema informa os dados obrigatórios que precisam ser preenchidos.
- **A2 — Foto inválida:** o sistema recusa arquivo em formato diferente de JPG, JPEG ou PNG, com mais de 5 MB, ou acima do limite de três fotos, e informa os limites aceitos.
- **A3 — Falha no envio:** o sistema informa que o relato não foi registrado e permite nova tentativa.
- **A4 — Limite de relatos atingido:** o sistema informa que a pessoa atingiu o limite de cinco relatos na hora atual e indica quando poderá enviar novamente.

**Pós-condições**

- Um relato não validado foi armazenado e associado à localização informada.

---

### UC10 — Excluir conta

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que a pessoa exclua a própria conta e seus dados pessoais. |
| **Ator principal** | Usuário autenticado. |
| **Pré-condições** | A pessoa está autenticada. |
| **Gatilho** | A pessoa seleciona a ação de excluir conta. |
| **Relacionado** | RF12, RN16, RNF09, RNF13, RNF15–RNF17, RNF19. |

**Fluxo principal**

1. O sistema explica os efeitos da exclusão e solicita confirmação.
2. A pessoa confirma a exclusão.
3. O sistema remove os dados pessoais, anonimiza a autoria dos relatos e mantém os relatos e fotos por até um mês como enviados por "usuário excluído".
4. O sistema invalida a sessão ativa e confirma a conclusão da solicitação.

**Fluxos alternativos**

- **A1 — Exclusão cancelada:** o sistema interrompe o processo e mantém a conta ativa.
- **A2 — Reautenticação necessária:** o sistema solicita as credenciais novamente antes da confirmação.

**Pós-condições**

- A conta e os dados pessoais associados foram excluídos; as sessões ativas foram invalidadas.

---

### UC11 — Confirmar relato de outro usuário (V2)

| Campo | Descrição |
|---|---|
| **Objetivo** | Permitir que uma pessoa confirme a existência de um problema relatado por outra pessoa. |
| **Ator principal** | Usuário autenticado. |
| **Pré-condições** | A pessoa está autenticada e o relato foi criado por outra pessoa. |
| **Gatilho** | A pessoa seleciona a ação de confirmar relato. |
| **Relacionado** | RF15, RF16, RF17, RN09, RNF17, RNF22. |

**Fluxo principal**

1. O sistema apresenta um relato não validado.
2. A pessoa seleciona a ação de confirmar.
3. O sistema verifica que a pessoa não é autora do relato e ainda não o confirmou.
4. O sistema registra a confirmação.
5. Quando o critério de validação for atingido, o sistema atualiza o estado do relato conforme a RN09.

**Fluxos alternativos**

- **A1 — Autor do próprio relato:** o sistema impede a confirmação.
- **A2 — Relato já confirmado pela pessoa:** o sistema impede uma nova confirmação.
- **A3 — Limite de solicitações atingido:** o sistema aplica a proteção definida contra abuso.

**Pós-condições**

- A confirmação foi registrada ou a pessoa recebeu o motivo pelo qual ela não pôde ser feita.

---

## 6. Matriz de rastreabilidade

| Caso de uso | Requisitos funcionais | Regras de negócio |
|---|---|---|
| UC01 | RF01 | RN14 |
| UC02 | RF02 | — |
| UC03 | RF08 | RN01, RN02 |
| UC04 | RF03, RF04, RF05, RF06 | RN01–RN07, RN13 |
| UC05 | RF07 | RN10, RN12 |
| UC06 | RF10 | RN15 |
| UC07 | RF11 | RN15 |
| UC08 | RF13 | RN17, RN18 |
| UC09 | RF09, RF14, RF18 | RN08, RN10, RN11 |
| UC10 | RF12 | RN16 |
| UC11 | RF15, RF16, RF17 | RN09 |

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 12/10/2026 | Versão inicial baseada nos requisitos e regras de negócio existentes. |
| 0.2 | 13/10/2026 | Definição dos campos obrigatórios para criação de conta. |
| 0.3 | 13/10/2026 | Inclusão de limites de fotos, sessão expirada e proteção contra abuso. |
| 1.0 | 13/10/2026 | Aprovação dos casos de uso, alinhamento com o desempenho das rotas e conclusão das pendências. |
