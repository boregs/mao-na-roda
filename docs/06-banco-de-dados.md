# Banco de Dados — Mão na Roda

> **Versão do documento:** 0.4  
> **Data:** 20/10/2026  
> **Status:** Rascunho para validação  
> **Documentos relacionados:** [Regras de negócio](https://github.com/boregs/mao-na-roda/blob/ce74c88/docs/02-regras-de-negocio.md) · [Requisitos](https://github.com/boregs/mao-na-roda/blob/ce74c88/docs/03-requisitos.md) · [Tech stack](https://github.com/boregs/mao-na-roda/blob/ce74c88/docs/05-tech-stack.md)

## 1. Objetivo e limites do documento

Propor a organização dos dados do Mão na Roda no Supabase, documentando entidades, relacionamentos, regras de integridade, acesso e descarte.

Este documento não descreve um banco inspecionado. Não houve acesso ao projeto Supabase nem a um esquema SQL existente. Todas as tabelas próprias, colunas, índices e estratégias aqui apresentados são propostas; não foram criados ou testados no banco.

Os tipos abaixo representam o modelo lógico inicial. A implementação física, especialmente dos campos que exigirem criptografia, deverá ser definida antes das migrations. As regras já aprovadas continuam sendo a referência; decisões ainda abertas estão na seção 12.

## 2. Responsabilidades e escopo

| Componente | Responsabilidade |
|---|---|
| Supabase Auth | Identidade, e-mail de login, tratamento de senhas e sessões. |
| PostgreSQL | Dados de domínio: usuários, perfis, classificação de barreiras, locais, observações de acessibilidade e relatos. |
| Supabase Storage | Arquivos das fotos. O banco próprio guarda apenas os metadados e a referência ao objeto. |
| API Java/Spring Boot | Autorização, regras de negócio, validação dos arquivos, consultas espaciais e coordenação da exclusão de conta e do descarte. |

**V1:** cadastro e preferências, consulta de acessibilidade, relatos não validados e fotos. **V2:** confirmações de relatos e incorporação dos relatos confirmados aos dados usados no cálculo automático.

Não se propõe tabela de histórico de rotas na V1. A RN18 proíbe persistir rota e perfil escolhido por quem usa o aplicativo sem conta; também não há RF aprovado que exija histórico para usuários cadastrados. O catálogo de perfis pode existir no banco sem registrar a escolha de visitantes.

### 2.1 Organização dos schemas

| Schema | Gestão e uso propostos |
|---|---|
| `auth` | Gerenciado pelo Supabase Auth. Referenciar `auth.users(id)`; não recriar suas tabelas nem persistir senhas em tabelas próprias. |
| `storage` | Gerenciado pelo Supabase Storage. Operar os arquivos pela API do Storage, sem manipular diretamente seus metadados por SQL. |
| `app` | Schema confirmado para as tabelas do domínio, acessado pela API Java. |

A estratégia definida é que a API Java acesse o banco por conexão JDBC e que o Flutter não acesse diretamente as tabelas de domínio. Manter `app` fora dos schemas expostos pela Data API e avaliar a desativação da Data API para as tabelas próprias. Auth e Storage continuam sendo integrações distintas.

## 3. Convenções

- Tabelas e colunas em `snake_case`, sem acentos.
- Todas as tabelas próprias do schema `app` usam `id bigserial PRIMARY KEY` como chave interna autoincrementável e `uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid()` como identificador público.
- As FKs entre tabelas próprias referenciam os IDs numéricos. A API recebe e devolve o `uuid` público e resolve internamente o respectivo `id`.
- As tabelas gerenciadas pelo Supabase são exceções a esse padrão. Em especial, `auth.users.id` permanece `uuid`; o vínculo com `app.usuarios` é feito por `auth_user_id`.
- PK significa chave primária e FK, chave estrangeira.
- Datas e horários em `timestamptz`, normalizados para UTC pela aplicação; exibição conforme o fuso do usuário.
- `criado_em` e `atualizado_em` representam eventos técnicos. Não substituem a data de verificação de uma condição de acessibilidade.
- Valores de classificação e estado devem ser limitados por `CHECK`, enum ou tabela de domínio, conforme a migration aprovada.
- PostGIS é uma extensão proposta, ainda sujeita a aprovação. Usar SRID 4326 e ordem longitude/latitude em GeoJSON.
- Catálogos referenciados devem ser desativados, quando necessário, em vez de apagados indiscriminadamente.

Estão confirmados nesta versão os nomes das tabelas apresentados no documento, o uso do schema `app`, o acesso ao banco pela API Java via JDBC e o uso de `bigserial` nas chaves primárias internas.

## 4. Relacionamentos principais

### 4.1 Conta, relatos e fotos

A autoria é obrigatória no envio, mas pode ficar nula após a exclusão da conta. O limite de três fotos precisa ser garantido por restrições de integridade, conforme detalhado na seção de anexos.

### 4.2 Dados de acessibilidade

Uma escada é um **tipo de barreira**. Uma escada existente em um endereço é uma **ocorrência observada**. Seu efeito depende da **classificação para o perfil escolhido**. Esses conceitos não devem ser condensados em um único campo global de gravidade.

## 5. Dicionário de dados — V1

O `auth.users` não integra o dicionário das tabelas próprias: sua estrutura é gerenciada pelo Supabase Auth. Seu `id` permanece do tipo `uuid` e é referenciado por `app.usuarios.auth_user_id`. O ciclo de vida e o armazenamento das credenciais pertencem ao Auth; não criar colunas próprias nessa tabela nem duplicar o e-mail em `app.usuarios`.

### 5.1 `app.usuarios` — dados da conta

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `auth_user_id` | `uuid` | Sim | FK única para `auth.users(id)`. |
| `nome` | `text` | Sim | Não vazio; armazenamento físico depende da classificação de dados da seção 9. |
| `sobrenome` | `text` | Sim | Não vazio; mesma condição de proteção do nome. |
| `perfil_mobilidade_id` | `bigint` | Não | FK lógica para `perfis_mobilidade(id)`; preferência da conta, se salva. |
| `estado_conta` | `text` | Sim | Proposta: `ativa` ou `exclusao_em_andamento`; controle do fluxo de remoção. |
| `criado_em` | `timestamptz` | Sim | Momento da criação. |
| `atualizado_em` | `timestamptz` | Sim | Última alteração do cadastro. |

Atende RF08, RF10, RF11 e RN15. Não incluir idade ou diagnóstico de deficiência. A preferência de mobilidade pode revelar informações pessoais e precisa entrar na avaliação de proteção, mesmo sem um campo de diagnóstico.

FK definida: `auth_user_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE`. Assim, remover a identidade elimina o cadastro próprio, mas não autoriza cascata dos relatos. A API deve concluir ou compensar falhas entre criação no Auth e criação do cadastro próprio.

### 5.2 `app.perfis_mobilidade` — catálogo

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `codigo` | `text` | Sim | Único e estável. |
| `nome` | `text` | Sim | Nome apresentado no aplicativo. |
| `descricao` | `text` | Não | Explicação do perfil. |
| `ativo` | `boolean` | Sim | Controla disponibilidade para novas escolhas. |

Carga inicial: cadeirante; muleta ou outro auxílio; mobilidade reduzida em outros casos, conforme a matriz aprovada. O perfil para deficiência visual permanece fora da V1.

### 5.3 `app.tipos_barreira` — catálogo de barreiras

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `codigo` | `text` | Sim | Único e estável, por exemplo `escada_sem_alternativa`. |
| `nome` | `text` | Sim | Nome da barreira. |
| `descricao` | `text` | Não | Critério para identificar a barreira. |
| `ativo` | `boolean` | Sim | Disponibilidade do tipo. |

Carga inicial: os dez tipos da seção 4 das regras de negócio, incluindo ausência de piso tátil, irrelevante para os perfis atuais. Novos tipos exigem definição de seu impacto em todos os perfis ativos.

### 5.4 `app.classificacoes_barreira` — matriz por perfil

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `perfil_mobilidade_id` | `bigint` | Sim | FK para `perfis_mobilidade(id)`. |
| `tipo_barreira_id` | `bigint` | Sim | FK para `tipos_barreira(id)`. |
| `classificacao` | `text` | Sim | `impeditiva`, `atencao` ou `irrelevante`. |

Restrição `UNIQUE(perfil_mobilidade_id, tipo_barreira_id)` para impedir classificações duplicadas. Carregar exatamente a matriz aprovada nas RN01–RN02. Um teste de integridade deve garantir uma classificação para cada combinação ativa; a restrição única não garante a completude da matriz. Uma combinação ausente não pode ser interpretada como irrelevante.

### 5.5 `app.locais` — trechos ou pontos de interesse

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()` e independente do fornecedor de mapas. |
| `tipo` | `text` | Sim | Proposta inicial: `trecho` ou `ponto`. |
| `nome` | `text` | Não | Referência legível do local. |
| `geometria` | PostGIS `geometry(Geometry,4326)` | Sim | `LineString` para trecho ou `Point` para ponto; validar correspondência e geometria não vazia. |
| `provedor_referencia` | `text` | Não | Origem do identificador externo, quando disponível. |
| `id_externo` | `text` | Não | Identificador no provedor; preencher junto com `provedor_referencia`. |
| `criado_em` | `timestamptz` | Sim | Cadastro técnico. |
| `atualizado_em` | `timestamptz` | Sim | Alteração da representação do local. |

O provedor do identificador não é necessariamente a fonte da condição de acessibilidade. A fonte da observação é registrada na próxima tabela. Validar se uma entidade externa poderá corresponder a vários trechos internos antes de criar uma restrição única por identificador externo.

### 5.6 `app.observacoes_acessibilidade` — condições conhecidas

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `local_id` | `bigint` | Sim | FK para `locais(id)`. |
| `tipo_barreira_id` | `bigint` | Sim | FK para `tipos_barreira(id)`. |
| `condicao` | `text` | Sim | Proposta: `presente`, `ausente` ou `desconhecida`. |
| `origem` | `text` | Sim | `dados_publicos`, `openstreetmap` ou `relato_usuario`. |
| `referencia_fonte` | `text` | Não | URL, conjunto de dados ou identificador da evidência. |
| `verificado_em` | `timestamptz` | Não* | Data efetiva de verificação da condição. |
| `importado_em` | `timestamptz` | Sim | Momento de entrada no sistema. |
| `relato_origem_id` | `bigint` | Não | FK para `relatos(id)`; obrigatório para origem de relato, somente na V2. |
| `ativa` | `boolean` | Sim | Indica se a observação ainda participa da avaliação. |

*A RN06 exige data de verificação. A nulabilidade é uma proposta para representar fontes incompletas honestamente: dados sem essa data devem ser identificados como não verificados e ter tratamento aprovado pela equipe. Não inventar uma verificação usando a data de importação. Se a RN06 for mantida com exigência absoluta, rejeitar ou manter fora do conjunto operacional registros sem data, tornando a coluna obrigatória.*

Não confundir ausência de observações com ausência de barreiras. Para RF05, a API deve avaliar a cobertura das informações por parte do percurso e mostrar quando não há verificação conhecida.

Na V1, relatos não alimentam automaticamente esta tabela como evidência elegível ao cálculo padrão. Na V2, somente relatos confirmados poderão fazê-lo. Em conflitos, aplicar a prioridade de dados públicos/OSM sobre relatos da RN06. Não resolver divergências apagando silenciosamente a evidência de menor prioridade.

### 5.7 `app.relatos` — problemas informados

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `autor_id` | `bigint` | No envio | FK para `usuarios(id)`; pode ficar nula após exclusão da conta. |
| `descricao` | `text` | Sim | Não vazia após remoção de espaços. Limite de caracteres a definir. |
| `localizacao` | PostGIS `geography(Point,4326)` | Sim | Coordenada do problema relatado; não representa histórico de deslocamento. |
| `local_id` | `bigint` | Não | FK para `locais(id)`; pode ser preenchida após associação espacial. |
| `tipo_barreira_id` | `bigint` | Não | FK para `tipos_barreira(id)`; tipificação não é conteúdo mínimo exigido pela RN08. |
| `estado_validacao` | `text` | Sim | `nao_validado` na V1; `confirmado` somente na V2. |
| `criado_em` | `timestamptz` | Sim | Data de envio. |
| `confirmado_em` | `timestamptz` | Não | Usado na V2. |
| `autor_excluido_em` | `timestamptz` | Não | Marco de exclusão da conta, sem guardar o antigo identificador. |
| `descartar_em` | `timestamptz` | Não | Prazo de descarte decorrente da RN16. |

No envio, localizar o usuário pelo `auth_user_id` da identidade autenticada e usar seu `id` interno como `autor_id`; nunca confiar em um ID enviado pelo cliente. Aplicar limite de cinco relatos por hora (RNF21), inclusive sob requisições simultâneas.

Proposta de FK para autoria: `ON DELETE SET NULL`, combinada com atualização dos campos de retenção no fluxo de exclusão. Definir uma restrição que exija autor ativo ou os campos de exclusão e descarte preenchidos. Não manter cópia do nome ou e-mail no relato. A API apresenta o rótulo "usuário excluído" quando a autoria foi removida.

### 5.8 `app.relato_fotos` — referências aos anexos

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `relato_id` | `bigint` | Sim | FK para `relatos(id)`. |
| `bucket` | `text` | Sim | Bucket definido para as fotos. |
| `caminho_objeto` | `text` | Sim | Caminho estável do arquivo no Storage. |
| `posicao` | `smallint` | Sim | Valor entre 1 e 3. |
| `mime_type` | `text` | Sim | `image/jpeg` ou `image/png`, aferido após validação. |
| `tamanho_bytes` | `bigint` | Sim | Maior que zero e dentro do limite aprovado. |
| `criado_em` | `timestamptz` | Sim | Registro do anexo validado. |

Restrições propostas: `UNIQUE(relato_id, posicao)`, `CHECK(posicao BETWEEN 1 AND 3)` e `UNIQUE(bucket, caminho_objeto)`. As duas primeiras garantem, juntas, no máximo três registros por relato mesmo com concorrência. Reservar a posição antes de concluir o envio para evitar arquivos excedentes e limpar uploads que falharem.

Guardar o caminho do objeto, não uma URL assinada que irá expirar. Não armazenar o binário em uma coluna do PostgreSQL. Proposta de caminho: `relatos/{relato_uuid}/{foto_uuid}.jpg`, usando os identificadores públicos e sem nome, e-mail ou ID interno do autor.

## 6. Evolução prevista — V2

### 6.1 `app.confirmacoes_relato`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `id` | `bigserial` | Sim | PK interna autoincrementável. |
| `uuid` | `uuid` | Sim | Identificador público, único, gerado por `gen_random_uuid()`. |
| `relato_id` | `bigint` | Sim | FK para `relatos(id)`. |
| `usuario_id` | `bigint` | Sim | FK para `usuarios(id)`. |
| `criado_em` | `timestamptz` | Sim | Momento da confirmação. |

A restrição `UNIQUE(relato_id, usuario_id)` impede confirmações duplicadas (RF17). Uma transação/função deve conferir que o usuário não é o autor (RF16), aplicar o limite de 30 confirmações por hora (RNF22), inserir e atualizar o estado do relato após três confirmadores distintos (RN09).

A comparação com o autor exige leitura de outra tabela; não usar um `CHECK` comum como se ele resolvesse essa regra entre tabelas. Serializar a decisão por relato para evitar erros quando confirmações chegam ao mesmo tempo.

Antes da V2, decidir o efeito da exclusão de um confirmador: a confirmação é removida? O relato confirmado continua confirmado? Qual observação derivada permanece após descarte do relato? Esses comportamentos não estão definidos nas RN atuais. Esta tabela não precisa ser criada para entregar a V1.

## 7. Operações e integridade

| Operação | Tratamento proposto |
|---|---|
| Cadastrar conta | Criar identidade via Auth e cadastro próprio; tratar falhas parciais e tentativas repetidas. |
| Consultar rota | Buscar evidências relevantes, aplicar a matriz do perfil e a prioridade das fontes. Não persistir a rota nesta proposta. |
| Enviar relato | Validar identidade, descrição, coordenadas e cota; inserir como não validado. |
| Solicitar desvio opcional | Considerar somente os relatos selecionados naquela consulta; não alterar a classificação global por essa ação. |
| Enviar foto | Validar conteúdo e tamanho, retirar EXIF desnecessário, reservar posição e enviar via API do Storage. |
| Excluir conta | Bloquear operações, remover dados pessoais e autoria, definir prazo dos relatos e remover identidade. Coordenar falhas entre os serviços. |
| Descartar relato | Remover arquivos pelo Storage e, após sucesso, metadados e relato no PostgreSQL. Repetir com segurança em caso de falha. |

Usar FKs e `RESTRICT` para evitar remoção acidental de catálogos e locais ainda referenciados. Pode haver cascata entre relato e seus metadados de fotos no descarte final, mas uma cascata SQL nunca remove o arquivo correspondente no Storage. Restringir a permissão de exclusão de relatos ao fluxo que já concluiu a remoção dos objetos.

PostgreSQL, Auth e Storage não formam uma única transação da aplicação. Registrar progresso mínimo para operações incompletas e executar reconciliação. O formato desse controle operacional deve ser definido na implementação; eliminar identificadores e dados temporários ao concluir o trabalho.

## 8. Fotos, exclusão de conta e retenção

### 8.1 Bucket proposto

Nome sugerido: `relato-fotos`. Visibilidade proposta: **privada**, com leitura autorizada pela API e URLs temporárias ou resposta intermediada. O uso sem conta exige que a API tenha um caminho de leitura pública controlada para fotos de relatos exibíveis, sem expor o bucket inteiro.

Aceitar JPG/JPEG/PNG, até 5 MB por arquivo (RNF20). JPEG e JPG usam o mesmo MIME `image/jpeg`. A equipe deve definir se 5 MB significa 5.000.000 ou 5.242.880 bytes e aplicar a mesma convenção em todas as camadas. A extensão do nome do arquivo não basta para validar seu conteúdo.

Proposta: o backend envia os arquivos após validá-los, usando credencial de servidor. Confirmar no protótipo a propriedade dos objetos: o Supabase impede a exclusão de um usuário Auth que ainda seja proprietário de objetos Storage. Objetos criados com service key não recebem automaticamente a propriedade do usuário final. A proteção continua dependendo das permissões e da autorização, não da ausência de proprietário.

### 8.2 Exclusão e descarte

1. Validar a solicitação e impedir novas operações da conta durante a exclusão.
2. Definir um único marco de exclusão. Nos relatos, preencher `autor_excluido_em` e `descartar_em` e remover `autor_id` de forma consistente.
3. Remover cadastro, preferências e identidade Auth; invalidar o acesso na API. Confirmar que não restaram identificadores pessoais nas tarefas temporárias.
4. Até o prazo de retenção, exibir relatos como enviados por "usuário excluído". Não mostrar registros com prazo vencido, mesmo se a limpeza física estiver atrasada.
5. No descarte, remover cada arquivo pela API do Storage. Se o arquivo já não existir, tratar como remoção concluída; em outras falhas, manter referências para nova tentativa.
6. Após remover os arquivos, excluir metadados e relato em transação PostgreSQL. Monitorar pendências de descarte.

Remover a FK de autoria não garante, sozinho, anonimização: descrição e fotos podem conter nomes, rostos ou outras informações identificáveis. Definir o tratamento desse conteúdo antes de afirmar que a RN16 está integralmente atendida.

**Decisão pendente de prazo:** a RN16 estabelece "até um mês", enquanto a stack prevê rotina diária. Uma limpeza somente depois do vencimento pode ultrapassar esse teto em quase 24 horas, além de falhas. Definir o significado de mês e uma programação que respeite o prazo, ou revisar expressamente a tolerância na RN. Ocultar dados vencidos não substitui sua exclusão física.

## 9. Segurança e dados sensíveis

### 9.1 Permissões

| Acesso | Permissão proposta |
|---|---|
| Visitante no aplicativo | Consultar mapa, rotas, perfis e dados exibíveis por endpoints da API. Sem acesso direto às tabelas. |
| Usuário autenticado | Operações do próprio cadastro e envio de relatos/fotos; confirmações apenas na V2. |
| API Java | Papel PostgreSQL com privilégios mínimos no schema `app`; autorização por usuário no servidor. |
| Rotina de descarte | Privilégios limitados à limpeza necessária e acesso ao Storage mantido somente no servidor. |
| Responsável por migrations | Papel administrativo separado da aplicação em execução. |

Se tabelas forem expostas via Data API, aplicar grants mínimos e RLS a cada tabela. RLS não protege chamadas que usam privilégios de bypass. Chaves `service_role`/secret não devem chegar ao Flutter, e chamadas privilegiadas precisam de autorização explícita na API.

Na conexão JDBC com papel compartilhado, o contexto do usuário Auth não é propagado automaticamente para `auth.uid()`. Não presumir que políticas com essa função protegerão consultas JDBC sem uma estratégia de contexto e privilégios validada.

### 9.2 Criptografia: decisão necessária antes do esquema físico

O RNF13 exige que dados classificados como sensíveis não apareçam legíveis numa consulta direta aos registros. Criptografia de disco, isoladamente, não produz esse resultado. Definir um inventário de campos e a criptografia na aplicação, com chaves separadas conforme RNF14.

| Grupo de dados | Encaminhamento para validação |
|---|---|
| Senha | Delegar o hash ao Auth; nunca criar coluna de senha no domínio. |
| E-mail de login | Gerenciado pelo Auth. Validar compatibilidade com o escopo do RNF13, sem alterar tabelas internas para contornar a autenticação. |
| Nome, sobrenome e preferência vinculada à conta | Classificar e decidir proteção física. Se criptografados, usar representação própria de ciphertext e versão da chave. |
| Descrição, fotos e localização do problema | Definir o que é conteúdo compartilhável e o que pode revelar informação privada. O bucket privado não equivale a criptografia de campo. |
| Origem/destino e localização corrente | Processar para a consulta e evitar persistência nesta V1, especialmente sem conta. Não incluir em logs. |

Se `perfil_mobilidade_id` for armazenado criptografado, a FK convencional sobre seu valor não será viável; a API deverá validar a referência após decifrá-la, ou o desenho deverá ser revisto. Coordenadas criptografadas também não admitem as mesmas consultas espaciais diretamente. Resolver essas escolhas antes de converter este modelo lógico em SQL.

### 9.3 Sessões e limites

O RNF16 exige expiração em 15 minutos e recusa de tokens antigos após logout/exclusão. A validação local da assinatura e expiração do JWT não verifica, sozinha, revogação de sessão. Definir verificação de sessão ativa ou registro de revogação no backend; não guardar tokens completos nas tabelas de domínio. O requisito também exige novo login após expiração, portanto revisar renovação automática dos clientes Auth.

Para RNF18 (login), RNF21 (relatos) e RNF22 (confirmações), definir contadores/janelas com atualização atômica. A limitação de login por e-mail não deve ser presumida a partir dos limites padrão do Auth. Se for implementada na API, impedir que um caminho direto de autenticação contorne a regra. Esses controles operacionais exigem projeto próprio, embora não apareçam como entidades do domínio neste rascunho.

## 10. Índices, migrations e operação

| Índice proposto | Finalidade |
|---|---|
| PKs numéricas e unicidade dos UUIDs de todas as tabelas próprias | Identidade interna eficiente e identificadores públicos não sequenciais. |
| Unicidades dos catálogos, da matriz e das confirmações | Consultas por código e prevenção de duplicidade lógica. |
| GiST em `locais.geometria` e `relatos.localizacao` | Filtrar locais e relatos próximos da rota ou do ponto consultado. |
| B-tree em `observacoes_acessibilidade(local_id, tipo_barreira_id)` | Buscar evidências e conflitos de um local. |
| B-tree em `relatos(autor_id, criado_em)` | Consulta de autoria e apoio ao limite de envios. |
| Índice parcial em `relatos(descartar_em)` para valores não nulos | Buscar registros com prazo de descarte. |
| Índices de FKs não cobertos por PK/UNIQUE | Apoiar joins e verificações de integridade. |

Medir consultas reais com `EXPLAIN ANALYZE` em ambiente de testes. `geometry` em SRID 4326 usa graus; para distâncias em metros, usar `geography` ou projeção adequada e garantir que o índice corresponda à expressão consultada. Índices não garantem sozinhos o RNF03, que inclui o provedor de rotas e a rede.

Versionar migrations e cargas iniciais no Git; escolher uma única ferramenta responsável pela evolução do schema (por exemplo, Flyway ou Supabase CLI). Não depender de mudanças manuais não registradas no painel. Manter ambientes separados e dados de teste fictícios.

Definir responsáveis, frequência e retenção de backups e um teste de restauração. Backup do PostgreSQL não inclui os arquivos do Storage: é necessário planejar os dois conjuntos e sua consistência. A disponibilidade de backup/PITR depende do plano contratado; não presumir esse recurso no plano gratuito. Uma restauração também deve reaplicar descartes já vencidos antes de reabrir a aplicação.

## 11. Rastreabilidade e validação

| Tema | Estruturas principais | Referências |
|---|---|---|
| Cadastro e preferência | Auth, usuários, perfis | RN15, RF08, RF10, RF11 |
| Classificação por perfil | Tipos e classificações | RN01–RN05, RF03, RF04 |
| Origem e atualização dos dados | Locais e observações | RN06, RN07, RF05 |
| Relatos sem validação | Relatos e associação espacial | RN08, RN10–RN12, RF07, RF09, RF14 |
| Fotos | Relato_fotos e Storage | RF18, RNF20 |
| Exclusão e retenção | Usuários, relatos, fotos e rotina | RN16, RF12 |
| Uso sem conta | Ausência de histórico persistido | RN17, RN18, RF13 |
| Confirmações | Confirmações_relato, somente V2 | RN09, RF15–RF17, RNF22 |
| Proteção de dados e acesso | Papéis, API, TLS e criptografia | RNF08–RNF19, conforme aplicabilidade |

Testes de aceite da modelagem:

- [ ] Rejeitar relato sem descrição, localização ou identidade autenticada.
- [ ] Exigir classificação para todas as combinações ativas de perfil e barreira.
- [ ] Distinguir dado ausente, dado desatualizado e barreira comprovadamente ausente.
- [ ] Impedir que um relato não validado altere a rota padrão da V1.
- [ ] Recusar uma quarta foto, inclusive em uploads concorrentes.
- [ ] Excluir a conta sem apagar antecipadamente os relatos e sem manter vínculo pessoal indevido.
- [ ] Concluir descarte após falhas/repetições sem perder a referência de arquivos ainda existentes.
- [ ] Recusar operações protegidas após logout ou exclusão, mesmo com JWT ainda não expirado.
- [ ] Confirmar ausência de persistência da rota/perfil de visitantes, inclusive em logs e caches.
- [ ] Na V2, impedir autoconfirmação e duplicidade e validar a terceira confirmação sob concorrência.

## 12. Decisões pendentes para aprovação

1. Aprovar PostGIS e o processo de associação de pontos relatados aos trechos do provedor.
2. Definir os dados sensíveis e sua representação criptografada; compatibilizar Auth e consultas espaciais com RNF13.
3. Definir como lidar com fontes sem data de verificação e conflitos entre dados públicos e OSM, cuja precedência mútua não está definida.
4. Definir o significado de "um mês", prazo físico de descarte e tratamento de conteúdo identificável em relatos e fotos.
5. Definir limite de descrição, unidade de 5 MB e fluxo de uploads com falhas parciais.
6. Definir janela fixa ou móvel dos limites por hora e mecanismo de bloqueio de login por e-mail.
7. Validar sessões de 15 minutos, revogação e exigência de novo login com Supabase Auth.
8. Definir leitura das fotos sem conta, propriedade dos objetos e limpeza de uploads órfãos.
9. Antes da V2, definir exclusão de confirmadores e destino de observações derivadas de relatos descartados.
10. Definir ferramenta de migrations, backups, responsáveis e critérios de restauração.

Depois da aprovação, produzir as migrations e atualizar o documento com o esquema efetivamente implantado, sem mudar o status para "Implementado" antes da verificação.

## 13. Referências técnicas

Documentação oficial consultada em 19/10/2026:

- [Supabase — Gestão de usuários e exclusão](https://supabase.com/docs/guides/auth/managing-user-data)
- [Supabase — PostGIS](https://supabase.com/docs/guides/database/extensions/postgis)
- [Supabase — Proteção da Data API](https://supabase.com/docs/guides/api/securing-your-api)
- [Supabase — Controle de acesso ao Storage](https://supabase.com/docs/guides/storage/security/access-control)
- [Supabase — Propriedade dos objetos](https://supabase.com/docs/guides/storage/security/ownership)
- [Supabase — Schema do Storage](https://supabase.com/docs/guides/storage/schema/design)
- [Supabase — Encerramento de sessão](https://supabase.com/docs/guides/auth/signout)
- [Supabase — Sessões](https://supabase.com/docs/guides/auth/sessions)
- [Supabase — Backups](https://supabase.com/docs/guides/platform/backups)

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 19/10/2026 | Proposta inicial de documentação e modelo lógico; pendente de validação e comparação com o projeto Supabase real. |
| 0.2 | 20/10/2026 | Padronização das tabelas próprias com PK numérica autoincrementável e UUID público único e obrigatório. |
| 0.3 | 20/10/2026 | Remoção dos diagramas, simplificação do objetivo e ajuste do dicionário para iniciar pelas tabelas próprias da aplicação. |
| 0.4 | 20/10/2026 | Confirmação dos nomes das tabelas, do schema `app`, do acesso via JDBC, do uso de `bigserial` e da FK com o Supabase Auth. |
