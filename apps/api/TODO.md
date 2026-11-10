# Pendências da API

## Estado atual — 09/11/2026

O planejamento público usa Google Routes. A API é publicada no Elastic Beanstalk
a cada alteração na `main` ([implantação](../../docs/08-implantacao-aws.md)); o
teste no emulador também pode usar o backend local. O contrato está em
[PLANEJAMENTO.md](PLANEJAMENTO.md) e os passos de teste no
[guia do time](../../docs/17-guia-testes-time.md).

- [x] Implementar catálogo, planejamento público, limites e avaliação piloto.
- [x] Configurar segurança explícita e validadores JWT; rotas não exigem conta.
- [x] Migrar candidatas para Google Routes e aceitar endereços diretamente.
- [x] Permitir HTTP apenas local no Android debug, preservando HTTPS em release.
- [x] Testar fluxo HTTP com Google simulado e executar os três testes no emulador.
- [ ] Configurar a chave Routes de servidor e um PostgreSQL/PostGIS preparado
  com V1–V7 e login da aplicação; validar a primeira consulta real do novo fluxo.
- [x] Executar a integração PostGIS no CI (localmente, `tools/testar-postgis.ps1`).
- [ ] Revisar os 30 pares da matriz e o dataset sobre as geometrias Google.
- [ ] Homologar latência, custo, rede e acessibilidade em campo.
- [x] Publicar a API em HTTPS (Elastic Beanstalk atrás de API Gateway), com segredos no Secrets Manager.
- [ ] Definir quota compartilhada na borda; o limite atual de consultas é local a cada instância.
- [ ] Registrar o baseline do Flyway na produção.
- [ ] Concluir cadastro/login com identidade real e integração da conta no Android.
- [ ] Implementar a criação de usuário (`POST /usuarios/me` retorna 501), definindo
  se cria a conta no provedor ou só o perfil de uma conta já autenticada.
- [ ] Usar DTO de saída explícito no `GET /usuarios/me` (hoje só nome e sobrenome).
- [ ] Definir e testar a exclusão de conta (hoje só altera o estado) e o acesso posterior.
- [ ] Concluir relatos remotos, fotos e descarte dentro do prazo aprovado.

O backend exige um banco real em execução; H2 e Google simulado são só de testes.

No Windows, execute os comandos Maven com `.\mvnw.cmd`; no Linux/macOS,
use `sh mvnw`, sempre a partir da pasta `apps/api`.
