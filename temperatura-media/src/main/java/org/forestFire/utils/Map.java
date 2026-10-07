package org.forestFire.utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

public class Map extends Mapper<Object, Text, Text, Text> {
    private final static Text chaveEstatica = new Text("temperatura");
    private Text valorEmitido = new Text();

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] colunas = value.toString().split(",");
        if (colunas.length > 8) {
            try {
                double temp = Double.parseDouble(colunas[8]);
                valorEmitido.set(temp + ",1");
                context.write(chaveEstatica, valorEmitido);
            } catch (NumberFormatException e) {/* Ignora linhas de cabeçalho ou dados corrompidos */ }
        }
    }
}