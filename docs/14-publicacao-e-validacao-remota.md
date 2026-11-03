# Publicação da branch e validação remota

Data: 02/11/2026. Repositório: [boregs/mao-na-roda](https://github.com/boregs/mao-na-roda).
Branch: `feat/planejamento-rotas-visitante`.
PR: [#22, em rascunho](https://github.com/boregs/mao-na-roda/pull/22).

## Publicação

Enviados os commits locais até `283be9c`. A branch agora acompanha
`origin/feat/planejamento-rotas-visitante`. A main não foi alterada e nenhum deploy
foi executado. O PR explica a inclusão da base AWS `5189a2a`, ainda ausente da main,
além dos oito commits novos da implementação. A integração exige revisão humana.

O conector GitHub não tinha permissão para criar o PR (HTTP 403). Foi usada a
autenticação local do Git no mesmo repositório, sem exibir ou salvar credenciais.
Nenhum pedido de revisão ou comentário foi enviado a outras pessoas.

## Primeira execução de CI

- [API/PostGIS](https://github.com/boregs/mao-na-roda/actions/runs/41382991494):
  verificação concluída com sucesso; preparação PostGIS, migrations, integridade,
  testes e empacotamento aprovados. Os 52 testes passaram sem falhas e sem casos
  ignorados, incluindo os três testes PostGIS anteriormente pendentes.
  Deploy ignorado, como esperado em PR.
- [Android](https://github.com/boregs/mao-na-roda/actions/runs/41382991462): falha
  antes do build. `setup-android@v3` tentou instalar o pacote padrão `tools`, que
  não foi encontrado no repositório atual do SDK.

Correção: listar explicitamente `platform-tools`, `platforms;android-35` e
`build-tools;35.0.0`, alinhados ao compileSdk 35 do projeto, substituindo o pacote
obsoleto. As licenças deixam de produzir páginas de texto no log. O build, lint e
testes continuam obrigatórios; nenhuma verificação foi desativada.

Referência da action: [configuração de packages](https://github.com/android-actions/setup-android/tree/v3#additional-packages).
Resultado da reexecução: aguardando conclusão após enviar a correção.

## Próximas prioridades

1. Confirmar o CI Android após a correção e revisar o PR com outra pessoa.
2. Configurar ambiente HTTPS e chaves ORS/geocoder/Maps restritas; testar a jornada
   completa em dispositivo, com os três perfis e origem manual.
3. Escolher a área, preencher os 30 pares e revisar evidências em campo. O vínculo
   atual é por geometria exata revisada; não associa novas geometrias por topologia.
4. Validar TalkBack, texto ampliado, temas, rotação, teclado, permissões, latência,
   custo e limites atrás do proxy.
5. Depois do piloto, completar identidade/cadastro, proteção das anotações locais,
   relatos georreferenciados, fotos, grupos/desvios, refinamento e ingestão OSM ampla.

O sucesso de CI com dados e provedores simulados não comprova a acessibilidade
das ruas nem substitui a homologação com credenciais reais e revisão em campo.
