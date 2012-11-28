package com.accertify.genetic.fitness;

import com.accertify.genetic.comparator.OrderRowComparator;
import com.accertify.genetic.comparator.OrderRowComparator2;
import com.accertify.genetic.comparator.OrderRowComparator3;
import com.accertify.genetic.convert.ChromosomeConverter;
import com.accertify.genetic.model.OrderRow;
import com.accertify.genetic.model.OrdersCollection;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.accertify.util.SimpleHashSet;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jgap.CachedFitnessFunction;
import org.jgap.IChromosome;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/9/11
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class PercentileFitnessFunction extends CachedFitnessFunction {
    protected static transient Log log = LogFactory.getLog(PercentileFitnessFunction.class);
//
//    public static final Integer NINETY_NINE = 99;
//    public static final Integer NINETY_FIVE = 95;
//    public static final Integer NINETY = 90;
//    public static final Set<Integer> DEFAULT_PERCENTILES = Collections.unmodifiableSet(new SimpleHashSet<Integer>(NINETY_NINE, NINETY_FIVE, NINETY));

    private final OrdersCollection orders;
    private final Set<Integer> percentiles;

    // This is used via the critical path
    @SuppressWarnings("unchecked")
    public PercentileFitnessFunction(OrdersCollection oc, Set<Integer> percentiles, int populationSize) {
        super((java.util.Map<java.lang.String,java.lang.Double>)Collections.synchronizedMap(new LRUMap(populationSize*4)));
        this.orders = oc;
        this.percentiles = percentiles;
    }

    /**
     * Determine the fitness of the given Chromosome instance. The higher the
     * return value, the more fit the instance. This method should always
     * return the same fitness value for two equivalent Chromosome instances.
     */
    @Override
    public double evaluate(IChromosome a_subject) {
        return evaluateForFullFitness(a_subject).getFitness();
    }

    public FitnessResult evaluateForFullFitness(IChromosome a_subject) {
        // convert the chromosome's array based view of the rule->score structure to a more convenient map
        int[] scores = ChromosomeConverter.convertToArray(a_subject);
        return ievaluateForFullFitness(scores, null);
    }

    public FitnessResult evaluateForFullFitness(Map<Integer, Integer> scoreMapping) {
        return ievaluateForFullFitness(null, scoreMapping);
    }

    /*
        It is imperitive that this stays thread safe
     */
    private FitnessResult ievaluateForFullFitness(int[] scores, Map<Integer, Integer> scoreMapping) {
        OrderRowComparator orc = null;
        if( scores == null ) {
            log.debug("slow path");
            orc = new OrderRowComparator2(scoreMapping);
        } else {
            log.debug("fast path");
            orc = new OrderRowComparator3(scores);
        }
        OrderRow[] ordersDefaultSorted = orders.getOrdersArray();
        long i1 = System.currentTimeMillis();

        Arrays.sort(ordersDefaultSorted, Collections.reverseOrder(orc));
        long i2 = System.currentTimeMillis();
        OrderRow[] sortedOrders = ordersDefaultSorted;

        log.debug("Sorting: " + DurationFormatUtils.formatDurationHMS(i2 - i1));
        log.debug("Comparisons: {0}", orc.getComparisonsMade());

        int reviewsNeeded = 0;
        Map<Integer, Integer> percentileFitness = new HashMap<Integer, Integer>();

        for(Integer percentile:percentiles) {
            double pp = ((double)percentile) / 100d;
            int howMuchFraudsIsPercentile = (int) Math.floor((double) orders.getFraudValue() * pp);
            int foundThatMuchFraudThroughPosition = findPositionOfNthFraudValue(sortedOrders, howMuchFraudsIsPercentile);

            reviewsNeeded = reviewsNeeded + foundThatMuchFraudThroughPosition;
            percentileFitness.put(percentile, foundThatMuchFraudThroughPosition);
        }

        // Give a bonus to those scoring less than
        int bonus = 0;
        if( scores == null ) {
            bonus = rulesScoringLessThanX(scoreMapping.values(), 0);
        } else {
            bonus = rulesScoringLessThanX(scores, 0);
        }

        int fitness = reviewsNeeded*10 - bonus;

        FitnessResult fr = new FitnessResult(fitness, percentileFitness);
        log.debug("Fitness is " + fitness);

        return fr;
    }

    private int findPositionOfNthFraudValue(OrderRow[] lor, int fraudValueToFind) {
        int rowsLookedAt = 0;
        int fraudValueFound = 0;

        for(OrderRow or: lor) {
            rowsLookedAt++;
            if( or.isFraud ) {
                fraudValueFound = fraudValueFound + or.monetary;
            }
            if( fraudValueFound >= fraudValueToFind) {
                break;
            }
        }
        return rowsLookedAt;
    }

    private int rulesScoringLessThanX(Iterable<Integer> scores, int threshold) {
        int counter = 0;

        for(int score: scores ) {
            if( Math.abs(score) <= threshold) {
                counter++;
            }
        }
        return counter;
    }
    private int rulesScoringLessThanX(int[] scores, int threshold) {
        int counter = 0;

        for(int score: scores ) {
            if( Math.abs(score) <= threshold) {
                counter++;
            }
        }
        return counter;
    }

}
