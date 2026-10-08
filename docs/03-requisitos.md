# Requisitos – Mão na Roda

> **Versão do documento:** 0.4 <br>
> **Data:** 08/10/2026 <br>
> **Documentos relacionados:** [Visão de negócio](01-visao-de-negocio.md) · [Regras de negócio](02-regras-de-negocio.md) · [Casos de uso](04-casos-de-uso.md) · [Tech stack](05-tech-stack.md)

---

## 1. Introdução

Este documento lista os requisitos do **Mão na Roda**, separados em:

- **Requisitos funcionais (RF):** o que o sistema **faz**, ou seja, as funcionalidades que o usuário ou outros sistemas podem usar.
- **Requisitos não funcionais (RNF):** **como** o sistema deve se comportar, ou seja, qualidades e restrições como desempenho, segurança, usabilidade e acessibilidade.

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

> Requisitos e regras de versões diferentes podem estar relacionados. Essa
> relação indica dependência conceitual ou evolução de uma funcionalidade, e
> não que ambos precisem ser implementados na mesma versão. Por exemplo, a
> RN09 da V2 depende dos relatos enviados pela RF09 desde a V1.

### 2.4 Status

`Proposto` · `Aprovado` · `Em desenvolvimento` · `Implementado` · `Cancelado`

### 2.5 Referências cruzadas

Cada requisito pode citar outros **requisitos funcionais** (`RF`), **requisitos não funcionais** (`RNF`), **regras de negócio** (`RN`) e **casos de uso** (`UC`) relacionados, para manter a rastreabilidade entre os documentos.

---

## 3. Requisitos funcionais (RF)

Agrupados por módulo, para facilitar a leitura.

### 3.1 Mapa e localização

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF01 | Exibir mapa e localização | O sistema deve exibir o mapa com a localização atual do usuário. | Essencial | V1 | Proposto | - |
| RF02 | Buscar destino | O sistema deve permitir que o usuário informe um destino por texto. | Essencial | V1 | Proposto | - |

### 3.2 Rotas acessíveis

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF03 | Calcular rota acessível | O sistema deve calcular rotas considerando o perfil de mobilidade escolhido pelo usuário. | Essencial | V1 | Proposto | RN01, RN03, RN04 |
| RF04 | Exibir resumo de barreiras | O sistema deve exibir, para cada rota, distância, tempo estimado e resumo das barreiras do trajeto. | Essencial | V1 | Proposto | RN02 |
| RF05 | Exibir ultima verificação da rota  | O sistema deve exibir a data da última verificação, de cada parte da rota, sinalizando se está desatualizada | Importante | V1 | Proposto | RN06, RN07 | 
| RF06 | Alertar que a rota é uma sugestão | O sistema deve alertar o usuário que a rota apresentada é baseada nos dados disponíveis, e que a situação verdadeira pode ser diferente | Importante | V1 | Proposto | RN13 |
| RF07 | Re-Routing opcional | Quando uma rota possuir relatos não validados, o sistema deve permitir que o usuário solicite uma alternativa que evite os trechos associados aos relatos selecionados. | Importante | V1 | Proposto | RN10, RN12 |


### 3.3 Preferências do usuário

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF08 | Selecionar perfil de mobilidade | O sistema deve permitir que o usuário escolha seu perfil de mobilidade para personalizar as rotas. | Essencial | V1 | Proposto | RN01 |

### 3.4 Relato de problemas

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF09 | Relatar problema no trajeto | O sistema deve permitir que o usuário registre um problema de acessibilidade em um local, com descrição em texto. | Importante | V1 | Proposto | RN08 |
| RF14 | Classificar relatos | O sistema deve diferenciar relatos com foto e descrição e apenas com descrição | Importante | V1 | Proposto | RN08, RN11 |
| RF15 | Confirmar relato | O sistema deve permitir que um usuário confirme a existência de um problema relatado por outro usuário. | Essencial | V2 | Proposto | RN09 |
| RF16 | Impedir confirmação do próprio relato | O sistema deve impedir que o autor confirme o próprio relato. | Essencial | V2 | Proposto | RN09 |
| RF17 | Impedir confirmações duplicadas | O sistema deve permitir que cada usuário confirme o mesmo relato apenas uma vez. | Essencial | V2 | Proposto | RN09 |

### 3.5 Conta de usuário

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF10 | Criar conta | O usuário deve poder criar uma conta no sistema. | Essencial | V1 | Proposto | RN15 |
| RF11 | Fazer login | O usuário deve poder acessar o sistema com a sua conta. | Essencial | V1 | Proposto | RN15 |
| RF12 | Excluir conta | O usuário deve poder excluir a própria conta, com a remoção de seus dados pessoais. | Essencial | V1 | Proposto | RN16 |
| RF13 | Usar sem conta | O sistema deve permitir o uso do app sem conta, sem o armazenamento dos dados da rota | Essencial | V1 | Proposto | RN17, RN18, RF03 |

> **Dica de redação:** descreva cada RF começando com "O sistema deve..." ou "O usuário deve poder...", com **um comportamento por requisito**. Se a descrição precisar de "e", considere dividir.

---

## 4. Requisitos não funcionais (RNF)

Agrupados por categoria de qualidade.

### 4.1 Usabilidade e acessibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF01 | Interface acessível | A interface deve seguir boas práticas de acessibilidade: contraste adequado, fontes legíveis e botões de tamanho adequado ao toque. | Revisão de telas e testes com usuários. | Essencial | V1 | Proposto | RF01–RF15 |
| RNF02 | Uso com uma mão | As ações principais devem ser alcançáveis com uma mão só. | Teste em dispositivos reais. | Importante | V1 | Proposto | RF02, RF04, RF07, RF08, RF09 |

### 4.2 Desempenho

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF03 | Tempo de resposta da rota | O cálculo de uma rota deve ser concluído em até **1 segundo** em condições normais de rede. | Teste de desempenho. | Importante | V1 | Proposto | RN04, RF03, RF07 |

### 4.3 Segurança e privacidade

#### 4.3.1 Proteção de dados e credenciais

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF08 | Proteção das senhas | As senhas devem ser armazenadas com uma função adaptativa própria para senhas, utilizando salt único por senha e parâmetros definidos pela referência de segurança adotada pelo projeto. As senhas nunca devem ser persistidas nem registradas em texto legível. | Criar contas com senhas iguais, inspecionar diretamente os registros e verificar o algoritmo, o salt e os parâmetros utilizados. Confirmar que senhas iguais produzam valores armazenados diferentes e testar a autenticação com senhas correta e incorreta. | Essencial | V1 | Proposto | RN15, RF10, RF11 |
| RNF10 | Proteção dos segredos da aplicação | Senhas, chaves de API, tokens e demais segredos da aplicação não devem ser armazenados no código-fonte nem versionados no repositório. | Inspecionar o repositório, os artefatos de build e as configurações da aplicação. Verificar que os segredos sejam obtidos pelo mecanismo de configuração segura adotado pelo projeto. | Essencial | V1 | Proposto | RF01, RF02, RF03, RF10, RF11 |
| RNF13 | Criptografia de dados sensíveis armazenados | Os dados definidos pelo projeto como sensíveis e que precisem ser recuperados pela aplicação devem ser armazenados de forma criptografada. | Persistir valores conhecidos, consultar diretamente os registros e verificar que os valores originais não estejam legíveis. Testar se somente a aplicação autorizada consegue recuperá-los. | Essencial | V1 | Proposto | RN14, RN16, RN18, RF01, RF08, RF09, RF10, RF12, RF13 |
| RNF14 | Gestão das chaves criptográficas | As chaves utilizadas para criptografar dados sensíveis devem ser armazenadas separadamente do banco de dados e do código-fonte, com acesso limitado aos serviços autorizados. | Inspecionar a configuração, a localização das chaves e suas permissões. Verificar que o acesso isolado ao banco de dados ou ao repositório não permita obter as chaves. | Essencial | V1 | Proposto | RNF13 |
| RNF19 | Proteção de dados no dispositivo e em registros | Dados sensíveis, tokens, localização e conteúdo privado não devem ser armazenados em texto legível no dispositivo nem incluídos em logs de produção. | Inspecionar armazenamento local, cache, logs, backups do aplicativo e artefatos de depuração após executar os fluxos principais. | Essencial | V1 | Proposto | RN14, RN18, RF01, RF08, RF09, RF11, RF13 |
| RNF20 | Segurança das fotos enviadas | O envio de fotos deve aceitar somente os formatos e tamanhos definidos pelo projeto, validar o conteúdo no servidor e remover metadados que não sejam necessários. | Enviar arquivos com formato inválido, extensão manipulada, tamanho excessivo e metadados de localização; verificar a rejeição ou o tratamento conforme a política definida. | Essencial | V1 | Proposto | RN08, RN11, RF09, RF14 |

#### 4.3.2 Banco de dados e consultas

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF09 | Controle de acesso ao banco de dados | O banco de dados deve aceitar conexões somente de serviços e usuários autorizados, utilizando contas com o menor conjunto de permissões necessário para cada operação. | Revisar usuários, papéis e permissões. Tentar acessar e executar operações com uma conta não autorizada ou sem a permissão necessária e verificar que as operações sejam recusadas. | Essencial | V1 | Proposto | RF09, RF10, RF11, RF12, RF14, RF15, RF16, RF17 |
| RNF11 | Criptografia da conexão com o banco | Toda comunicação entre a aplicação e o banco de dados deve utilizar TLS com validação do certificado do servidor. Conexões não criptografadas devem ser recusadas. | Inspecionar a configuração da conexão e tentar conectar sem TLS ou com certificado inválido, verificando que ambas as conexões sejam recusadas. | Essencial | V1 | Proposto | RF09, RF10, RF11, RF12, RF14, RF15, RF16, RF17 |
| RNF12 | Proteção contra injeção de SQL | Consultas que utilizem dados provenientes de usuários ou fontes externas devem empregar parâmetros ou mecanismos equivalentes. Dados externos não devem ser concatenados diretamente em comandos SQL. | Revisar o código e executar testes com entradas maliciosas representativas, verificando que sejam tratadas como valores e não alterem a estrutura das consultas. | Essencial | V1 | Proposto | RF02, RF09, RF10, RF11, RF15 |

#### 4.3.3 Autenticação e autorização

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF16 | Proteção das sessões | Tokens de autenticação devem possuir expiração, ser armazenados utilizando os mecanismos seguros da plataforma e ser invalidados quando a conta for excluída ou a sessão for encerrada. | Inspecionar o armazenamento local, testar expiração, encerramento de sessão e exclusão da conta e verificar que tokens antigos não autorizem novas requisições. | Essencial | V1 | Proposto | RN15, RN16, RF11, RF12, RF15, RF16, RF17 |
| RNF17 | Autorização no servidor | Toda operação protegida deve verificar no servidor se o usuário autenticado possui permissão para executá-la, independentemente das restrições exibidas pelo aplicativo. | Tentar excluir dados de outra conta, confirmar o próprio relato e repetir uma confirmação por meio de requisições manipuladas, verificando que as operações sejam recusadas. | Essencial | V1 | Proposto | RN09, RN15, RN16, RF12, RF15, RF16, RF17 |
| RNF18 | Proteção contra abuso | Operações de autenticação, criação de relatos e confirmação de relatos devem possuir limites de requisições compatíveis com o uso normal. | Enviar requisições repetidas acima dos limites definidos e verificar o bloqueio temporário ou a limitação sem impedir o uso normal. | Essencial | V1 | Proposto | RN09, RF09, RF10, RF11, RF15, RF16, RF17 |

#### 4.3.4 Privacidade e localização

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF04 | Proteção da localização | A localização só deve ser coletada após consentimento explícito, para as finalidades informadas ao usuário, e sua coleta deve cessar quando a permissão for negada ou revogada. | Testar concessão, negação e revogação da permissão. Inspecionar requisições, registros e armazenamento para confirmar que a localização não seja coletada após a negação ou revogação. | Essencial | V1 | Proposto | RN14, RF01, RF03 |

#### 4.3.5 Comunicação de rede

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF15 | Proteção das comunicações da API | Toda comunicação entre o aplicativo e os serviços remotos deve utilizar TLS com validação do certificado do servidor. | Interceptar o tráfego em ambiente de teste e verificar que não existam requisições em texto legível. Tentar conexão com certificado inválido e confirmar que seja recusada. | Essencial | V1 | Proposto | RF01–RF17 |


### 4.4 Disponibilidade e confiabilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF05 | Comportamento sem conexão | O app deve informar de forma clara quando estiver sem conexão com a internet, em vez de falhar silenciosamente. | Teste manual. | Importante | V1 | Proposto | RF01–RF09 |

### 4.5 Compatibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF06 | Plataformas suportadas | O app deve funcionar no Android 24 e versões posteriores. | Testes em dispositivos. | Essencial | V1 | Proposto | RF01–RF17 |

### 4.6 Manutenibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF07 | Padrão de código e versionamento | O versionamento do código deve seguir o padrão do [guia de contribuição](../CONTRIBUTING.md). | Revisão de Pull Requests e mensagens de commit. | Importante | V1 | Proposto | [Guia de contribuição](../CONTRIBUTING.md) |

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
| Não funcionais (RNF) | 20 | 16 | 4 | 0 | 0 |

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
| 0.2 | 07/10/2026 | Modelo base |
| 0.3 | 08/10/2026 | Modelo base |
| 0.4 | 08/10/2026 | Reorganização e ampliação dos requisitos não funcionais de segurança e privacidade |
