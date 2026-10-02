package org.forestFire;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Reduce extends Reducer<Text, Text, Text, DoubleWritable> {
    private DoubleWritable mediaFinal = new DoubleWritable();

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        double somaTemperaturas = 0.0;
        int contagemTotal = 0;

        for (Text val : values) {
            String[] partes = val.toString().split(",");
            somaTemperaturas += Double.parseDouble(partes[0]);
            contagemTotal += Integer.parseInt(partes[1]);
        }

        if (contagemTotal > 0) {
            double media = somaTemperaturas / contagemTotal;
            mediaFinal.set(media);
            context.write(new Text("Temperatura Media Total:"), mediaFinal);
        }
    }
}