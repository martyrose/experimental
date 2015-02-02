package com.mrose.flume;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Created by martinrose on 2/2/15.
 */
public class ParseCounters {
  private static final Logger log = LoggerFactory.getLogger(ParseCounters.class);

  public static void main(String[] args) throws Exception {
    String fileName = "/usr/local/google/home/martinrose/flume.data.txt";
    Path p = FileSystems.getDefault().getPath(fileName);
    List<String> linez = Files.readAllLines(p, Charset.defaultCharset());
    Map<String, Long> queries = new HashMap<>();
    Map<String, Long> msecs = new HashMap<>();

    for(String line : linez) {
      Iterable<String> it = Splitter.on(Pattern.compile("\\s+")).trimResults().split(line);
      String key = Iterables.getFirst(it, null);
      String value = Iterables.getLast(it).replace(",", "");

      if(key.endsWith("-queried")) {
        queries.put(key.substring(0, key.indexOf("-queried")), Long.parseLong(value));
      }
      if(key.endsWith("-query-msec")) {
        msecs.put(key.substring(0, key.indexOf("-query-msec")), Long.parseLong(value));
      }
    }

    Map<String, Double> latency = new HashMap<>();
    List<Pair<String, Long>> totalTime = new ArrayList<>();
    for (Entry<String, Long> e : queries.entrySet()) {
      String key = e.getKey();
      double count = e.getValue().doubleValue();
      double msec = msecs.get(key).doubleValue();

      Double msecPerCall = msec/count;
      latency.put(key, msecPerCall);

      totalTime.add(Pair.of(key, msecs.get(key)));
    }

    Collections.sort(totalTime, Collections.reverseOrder(new Comparator<Pair<String, Long>>() {
      @Override
      public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
        return o1.getRight().compareTo(o2.getRight());
      }
    }));

    NumberFormat nf = NumberFormat.getIntegerInstance();
    StringBuilder sb = new StringBuilder();
    for(Pair<String, Long> e : totalTime) {
      sb.append(Strings.padStart(e.getKey(), 28, ' ')
          + " => " + Strings.padStart(DurationFormatUtils.formatDurationHMS(e.getValue()), 12, ' ')
          + " totalTime on   " + Strings.padStart(nf.format(queries.get(e.getKey())), 9, ' ')
          + " calls latency " + nf.format(latency.get(e.getKey())) + " msec/call\n");
    }
    log.warn("\n" + sb.toString());
  }

}
