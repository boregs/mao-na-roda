# Regras de Negócio – Mão na Roda

> **Versão do documento:** 0.3  <br> 
> **Data:** 08/10/2026 <br>
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
| **Relacionado** | RF e UC que dependem da regra |

---

## 3. Regras de negócio (RN)

### 3.1 Perfis de mobilidade e rotas

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN01 | Perfil define as barreiras impeditivas | Cada perfil de mobilidade tem uma lista própria de barreiras **impeditivas**. Uma rota que contenha barreira impeditiva para o perfil escolhido não deve ser indicada como acessível. | Uma barreira que não afeta uma pessoa pode impedir outra (por exemplo, escada para cadeirante). | V1 | Proposta | RF03, RF08 |
| RN02 | Classificação das barreiras | Toda barreira é classificada como **impeditiva**, **de atenção** ou **irrelevante** para cada perfil (veja a matriz da seção 4). | Permite mostrar ao usuário o que bloqueia a rota e o que apenas exige cuidado. | V1 | Proposta | RF04 |
| RN03 | Rota sem opção totalmente acessível | Se não existir rota sem barreiras impeditivas, o sistema deve informar isso ao usuário e mostrar a rota com **menos** barreiras, destacando as que restam. | Evitar que o usuário fique sem resposta e deixar a decisão com ele. | V1 | Proposta | RF03 |
| RN04 | Rota mais curta não é o único critério | A escolha da rota deve priorizar a acessibilidade antes de tempo e distância. | É a proposta de valor do produto. | V1 | Proposta | RF03 |

### 3.2 Barreiras e dados de acessibilidade

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN05 | Tipos de barreira reconhecidos | O sistema considera, no mínimo: escadas, ausência ou inclinação inadequada de rampas, calçadas estreitas ou danificadas, obstáculos, elevadores indisponíveis, ausência de piso tátil, banheiros não acessíveis e entradas inadequadas. | Lista de barreiras definida pela equipe com base no problema identificado. | V1 | Proposta | RF04 |
| RN06 | Dado de acessibilidade com data | Toda informação de acessibilidade de um trecho ou local deve ter **data da última verificação** e **origem** (dado oficial ou relato de usuário). | Condições mudam, e o usuário precisa saber o quão confiável é a informação. | V1 | Proposta | RF04, RF05 |
| RN07 | Dados desatualizados | Informações com mais de **[X meses]** sem verificação devem ser sinalizadas como desatualizadas. | Reduz o risco de o usuário confiar em dado antigo. | V1 | Proposta | RF04, RF05 |

### 3.3 Relatos de usuários

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN08 | Conteúdo mínimo de um relato | Um relato pode ser aceito sem foto, mas deve possuir descrição e localização. | A descrição e a localização permitem interpretar o problema e associá-lo ao trecho correto. A foto fornece evidência complementar para análise. | V1 | Proposta | RF09 |
| RN09 | Validação dos relatos | Um relato só altera a classificação de um trecho depois de confirmado por 3 outros usuários. | Na V1, os relatos são recebidos e apresentados como não validados. Na V2, a validação permite que relatos considerados confiáveis atualizem a classificação dos trechos e influenciem o cálculo das rotas. | V2 | Proposta | RF09, RF03 |
| RN10 | Relatos sem validação | Relatos sem validação podem ser apresentados como possíveis obstáculos na rota | Evita não relatar um problema na rota por falta de verificação | V1 | Proposta | RF04, RF07 |
| RN11 | Relatos com e sem fotos | Relatos que possuem apenas descrição devem ser classificados diferentemente daqueles que possuem foto de comprovação | Ajuda o usuário a decidir se continua na mesma rota | V1 | Proposta | RF14 |
| RN12 | Rotas com relatos sem validação | Relatos não validados não alteram automaticamente a rota calculada. O usuário pode solicitar uma alternativa que evite os trechos associados aos relatos selecionados. | A informação ainda não foi confirmada, portanto não modifica automaticamente os critérios de cálculo. | V1 | Proposta | RF07 |


### 3.4 Responsabilidade e privacidade

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN13 | Rota é sugestão informativa | O app deve informar que a rota é uma **sugestão baseada nos dados disponíveis** e que as condições reais podem ser diferentes. | Os dados podem estar incompletos ou desatualizados, e a decisão final é do usuário. | V1 | Proposta | RF04, RF06 |
| RN14 | Consentimento para localização | A localização do usuário só pode ser coletada após consentimento explícito e usada apenas para o funcionamento do app. | LGPD e confiança do usuário. | V1 | Proposta | RNF04 |

### 3.5 Contas de usuário

| ID | Nome | Descrição | Justificativa | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RN15 | Criação de conta e acesso | O usuário pode criar uma conta e fazer login nela. | Permite guardar as preferências do usuário e associar seus relatos a uma identidade (reputação, validação e consentimento). | V1 | Proposta | RF10, RF11 |
| RN16 | Exclusão de conta | O usuário pode excluir a própria conta a qualquer momento. Os dados pessoais associados à conta são removidos. **[Definir o tratamento de relatos e fotos já enviados.]** | Direito do titular de dados previsto na LGPD. | V1 | Proposta | RF12 |
| RN17 | Uso sem conta | O app pode ser usado sem conta. | Reduz a barreira de entrada para quem só quer uma rota. | V1 | Proposta | RF13 |
| RN18 | Armazenamento da rota sem conta | Sem conta, a rota estipulada pelo algoritmo, juntamente com o tipo de limitação que o usuário possui não serão armazenadas | O usuário não deu o consentimento do armazenamento, devido que não criou uma conta | V1 | Proposta | RF03, RF13 |

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

## 6. Pendências

- [ ] Revisar as regras com o grupo (RN01 a RN18)
- [ ] Validar a matriz de barreiras por perfil (seção 4)
- [ ] Definir os valores entre colchetes (prazo de dado desatualizado, critério de validação de relatos)
- [ ] Definir a origem dos dados de acessibilidade (dados oficiais, relatos ou ambos)
- [ ] Definir consentimento para uso das fotos no treinamento do modelo e o tratamento de privacidade das imagens (LGPD)
- [ ] Definir quais dados da conta, além da idade, influenciam o cálculo de rota (RN15, V2)
- [ ] Definir o que acontece com relatos e fotos já enviados quando a conta é excluída (RN13)
- [ ] Voltar ao `03-requisitos.md` e ligar cada RF às regras daqui
- [ ] Atualizar versão e data ao aprovar o documento

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 06/10/2026 | Modelo base |
| 0.2 | 07/10/2026 | Modelo base |
| 0.3 | 08/10/2026 | Modelo base |
