# Índice de arquitetura

## Implementation Guide

Este índice é um guia de navegação, não o contrato completo. **Não implemente
somente a partir deste arquivo; siga os caminhos dos artefatos abaixo.**

### Artefatos globais

- `unit_graph.yaml`: limites, gatilhos, assinaturas e âncoras; filtrar pelo
  `name` da unidade.
- `migration_boundary.yaml`: escopo de implementação (`must_rewrite`), cópia
  e legado permitido; `source_anchors` não são escopo de reescrita.
- `wire_contracts.yaml`: contratos CLI/arquivo; filtrar por `unit`.
- `shared_modules.yaml`: módulos compartilhados; filtrar por
  `used_by_units`.
- `cross_unit_state.yaml`: fluxos implícitos; consultar `writer.unit` e
  `reader.unit`.
- `project-structure.md`, `tech-stack.md` e `data-model.md`: contexto global
  de estrutura, stack e modelo de dados.

### Unidade `wordcount-job`

- Gatilho: CLI `org.countWords.Main <input> <output>`.
- Ler `units/wordcount-job/behavior.yaml` para efeitos, branches, erros e
  sincronização.
- Ler `units/wordcount-job/bindings.yaml` para registro Hadoop e configurações
  `mapreduce.framework.name`/`fs.defaultFS`.
- Ler `units/wordcount-job/unit_decomposition.yaml` apenas como candidatos de
  divisão; `commit: false` não define a estrutura futura.
- Filtrar contratos globais por `unit: wordcount-job`.
- Evidência de conclusão: listar artefatos lidos, preservar tokenização,
  formato `palavra\ttotal`, códigos de saída e evidência de build/execução.

### Unidade `forestfire-clustering-job`

- Gatilho: CLI `org.forestFire.Main <input> <output>`.
- Ler `units/forestfire-clustering-job/behavior.yaml` para validação CSV,
  iterações, saída, erros e sincronização entre jobs.
- Ler `units/forestfire-clustering-job/bindings.yaml` para classes Hadoop e
  chaves de configuração serializadas.
- Ler `units/forestfire-clustering-job/unit_decomposition.yaml` apenas como
  candidatos; não converter candidatos em decisão sem design.
- Filtrar contratos globais por `unit: forestfire-clustering-job`.
- Evidência de conclusão: listar itens `must_preserve`, confirmar formatos de
  clusters/correlações e registrar build, execução Hadoop e validações de
  registros inválidos.

## Verificação do artefato

- Há um conjunto de três arquivos por unidade em `units/`.
- Não há `TBD` nos contratos.
- A análise não declarou reescrita: `must_rewrite` está vazio.
- `CorrelationMapper`, `CorrelationCombiner` e `CorrelationReducer` foram
  registrados como código existente não alcançado pelo `Main` atual, não como
  fluxo executável confirmado.
