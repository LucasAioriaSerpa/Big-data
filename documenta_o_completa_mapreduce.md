# Documentação: Análise Estatística com Hadoop MapReduce

Este documento explica detalhadamente o código desenvolvido para calcular **Mínimo, Máximo, Média, Variância e Desvio Padrão** usando o ecossistema Hadoop MapReduce.

---

## 1. Como a Arquitetura Funciona (O "Porquê")

O MapReduce é um modelo de programação distribuída. Quando lidamos com Terabytes de dados, não podemos carregar tudo na memória para calcular uma média ou variância.
A solução é usar a matemática a nosso favor: em vez de guardar os números, guardamos **estado** (soma, contagem, soma dos quadrados, etc.).

A arquitetura usa três passos:

1. **Map (Mapeamento):** Lê cada linha, isola o valor desejado (temperatura) e o prepara para envio.
2. **Combine (Combinação Local):** Um "mini-reducer" que roda no mesmo nó do Mapper. Ele junta os milhares de registros daquele nó em um único pacote consolidado. Isso reduz drasticamente o tráfego de rede.
3. **Reduce (Redução Global):** Recebe os pacotes consolidados de todos os Combiners, faz a soma final e aplica as fórmulas matemáticas definitivas.

---

## 2. Explicação Linha a Linha dos Códigos

### A. Classe `Main.java` (A Orquestração)

Esta classe configura o "Job" (tarefa) do Hadoop. É o ponto de entrada da aplicação.

```java
package org.forestFire; // Define o pacote do projeto

import org.apache.hadoop.conf.Configuration; // Classe para gerenciar configurações do Hadoop
import org.apache.hadoop.fs.Path; // Classe para lidar com caminhos de arquivos HDFS/locais
import org.apache.hadoop.io.Text; // Tipo de dado otimizado do Hadoop equivalente ao String do Java
import org.apache.hadoop.mapreduce.Job; // Classe que representa a tarefa MapReduce
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat; // Lida com a entrada de dados
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat; // Lida com a saída de dados

public class Main {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration(); // Cria o objeto de configuração base

        // As duas linhas abaixo forçam a execução no modo local (ideal para testes sem cluster real)
        conf.set("mapreduce.framework.name", "local");
        conf.set("fs.defaultFS", "file:///");

        // Verifica se o usuário passou os dois caminhos (entrada e saída) no terminal
        if (args.length != 2) {
            System.err.println("Uso: Main <caminho_entrada> <diretorio_saida>");
            System.exit(2); // Encerra com erro se os argumentos faltarem
        }

        // Cria a instância do Job e dá um nome para ele aparecer nos logs
        Job job = Job.getInstance(conf, "Analise de Temperatura Completa");
        job.setJarByClass(Main.class); // Ajuda o Hadoop a encontrar os arquivos compilados (.jar)

        // Define quais classes executarão as fases do processo
        job.setMapperClass(Map.class);
        job.setCombinerClass(Combine.class); // Ativa otimização de rede
        job.setReducerClass(Reduce.class);

        // Define os tipos de dados (Chave, Valor) que sairão do Map/Combine
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
      
        // Define os tipos de dados (Chave, Valor) finais gerados no arquivo de saída
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Define de onde ler e onde salvar os resultados (baseado nos args do terminal)
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Dispara o Job e aguarda a conclusão. Retorna 0 (sucesso) ou 1 (erro) para o SO.
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
```

### B. Classe `Map.java` (A Extração)

O Mapper é executado para *cada linha* do seu arquivo `.csv`.

```java
package org.forestFire;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

// A classe herda Mapper. Os genéricos <Object, Text, Text, Text> representam:
// <ChaveEntrada, ValorEntrada, ChaveSaida, ValorSaida>
public class Map extends Mapper<Object, Text, Text, Text> {
  
    // Como queremos a média GLOBAL, todos os dados precisam ir para o mesmo Reducer.
    // Para forçar isso, emitimos todos os valores com a MESMA chave estática.
    private final static Text chaveEstatica = new Text("temperatura_estatisticas");
  
    // Objeto reaproveitado para evitar criação excessiva na memória
    private Text valorEmitido = new Text(); 

    // O método map é chamado automaticamente para cada linha de texto
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        // Separa a linha (String) do CSV em um array, usando a vírgula como delimitador
        String[] colunas = value.toString().split(",");

        // Só processa se a linha tiver colunas suficientes (evita erro IndexOutOfBounds)
        if (colunas.length > 8) {
            try {
                // Pega a coluna 8 (Temperatura) e converte de String para Double
                double temp = Double.parseDouble(colunas[8]);
              
                // Prepara a variável ao quadrado (necessária para variância)
                double tempQuadrado = temp * temp;
              
                // Monta o pacote de dados no formato: "soma,soma_quadrados,contagem,min,max"
                // Aqui no nível individual, a temperatura é ela mesma o min e o max.
                valorEmitido.set(temp + "," + tempQuadrado + ",1," + temp + "," + temp);
              
                // Escreve a chave e o pacote de dados para ir ao Combiner/Reducer
                context.write(chaveEstatica, valorEmitido);
              
            } catch (NumberFormatException e) {
                // Se a conversão falhar (ex: cabeçalho com texto em vez de número), ignora a linha.
            }
        }
    }
}
```

### C. Classe `Combine.java` (A Otimização Local)

O Combiner agrupa os registros dentro da própria máquina antes de enviar pela rede.

```java
package org.forestFire;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

// Funciona exatamente como um Reducer, mas a saída deve ter a mesma estrutura da entrada
public class Combine extends Reducer<Text, Text, Text, Text> {
    private Text valorParcial = new Text();

    // Recebe a chave ("temperatura_estatisticas") e uma lista iterável contendo todos 
    // os valores (Strings) gerados pelos Mappers locais
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // Inicializa acumuladores
        double somaTemperaturas = 0.0;
        double somaQuadrados = 0.0;
        int contagemTotal = 0;
      
        // Inicializa min/max com extremos (Double.MAX_VALUE é o maior número possível)
        // Isso garante que o primeiro número que chegar irá substituir esses valores.
        double minLocal = Double.MAX_VALUE;
        double maxLocal = Double.MIN_VALUE;

        // Itera por todos os valores enviados pelos Mappers
        for (Text val : values) {
            // Quebra o pacote do Mapper
            String[] partes = val.toString().split(",");
          
            // Soma os valores nos acumuladores locais
            somaTemperaturas += Double.parseDouble(partes[0]);
            somaQuadrados += Double.parseDouble(partes[1]);
            contagemTotal += Integer.parseInt(partes[2]);
          
            // Pega o min/max do pacote atual
            double tempMinAtual = Double.parseDouble(partes[3]);
            double tempMaxAtual = Double.parseDouble(partes[4]);
          
            // Descobre qual é o verdadeiro menor e maior valor desse nó
            if (tempMinAtual < minLocal) minLocal = tempMinAtual;
            if (tempMaxAtual > maxLocal) maxLocal = tempMaxAtual;
        }

        // Reconstrói o pacote com os valores agregados e repassa pra frente
        valorParcial.set(somaTemperaturas + "," + somaQuadrados + "," + contagemTotal + "," + minLocal + "," + maxLocal);
        context.write(key, valorParcial);
    }
}
```

### D. Classe `Reduce.java` (O Cálculo Final)

Recebe os pacotes já parcialmente somados pelos Combiners de todo o cluster.

```java
package org.forestFire;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Reduce extends Reducer<Text, Text, Text, Text> {
    private Text resultadoEstatisticas = new Text();

    // Estrutura idêntica ao Combiner, mas o iterable agora tem os pacotes dos Combiners
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        double somaTemperaturas = 0.0;
        double somaQuadrados = 0.0;
        long contagemTotal = 0; // 'long' porque N pode ficar gigantesco no cluster inteiro
      
        double minGlobal = Double.MAX_VALUE;
        double maxGlobal = Double.MIN_VALUE;

        // O processo de agregação é o mesmo do Combiner
        for (Text val : values) {
            String[] partes = val.toString().split(",");
          
            somaTemperaturas += Double.parseDouble(partes[0]);
            somaQuadrados += Double.parseDouble(partes[1]);
            contagemTotal += Long.parseLong(partes[2]);
          
            double tempMinAtual = Double.parseDouble(partes[3]);
            double tempMaxAtual = Double.parseDouble(partes[4]);
          
            if (tempMinAtual < minGlobal) minGlobal = tempMinAtual;
            if (tempMaxAtual > maxGlobal) maxGlobal = tempMaxAtual;
        }

        // Impede divisão por zero se houver 1 ou 0 registros
        if (contagemTotal > 1) { 
            // Calcula as estatísticas
            double media = somaTemperaturas / contagemTotal;
          
            // Fórmula computacional da variância amostral
            // $$Vari\hat{a}ncia = \frac{\sum{x^2} - \frac{(\sum{x})^2}{N}}{N-1}$$
            double variancia = (somaQuadrados - ((somaTemperaturas * somaTemperaturas) / contagemTotal)) / (contagemTotal - 1);
          
            // O Desvio padrão é a raiz da variância
            double desvioPadrao = Math.sqrt(variancia);
          
            // String.format() formata os doubles limitando casas decimais (ex: %.2f é 2 casas)
            String saida = String.format(" | Min: %.2f | Max: %.2f | Media: %.4f | Variancia: %.4f | Desvio Padrao: %.4f", 
                                        minGlobal, maxGlobal, media, variancia, desvioPadrao);
                                      
            resultadoEstatisticas.set(saida);
          
            // Escreve a resposta definitiva no arquivo (part-r-00000)
            context.write(new Text("Resultados:"), resultadoEstatisticas);
        }
    }
}
```

---

## 3. Como Utilizar (Compilação e Execução)

Para executar qualquer alteração neste projeto, os passos são padrão para projetos Maven com Hadoop:

1. **Limpar e Compilar:** Gera o pacote JAR.

   ```bash
   mvn clean package
   ```
2. **Executar:** Envia o JAR para o Hadoop processar com os argumentos passados.

   ```bash
   hadoop jar target/temperatura-media-1.0-SNAPSHOT.jar org.forestFire.Main caminho/do/seu/dataset.csv caminho/pasta_de_saida
   ```

   *Atenção:* A pasta de saída não pode existir previamente, o Hadoop a criará. Se existir, ele retorna erro para evitar sobrescrita acidental.

---

## 4. Aplicações para Outras Situações

Este modelo (`Soma, Soma Quadrados, Contagem, Min, Max`) é um **Padrão de Agregação Matemática**. Ele é universal e altamente escalável. Você pode aplicá-lo em muitos outros contextos:

### A. Estatísticas Agrupadas por Categoria

Atualmente estamos emitindo uma chave estática `"temperatura_estatisticas"`. Se você mudar o `Map.java` para emitir categorias em vez de uma chave fixa, o Hadoop fará o cálculo para cada grupo separadamente.

* **Exemplo (Varejo):** Qual a média e variação de gasto por Estado?
  * *Mapper emite:* `("São Paulo", "150,22500,1,150,150")`
  * *Resultado:* O Hadoop criará uma linha no arquivo final para cada Estado com suas próprias métricas de Min, Max, Média e Desvio Padrão.

### B. Análise Financeira ou Sensor IoT

* **Ações na Bolsa:** Calcular a volatilidade (Desvio Padrão) dos preços diários de fechamento.
* **Sensores IoT:** Em uma fábrica com milhares de sensores lendo a vibração por segundo. Extraia as métricas e observe o *Max* (pico de anomalia) e o *Desvio Padrão* (instabilidade).

### C. Alterando o índice da coluna

Basta mudar a linha `Double.parseDouble(colunas[8]);` para o índice de qualquer outra coluna no seu dataset para extrair as estatísticas de velocidade do vento (índice 10) ou chuva (índice 11) do mesmo CSV.
