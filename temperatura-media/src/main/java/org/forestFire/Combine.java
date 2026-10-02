package org.forestFire;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Combine extends Reducer {
    private Text valorParcial = new Text();

    public void reduce(Text key, Iterable values, Context context) throws IOException, InterruptedException {
        double somaTemperaturas = 0.0;
        int contagemTotal = 0;

        for (Object val : values) {
            String[] partes = val.toString().split(",");
            somaTemperaturas += Double.parseDouble(partes[0]);
            contagemTotal += Integer.parseInt(partes[1]);
        }

        valorParcial.set(somaTemperaturas + "," + contagemTotal);
        context.write(key, valorParcial);
    }
}