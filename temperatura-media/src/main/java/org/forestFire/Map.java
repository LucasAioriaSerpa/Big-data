package org.forestFire;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

public class Map extends Mapper<Object, Text, Text, Text> {
    private final static Text chaveEstatica = new Text("temperatura");
    private Text valorEmitido = new Text();

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] colunas = value.toString().split(",");

        // O arquivo forestfire tem 13 colunas. A temperatura está no índice 8.
        if (colunas.length > 8) {
            try {
                double temp = Double.parseDouble(colunas[8]);

                // Emite a temperatura e o peso "1" para a contagem
                // Formato: "valor_temperatura,1"
                valorEmitido.set(temp + ",1");
                context.write(chaveEstatica, valorEmitido);

            } catch (NumberFormatException e) {
                // Ignora linhas de cabeçalho ou dados corrompidos
            }
        }
    }
}