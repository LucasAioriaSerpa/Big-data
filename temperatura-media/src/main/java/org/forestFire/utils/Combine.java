package org.forestFire.utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Combine extends Reducer<Text, Text, Text, Text> {
    private Text valorParcial = new Text();

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        double somaTemperaturas = 0.0;
        double somaQuadrados = 0.0;
        int contagemTotal = 0;

        double minLocal = Double.MAX_VALUE;
        double maxLocal = Double.MIN_VALUE;

        for (Text val : values) {
            String[] partes = val.toString().split(",");
            somaTemperaturas += Double.parseDouble(partes[0]);
            somaQuadrados += Double.parseDouble(partes[1]);
            contagemTotal += Integer.parseInt(partes[2]);

            double tempMinAtual = Double.parseDouble(partes[3]);
            double tempMaxAtual = Double.parseDouble(partes[4]);

            if (tempMinAtual < minLocal) minLocal = tempMinAtual;
            if (tempMaxAtual > maxLocal) maxLocal = tempMaxAtual;
        }

        valorParcial.set(
            somaTemperaturas + "," + 
            somaQuadrados + "," + 
            contagemTotal + "," + 
            minLocal + "," + maxLocal
        );
        context.write(key, valorParcial);
    }
}
