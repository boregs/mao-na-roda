# Requisitos – Mão na Roda

> **Versão do documento:** 0.1 (modelo base) <br>
> **Data:** 06/10/2026 <br>
> **Documentos relacionados:** [Visão de negócio](01-visao-de-negocio.md) · [Regras de negócio](02-regras-de-negocio.md) · [Casos de uso](04-casos-de-uso.md) · [Tech stack](05-tech-stack.md)

---

## 1. Introdução

Este documento lista os requisitos do **Mão na Roda**, separados em:

- **Requisitos funcionais (RF):** o que o sistema **faz**, ou seja, as funcionalidades que o usuário ou outros sistemas podem usar.
- **Requisitos não funcionais (RNF):** **como** o sistema deve se comportar, ou seja, qualidades e restrições como desempenho, segurança, usabilidade e acessibilidade.

> **Nota:** as linhas marcadas com *(exemplo)* servem apenas para mostrar o formato. Devem ser substituídas pelos requisitos reais do grupo.

---

## 2. Convenções

### 2.1 Identificação

| Prefixo | Significado | Formato | Exemplo |
|---|---|---|---|
| `RF` | Requisito funcional | `RF` + número com 2 dígitos | `RF01` |
| `RNF` | Requisito não funcional | `RNF` + número com 2 dígitos | `RNF01` |

Os identificadores são **fixos**: se um requisito for removido, o número não é reaproveitado. Marque-o como `Cancelado`.

### 2.2 Prioridade (MoSCoW)

| Valor | Significado |
|---|---|
| **Essencial** | Sem ele o sistema não cumpre seu propósito. Obrigatório na versão indicada. |
| **Importante** | Agrega muito valor, mas o sistema funciona sem ele no curto prazo. |
| **Desejável** | Melhora a experiência, entra se houver tempo. |
| **Futuro** | Reconhecido como necessidade, mas fora do escopo atual. |

### 2.3 Versão do produto

| Valor | Significado |
|---|---|
| `V1` | Escopo inicial (rotas acessíveis para deficiência de mobilidade) |
| `V2` | Evolução prevista (por exemplo, auxílio por voz para pessoas sem visão) |

### 2.4 Status

`Proposto` · `Aprovado` · `Em desenvolvimento` · `Implementado` · `Cancelado`

### 2.5 Referências cruzadas

Cada requisito pode citar as **regras de negócio** (`RN`) e os **casos de uso** (`UC`) relacionados, para manter a rastreabilidade entre os documentos.

---

## 3. Requisitos funcionais (RF)

Agrupados por módulo, para facilitar a leitura.

### 3.1 Mapa e localização

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF01 | *(exemplo)* Exibir mapa e localização | O sistema deve exibir o mapa com a localização atual do usuário. | Essencial | V1 | Proposto | UC01 |
| RF02 | *(exemplo)* Buscar destino | O sistema deve permitir que o usuário informe um destino por texto. | Essencial | V1 | Proposto | UC02 |

### 3.2 Rotas acessíveis

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF03 | *(exemplo)* Calcular rota acessível | O sistema deve calcular rotas considerando o perfil de mobilidade escolhido pelo usuário. | Essencial | V1 | Proposto | RN01, UC03 |
| RF04 | *(exemplo)* Exibir resumo de barreiras | O sistema deve exibir, para cada rota, distância, tempo estimado e resumo das barreiras do trajeto. | Essencial | V1 | Proposto | RN02, UC03 |

### 3.3 Preferências do usuário

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF05 | *(exemplo)* Selecionar perfil de mobilidade | O sistema deve permitir que o usuário escolha seu perfil de mobilidade para personalizar as rotas. | Essencial | V1 | Proposto | RN01, UC04 |

### 3.4 Relato de problemas

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF06 | *(exemplo)* Relatar problema no trajeto | O sistema deve permitir que o usuário registre um problema de acessibilidade em um local, com descrição em texto. | Importante | V1 | Proposto | RN03, UC05 |

### 3.5 Conta de usuário

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF07 | Criar conta | O usuário deve poder criar uma conta no sistema. | Essencial | V1 | Proposto | RN12 |
| RF08 | Fazer login | O usuário deve poder acessar o sistema com a sua conta. | Essencial | V1 | Proposto | RN12 |
| RF09 | Excluir conta | O usuário deve poder excluir a própria conta, com a remoção de seus dados pessoais. | Essencial | V1 | Proposto | RN13 |
| RF10 | Usar sem conta | O sistema deve permitir o uso do app sem conta, com limitações no cálculo de rotas em relação ao uso com conta. | Futuro | V2 | Proposto | RN14, RN15, RF03 |

> **Dica de redação:** descreva cada RF começando com "O sistema deve..." ou "O usuário deve poder...", com **um comportamento por requisito**. Se a descrição precisar de "e", considere dividir.

---

## 4. Requisitos não funcionais (RNF)

Agrupados por categoria de qualidade.

### 4.1 Usabilidade e acessibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Status |
|---|---|---|---|---|---|
| RNF01 | *(exemplo)* Interface acessível | A interface deve seguir boas práticas de acessibilidade: contraste adequado, fontes legíveis e botões de tamanho adequado ao toque. | Revisão de telas e testes com usuários | Essencial | Proposto |
| RNF02 | *(exemplo)* Uso com uma mão | As ações principais devem ser alcançáveis com uma mão só. | Teste em dispositivos reais | Importante | Proposto |

### 4.2 Desempenho

| ID | Nome | Descrição | Como verificar | Prioridade | Status |
|---|---|---|---|---|---|
| RNF03 | *(exemplo)* Tempo de resposta da rota | O cálculo de uma rota deve ser concluído em até **[X] segundos** em condições normais de rede. | Teste de desempenho | Importante | Proposto |

### 4.3 Segurança e privacidade

| ID | Nome | Descrição | Como verificar | Prioridade | Status |
|---|---|---|---|---|---|
| RNF04 | *(exemplo)* Proteção da localização | A localização do usuário só deve ser coletada com consentimento explícito e usada apenas para o funcionamento do app, conforme a LGPD. | Revisão de fluxo de consentimento | Essencial | Proposto |

### 4.4 Disponibilidade e confiabilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Status |
|---|---|---|---|---|---|
| RNF05 | *(exemplo)* Comportamento sem conexão | O app deve informar de forma clara quando estiver sem conexão com a internet, em vez de falhar silenciosamente. | Teste manual | Importante | Proposto |

### 4.5 Compatibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Status |
|---|---|---|---|---|---|
| RNF06 | *(exemplo)* Plataformas suportadas | O app deve funcionar em **[Android / iOS / versões mínimas]**. | Testes em dispositivos | Essencial | Proposto |

### 4.6 Manutenibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Status |
|---|---|---|---|---|---|
| RNF07 | *(exemplo)* Padrão de código e versionamento | O código deve seguir o padrão do [guia de contribuição](../CONTRIBUTING.md). | Revisão de PR | Importante | Proposto |

> **Dica de redação:** um bom RNF é **mensurável**. Em vez de "o app deve ser rápido", escreva "a rota deve ser calculada em até X segundos". A coluna *Como verificar* ajuda a garantir isso.

---

## 5. Matriz de rastreabilidade

Relaciona os requisitos aos casos de uso e às regras de negócio.

| Requisito | Caso de uso | Regra de negócio | Versão |
|---|---|---|---|
| RF01 | UC01 | n/a | V1 |
| RF03 | UC03 | RN01 | V1 |
| [RF/RNF] | [UC] | [RN] | [V1/V2] |

---

## 6. Resumo

| Tipo | Total | Essencial | Importante | Desejável | Futuro |
|---|---|---|---|---|---|
| Funcionais (RF) | [n] | [n] | [n] | [n] | [n] |
| Não funcionais (RNF) | [n] | [n] | [n] | [n] | [n] |

---

## 7. Pendências

- [ ] Substituir os exemplos pelos requisitos reais do grupo
- [ ] Revisar as prioridades com a equipe
- [ ] Ligar cada RF aos casos de uso (`04-casos-de-uso.md`) e às regras (`02-regras-de-negocio.md`)
- [ ] Definir valores numéricos dos RNF (tempos, plataformas, versões)
- [ ] Atualizar versão e data ao aprovar o documento

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 06/10/2026 | Modelo base |
