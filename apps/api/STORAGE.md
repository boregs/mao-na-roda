# Integração Supabase Storage — tarefa #17

Status: implementação interna com testes simulados; homologação real pendente.

## Localização e origem

A branch local é `feat/17-integracao-supabase-storage`. Como os ZIPs não incluem
histórico Git, a pasta `apps/api/` foi importada de `feat/7-base-api-java` para o
checkout atual. Não houve merge das branches de planejamento ou implantação.
Ao abrir um PR, alinhar a base com a equipe para não duplicar a API já existente.
Os arquivos específicos da tarefa estão em `src/main/java/com/maonaroda/api/storage`,
`src/test/java/com/maonaroda/api/storage`, neste documento, no exemplo de ambiente
e nas propriedades `supabase.storage.*`.

## O que foi implementado

Componente Spring `SupabaseStorage`, sem endpoints públicos, com três operações:

- `enviar(UUID relato, byte[] imagem)`: valida e recodifica JPEG/PNG, envia sem
  sobrescrever e retorna bucket, caminho, MIME e tamanho do arquivo final.
- `linkTemporario(String caminho, int segundos)`: solicita URL assinada por 1 a
  3600 segundos. A URL é um segredo temporário; não registrar em logs.
- `excluir(String caminho)`: remove o objeto exato pela API do Storage. Falhas
  preservam a possibilidade de repetição pelo chamador; nenhum metadado é apagado aqui.

O serviço limita entrada e saída a 5.000.000 bytes e imagem a 20 milhões de pixels.
Essas convenções são propostas e precisam ser alinhadas com a equipe. O formato é
identificado pelo conteúdo, não pela extensão fornecida pelo usuário. A recodificação
remove metadados de entrada, mas pode alterar orientação/qualidade; não anonimiza
rostos, placas ou texto visível na foto. Os nomes usam UUIDs sem dados pessoais:
`relatos/{relato_uuid}/{foto_uuid}.jpg` ou `.png`.

A URL de configuração deve ser a origem HTTPS do projeto, sem `/storage/v1`.
HTTP é aceito apenas em `127.0.0.1` para os testes locais. Não há redirects nem
retries automáticos. Timeout padrão de conexão e operação: cinco segundos.
A ausência de configuração não impede construir o componente, mas qualquer operação
falha explicitamente. Isso não remove a necessidade de configurar o banco da API.

## Executar os testes sem Supabase

Requer JDK 25 ou compatível com a base, com `JAVA_HOME` configurado. Na pasta `apps/api`:

```powershell
.\mvnw.cmd "-Dtest=SupabaseStorageTests" test
```

Os testes iniciam um servidor HTTP em loopback com chave fictícia. Não usam banco,
Google ou Supabase e não criam arquivos em nuvem. Verificam upload JPEG/PNG,
headers e caminhos, assinatura, exclusão, entradas inválidas, respostas malformadas,
erros remotos, falta de configuração, timeout e falha de conexão.

## Verificação executada

Em 02/11/2026, com JDK 25, o comando de testes acima compilou a API e executou
10 testes de Storage, todos aprovados (zero falhas, erros ou testes ignorados).
Não foi executada a suíte completa da API, que tem pendências anteriores descritas
no README da base. Nenhuma operação foi feita em um projeto Supabase real.

## Configuração real, quando houver acesso

1. Entrar no projeto de desenvolvimento do Supabase, em Storage.
2. Criar um bucket **privado** chamado `relato-fotos`; manter o bucket sem acesso
   público e configurar os limites de MIME (`image/jpeg`, `image/png`) e tamanho
   compatíveis com o serviço.
3. Copiar `.env.example` para `.env` somente se o arquivo local ainda não existir.
4. Preencher localmente, sem enviar os valores pelo chat ou versioná-los:

```dotenv
SUPABASE_URL=https://SEU_PROJETO.supabase.co
SUPABASE_SERVICE_ROLE_KEY=CHAVE_JWT_SERVICE_ROLE_DO_SERVIDOR
SUPABASE_STORAGE_BUCKET=relato-fotos
```

Esta implementação usa a chave legada JWT `service_role`, enviada em `apikey` e
`Authorization`. Não colocar uma chave `sb_secret_...` nesse campo como substituição
sem adaptar e validar o contrato. Não usar chave pública/anon como se fosse privilegiada.
A chave deve existir somente no servidor: ela ignora RLS e nunca pode chegar ao Android.
O bucket não é criado automaticamente pelo componente.

## Uso interno e responsabilidades do chamador

Após autorizar a operação sobre um relato real, o serviço de domínio pode chamar:

```java
var arquivo = storage.enviar(relatoUuid, bytesDaFoto);
// Persistir arquivo.bucket(), caminho(), mimeType() e tamanhoBytes().
// Guardar caminho estável, nunca a URL temporária.
var url = storage.linkTemporario(arquivo.caminho(), 300);
// Na rotina autorizada de descarte:
storage.excluir(arquivo.caminho());
// Só então remover os metadados correspondentes do banco.
```

O chamador deve buscar o caminho persistido e autorizar o acesso antes de assinar ou
excluir. Não aceitar um caminho arbitrário vindo do Android como prova de propriedade.
Não há controller de teste aberto que permita usar a chave privilegiada sem autorização.

## Pendências para concluir o fluxo do produto

- Integrar autenticação/autorização e o vínculo com relatos reais.
- Persistir metadados em `app.relato_fotos`; reservar posição de 1 a 3 por relato
  em operação concorrente segura. Este adaptador isolado não impõe a cota de três fotos.
- Tratar upload bem-sucedido seguido de falha no banco, com compensação ou limpeza
  rastreável de arquivos órfãos; não perder a referência de objetos cuja limpeza falhar.
- Integrar descarte à rotina de retenção, sem apagar metadados antes do arquivo.
- Definir leitura de fotos por visitantes e elegibilidade do relato para publicação.
- Configurar limite de requisição na camada HTTP, sem carregar uploads ilimitados em memória.
- Homologar no ambiente de desenvolvimento: enviar foto, salvar referência, abrir link,
  verificar expiração, excluir, conferir bucket privado e repetir cenários de falha.
- Não marcar a issue como integração real concluída antes dessa homologação e do acordo
  dos critérios de aceite com a equipe.

## Referências

- [Supabase — controle de acesso](https://supabase.com/docs/guides/storage/security/access-control)
- [Cliente oficial — contratos REST de arquivos](https://github.com/supabase/storage-js/blob/master/src/packages/StorageFileApi.ts)
- [Banco e fotos do projeto](../../docs/06-banco-de-dados.md)

Consultadas em 02/11/2026. A implementação não altera regras de negócio aprovadas.
