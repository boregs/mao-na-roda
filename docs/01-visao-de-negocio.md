# Visão de Negócio – Mão na Roda

> **Projeto:** Mão na Roda – Acessibilidade para Todos
> **Versão do documento:** 1.0 | **Data:** [06/10/2026]
> **Documentos relacionados:** [Regras de negócio](02-regras-de-negocio.md) · [Requisitos](03-requisitos.md) · [Casos de uso](04-casos-de-uso.md) · [Tech stack](05-tech-stack.md)

---

## 1. Resumo

O **Mão na Roda** é um aplicativo móvel que ajuda pessoas com deficiência de mobilidade a se locomoverem pela cidade de São Paulo de forma segura e acessível, indicando rotas de ruas mais adequadas às suas necessidades.

A pergunta que guia o projeto é: **"por onde essa pessoa consegue realmente passar?"**

---

## 2. Contexto e problema

Aplicativos de mapas indicam rotas rápidas e eficientes, mas nem sempre consideram se o caminho é realmente acessível. Em São Paulo, muitas ruas e calçadas são mal conservadas ou mal projetadas, o que impede pessoas com mobilidade reduzida de se locomoverem livremente. O problema é mais evidente nas áreas de periferia.

Pessoas com outras deficiências, como as cegas, também enfrentam grande dificuldade para se deslocar, **independentemente da região**, seja na periferia ou no centro da cidade.

**Barreiras mais comuns:**
- Escadas e ausência de rampas, ou rampas com inclinação inadequada
- Calçadas estreitas, danificadas ou com obstáculos
- Elevadores indisponíveis
- Ausência de piso tátil
- Entradas inadequadas e banheiros não acessíveis em estabelecimentos

**Dados de apoio:** [inserir estatísticas e fontes, como IBGE sobre pessoas com deficiência e dados sobre calçadas em São Paulo]

---

## 3. Proposta de valor

Um aplicativo que coloca a **acessibilidade como critério central** na escolha da rota, em vez de apenas tempo ou distância. O usuário conhece de antemão as possíveis barreiras do trajeto e decide com mais autonomia e segurança.

---

## 4. Objetivos

**Objetivo geral:** oferecer rotas acessíveis que facilitem a locomoção de pessoas com deficiência pela cidade.

**Objetivos específicos:**
- Indicar rotas de ruas mais acessíveis para pessoas com deficiência de mobilidade (V1).
- Identificar e mostrar barreiras presentes no percurso e em estabelecimentos.
- Ampliar o acesso à informação sobre as condições de acessibilidade dos espaços urbanos.
- Evoluir o produto para atender pessoas com deficiência visual, com auxílio por voz (V2).

---

## 5. Público-alvo

**Público principal (V1):** pessoas com deficiência de mobilidade, como:
- Cadeirantes
- Pessoas que sofreram algum acidente e têm a mobilidade afetada
- Pessoas que usam muletas ou outro equipamento de auxílio à locomoção

**Público futuro (V2):** pessoas com deficiência visual, atendidas por orientação por voz.

---

## 6. Escopo

### 6.1 V1 – Escopo inicial

Aplicativo que **sugere rotas acessíveis para pessoas com deficiência de mobilidade**, incluindo:
- Mapa e localização do usuário
- Busca de destino e ações (começar, ver rotas, relatar)
- Preferências de rota por perfil de mobilidade
- Rota acessível com distância, tempo e resumo das barreiras (rampas, escadas, travessias)
- Relato de problemas encontrados no percurso

### 6.2 V2 – Evolução prevista

- Navegação com **auxílio por voz** para pessoas sem visão
- Perfis adicionais de acessibilidade (visual, auditiva) [confirmar]
- Atualização colaborativa das condições dos locais
- Filtros personalizados de acessibilidade
- Aprimoramento do cálculo das rotas

### 6.3 Fora do escopo da V1

- Navegação guiada por voz
- [outros itens a definir pelo grupo]

---

## 7. Riscos

> Riscos **sugeridos como ponto de partida**. O grupo deve revisar, ajustar probabilidade e impacto e acrescentar os que fizerem sentido.

| # | Risco | Prob. | Impacto | Mitigação sugerida |
|---|---|---|---|---|
| R1 | **Falta de dados confiáveis de acessibilidade**, principalmente na periferia, onde o problema é maior | Alta | Alto | Combinar dados abertos (prefeitura, OpenStreetMap) com relatos dos usuários; começar por uma região piloto |
| R2 | **Dados desatualizados ou incorretos** levarem o usuário a uma rota perigosa ou inacessível | Média | Alto | Data da última verificação em cada trecho; confirmação por outros usuários; aviso de que as condições podem ter mudado |
| R3 | **Poucos usuários relatando problemas** (a base colaborativa não decola) | Média | Médio | Fluxo de relato simples e rápido; parcerias com ONGs e associações de pessoas com deficiência |
| R4 | **Relatos falsos ou mal-intencionados** | Baixa | Médio | Moderação, validação cruzada de relatos e reputação de usuários |
| R5 | **Custo e limites de uso das APIs de mapa e rotas** | Média | Médio | Avaliar alternativas gratuitas ou abertas; uso de cache; monitorar consumo |
| R6 | **Privacidade e LGPD** no uso da localização do usuário | Média | Alto | Coletar apenas o necessário; pedir consentimento claro; política de privacidade |
| R7 | **Escopo maior que o prazo da equipe** | Média | Médio | Manter a V1 enxuta (só mobilidade); deixar voz e filtros avançados para a V2 |
| R8 | **O próprio app não ser acessível** | Baixa | Alto | Seguir diretrizes de acessibilidade (contraste, fontes, leitor de tela); testar com usuários reais |
| R9 | **Responsabilidade por uma rota indicada incorretamente** | Baixa | Alto | Termos de uso e avisos claros de que a rota é uma sugestão informativa |

---

## 8. Critérios de sucesso

[Definir como o grupo vai medir o resultado da V1. Exemplos: percentual de rotas validadas por usuários reais, número de relatos recebidos, satisfação em testes com usuários, regiões cobertas.]

---

## 9. Pendências do documento

- [ ] Inserir dados e fontes de apoio na seção 2
- [ ] Revisar os riscos com o grupo (seção 7)
- [ ] Definir critérios de sucesso (seção 8)
