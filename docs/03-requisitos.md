# Requisitos – Mão na Roda

> **Versão do documento:** 1.0 <br>
> **Data:** 13/10/2026 <br>
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
| RF01 | Exibir mapa e localização | O sistema deve exibir o mapa com a localização atual do usuário. | Essencial | V1 | Aprovado | - |
| RF02 | Buscar destino | O sistema deve permitir que o usuário informe um destino por texto. | Essencial | V1 | Aprovado | - |

### 3.2 Rotas acessíveis

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF03 | Calcular rota acessível | O sistema deve calcular rotas considerando o perfil de mobilidade escolhido pelo usuário. | Essencial | V1 | Aprovado | RN01, RN03, RN04 |
| RF04 | Exibir resumo de barreiras | O sistema deve exibir, para cada rota, distância, tempo estimado e resumo das barreiras do trajeto. | Essencial | V1 | Aprovado | RN02 |
| RF05 | Exibir ultima verificação da rota | O sistema deve exibir a data da última verificação, de cada parte da rota, sinalizando se está desatualizada | Importante | V1 | Aprovado | RN06, RN07 |
| RF06 | Alertar que a rota é uma sugestão | O sistema deve alertar o usuário que a rota apresentada é baseada nos dados disponíveis, e que a situação verdadeira pode ser diferente | Importante | V1 | Aprovado | RN13 |
| RF07 | Re-Routing opcional | Quando uma rota possuir relatos não validados, o sistema deve permitir que o usuário solicite uma alternativa que evite os trechos associados aos relatos selecionados. | Importante | V1 | Aprovado | RN10, RN12 |


### 3.3 Preferências do usuário

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF08 | Selecionar perfil de mobilidade | O sistema deve permitir que o usuário escolha seu perfil de mobilidade para personalizar as rotas. | Essencial | V1 | Aprovado | RN01 |

### 3.4 Relato de problemas

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF09 | Relatar problema no trajeto | O sistema deve permitir que o usuário registre um problema de acessibilidade em um local, com descrição em texto. | Importante | V1 | Aprovado | RN08 |
| RF14 | Classificar relatos | O sistema deve diferenciar relatos com foto e descrição e relatos apenas com descrição. | Importante | V1 | Aprovado | RN08, RN11 |
| RF18 | Anexar fotos ao relato | O sistema deve permitir que o usuário autenticado anexe até três fotos a cada relato. | Importante | V1 | Aprovado | RN08, RN11, RNF20 |
| RF15 | Confirmar relato | O sistema deve permitir que um usuário confirme a existência de um problema relatado por outro usuário. | Essencial | V2 | Aprovado | RN09 |
| RF16 | Impedir confirmação do próprio relato | O sistema deve impedir que o autor confirme o próprio relato. | Essencial | V2 | Aprovado | RN09 |
| RF17 | Impedir confirmações duplicadas | O sistema deve permitir que cada usuário confirme o mesmo relato apenas uma vez. | Essencial | V2 | Aprovado | RN09 |

### 3.5 Conta de usuário

| ID | Nome | Descrição | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|
| RF10 | Criar conta | O usuário deve poder criar uma conta informando nome, sobrenome, e-mail e senha. | Essencial | V1 | Aprovado | RN15 |
| RF11 | Fazer login | O usuário deve poder acessar o sistema com a sua conta. | Essencial | V1 | Aprovado | RN15 |
| RF12 | Excluir conta | O usuário deve poder excluir a própria conta, com a remoção de seus dados pessoais. | Essencial | V1 | Aprovado | RN16 |
| RF13 | Usar sem conta | O sistema deve permitir o uso do app sem conta, sem o armazenamento dos dados da rota | Essencial | V1 | Aprovado | RN17, RN18, RF03 |

> **Dica de redação:** descreva cada RF começando com "O sistema deve..." ou "O usuário deve poder...", com **um comportamento por requisito**. Se a descrição precisar de "e", considere dividir.

---

## 4. Requisitos não funcionais (RNF)

Agrupados por categoria de qualidade.

### 4.1 Usabilidade e acessibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF01 | Interface acessível | A interface deve seguir a WCAG 2.2 no nível AA: contraste mínimo de 4,5:1 para texto normal e 3:1 para texto grande e componentes essenciais; áreas de toque de pelo menos 48 × 48 dp; respeito à escala de fonte do Android e rótulos compatíveis com TalkBack. | Revisar telas, medir contraste e áreas de toque, alterar a escala de fonte e testar os controles principais com TalkBack. | Essencial | V1 | Aprovado | RF01–RF18 |
| RNF02 | Uso com uma mão | As ações principais de busca de destino, seleção de perfil, consulta de rota, solicitação de alternativa e envio de relato devem poder ser concluídas com uma mão, em modo retrato, em dispositivo Android de referência de aproximadamente 6,5 polegadas, sem gestos simultâneos ou arrasto prolongado. | Executar os fluxos definidos com uma mão em dispositivo ou emulador de referência. | Importante | V1 | Aprovado | RF02, RF04, RF07, RF08, RF09 |

### 4.2 Desempenho

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF03 | Tempo de resposta da rota | Em conexão estável por Wi-Fi ou 4G, 95% das solicitações de rota devem apresentar resultado em até 3 segundos, contados após a confirmação de origem, destino e perfil de mobilidade. Após 3 segundos, o app deve manter indicador de carregamento; após 10 segundos sem resposta, deve informar a falha e permitir nova tentativa. | Medir o tempo entre a confirmação da consulta e a apresentação da rota em testes de desempenho; verificar o indicador de carregamento e o tratamento após 10 segundos. | Importante | V1 | Aprovado | RN04, RF03, RF07 |

### 4.3 Segurança e privacidade

#### 4.3.1 Proteção de dados e credenciais

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF08 | Proteção das senhas | As senhas devem ser armazenadas com uma função adaptativa própria para senhas, utilizando salt único por senha e parâmetros definidos pela referência de segurança adotada pelo projeto. As senhas nunca devem ser persistidas nem registradas em texto legível. | Criar contas com senhas iguais, inspecionar diretamente os registros e verificar o algoritmo, o salt e os parâmetros utilizados. Confirmar que senhas iguais produzam valores armazenados diferentes e testar a autenticação com senhas correta e incorreta. | Essencial | V1 | Aprovado | RN15, RF10, RF11 |
| RNF10 | Proteção dos segredos da aplicação | Senhas, chaves de API, tokens e demais segredos da aplicação não devem ser armazenados no código-fonte nem versionados no repositório. | Inspecionar o repositório, os artefatos de build e as configurações da aplicação. Verificar que os segredos sejam obtidos pelo mecanismo de configuração segura adotado pelo projeto. | Essencial | V1 | Aprovado | RF01, RF02, RF03, RF10, RF11 |
| RNF13 | Criptografia de dados sensíveis armazenados | Os dados definidos pelo projeto como sensíveis e que precisem ser recuperados pela aplicação devem ser armazenados de forma criptografada. | Persistir valores conhecidos, consultar diretamente os registros e verificar que os valores originais não estejam legíveis. Testar se somente a aplicação autorizada consegue recuperá-los. | Essencial | V1 | Aprovado | RN14, RN16, RN18, RF01, RF08, RF09, RF10, RF12, RF13 |
| RNF14 | Gestão das chaves criptográficas | As chaves utilizadas para criptografar dados sensíveis devem ser armazenadas separadamente do banco de dados e do código-fonte, com acesso limitado aos serviços autorizados. | Inspecionar a configuração, a localização das chaves e suas permissões. Verificar que o acesso isolado ao banco de dados ou ao repositório não permita obter as chaves. | Essencial | V1 | Aprovado | RNF13 |
| RNF19 | Proteção de dados no dispositivo e em registros | Dados sensíveis, tokens, localização e conteúdo privado não devem ser armazenados em texto legível no dispositivo nem incluídos em logs de produção. | Inspecionar armazenamento local, cache, logs, backups do aplicativo e artefatos de depuração após executar os fluxos principais. | Essencial | V1 | Aprovado | RN14, RN18, RF01, RF08, RF09, RF11, RF13 |
| RNF20 | Segurança das fotos enviadas | O envio de fotos deve aceitar somente os formatos JPG, JPEG e PNG, com até 5 MB por arquivo; validar o conteúdo no servidor e remover metadados que não sejam necessários, incluindo localização EXIF. | Enviar arquivos em formato inválido, extensão manipulada, tamanho superior a 5 MB e metadados de localização; verificar a rejeição ou a remoção dos metadados. | Essencial | V1 | Aprovado | RN08, RN11, RF09, RF14, RF18 |

#### 4.3.2 Banco de dados e consultas

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF09 | Controle de acesso ao banco de dados | O banco de dados deve aceitar conexões somente de serviços e usuários autorizados, utilizando contas com o menor conjunto de permissões necessário para cada operação. | Revisar usuários, papéis e permissões. Tentar acessar e executar operações com uma conta não autorizada ou sem a permissão necessária e verificar que as operações sejam recusadas. | Essencial | V1 | Aprovado | RF09, RF10, RF11, RF12, RF14, RF15, RF16, RF17 |
| RNF11 | Criptografia da conexão com o banco | Toda comunicação entre a aplicação e o banco de dados deve utilizar TLS com validação do certificado do servidor. Conexões não criptografadas devem ser recusadas. | Inspecionar a configuração da conexão e tentar conectar sem TLS ou com certificado inválido, verificando que ambas as conexões sejam recusadas. | Essencial | V1 | Aprovado | RF09, RF10, RF11, RF12, RF14, RF15, RF16, RF17 |
| RNF12 | Proteção contra injeção de SQL | Consultas que utilizem dados provenientes de usuários ou fontes externas devem empregar parâmetros ou mecanismos equivalentes. Dados externos não devem ser concatenados diretamente em comandos SQL. | Revisar o código e executar testes com entradas maliciosas representativas, verificando que sejam tratadas como valores e não alterem a estrutura das consultas. | Essencial | V1 | Aprovado | RF02, RF09, RF10, RF11, RF15 |

#### 4.3.3 Autenticação e autorização

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF16 | Proteção das sessões | Tokens de autenticação devem expirar após 15 minutos, ser armazenados utilizando os mecanismos seguros da plataforma e ser invalidados quando a conta for excluída ou a sessão for encerrada. Após a expiração, o app pode exibir a última conta usada apenas como identificação visual, mas deve exigir novo login para ações protegidas. | Inspecionar o armazenamento local; testar expiração, encerramento de sessão e exclusão da conta; confirmar que tokens antigos não autorizem requisições e que ações protegidas exijam novo login. | Essencial | V1 | Aprovado | RN15, RN16, RF11, RF12, RF15, RF16, RF17 |
| RNF17 | Autorização no servidor | Toda operação protegida deve verificar no servidor se o usuário autenticado possui permissão para executá-la, independentemente das restrições exibidas pelo aplicativo. | Tentar excluir dados de outra conta, confirmar o próprio relato e repetir uma confirmação por meio de requisições manipuladas, verificando que as operações sejam recusadas. | Essencial | V1 | Aprovado | RN09, RN15, RN16, RF12, RF15, RF16, RF17 |
| RNF18 | Limite de tentativas de login | Cinco tentativas de login falhas em 15 minutos para o mesmo e-mail devem bloquear novas tentativas por 15 minutos. | Realizar cinco tentativas de acesso inválidas para o mesmo e-mail dentro de 15 minutos e verificar o bloqueio; confirmar que o acesso volta a ser permitido após o período. | Essencial | V1 | Aprovado | RN15, RF10, RF11 |
| RNF21 | Limite de envio de relatos | Cada usuário autenticado pode enviar no máximo cinco relatos por hora. | Enviar relatos até atingir o limite e verificar que a próxima tentativa seja recusada até o início de uma nova janela de uma hora. | Essencial | V1 | Aprovado | RN08, RN15, RF09 |
| RNF22 | Limite de confirmações de relatos | Cada usuário autenticado pode confirmar no máximo 30 relatos por hora. | Confirmar relatos até atingir o limite e verificar que a próxima tentativa seja recusada até o início de uma nova janela de uma hora. | Essencial | V2 | Aprovado | RN09, RN15, RF15, RF16, RF17 |

#### 4.3.4 Privacidade e localização

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF04 | Proteção da localização | A localização só deve ser coletada após consentimento explícito, para as finalidades informadas ao usuário, e sua coleta deve cessar quando a permissão for negada ou revogada. | Testar concessão, negação e revogação da permissão. Inspecionar requisições, registros e armazenamento para confirmar que a localização não seja coletada após a negação ou revogação. | Essencial | V1 | Aprovado | RN14, RF01, RF03 |

#### 4.3.5 Comunicação de rede

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF15 | Proteção das comunicações da API | Toda comunicação entre o aplicativo e os serviços remotos deve utilizar TLS com validação do certificado do servidor. | Interceptar o tráfego em ambiente de teste e verificar que não existam requisições em texto legível. Tentar conexão com certificado inválido e confirmar que seja recusada. | Essencial | V1 | Aprovado | RF01–RF17 |


### 4.4 Disponibilidade e confiabilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF05 | Comportamento sem conexão | O app deve informar de forma clara quando estiver sem conexão com a internet, em vez de falhar silenciosamente. | Teste manual. | Importante | V1 | Aprovado | RF01–RF09 |

### 4.5 Compatibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF06 | Plataformas suportadas | O app deve funcionar no Android 24 e versões posteriores. | Testes em dispositivos. | Essencial | V1 | Aprovado | RF01–RF17 |

### 4.6 Manutenibilidade

| ID | Nome | Descrição | Como verificar | Prioridade | Versão | Status | Relacionado |
|---|---|---|---|---|---|---|---|
| RNF07 | Padrão de código e versionamento | O versionamento do código deve seguir o padrão do [guia de contribuição](../CONTRIBUTING.md). | Revisão de Pull Requests e mensagens de commit. | Importante | V1 | Aprovado | [Guia de contribuição](../CONTRIBUTING.md) |

---

## 5. Matriz de rastreabilidade

Relaciona os requisitos aos casos de uso e às regras de negócio.

| Requisito | Caso de uso | Regra de negócio | Versão |
|---|---|---|---|
| RF01 | UC01 | RN14 | V1 |
| RF02 | UC02 | — | V1 |
| RF03 | UC04 | RN01, RN03, RN04, RN14 | V1 |
| RF04 | UC04 | RN02, RN05, RN06, RN07, RN10 | V1 |
| RF05 | UC04 | RN06, RN07 | V1 |
| RF06 | UC04 | RN13 | V1 |
| RF07 | UC05 | RN10, RN12 | V1 |
| RF08 | UC03 | RN01, RN02 | V1 |
| RF09 | UC09 | RN08, RN15 | V1 |
| RF10 | UC06 | RN15 | V1 |
| RF11 | UC07 | RN15 | V1 |
| RF12 | UC10 | RN16 | V1 |
| RF13 | UC08 | RN17, RN18 | V1 |
| RF14 | UC09 | RN08, RN11 | V1 |
| RF15 | UC11 | RN09 | V2 |
| RF16 | UC11 | RN09 | V2 |
| RF17 | UC11 | RN09 | V2 |
| RF18 | UC09 | RN08, RN11 | V1 |
| RNF01 | UC01–UC10 | — | V1 |
| RNF02 | UC02–UC05, UC09 | — | V1 |
| RNF03 | UC04, UC05 | RN04, RN12 | V1 |
| RNF04 | UC01, UC04, UC09 | RN14 | V1 |
| RNF05 | UC01–UC09 | — | V1 |
| RNF06 | UC01–UC10 | — | V1 |
| RNF07 | — | — | V1 |
| RNF08 | UC06, UC07 | RN15 | V1 |
| RNF09 | UC06, UC07, UC09, UC10, UC11 | RN08, RN09, RN15, RN16 | V1 |
| RNF10 | UC01, UC02, UC04, UC06, UC07 | RN14, RN15 | V1 |
| RNF11 | UC06, UC07, UC09, UC10, UC11 | RN08, RN09, RN15, RN16 | V1 |
| RNF12 | UC02, UC06, UC07, UC09, UC11 | RN08, RN09, RN15 | V1 |
| RNF13 | UC01, UC03, UC06, UC08, UC09, UC10 | RN14, RN15, RN16, RN18 | V1 |
| RNF14 | — | RNF13 | V1 |
| RNF15 | UC01–UC10 | RN14, RN15 | V1 |
| RNF16 | UC07, UC10, UC11 | RN15, RN16 | V1 |
| RNF17 | UC09, UC10, UC11 | RN08, RN09, RN15, RN16 | V1 |
| RNF18 | UC06, UC07 | RN15 | V1 |
| RNF19 | UC01, UC03, UC07, UC08, UC09, UC10 | RN14, RN15, RN16, RN18 | V1 |
| RNF20 | UC09 | RN08, RN11 | V1 |
| RNF21 | UC09 | RN08, RN15 | V1 |
| RNF22 | UC11 | RN09, RN15 | V2 |

---

## 6. Resumo

| Tipo | Total | Essencial | Importante | Desejável | Futuro |
|---|---|---|---|---|---|
| Funcionais (RF) | 18 | 12 | 6 | 0 | 0 |
| Não funcionais (RNF) | 22 | 18 | 4 | 0 | 0 |

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 06/10/2026 | Modelo base |
| 0.2 | 07/10/2026 | Modelo base |
| 0.3 | 08/10/2026 | Modelo base |
| 0.4 | 08/10/2026 | Reorganização e ampliação dos requisitos não funcionais de segurança e privacidade |
| 0.5 | 09/10/2026 | Aprovação parcial de requisitos funcionais e não funcionais |
| 0.6 | 13/10/2026 | Atualização da estrutura após mover os critérios de aceite para a visão de negócio. |
| 0.7 | 13/10/2026 | Inclusão da idade como dado opcional no cadastro. |
| 0.8 | 13/10/2026 | Definição dos campos obrigatórios do cadastro e remoção da idade opcional. |
| 0.9 | 13/10/2026 | Preenchimento da matriz de rastreabilidade entre requisitos, regras e casos de uso. |
| 0.10 | 13/10/2026 | Remoção da pendência de revisão de prioridades. |
| 0.11 | 13/10/2026 | Definição dos critérios de acessibilidade, desempenho, sessões, limites e fotos; inclusão dos requisitos RF18, RNF21 e RNF22. |
| 0.12 | 13/10/2026 | Conclusão das pendências de parâmetros objetivos dos requisitos não funcionais. |
| 1.0 | 13/10/2026 | Aprovação da especificação inicial de requisitos para a V1. |
