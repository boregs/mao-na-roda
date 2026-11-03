# Planejamento de rotas de visitante

## Contrato implementado

| Método e caminho | Acesso | Resultado |
|---|---|---|
| `GET /api/v1/perfis-mobilidade` | Público | Catálogo ativo do banco, com código e nome |
| `POST /api/v1/destinos/busca` | Público, limitado | HTTP 410: busca separada descontinuada |
| `POST /api/v1/rotas` | Público, limitado | Até três candidatas Google Routes avaliadas |
| `POST /api/v1/rotas/base` | Público, limitado | Referência Google existente, sem avaliação de acessibilidade |
| `/api/v1/usuarios/me` | JWT | Operações existentes de perfil; cadastro permanece 501 |

Planejamento recebe endereços ou coordenadas, inclusive um formato em cada ponto:

```json
{
  "origem": {"endereco": "Estação Carrão, São Paulo, SP, Brasil"},
  "destino": {"longitude": -46.601, "latitude": -23.501},
  "perfil": "cadeirante"
}
```

Cada ponto aceita `{"endereco":"..."}` (não vazio, até 500 caracteres na API;
200 no aplicativo) ou `{"longitude":-46.6,"latitude":-23.5}`. Não misturar os dois
formatos no mesmo ponto. O Routes resolve o endereço na própria consulta; não é
necessária outra API de geocodificação. Não há autocomplete nem escolha de resultado
de busca nesta versão. Use endereços completos e confira o acesso real no mapa.
Clientes da antiga busca ORS devem migrar para esse contrato; o endpoint antigo
responde 410 com essa orientação, sem consumir o provedor.

O Google recebe `WALK`, alternativas habilitadas e geometria de alta qualidade em
`GEO_JSON_LINESTRING`. A máscara solicita somente duração, distância e
`routes.polyline.geoJsonLinestring`. O Google não recebe o perfil de mobilidade;
o perfil é aplicado pela avaliação de evidências da própria API. As rotas a pé
do Google podem omitir caminhos/calçadas e não garantem acessibilidade. O aviso
é incluído no resultado global e nos detalhes de cada candidata.

Os três códigos são `cadeirante`, `muleta_ou_outro_auxilio` e
`mobilidade_reduzida`. Eles vêm da V4 Flyway. A branch antiga de catálogos usa
outros nomes para alguns registros; não executar seus scripts além do Flyway.
Não renomear migrations aplicadas nem fazer baseline automático de banco antigo.

A resposta separa `resultado` global (`CONCLUIDA`, `PARCIAL`, `SEM_CANDIDATA`),
versões de matriz/dataset e qualidade de cada rota (`DOCUMENTADA`, `PARCIAL`,
`DESCONHECIDA`). Interrupções/timeouts retornam Problem Details HTTP 503/504.
Cada candidata tem GeoJSON WGS84 `[longitude, latitude]`, distância, duração,
contagem de barreiras, cobertura e detalhes por trecho. O campo `id` é a assinatura
da geometria, sem armazenar a consulta do visitante.

Ranking: documentação completa, parcial e desconhecida; depois impedimentos,
atenção, cobertura decrescente, distância e duração. Candidata documentada com
impedimento pode aparecer primeiro e sempre exibe o impedimento. Nenhuma rota
recebe selo automático de acessível. Cobertura completa exige as dez condições
observadas e verificadas no último mês em toda a rota. Datas futuras/ausentes e
lacunas permanecem desconhecidas. Conhecimento parcial não equivale a segurança.

## Configuração segura

Copiar `.env.example` para `.env` local e configurar JDBC com o papel da API.
Em ambiente remoto, usar TLS com validação do certificado para JDBC.
Definir `GOOGLE_ROUTES_API_KEY`, uma chave de servidor restrita à Routes API e ao
IP público de saída do backend. Habilitar faturamento e Routes API no projeto
Google Cloud. A chave restrita ao Android/Maps SDK é outra e não deve ser reutilizada.
`GOOGLE_MAPS_API_KEY` continua aceito como nome legado somente se o novo nome estiver
ausente; não deixar a nova variável vazia se for usar apenas a antiga. ORS e Pelias
foram removidos da execução. Não há troca automática de provedor.

É possível testar sem publicar: o perfil `local` limita o backend a `127.0.0.1`;
o Android debug acessa `http://10.0.2.2:8080`. Veja o
[passo a passo e os pré-requisitos](../../docs/16-migracao-google-routes.md#teste-local-no-android-studio).

AUTH_ISSUER e AUTH_JWK_SET_URI exigem HTTPS. AUTH_AUDIENCE usa `authenticated`
por padrão. São validados assinatura, emissor, audiência e validade; tokens sem
expiração ou com duração superior a 15 minutos são recusados. Ajustar o emissor
para emitir tokens com esse prazo. Sem configuração, as operações públicas
funcionam e operações protegidas recusam tokens. A integração real com o provedor
de identidade e o cadastro não foram concluídos por esta entrega.

## Limites e privacidade

- Orçamento de planejamento de 8 segundos; consulta JDBC e aquisição de conexão
  têm limites próprios. Android encerra a chamada em 9 segundos.
- Uma chamada Google Routes por planejamento, sem retry interativo e sem refinamento automático nesta versão;
  até seis candidatas avaliadas/deduplicadas, até três exibidas.
- Até quatro planejamentos simultâneos por processo. Até 30 consultas públicas
  por minuto por endereço remoto, com memória limitada. Atrás de proxy, validar a
  política de IP no ambiente; não confiar indiscriminadamente em X-Forwarded-For.
  Múltiplas instâncias exigem quota/rate limit compartilhado na borda antes de
  homologar produção.
- Cache-Control no-store nas consultas. Não há persistência de rota/perfil de
  visitante nem logs próprios com origem/destino ou chaves.
- Sem dados reais de campo no código. Dataset ausente resulta em desconhecido,
  não em acessibilidade garantida. Ver [carga revisada do piloto](database/piloto/README.md).

## Verificações reproduzíveis

API com Java 25:

```powershell
cd apps/api
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Sem banco configurado para integração, os testes PostGIS são explicitamente
ignorados. Os demais usam H2 apenas para contexto/usuário e simuladores HTTP dos
provedores, sem custo externo. Isso não comprova PostGIS.

Com Docker instalado/iniciado, banco local descartável:

```powershell
.\tools\testar-postgis.ps1
```

O script inicia o Compose, prepara Auth simulado/PostGIS, executa V1–V6 e
integridade SQL, depois habilita os testes JDBC/HTTP PostGIS. Credenciais contidas
no Compose são exclusivamente do banco local descartável, na porta 55432.
O script preserva as variáveis de ambiente anteriores ao terminar. Não apontá-lo
para Supabase nem banco compartilhado. Parar com `docker compose -f compose.test.yml down`.

O CI repete esse caminho de migrations/integração antes de permitir deploy.
O teste de publicação verifica transação e preservação de versão em caso de erro;
o teste HTTP consulta os três perfis como visitante com Google simulado.

## Pendências de homologação

Chave e chamada real do Google Routes no novo fluxo, revisão dos 30 pares,
qualidade da associação em campo, latência/custo em rede real e TLS/identidade reais
ainda precisam de validação. Os testes no emulador usam fixtures, sem validar
trajetos reais. O vínculo por assinatura é um piloto conservador,
não um matcher espacial geral. Refinamento/exclusões, ingestão OSM ampla, relatos
remotos, grupos, upload de fotos e desvios permanecem etapas seguintes ao piloto.
Traçados do Google podem ter assinaturas diferentes dos usados com ORS: não
transferir evidências por proximidade. Revalidar o dataset piloto sobre o novo
traçado; assinaturas sem correspondência permanecem desconhecidas.
