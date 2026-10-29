# Mão na Roda

Aplicativo para ajudar pessoas com mobilidade reduzida a encontrar rotas considerando as condições de acessibilidade dos trajetos e barreiras relatadas por usuários.

## Sobre o projeto

O Mão na Roda propõe apoiar pessoas com mobilidade reduzida na consulta de rotas mais compatíveis com suas necessidades. A solução considera perfis de mobilidade, informações sobre barreiras nos trajetos e relatos enviados por usuários.

Na primeira versão, o foco é consultar informações de acessibilidade e receber relatos. A validação dos relatos e seu uso para alterar as rotas estão previstos para uma etapa posterior, conforme as regras de negócio aprovadas.

## Origem

O projeto surgiu durante a 6ª WECTI da UNICID, em uma apresentação de MVPs. Na ocasião, nosso grupo apresentou o pitch de uma ideia de aplicativo voltado a alunos, professores e visitantes da faculdade. A proposta conquistou o 1º lugar entre as apresentações do dia. Esse reconhecimento nos motivou a dar continuidade à ideia, tirá-la do papel e transformá-la em realidade.

Desde então, a proposta evoluiu e passou a investigar como informações sobre barreiras de acessibilidade podem ajudar pessoas com mobilidade reduzida a planejar seus deslocamentos.

## Integrantes

| Integrante | Linkedin/Github | Responsabilidade |
|---|---|---|
| **Arthur** | https://www.linkedin.com/in/arthur-oshiro-b81392341/ | Administrador do banco de dados |
| **Guilherme** | https://www.linkedin.com/in/luiz-guilherme-rom%C3%A3o-85646b35b/ | Desenvolvedor UI/UX do aplicativo |
| **Felipe** | https://www.linkedin.com/in/felipe-filho-3574a4173/ | Desenvolvedor da API |
| **Pablo** | https://www.linkedin.com/in/pablo-calegari-997a40234/ | Desenvolvedor das funcionalidades do aplicativo |
| **Rafael** | https://www.linkedin.com/in/rafaelboregs/ | Líder e auxílio geral |

## Estrutura do repositório

A árvore abaixo combina a documentação já conhecida com uma estrutura proposta para organizar o desenvolvimento. As pastas de implementação podem ser criadas conforme os respectivos componentes forem iniciados.

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/                  # Modelos de issues, quando definidos
│   └── pull_request_template.md         # Modelo de pull request
├── apps/
│   ├── android/                         # Aplicativo Android nativo
│   │   ├── app/src/main/java/           # Código Java do aplicativo
│   │   ├── app/src/main/res/            # Layouts, estilos e recursos XML
│   └── api/                             # API Java com Spring Boot
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/                # Código-fonte da API
│       │   │   └── resources/           # Configurações e recursos
│       │   └── test/
│       │       └── java/                # Testes da API
├── database/
│   ├── migrations/                      # Evoluções versionadas do banco
│   └── seeds/                           # Dados iniciais de catálogos
├── docs/
│   ├── 01-visao-de-negocio.md
│   ├── 02-regras-de-negocio.md
│   ├── 03-requisitos.md
│   ├── 05-tech-stack.md
│   ├── 06-banco-de-dados.md
│   └── diagrams/                        # Diagramas do projeto
├── CONTRIBUTING.md
└── README.md
```

### Responsabilidade de cada área

- `apps/android/`: aplicativo Android nativo, com Java, layouts e estilos XML.
- `apps/api/`: endpoints, autenticação/autorização, regras de negócio e integração com o banco e serviços externos.
- `database/`: scripts de migração e dados iniciais do banco, quando a estratégia de migrations for definida.
- `docs/`: visão, regras, requisitos, decisões técnicas e diagramas.
- `.github/`: modelos e automações relacionados a issues e pull requests.

A localização definitiva das migrations deve acompanhar a ferramenta escolhida pela equipe. Se elas forem gerenciadas pela aplicação, por exemplo, podem ficar dentro de `apps/api/`; o importante é manter uma única fonte versionada para as alterações do banco.

## Tecnologias

As tecnologias documentadas atualmente incluem:

- **Aplicativo:** Android nativo, Java, XML, AndroidX e Material Components.
- **API:** Java com Spring Boot.
- **Banco de dados e autenticação:** Supabase.

Consulte [`docs/05-tech-stack.md`](docs/05-tech-stack.md) para decisões e pendências técnicas. A ferramenta de migrations e outras escolhas ainda pendentes devem ser confirmadas antes de serem tratadas como padrão do projeto.

## Documentação

- [Visão de negócio](docs/01-visao-de-negocio.md)
- [Regras de negócio](docs/02-regras-de-negocio.md)
- [Requisitos](docs/03-requisitos.md)
- [Tech stack](docs/05-tech-stack.md)
- [Banco de dados](docs/06-banco-de-dados.md)

## Como contribuir

1. Consulte a documentação antes de implementar ou alterar uma regra do produto.
2. Abra uma issue descrevendo o problema ou a mudança proposta.
3. Crie uma branch com um nome relacionado à tarefa.
4. Faça commits objetivos e abra um pull request usando o modelo do repositório.
5. Atualize a documentação quando uma alteração modificar requisitos, regras de negócio ou decisões técnicas.

Consulte [`CONTRIBUTING.md`](CONTRIBUTING.md) para as convenções completas de contribuição.

## Licença

A licença do projeto ainda precisa ser definida. Até lá, não presuma que o código pode ser reutilizado ou redistribuído.
