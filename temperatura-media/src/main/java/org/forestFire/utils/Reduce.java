package org.forestFire.utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Reduce extends Reducer<Text, Text, Text, Text> {
    private Text resultadoEstatisticas = new Text();

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        double somaTemperaturas = 0.0;
        double somaQuadrados = 0.0;
        long contagemTotal = 0;
        
        double minGlobal = Double.MAX_VALUE;
        double maxGlobal = Double.MIN_VALUE;

        for (Text val : values) {
            String[] partes = val.toString().split(",");
            
            somaTemperaturas += Double.parseDouble(partes[0]);
            somaQuadrados += Double.parseDouble(partes[1]);
            contagemTotal += Long.parseLong(partes[2]);
            
            // Descobre o extremo global absoluto
            double tempMinAtual = Double.parseDouble(partes[3]);
            double tempMaxAtual = Double.parseDouble(partes[4]);
            
            if (tempMinAtual < minGlobal) minGlobal = tempMinAtual;
            if (tempMaxAtual > maxGlobal) maxGlobal = tempMaxAtual;
        }

        if (contagemTotal > 1) { 
            double media = somaTemperaturas / contagemTotal;
            double variancia = (somaQuadrados - ((somaTemperaturas * somaTemperaturas) / contagemTotal)) / (contagemTotal - 1);
            double desvioPadrao = Math.sqrt(variancia);
            
            String saida = String.format(
                " | Min: %.2f | Max: %.2f | Media: %.4f | Variancia: %.4f | Desvio Padrao: %.4f", 
                minGlobal, maxGlobal, media, variancia, desvioPadrao
            );
            resultadoEstatisticas.set(saida);
            context.write(new Text("Resultados:"), resultadoEstatisticas);
        }
    }
}
