# Corpus de validação em campo

`corpus.csv` é um roteiro de 30 pares, não um dataset revisado. Coordenadas e datas
permanecem vazias até a escolha da área e a revisão humana. Nenhuma linha é
importada automaticamente ou apresentada no aplicativo como condição real.

Para cada par, executar os três perfis da matriz, com alternativas quando houver.
Preencher origem/destino em WGS84, descrição do cenário, fonte, data de revisão,
resultado esperado e observado. Guardar geometrias completas e evidências em
arquivo local protegido; só publicar a versão revisada pelo importador do piloto.

Reservar os pares 25–30 para validação final, sem ajustar o associador a partir
deles. Medir associações erradas e cobertura desconhecida separadamente. Registrar
tempo total, primeira consulta/cache, rede e quantidade de chamadas externas.

Critério para avançar: nenhum caso crítico pode herdar evidência do caminho errado.
Não atingir cobertura suficiente exige reduzir a área ou rever o modelo, não
relaxar a correspondência por distância sem validação.
