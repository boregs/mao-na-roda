# Fontes do aplicativo

As fontes vêm dos arquivos variáveis incluídos em `app/src/main/res/font/`, originalmente usados na versão Flutter:

- Source Sans 3: peso regular 400 nos textos, campos e botões; 700 nos títulos.
- Source Serif 4: peso 600 no título de boas-vindas.

Os arquivos variáveis originais também são preservados em `app/src/main/res/font/`.
`tools/generate_fonts.py` gera as instâncias estáticas usadas pelo Android, evitando
o peso padrão ExtraLight do arquivo variável e garantindo suporte ao Android 7.
Para regenerar, instale `fonttools` no ambiente Python e execute o script.

Os tamanhos maiores solicitados para o Android são mantidos: textos e controles
20sp, textos secundários 18sp e rodapés 17sp. As alturas de linha são definidas
nos estilos compartilhados `MaoText.*`, com atributos compatíveis com Android 7
e atributos nativos adicionais a partir do Android 9. Isso evita que os títulos
herdem uma altura de linha menor do estilo de corpo do Material.

As licenças SIL Open Font License estão nos arquivos desta pasta.
