# Publicação de evidências revisadas do piloto

Esta carga é administrativa e nunca ocorre durante uma consulta HTTP.
Não há dados de campo incluídos no repositório. Sem dataset publicado, as rotas
retornam cobertura zero e qualidade `DESCONHECIDA`.

## Preparação

1. Definir uma área pequena e preencher `docs/piloto/corpus.csv` com 30 pares reais.
   Executar cada par nos três perfis, registrando acertos, ambiguidades, custo e tempo.
2. Executar Flyway V1–V6 em banco de teste preparado conforme `database/README.md`.
3. Consultar `POST /api/v1/rotas` e preservar o `id` e a `geometria` exatos de cada
   candidata. O ID é SHA-256 de pares `Double.toString(longitude),Double.toString(latitude)`
   unidos por `;`, sem arredondar. A mudança de direção também muda o ID.
4. Revisar em campo calçada/lado, nível, travessia, ligação com entradas e as dez
   condições da matriz. Não usar presença de tag como prova de ausência de barreira.

## Formato do arquivo local

O documento JSON contém `versao` e uma lista `rotas`. Cada rota contém:

| Campo | Conteúdo |
|---|---|
| `assinatura` | `id` exato retornado pela consulta |
| `geometria` | Objeto GeoJSON LineString retornado pela consulta |
| `trechos` | Lista ordenada de intervalos revisados, sem sobreposição |

Cada trecho contém `inicio` e `fim` entre 0 e 1, `lado` descritivo, `nivel`
inteiro, `travessia` e `entrada` booleanos, `contextoRevisadoEm` em ISO-8601 com
fuso, e `evidencias`. Intervalos usam fração do comprimento geométrico WGS84,
conforme ST_LineSubstring; não representam fração da duração. Podem existir lacunas:
elas permanecem desconhecidas. A geometria do trecho é extraída no servidor.

As dez evidências usam os códigos de `V4__carregar_matriz_de_barreiras.sql`:
`escada_sem_alternativa`, `ausencia_rampa`, `rampa_inclinacao_inadequada`,
`calcada_estreita`, `calcada_danificada_irregular`, `obstaculo_calcada`,
`elevador_indisponivel`, `entrada_inadequada`, `banheiro_nao_acessivel` e
`ausencia_piso_tatil`.

Cada evidência contém:

```json
{
  "tipo": "escada_sem_alternativa",
  "condicao": "desconhecida",
  "fonte": "openstreetmap",
  "idFonte": "identificador original da fonte",
  "versaoFonte": "versão original",
  "lote": "identificador do lote",
  "dataOriginal": null,
  "verificadoEm": null,
  "motivoSemVerificacao": "A fonte não informa uma data real de verificação"
}
```

O exemplo descreve ausência de verificação, não uma observação de campo.
`condicao` aceita `presente`, `ausente` ou `desconhecida`; `fonte` aceita
`openstreetmap` ou `dados_publicos`. Data de importação não preenche verificação.
Datas de revisão/verificação futuras são recusadas. Data ausente ou antiga não
contribui para cobertura documentada. Evidências de relatos não entram nesta carga.

## Executar

Usar Java 25 e um login administrativo/migrador com permissão de escrita nas novas
tabelas. O login comum `maonaroda_api` tem somente SELECT nessas tabelas.
Definir as propriedades JDBC no ambiente seguro, sem registrar os valores em logs.

```powershell
cd apps/api
.\mvnw.cmd --batch-mode --no-transfer-progress verify
java -jar target/api-0.0.1-SNAPSHOT.jar --spring.main.web-application-type=none --piloto.arquivo=C:/dados/revisao-piloto.json
```

O importador valida assinatura, contexto e evidências. Publica tudo numa transação,
com lock administrativo e apenas um dataset ativo. Falha mantém o dataset anterior;
repetir uma versão já publicada falha, sem duplicar dados. Para corrigir conteúdo,
publicar nova versão; não editar versões históricas. O processo fecha o contexto
ao concluir. Nenhuma chave ORS/Google é necessária para importar o arquivo revisado.

## Limites e evolução

Este mecanismo é um vínculo manual de geometria exata, não um associador geral
por proximidade. Mudanças pequenas de geometria voltam a ser desconhecidas, o que
é preferível a herdar evidências da calçada errada. O corpus deve medir essa perda
de cobertura e decidir se o modelo espacial precisa evoluir antes de escalar OSM.

O dataset inteiro é lido numa única consulta MVCC com a matriz RN-v1. Essa matriz
é a versão das migrations atuais e não deve ser editada ao vivo; sua evolução
precisa de nova versão explícita de contrato/dataset. Apenas operadores revisam e
publicam evidências. A validação automática de relatos por três pessoas é V2.
