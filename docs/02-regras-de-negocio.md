# Regras de Negócio – Mão na Roda

> **Versão do documento:** 1.2 <br>
> **Data:** 13/10/2026 <br>
> **Documentos relacionados:** [Visão de negócio](01-visao-de-negocio.md) · [Requisitos](03-requisitos.md) · [Casos de uso](04-casos-de-uso.md) · [Tech stack](05-tech-stack.md)

---

## 1. Introdução

**Regras de negócio (RN)** são as políticas, restrições e critérios que definem **como o negócio funciona**, independentemente da tecnologia. Elas respondem perguntas como: "o que torna uma rota acessível?", "quando um relato é válido?", "o que o app pode ou não afirmar ao usuário?".

**Diferença entre RN e requisito funcional (RF):**

| | Regra de negócio (RN) | Requisito funcional (RF) |
|---|---|---|
| **Define** | A regra ou critério do domínio | A funcionalidade do sistema |
| **Exemplo** | "Trechos com escada são impeditivos para o perfil cadeirante." | "O sistema deve calcular rotas considerando o perfil do usuário." |

As RN alimentam os RF: o RF diz que o sistema calcula a rota, e a RN diz **com quais critérios**.

---

## 2. Convenções

### 2.1 Identificação

| Prefixo | Significado | Formato | Exemplo |
|---|---|---|---|
| `RN` | Regra de negócio | `RN` + número com 2 dígitos | `RN01`, `RN12` |

Os identificadores são **fixos**: se uma regra for removida, o número não é reaproveitado. Marque-a como `Cancelada`.

### 2.2 Campos de cada regra

| Campo | Descrição |
|---|---|
| **ID** | Identificador da regra (`RN01`) |
| **Nome** | Título curto |
| **Descrição** | A regra, escrita de forma clara e verificável |
| **Justificativa** | Por que a regra existe (necessidade do usuário, lei, decisão do grupo) |
| **Versão** | `V1` ou `V2` |
| **Status** | `Proposta` · `Aprovada` · `Implementada` · `Cancelada` |
| **Relacionado** | RF, RNF e UC que dependem da regra ou restringem sua implementação |

---

## 3. Regras de negócio (RN)

### 3.1 Perfis de mobilidade e rotas

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN01 | Perfil define as barreiras impeditivas | Cada perfil de mobilidade tem uma lista própria de barreiras **impeditivas**. Uma rota que contenha barreira impeditiva para o perfil escolhido não deve ser indicada como acessível. | Uma barreira que não afeta uma pessoa pode impedir outra (por exemplo, escada para cadeirante). | V1 | Aprovada | RF03, RF08, UC03, UC04 |
| RN02 | Classificação das barreiras | Toda barreira é classificada como **impeditiva**, **de atenção** ou **irrelevante** para cada perfil (veja a matriz da seção 4). | Permite mostrar ao usuário o que bloqueia a rota e o que apenas exige cuidado. | V1 | Aprovada | RF04, UC03, UC04 |
| RN03 | Rota sem opção totalmente acessível | Se não existir rota sem barreiras impeditivas, o sistema deve informar isso ao usuário e mostrar a rota com **menos** barreiras, destacando as que restam. | Evitar que o usuário fique sem resposta e deixar a decisão com ele. | V1 | Aprovada | RF03, UC04 |
| RN04 | Rota mais curta não é o único critério | A escolha da rota deve priorizar a acessibilidade antes de tempo e distância. | É a proposta de valor do produto. | V1 | Aprovada | RF03, UC04 |

### 3.2 Barreiras e dados de acessibilidade

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN05 | Tipos de barreira reconhecidos | O sistema considera, no mínimo: escadas, ausência ou inclinação inadequada de rampas, calçadas estreitas ou danificadas, obstáculos, elevadores indisponíveis, ausência de piso tátil, banheiros não acessíveis e entradas inadequadas. | Lista de barreiras definida pela equipe com base no problema identificado. | V1 | Aprovada | RF04, UC04, UC09 |
| RN06 | Origem, prioridade e data do dado de acessibilidade | Toda informação de acessibilidade de um trecho ou local deve ter **data da última verificação** e origem identificada: dados públicos, OpenStreetMap ou relato de usuário. Quando houver conflito entre dados públicos ou do OpenStreetMap e relatos de usuários, prevalece a informação dos dados públicos ou do OpenStreetMap. | Condições mudam, e o usuário precisa saber a origem e a confiabilidade da informação. | V1 | Aprovada | RF04, RF05, UC04 |
| RN07 | Dados desatualizados | Informações sem verificação há mais de **um mês** devem ser sinalizadas como desatualizadas. | Reduz o risco de o usuário confiar em dado antigo. | V1 | Aprovada | RF04, RF05, UC04 |

### 3.3 Relatos de usuários

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN08 | Conteúdo mínimo e autoria de um relato | Somente usuário autenticado pode enviar relato. Um relato pode ser aceito sem foto, mas deve possuir descrição e localização. | A descrição e a localização permitem interpretar o problema e associá-lo ao trecho correto. A autenticação associa o relato a uma identidade, e a foto fornece evidência complementar para análise. | V1 | Aprovada | RF09, RF18, RN15, UC09, RNF04, RNF12, RNF13, RNF15, RNF19–RNF21 |
| RN09 | Validação dos relatos | Um relato só altera a classificação de um trecho depois de confirmado por três usuários autenticados distintos. | Na V1, os relatos são recebidos e apresentados como não validados. Na V2, a validação permite que relatos considerados confiáveis atualizem a classificação dos trechos e influenciem o cálculo das rotas. | V2 | Aprovada | RF03, RF09, RF15, RF16, RF17, UC11, RNF16, RNF17, RNF18 |
| RN10 | Relatos sem validação | Relatos sem validação podem ser apresentados como possíveis obstáculos na rota. | Evita não relatar um problema na rota por falta de verificação. | V1 | Aprovada | RF04, RF07, UC04, UC05 |
| RN11 | Relatos com e sem fotos | Relatos com foto devem ser identificados como contendo evidência visual complementar. A presença ou ausência de foto não altera o estado de validação: todo novo relato permanece não validado até cumprir o processo da RN09. As fotos são usadas somente como evidência do relato e não são usadas para treinamento de modelos. | A foto ajuda o usuário a avaliar o relato, mas não comprova sozinha que a condição relatada seja verdadeira ou atual. | V1 | Aprovada | RF14, RF18, UC09, RNF13, RNF19, RNF20 |
| RN12 | Rotas com relatos sem validação | Relatos não validados não alteram automaticamente a rota calculada. O usuário pode solicitar uma alternativa que evite os trechos associados aos relatos selecionados. | A informação ainda não foi confirmada, portanto não modifica automaticamente os critérios de cálculo. | V1 | Aprovada | RF07, UC05 |


### 3.4 Responsabilidade e privacidade

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN13 | Rota é sugestão informativa | O app deve informar que a rota é uma **sugestão baseada nos dados disponíveis** e que as condições reais podem ser diferentes. | Os dados podem estar incompletos ou desatualizados, e a decisão final é do usuário. | V1 | Aprovada | RF04, RF06, UC04 |
| RN14 | Consentimento para localização | A localização do usuário só pode ser coletada após consentimento explícito e usada apenas para o funcionamento do app. | LGPD e confiança do usuário. | V1 | Aprovada | RF01, RF03, UC01, UC04, RNF04, RNF13, RNF15, RNF19 |

### 3.5 Contas de usuário

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN15 | Criação de conta e acesso | O usuário pode criar uma conta e fazer login nela. O cadastro exige nome, sobrenome, e-mail e senha. O app não coleta idade nem tipo de deficiência. | Permite guardar as preferências do usuário e associar seus relatos a uma identidade (reputação, validação e consentimento), coletando somente os dados necessários para a V1. | V1 | Aprovada | RF10, RF11, UC06, UC07, UC09, RNF08, RNF10, RNF12, RNF13, RNF15, RNF16, RNF17, RNF18 |
| RN16 | Exclusão de conta | O usuário pode excluir a própria conta a qualquer momento. Os dados pessoais associados à conta são removidos. Relatos e fotos já enviados permanecem no Supabase por até um mês após a exclusão, são exibidos no app como enviados por **"usuário excluído"** e são descartados ao fim desse período por rotina automática do backend. | Direito do titular de dados previsto na LGPD, preservando temporariamente a informação de acessibilidade sem identificar o autor. | V1 | Aprovada | RF12, UC10, RNF13, RNF16, RNF17, RNF19 |
| RN17 | Uso sem conta | O app pode ser usado sem conta. | Reduz a barreira de entrada para quem só quer uma rota. | V1 | Aprovada | RF13, UC08, RNF04, RNF15, RNF19 |
| RN18 | Armazenamento da rota sem conta | Sem conta, a rota calculada e o perfil de mobilidade escolhido não devem ser armazenados. | A pessoa não criou uma conta nem forneceu autorização para o armazenamento dessas informações. | V1 | Aprovada | RF03, RF13, UC08, RNF04, RNF19 |

---

## 4. Matriz de barreiras por perfil

Apoia as regras RN01 e RN02. Cada combinação de barreira e perfil é classificada como:

- **I** = impeditiva (a rota não deve passar por aqui)
- **A** = atenção (a rota pode passar, mas o usuário deve ser avisado)
- **–** = irrelevante para o perfil

| Barreira | Cadeirante | Muleta ou outro auxílio | Mobilidade reduzida (outros casos) |
|---|---|---|---|
| Escada sem alternativa | I | A | A |
| Ausência de rampa | I | A | A |
| Rampa com inclinação inadequada | I | A | A |
| Calçada estreita | I | A | – |
| Calçada danificada ou irregular | A | I | A |
| Obstáculo na calçada | I | A | A |
| Elevador indisponível | I | A | A |
| Entrada inadequada (degrau, porta estreita) | I | A | A |
| Banheiro não acessível | I | A | – |
| Ausência de piso tátil | – | – | – |

> O piso tátil é uma barreira relevante para pessoas com deficiência visual, que ficam para a **V2**. Por isso aparece como irrelevante na V1.

---

## 5. Glossário

| Termo | Definição |
|---|---|
| Perfil de mobilidade | Conjunto de necessidades de locomoção escolhido pelo usuário (por exemplo, cadeirante ou muleta) |
| Barreira | Qualquer elemento do trajeto ou do local que dificulte ou impeça a locomoção |
| Barreira impeditiva | Barreira que impede a passagem para determinado perfil |
| Rota acessível | Rota sem barreiras impeditivas para o perfil escolhido |
| Relato | Informação enviada por um usuário sobre uma condição de acessibilidade |

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 06/10/2026 | Modelo base |
| 0.2 | 07/10/2026 | Modelo base |
| 0.3 | 08/10/2026 | Modelo base |
| 0.4 | 08/10/2026 | Inclusão das relações entre regras de negócio e requisitos não funcionais |
| 1.0 | 13/10/2026 | Aprovação das regras e definição de validade dos dados, relatos, fontes, fotos, exclusão de conta e idade opcional. |
| 1.1 | 13/10/2026 | Definição dos campos de cadastro e do descarte automático de relatos e fotos. |
| 1.2 | 13/10/2026 | Inclusão das relações com anexos de fotos e limites de envio. |
