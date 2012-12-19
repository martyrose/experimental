package com.accertify.velocity;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.CharSetUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 *
 */
public class ProcessProfiles {
    protected static transient Log log = LogFactory.getLog(ProcessProfiles.class);
    public static final String DIRECTORY = "/home/mrose/tmp2/velocity";
    public static final Deque<FileData> FILE_DATA = new LinkedList<>();
    public static final Set<Long> RULES_TO_TRACK = new HashSet<>();
    public static final Long TRACE = -1l; // 5237260000000298479l;

    public static void main(String[] args) {
        NumberFormat df = DecimalFormat.getNumberInstance();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df.setMinimumIntegerDigits(1);

        Deque<File> files = getFiles();

        for(File f: files) {
            FileData fd = processFile(f);
            FILE_DATA.offerLast(fd);
        }

        log.warn("First: " + FILE_DATA.peekFirst().filename);
        log.warn("Last: " + FILE_DATA.peekLast().filename);
        RULES_TO_TRACK.addAll(FILE_DATA.peekLast().data.keySet());
        log.warn("# Rules Tracking: " + RULES_TO_TRACK.size());

        for (Long rid: RULES_TO_TRACK) {
            Deque<Long> values =  getCounts(rid);
            Deque<Long> changes = getChanges(values);
            Double average = getAverage(changes);
            Long fifty = getMedian(changes);
            Long ninetynine = getPercentile(changes, .99);
            Double stdDev = getStandardDev(changes);

            if (stdDev > 4.0) {
                log.warn("RID: " + rid + " 50%: " + fifty + " 99%: " + ninetynine + " Average: " + df.format(average) + " StdDev: " + df.format(stdDev));
//                log.warn(changes);
            }

            if( rid.equals(TRACE) ) {
                log.warn("Counts: " + values);
                log.warn("Changes: " + changes);
                log.warn("Mean: " + average);
                log.warn("Median: " + fifty);
                log.warn("Standard Dev: " + stdDev);
            }
        }
    }

    public static Deque<Long> getCounts(Long rid) {
        Deque<Long> values = new LinkedList<>();
        for( FileData fd: FILE_DATA) {
            Long count = fd.data.get(rid);
            values.offerLast(count);
        }
        return values;
    }

    public static Deque<Long> getChanges(Deque<Long> values) {
        Deque<Long> changes = new LinkedList<>();

        Long oldValue = null;
        for( Long value: values ) {
            if(oldValue!=null) {
                Long diff = value-oldValue;
                changes.offerLast(diff);
            }
            oldValue = value;
        }
        return changes;
    }

    public static Long getMedian(Deque<Long> q) {
        return getPercentile(q, .5);
    }

    public static Long getPercentile(Deque<Long> q, double d) {
        List<Long> values = new ArrayList<>(q);
        Collections.sort(values);

        int offset = (int)(values.size()*d);
        return values.get(offset);
    }

    public static Double getStandardDev(Deque<Long> q) {
        Double mean = getAverage(q);
        double sum = 0;
        for(Long l: q) {
            double d = l - mean;
            double sq = d * d;
            sum += sq;
        }
        return Math.sqrt(sum / q.size());
    }

    public static Double getAverage(Deque<Long> q) {
        Long sum = 0l;

        for(Long v: q) {
            sum += v;
        }
        return (double)sum / ((double)q.size());
    }

    private static FileData processFile(File f) {
        FileData fd = new FileData();
        fd.filename = f.getName();
        fd.data = new HashMap<>();
        try {
            String s = FileUtils.readFileToString(f);
            String[] tuples = StringUtils.split(s, ";");

            for( String tuple: tuples) {
                if(StringUtils.isBlank(tuple)) {
                    continue;
                }
                String[] pair = StringUtils.split(tuple, "=");
                Long rid = Long.parseLong(pair[0]);
                Long count = Long.parseLong(CharSetUtils.keep(pair[1], "0-9"));

                if( rid.equals(TRACE)) {
                    log.warn("Reading " + f.getName() + "  count = " + count);
                }
                fd.data.put(rid, count);
            }
        } catch (IOException e) {
            ;
        }
        return fd;
    }

    private static Deque<File> getFiles() {
        Collection<File> filez = FileUtils.listFiles(new File(DIRECTORY), TrueFileFilter.INSTANCE, FalseFileFilter.INSTANCE);
        LinkedList<File> filez2 = new LinkedList<>();
        filez2.addAll(filez);
        Collections.sort(filez2, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return filez2;
    }

    static class FileData {
        String filename;
        Map<Long, Long> data;
    }
}
