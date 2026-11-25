# Skill: Spec-First (Planejar Antes de Codar)

Use esta skill antes de iniciar qualquer feature nova, refatoração ou mudança de banco de dados.

## Fluxo Obrigatório
1. **Leitura de Contexto:** Leia as specs relevantes em `docs/specs/` e inspecione os arquivos existentes relacionados à tarefa.
2. **Mapeamento de Impacto:** Liste exatamente:
   - Arquivos que serão **criados**
   - Arquivos que serão **modificados**
   - Migrações, variáveis de ambiente ou contratos de API impactados
3. **Plano em Passos:** Descreva a implementação em passos curtos e verificáveis, incluindo quais testes serão adicionados.
4. **Gate de Aprovação:** **PARE e aguarde aprovação explícita do usuário** antes de modificar ou criar código-fonte.
5. **Atualização de Spec:** Se a mudança alterar arquitetura, contrato de API ou modelo de dados, inclua a atualização da spec correspondente no plano.
