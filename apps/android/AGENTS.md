# Orientações para a interface Android

Estas orientações se aplicam ao aplicativo nativo em `apps/android/`. Preserve o trabalho existente e as regras em `../../docs/02-regras-de-negocio.md` e `../../docs/03-requisitos.md`.

## Identidade e componentes

- A interface usa Java, layouts XML, AndroidX e Material Components. Antes de alterar telas, inspecione os layouts e fragments relevantes, `app/src/main/res/values/styles.xml`, `values/colors.xml`, `values-night/colors.xml` e os componentes existentes.
- Use os tokens e estilos compartilhados para cores, texto, espaçamento e controles. Apoie-se na paleta azul-petróleo e cinza aprovada, nos temas claro e escuro e nas preferências atuais do usuário. Ajustes de identidade devem ser tratados como decisões do aplicativo inteiro.
- Preserve a hierarquia inspirada em mapas: mapa como conteúdo principal e painel inferior compacto para busca e conta, expansível para mobilidade e locais recentes. Não apresente mapas, busca, autenticação ou rotas como disponíveis antes de estarem implementados.
- Use Source Sans 3 para interface e Source Serif 4 para o título de boas-vindas. Os textos comuns e controles usam peso regular e tamanhos legíveis; preserve a entrelinha confortável dos títulos e o redimensionamento de texto do Android.
- Reutilize os ícones Material incluídos, com tratamento consistente e licenças preservadas. Ícones decorativos não devem gerar anúncios redundantes no TalkBack; controles precisam de rótulos significativos.

## Acessibilidade e comportamento

- Assegure contraste mínimo de 4,5:1 para texto normal e 3:1 para texto grande e elementos gráficos essenciais, considerando fundo real e ambos os temas.
- Use alvos de toque de pelo menos 48dp, ordem de leitura e foco lógica, rótulos TalkBack e alternativa acessível a gestos. Não dependa apenas de cor para transmitir estado.
- Respeite as barras do sistema e recortes, o teclado e a navegação de volta. Permita rolagem e crescimento do conteúdo em telas pequenas e com texto ampliado; nunca reduza a fonte para fazê-la caber.
- Toda interface deve usar português brasileiro e mostrar estados reais, incluindo carregamento, erros, indisponibilidade e ausência de dados. Para rotas e barreiras, diferencie condição, impacto, fonte, data e validação; não apresente relato não validado como verificado.
- No onboarding, mantenha a entrada sem conta, a criação de conta, o acesso à conta, a explicação e a preferência de conclusão. Peça localização apenas quando a pessoa escolher uma ação que precise dela.

## Revisão e verificação

- Para novas telas ou mudanças visuais significativas, examine referências relevantes, incluindo as fornecidas pelo usuário; adapte padrões sem copiar marcas ou conteúdo. Avalie hierarquia, estados, legibilidade, acessibilidade e consistência com as regras do produto.
- Execute a compilação e o lint Android e os testes relevantes à mudança. Avalie tema claro e escuro, tamanhos de tela menores, texto ampliado, navegação e teclado quando possível; teste no dispositivo ou emulador disponível.
- Relate limitações e verificações que não puderam ser executadas. Não afirme que uma tela foi verificada visualmente apenas por inspeção do código.
