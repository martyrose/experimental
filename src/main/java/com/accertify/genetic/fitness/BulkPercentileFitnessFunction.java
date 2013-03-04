package com.accertify.genetic.fitness;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.jgap.BulkFitnessFunction;
import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.Population;

import java.util.concurrent.*;

/**
 * This abstract class should be extended and the evaluateChromosomes() method implemented to evaluate each of the Chromosomes given in an array and set their fitness values prior to returning.
 */
public class BulkPercentileFitnessFunction extends BulkFitnessFunction {
    protected static transient Log log = LogFactory.getLog(BulkPercentileFitnessFunction.class);

    private final FitnessFunction fitnessFunction;
    private final ThreadPoolExecutor tpe;
    private final int threads;

    public BulkPercentileFitnessFunction(FitnessFunction ff, int threads) {
        this.fitnessFunction = ff;
        this.threads = threads;
        int numProcessors = Runtime.getRuntime().availableProcessors();
//        int threads = Math.max(numProcessors-1, 1);
//        threads = 1;

        log.warn("Found {0} procesors     Starting {1} threads", numProcessors, threads);
        this.tpe = new ThreadPoolExecutor(threads, threads, Long.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory());
    }

    @Override
    public void evaluate(Population a_chromosomes) {
        int chromosomesCalculate = a_chromosomes.getChromosomes().size();
        CountDownLatch latch = new CountDownLatch(chromosomesCalculate);

        log.debug("Calculating Fitness for {0} chromosomes ", chromosomesCalculate);
        for(IChromosome chromosome: a_chromosomes.getChromosomes() ) {
            CalculateFitness runnable = new CalculateFitness(fitnessFunction, chromosome, latch);
            tpe.execute(runnable);
        }

        while (latch.getCount() > 0) {
            try {
                latch.await(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
            }
            log.debug("{0} fitness(s) to calculate", latch.getCount());
        }
    }

    @Override
    public Object clone() {
        return new BulkPercentileFitnessFunction(fitnessFunction, threads);
    }

    public void shutdown() {
        if( tpe != null ) {
            tpe.shutdownNow();
        }
    }

    private class CalculateFitness implements Runnable {
        private FitnessFunction fitnessFunction;
        private IChromosome chromosome;
        private CountDownLatch cdlatch;

        CalculateFitness(FitnessFunction fitnessFunction, IChromosome chromosome, CountDownLatch cdlatch) {
            this.fitnessFunction = fitnessFunction;
            this.chromosome = chromosome;
            this.cdlatch = cdlatch;
        }

        public void run() {
            try {
                double fitness = fitnessFunction.getFitnessValue(chromosome);
                log.debug("Calculated Fitness to be: " + fitness);
                chromosome.setFitnessValue(fitness);
            } finally {
                cdlatch.countDown();
            }
        }
    }
}
