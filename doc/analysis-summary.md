# Resumo da análise do sistema

## Contexto

O repositório é uma coleção acadêmica de dois jobs batch Java/Hadoop, não uma
aplicação web. O primeiro conta palavras em um texto; o segundo analisa o
dataset Forest Fires com clustering iterativo e correlações.

## Estado atual

- Ambos os módulos compilam com Maven.
- O código declara Java 11 e Hadoop 3.3.6.
- A máquina de análise tem Java 25 e não tem o binário Hadoop. A compilação
  passou, mas a execução falhou antes do job iniciar porque Hadoop 3.3.6 chama
  `javax.security.auth.Subject.getSubject`, que não é suportado pelo Java 25.
  Portanto, a execução precisa ser validada com Java 11 ou runtime compatível.
- A documentação `documenta_o_completa_mapreduce.md` descreve principalmente um
  job direto de correlação, enquanto o `Main` atual executa clustering e
  correlação por cluster. Essa divergência deve ser resolvida antes de usar a
  documentação como contrato operacional.
- Existem classes de correlação direta não registradas pelo `Main`; são
  candidatas a legado ou a um fluxo que perdeu seu entrypoint.

## Pontos fortes

- Dependência Hadoop está centralizada nos POMs.
- O job de correlação direta usa estado associativo e `CorrelationStats.merge`,
  compatível com Combiner.
- O parser `FireRecord` valida quantidade de colunas, categorias e valores
  finitos.
- A inicialização K-means++ usa semente determinística por `k`.

## Pontos de atenção

- A execução depende de Hadoop instalado e de Java 11, mas isso não é
  verificado pelos scripts de forma consistente.
- O `Main` de incêndios carrega todos os registros válidos em memória antes de
  iniciar os jobs, limitando escalabilidade para arquivos grandes.
- Cada iteração cria um job Hadoop e usa um único reducer, o que aumenta custo
  de coordenação e limita paralelismo da agregação.
- A métrica `movement` compara cada novo centróide ao centróide anterior mais
  próximo, não necessariamente pelo mesmo índice; isso deve ser tratado como
  semântica atual ao validar mudanças.
- Saídas geradas, `target/` e arquivos auxiliares aparecem no repositório,
  misturando fonte com resultado de execução.

## Roteiro incremental

1. **Reprodutibilidade:** fixar/verificar Java 11, documentar instalação do
   Hadoop e adicionar uma execução local automatizada que confirme os formatos
   de saída.
2. **Contrato documental:** atualizar a documentação para refletir o pipeline
   de clustering atual e separar explicitamente o pipeline de correlação
   legado não registrado.
3. **Validação:** criar testes unitários para parser, normalização,
   `CorrelationStats.merge`, convergência e formatos de saída; adicionar um
   dataset pequeno determinístico.
4. **Escala:** medir memória da leitura inicial e custo de jobs por iteração;
   somente depois decidir entre manter Hadoop, reduzir rounds ou mover o
   treinamento para uma etapa dedicada.
5. **Higiene de repositório:** remover ou ignorar artefatos gerados, mantendo
   apenas fontes, entradas de exemplo e documentação em `doc/`.
6. **Decisão do legado:** decidir, com evidência de uso, se as classes
   `CorrelationMapper`/`Combiner`/`Reducer` serão reativadas como job separado
   ou removidas em uma mudança controlada.
