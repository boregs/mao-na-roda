# Guia de Contribuição – Mão na Roda

> **Versão do documento:** 0.1 (inicial, em revisão)
> **Data:** 06/10/2026
> **Última atualização:** 06/10/2026

Este guia define como a equipe trabalha no repositório: como escrever mensagens de commit, como nomear branches e como abrir Pull Requests (PRs). O objetivo é manter o histórico legível e a revisão de código rápida.

**Idioma:** todas as mensagens de commit, títulos e descrições de PR são escritos em **português**.

---

## 1. Mensagens de commit

Usamos o padrão **Conventional Commits**, adaptado para português.

### 1.1 Formato

```
tipo(escopo): descrição curta

corpo opcional, explicando o que mudou e por quê

rodapé opcional (referências, avisos)
```

- **tipo:** obrigatório. Diz que tipo de mudança é.
- **escopo:** opcional, entre parênteses. Indica a parte do sistema afetada.
- **descrição curta:** obrigatória. Resume a mudança em uma linha.
- **corpo:** opcional. Use quando só a descrição não explicar o motivo da mudança.
- **rodapé:** opcional. Use para referenciar requisitos, regras ou issues.

### 1.2 Tipos (prefixos) e quando usar

| Tipo | Significado | Quando usar | Exemplo |
|---|---|---|---|
| `feat` | Nova funcionalidade | Quando você adiciona algo novo que o usuário ou outro módulo passa a poder usar. | `feat(rotas): exibe resumo de barreiras na rota acessível` |
| `fix` | Correção de bug | Quando algo funcionava errado e você corrige. | `fix(mapa): corrige marcador fora da posição do usuário` |
| `docs` | Documentação | Mudanças só em documentos (README, `docs/`, comentários de documentação). Nenhum código muda. | `docs: adiciona regras de negócio` |
| `style` | Formatação | Mudanças que não alteram o comportamento: espaços, indentação, ponto e vírgula, ordem de imports. | `style(relatos): ajusta indentação do formulário` |
| `refactor` | Refatoração | Reorganiza o código sem mudar o que ele faz e sem corrigir bug nem criar funcionalidade. | `refactor(rotas): extrai cálculo de distância para função própria` |
| `perf` | Desempenho | Mudança feita especificamente para deixar algo mais rápido ou consumir menos recursos. | `perf(mapa): reduz chamadas à API ao mover o mapa` |
| `test` | Testes | Adiciona ou ajusta testes, sem mudar código de produção. | `test(rotas): adiciona testes do cálculo de rota acessível` |
| `build` | Build e dependências | Mudanças no processo de build ou nas dependências (gerenciador de pacotes, configuração de empacotamento). | `build: atualiza biblioteca de mapas para a versão 2.1` |
| `ci` | Integração contínua | Mudanças em pipelines e automações (GitHub Actions, por exemplo). | `ci: adiciona execução de testes no pull request` |
| `chore` | Tarefas gerais | Manutenção que não se encaixa nos outros tipos e não afeta o código de produção (ajustes de configuração, `.gitignore`). | `chore: adiciona .gitignore para arquivos de IDE` |
| `revert` | Reversão | Desfaz um commit anterior. Cite o commit revertido no corpo. | `revert: desfaz "feat(rotas): exibe resumo de barreiras"` |

**Como decidir o tipo:** pergunte-se "o que o commit faz?".
- Adiciona algo novo para o usuário? → `feat`
- Conserta algo quebrado? → `fix`
- Só mexe em texto/documento? → `docs`
- Só reorganiza o código, sem mudar o comportamento? → `refactor`
- Não sabe? Provavelmente é `chore`, mas revise se não cabe em outro tipo.

### 1.3 Escopo

O escopo é opcional, mas ajuda a localizar a mudança. Escopos sugeridos para este projeto:

| Escopo | Área |
|---|---|
| `mapa` | Mapa e localização |
| `rotas` | Cálculo e exibição de rotas |
| `perfil` | Preferências de rota e perfis de mobilidade |
| `relatos` | Relato de problemas |
| `api` | Back-end e serviços |
| `ui` | Interface e componentes visuais |
| `docs` | Documentação (use junto com `docs` só se precisar detalhar) |

A equipe pode adicionar novos escopos conforme o projeto crescer. Se a mudança afeta várias áreas, omita o escopo.

### 1.4 Regras para a descrição

- Escreva em **português**, em letras minúsculas (exceto nomes próprios e siglas).
- Use o **verbo na 3ª pessoa do presente**, como se completasse a frase "este commit...": `adiciona`, `corrige`, `remove`, `atualiza`.
- Máximo de **72 caracteres** na primeira linha.
- **Sem ponto final.**
- Seja específico: diga **o que** mudou, não o que você fez ("ajustes" não diz nada).

| Ruim | Bom |
|---|---|
| `fix: ajustes` | `fix(mapa): corrige marcador fora da posição do usuário` |
| `feat: Adicionei a tela de relatos.` | `feat(relatos): adiciona tela de relato de problemas` |
| `update` | `chore: atualiza dependências do projeto` |
| `feat(rotas): várias mudanças nas rotas e no mapa e nos relatos` | Divida em commits separados |

### 1.5 Corpo e rodapé

Use o **corpo** quando o motivo da mudança não for óbvio. Separe do título por uma linha em branco e explique o *porquê*, não o *como* (o código já mostra o como).

Use o **rodapé** para referenciar a documentação do projeto:

```
feat(rotas): evita trechos com escada no perfil cadeirante

A rota passava por escadas mesmo quando o usuário escolhia o perfil
de cadeira de rodas. Agora trechos com escada são descartados no cálculo.

Refs: RF04, RN02
```

Para mudanças que quebram compatibilidade (por exemplo, alteram a API), adicione `!` após o tipo e explique no rodapé:

```
feat(api)!: renomeia campo "barreiras" para "obstaculos" na resposta de rotas

BREAKING CHANGE: clientes que usam o campo "barreiras" precisam atualizar.
```

### 1.6 Boas práticas

- **Um commit, uma mudança.** Se o commit precisa de "e" na descrição, provavelmente são dois commits.
- Faça commits **pequenos e frequentes**, que deixem o projeto funcionando.
- Não envie commits com código quebrado na `main`.
- Nunca inclua senhas, chaves de API ou dados pessoais no repositório.

---

## 2. Branches

Fluxo simples, adequado a uma equipe pequena: uma branch principal e branches curtas para cada tarefa.

| Branch | Uso |
|---|---|
| `main` | Código estável. Só recebe alterações via PR aprovado. **Não se faz commit direto.** |
| `tipo/nome-curto` | Branch de trabalho, criada a partir da `main`, uma por tarefa. |

### 2.1 Nome das branches

Formato: `tipo/descricao-curta-em-kebab-case`, usando os mesmos tipos dos commits.

```
feat/resumo-barreiras-rota
fix/marcador-fora-posicao
docs/regras-de-negocio
refactor/calculo-distancia
```

- Use letras minúsculas, sem acentos e com hífens.
- O nome deve dizer o que a branch faz, de forma curta.
- Se houver issue, pode incluir o número: `feat/12-resumo-barreiras-rota`.

### 2.2 Fluxo de trabalho

1. Atualize a `main`: `git checkout main && git pull`
2. Crie sua branch: `git checkout -b feat/resumo-barreiras-rota`
3. Faça commits seguindo o padrão da seção 1.
4. Envie a branch: `git push -u origin feat/resumo-barreiras-rota`
5. Abra um Pull Request para a `main`.
6. Após aprovação e merge, **apague a branch**.

Mantenha as branches **curtas** (idealmente alguns dias). Branches longas acumulam conflitos.

---

## 3. Pull Requests

### 3.1 Regras

- O **título** segue o mesmo padrão dos commits: `tipo(escopo): descrição`.
- Cada PR trata de **um assunto**. PRs muito grandes são difíceis de revisar.
- É necessária **pelo menos 1 aprovação** de outra pessoa da equipe antes do merge.
- Quem abriu o PR **não aprova o próprio PR**.
- Resolva todos os comentários da revisão antes do merge.
- Prefira **Squash and merge** para manter a `main` limpa, com um commit por PR. O título do PR vira a mensagem do commit, por isso ele deve seguir o padrão.
- Referencie os requisitos e regras da documentação (RF, RN, UC) que o PR implementa.

### 3.2 Template de PR

> Para o GitHub preencher este modelo automaticamente em todo PR, salve o conteúdo abaixo em `.github/pull_request_template.md`.

```markdown
## Descrição
<!-- O que este PR faz? Explique de forma breve. -->


## Motivação
<!-- Por que esta mudança é necessária? Qual problema resolve? -->


## Tipo de mudança
- [ ] `feat`: nova funcionalidade
- [ ] `fix`: correção de bug
- [ ] `docs`: documentação
- [ ] `refactor`: refatoração
- [ ] `test`: testes
- [ ] `chore` / `build` / `ci`: manutenção

## Documentação relacionada
<!-- Cite os requisitos, regras ou casos de uso. Ex.: RF04, RN02, UC03 -->


## Como testar
<!-- Passo a passo para quem for revisar reproduzir e validar. -->
1.
2.
3.

## Capturas de tela (se houver mudança visual)
<!-- Cole imagens ou prints das telas alteradas. -->


## Checklist
- [ ] O título do PR segue o padrão `tipo(escopo): descrição`
- [ ] Testei as mudanças localmente
- [ ] O código segue os padrões do projeto
- [ ] Atualizei a documentação, se necessário
- [ ] Não há senhas, chaves de API ou dados sensíveis no código
```

---

## 4. Pendências deste documento

- [ ] Revisão do grupo (versão 0.1)
- [ ] Confirmar se a equipe quer exigir mais de 1 aprovação por PR
- [ ] Definir o padrão de testes, caso o projeto adote integração contínua
- [x] Salvar o template de PR em `.github/pull_request_template.md`
- [ ] Atualizar versão e data ao aprovar o documento

---

## Histórico de versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 06/10/2026 | Versão inicial |
