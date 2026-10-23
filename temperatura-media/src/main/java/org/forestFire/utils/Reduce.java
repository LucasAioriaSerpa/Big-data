package org.forestFire.utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Reduce extends Reducer<Text, Text, Text, Text> {
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        CorrelationStats combined = new CorrelationStats();
        for (Text val : values) {
            combined.merge(CorrelationStats.parse(val.toString()));
        }

        if (combined.getCount() < 2) {
            context.write(key, new Text("indefinida (menos de 2 registros)"));
            return;
        }

        double correlation = combined.getCorrelation();
        if (!Double.isFinite(correlation)) {
            context.write(key, new Text("indefinida (variancia zero)"));
            return;
        }

        context.write(key, new Text(String.format(java.util.Locale.ROOT,
                "r=%.6f; n=%d", correlation, combined.getCount())));
    }
}
