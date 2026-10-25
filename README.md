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
| **Felipe** | https://github.com/Triplof | Desenvolvedor da API |
| **Pablo** | https://www.linkedin.com/in/pablo-calegari-997a40234/ | Desenvolvedor das funcionalidades do aplicativo |
| **Rafael** | https://www.linkedin.com/in/rafaelboregs/ | Líder e auxílio geral |

## Estrutura atual do repositório

A árvore abaixo representa a estrutura atual do repositório.

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/                  # Modelos de issues
│   └── pull_request_template.md         # Modelo de pull request
├── apps/
│   ├── mobile/                          # Aplicativo Flutter
│   │   ├── android/                      # Projeto Android
│   │   ├── assets/                       # Fontes e recursos
│   │   ├── lib/                         # Código-fonte do aplicativo
│   │   └── test/                         # Testes do aplicativo
│   └── api/                             # API Java com Spring Boot
│       ├── database/                    # Documentação e testes do banco
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/                # Código-fonte da API
│       │   │   └── resources/
│       │   │       ├── application.properties
│       │   │       └── db/migration/    # Migrations Flyway V1–V5
│       │   └── test/
│       │       └── java/                # Testes da API
│       ├── pom.xml
│       └── README.md
├── docs/
│   ├── 01-visao-de-negocio.md
│   ├── 02-regras-de-negocio.md
│   ├── 03-requisitos.md
│   ├── 04-casos-de-uso.md
│   ├── 05-tech-stack.md
│   └── 06-banco-de-dados.md
├── CONTRIBUTING.md
└── README.md
```

### Responsabilidade de cada área

- `apps/mobile/`: interface e funcionalidades executadas no aplicativo Android.
- `apps/api/`: endpoints, autenticação/autorização, regras de negócio e integração com o banco e serviços externos.
- `apps/api/src/main/resources/db/migration/`: migrations Flyway para PostgreSQL/PostGIS e carga inicial da matriz de barreiras.
- `apps/api/database/`: documentação operacional do banco e testes SQL locais.
- `docs/`: visão, regras, requisitos, casos de uso, decisões técnicas e modelo de dados.
- `.github/`: modelos e automações relacionados a issues e pull requests.


## Tecnologias

As tecnologias documentadas atualmente incluem:

- **Aplicativo:** Flutter.
- **API:** Java com Spring Boot.
- **Banco de dados:** PostgreSQL com PostGIS, hospedado no Supabase.
- **Autenticação e armazenamento:** Supabase Auth e Supabase Storage.
- **Migrations:** Flyway Maven Plugin, executado a partir de `apps/api/`.

Consulte [`docs/05-tech-stack.md`](docs/05-tech-stack.md) para decisões e pendências técnicas. Para o banco, consulte também [`apps/api/database/README.md`](apps/api/database/README.md).

## Documentação

- [Aplicativo mobile](apps/mobile/README.md)
- [API](apps/api/README.md)
- [Banco de dados e migrations](apps/api/database/README.md)
- [Visão de negócio](docs/01-visao-de-negocio.md)
- [Regras de negócio](docs/02-regras-de-negocio.md)
- [Requisitos](docs/03-requisitos.md)
- [Casos de uso](docs/04-casos-de-uso.md)
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
