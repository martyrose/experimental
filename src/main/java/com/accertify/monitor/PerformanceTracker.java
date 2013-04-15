package com.accertify.monitor;

import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class which lets us track values historical as well as the current X interval and the previous X interval
 */
public class PerformanceTracker {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentHistogram.class);

    private final long[] upperBounds;
    private final long intervalTimeNanos;

    private  final AtomicLong currentSince;
    private final AtomicReference<ConcurrentHistogram> current;
    private final AtomicReference<ConcurrentHistogram> previous;
    private final AtomicReference<ConcurrentHistogram> historical;

    enum TimePeriod {
        CURRENT(new Supplier<ConcurrentHistogram>() {
            @Override
            public ConcurrentHistogram get() {
                return current.get();
            }
        }),
        PREVIOUS(new Supplier<ConcurrentHistogram>() {
            @Override
            public ConcurrentHistogram get() {
                return previous.get();
            }
        }),
        HISTORICAL(new Supplier<ConcurrentHistogram>() {
            @Override
            public ConcurrentHistogram get() {
                return historical.get();
            }
        });

        private Supplier<ConcurrentHistogram> supplier;

        TimePeriod(Supplier<ConcurrentHistogram> supplier) {
            this.supplier = supplier;
        }


        private ConcurrentHistogram getHistogram() {
            return supplier.get();
        }
    }

    public PerformanceTracker(final long[] upperBounds, long period, TimeUnit unit) {
        this.upperBounds = upperBounds;
        this.currentSince = new AtomicLong(System.nanoTime());
        this.current = new AtomicReference<>(new ConcurrentHistogram(upperBounds));
        this.previous = new AtomicReference<>(new ConcurrentHistogram(upperBounds));
        this.historical = new AtomicReference<>(new ConcurrentHistogram(upperBounds));

        this.intervalTimeNanos = unit.toNanos(period);
    }

    private void swap() {
        long now = System.nanoTime();
        long currentSinceValue = currentSince.get();
        if( now - currentSinceValue > intervalTimeNanos ) {
            if( currentSince.compareAndSet(currentSinceValue, now) ) {
                log.warn("Swapping");
                ConcurrentHistogram newPrevious = current.get();
                current.set(new ConcurrentHistogram(upperBounds));
                previous.set(newPrevious);
            }
        }
    }

    public void addObservation(long value) {
        swap();

        current.get().addObservation(value);
        historical.get().addObservation(value);
    }

    public double getCurrentRate() {
        return 0.0d;
    }

//    public long getUpperBoundForFactor(double d) {
//
//    }
}
