# 06 — Guia de Contribuição para Múltiplos Colaboradores

Este projeto é desenvolvido de forma colaborativa. Siga este fluxo para evitar conflitos na `main`, manter o padrão arquitetural e garantir segurança em todas as entregas.

---

## 1. Fluxo Git Passo a Passo

Nunca desenvolva nem faça push direto na branch `main` durante features do dia a dia. Trabalhe sempre em **branches curtas por tarefa** e abra **Pull Request (PR)**:

1. **Atualize sua `main` local:**
   ```bash
   git checkout main
   git pull --rebase origin main
   ```
2. **Crie uma branch com prefixo padronizado:**
   - Nova funcionalidade: `feat/nome-curto` (ex.: `feat/opportunities-crud`)
   - Correção de bug: `fix/descricao-curta` (ex.: `fix/login-trim-email`)
   - Ajuste de segurança: `sec/descricao-curta` (ex.: `sec/rate-limit-headers`)
   - Documentação: `docs/descricao-curta`
   - Infra/CI/Dependências: `chore/descricao-curta`
   ```bash
   git checkout -b feat/nome-da-tarefa
   ```
3. **Implemente seguindo o método Spec-First:**
   - Leia as specs em `docs/specs/` antes de escrever código.
   - Se sua tarefa adicionar endpoint, campo JSON ou tabela, atualize a spec correspondente e a documentação em `docs/` **no mesmo PR**.
4. **Rode todos os testes antes de commitar:**
   ```bash
   ./mvnw clean test
   ```
5. **Faça commits claros no padrão Conventional Commits:**
   ```bash
   git add <arquivos-da-tarefa>
   git commit -m "feat(opportunities): adiciona listagem paginada de oportunidades"
   ```
   Prefixos recomendados: `feat`, `fix`, `sec`, `docs`, `test`, `refactor`, `chore`.
6. **Envie sua branch e abra um Pull Request no GitHub:**
   ```bash
   git push -u origin feat/nome-da-tarefa
   ```
   Abra o PR para `main`, peça revisão de pelo menos **1 colaborador(a)** e aguarde aprovação antes do merge.

---

## 2. Coordenação Entre Colaboradores (Evitando Conflitos)

- **Migrações Flyway (`V2__`, `V3__`, ...):**
  - Se dois colaboradores criarem `V2__...sql` em branches paralelas, o segundo PR a entrar na `main` quebrará o Flyway.
  - **Regra:** antes do merge, faça `git fetch origin && git rebase origin/main` e renomeie sua migração para o próximo número sequencial disponível caso outra já tenha entrado na `main`.
- **Escopo enxuto por PR:**
  - Prefira PRs pequenos focados em 1 módulo/feature. Evite refatorar arquivos de outros módulos fora da sua tarefa sem alinhar com o time.
- **Comunicação de contratos com Frontend:**
  - Sempre mantenha `docs/specs/02-contrato-api.md` sincronizada com os DTOs Java para que o time de frontend não dependa de adivinhação.

---

## 3. Regras Inegociáveis de Código e Segurança

1. **Zero segredos no Git:** nunca commite `.env`, `.env.mcp`, senhas reais, tokens ou chaves privadas.
2. **Zero PII em logs ou erros:** nunca registre e-mail, CPF, telefone, nome completo, data de nascimento ou senha em `log.info`/`log.error` ou respostas de erro.
3. **Validação na borda:** todo `Request` deve ser Java `record` com anotações `jakarta.validation.constraints.*`.
4. **Injeção via construtor:** nunca use `@Autowired` em atributos de classe.
5. **Comentários enxutos em PT-BR:** documente classes/métodos públicos e o *porquê* de decisões não óbvias de forma direta e curta.
6. **Testes obrigatórios:** toda nova regra de negócio ou correção de bug/segurança precisa de teste unitário e/ou de integração correspondente.

---

## 4. Checklist Antes de Abrir o Pull Request (DoD)

- [ ] `./mvnw clean test` passa 100% sem falhas, skips ou warnings novos de compilação
- [ ] Nenhum arquivo `.env` ou segredo está no `git status`
- [ ] Logs revisados para garantir ausência de dados pessoais (LGPD)
- [ ] Migrações Flyway possuem script de rollback correspondente em `docs/db/rollback/`
- [ ] Specs (`docs/specs/`) e documentação (`docs/`) atualizadas caso haja mudança de contrato, schema ou fluxo
- [ ] Descrição do PR explica **o quê mudou**, **por quê** e **como testar**
