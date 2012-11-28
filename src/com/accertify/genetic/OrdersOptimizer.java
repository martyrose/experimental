package com.accertify.genetic;

import com.accertify.genetic.convert.ChromosomeConverter;
import com.accertify.genetic.fitness.BulkPercentileFitnessFunction;
import com.accertify.genetic.fitness.FitnessResult;
import com.accertify.genetic.fitness.PercentileFitnessFunction;
import com.accertify.genetic.model.OrdersCollection;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jgap.*;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.IntegerGene;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/9/11
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class OrdersOptimizer {
    protected static transient Log log = LogFactory.getLog(OrdersOptimizer.class);

   /**
     * The total number of times we'll let the population evolve.
     */

    public OrdersOptimizer() {
    }

    public Map<Integer, Integer> findFitSolution(OrdersCollection oc, Set<Integer> percentiles, String rulesFile, int populationSize, int evolveForMinutes, int parallelism, Map<Integer,Integer> existingSolution) {

        BulkPercentileFitnessFunction bulkFitness = null;
        try {
            OrdersCollectionRuleSetConfiguration ocrc = OrdersCollectionRuleSetConfiguration.calculateInPlaceRuleSetConfiguration(oc);
            Set<Integer> ruleIds = ocrc.getRuleConfigs().keySet();
            log.trace("RuleIds: " + ruleIds);

            // Setup our fitness functions
            FitnessFunction myFunc = new PercentileFitnessFunction(oc, percentiles, populationSize);
            bulkFitness = new BulkPercentileFitnessFunction(myFunc, parallelism);

            Genotype genotype = setupConfigurationWithFitness(populationSize, bulkFitness, ruleIds.size(), existingSolution);

            IChromosome bestSolution = setupAndEvolvePopulation(oc, rulesFile, percentiles, evolveForMinutes, genotype);

            return ChromosomeConverter.convert(bestSolution);
        } catch (InvalidConfigurationException e) {
            log.warn(e.getMessage());
        } finally {
            if( bulkFitness != null ) {
                bulkFitness.shutdown();
            }
        }
        return Collections.emptyMap();
    }

    private Genotype setupConfigurationWithFitness(int populationSize, BulkPercentileFitnessFunction bulkFitness, int numRules, Map<Integer,Integer> existingSolution) throws InvalidConfigurationException {// Start with a DefaultConfiguration, which comes setup with the
        // most common settings.
        // -------------------------------------------------------------
        Configuration conf = new DefaultConfiguration();
        conf.setPreservFittestIndividual(true);
        conf.setKeepPopulationSizeConstant(false);
        // TODO Look at what this should be
        conf.setSelectFromPrevGen(.5);

        // This will *invert* the fitness logic so that we see lower fitness values as better (ie they are a defect rate not a fitness rate)
        Configuration.resetProperty(Configuration.PROPERTY_FITEVAL_INST); // TODO WTF Is this a static reference!!!!!
        conf.setFitnessEvaluator(new DeltaFitnessEvaluator());

        // TODO How to set these?
        log.warn("Genetic Operators: " + conf.getGeneticOperators());

        log.warn("Select from previous gen: " + conf.getSelectFromPrevGen());
        log.warn("Preserve Fittest Individual: " + true);
        log.warn("Finding Scores in [{0},{1}]", OptimizerConstants.MIN_RULE_SCORE, OptimizerConstants.MAX_RULE_SCORE);
        
        conf.setBulkFitnessFunction(bulkFitness);

        // Setup our genes that we are going to use to mutate
        // and give it to the configuration as a sample to mutate
        Gene[] sampleGenes = new Gene[numRules];
        for(int ruleId=0; ruleId<numRules; ruleId++ ) {
            sampleGenes[ruleId] = new IntegerGene(conf, OptimizerConstants.MIN_RULE_SCORE, OptimizerConstants.MAX_RULE_SCORE);
        }

        IChromosome sampleChromosome = new Chromosome(conf, sampleGenes);
        conf.setSampleChromosome(sampleChromosome);

        conf.setPopulationSize(populationSize);

        Genotype genotype = Genotype.randomInitialGenotype(conf);
        if( !existingSolution.isEmpty() ) {
            log.warn("Setting one of the chromosomes to preExisting solution");
            ChromosomeConverter.populate(existingSolution, genotype.getPopulation().getChromosome(0));

            log.warn("Setting ALL of the chromosomes to preExisting solution");
            for( IChromosome chromosome : genotype.getPopulation().getChromosomes() ) {
                ChromosomeConverter.populate(existingSolution, chromosome);
            }
        }
        return genotype;
    }

    private IChromosome setupAndEvolvePopulation(final OrdersCollection oc, final String rulesFile, final Set<Integer> percentiles, int evolveForMinutes, Genotype genotype) throws InvalidConfigurationException {// Create random initial population of Chromosomes.
        // TODO Start with impl team initial rule set to start with a close solution (or optionally NOT)

        // Evolve the population. Since we don't know what the best answer
        // is going to be, we just evolve the max number of times.
        long iterations = 0;
        long startTime = System.currentTimeMillis();
        long lastEvolveStatusTime = startTime;
        long lastStatusTime = startTime;
        long lastSortAndRestore = startTime;
        while(System.currentTimeMillis()-startTime < evolveForMinutes* DateUtils.MILLIS_PER_MINUTE) {
            genotype.evolve();
            iterations++;
            long now = System.currentTimeMillis();

            if (System.currentTimeMillis() - lastEvolveStatusTime > 15*DateUtils.MILLIS_PER_SECOND) {
                lastEvolveStatusTime = System.currentTimeMillis();
                log.warn("Iterations {0} runtime {1}", iterations, DurationFormatUtils.formatDurationHMS(now-startTime));
            }

            final IChromosome bestSolutionSoFar = genotype.getFittestChromosome();

            if (System.currentTimeMillis() - lastStatusTime > DateUtils.MILLIS_PER_MINUTE) {
                lastStatusTime = now;
                new Thread(new Runnable() {
                    public void run() {
                        printCurrentBestFitness(rulesFile, ChromosomeConverter.convert(bestSolutionSoFar), oc, percentiles);
                        OrdersManager.scoreHistorgram(oc, ChromosomeConverter.convert(bestSolutionSoFar));
                    }
                }
                ).start();
            }

            if (System.currentTimeMillis() - lastSortAndRestore > 5*DateUtils.MILLIS_PER_MINUTE) {
                lastSortAndRestore = now;

                // Sorting is our *slow* part of this whole process
                // If every 10th iteration we sort the collection so the layout nearly matches what the best effort is, the sorts will go faster
                // and the evolving will go faster
                log.warn("Sorting and restoring original collection");
                OrdersManager.reSortAndStore(oc, ChromosomeConverter.convert(bestSolutionSoFar));
            }

            // Periodically dump full details
        }

        long endTime = System.currentTimeMillis();
        log.debug("Total evolution time: {0}", DurationFormatUtils.formatDurationHMS(endTime - startTime));

        // Save progress to file. A new run of this example will then be able to
        // resume where it stopped before! --> this is completely optional.
        // ---------------------------------------------------------------------

        // -----------------------------------
        IChromosome bestSolutionSoFar = genotype.getFittestChromosome();

        return bestSolutionSoFar;
    }

    private void printCurrentBestFitness(String rulesFile, Map<Integer, Integer> bestSolutionSoFar, OrdersCollection oc, Set<Integer> percentiles) {
        PercentileFitnessFunction fitness = new PercentileFitnessFunction(oc, percentiles, 1);
        FitnessResult fr = fitness.evaluateForFullFitness(bestSolutionSoFar);
        log.warn("Current Fitness Information " + fr.renderFitness());
        MainEntry.printCurrentBestSolutionDetail(rulesFile, bestSolutionSoFar, oc); // TODO This shouldn't be referencing MainEntry
    }
}
