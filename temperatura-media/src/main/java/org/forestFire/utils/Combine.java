package org.forestFire.utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class Combine extends Reducer<Text, Text, Text, Text> {
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        CorrelationStats combined = new CorrelationStats();
        for (Text val : values) { combined.merge(CorrelationStats.parse(val.toString())); }
        context.write(key, new Text(combined.serialize()));
    }
}
