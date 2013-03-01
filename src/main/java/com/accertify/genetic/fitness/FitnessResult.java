package com.accertify.genetic.fitness;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/11/11
 * Time: 10:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class FitnessResult {
    private final double fitness;
    private final Map<Integer, Integer> percentileFitness;

    public FitnessResult(double fitness, Map<Integer, Integer> percentileFitness) {
        this.fitness = fitness;
        this.percentileFitness = Collections.unmodifiableMap(percentileFitness);
    }

    public double getFitness() {
        return fitness;
    }

    public Map<Integer, Integer> getPercentileFitness() {
        return percentileFitness;
    }

    public String renderFitness() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageFormat.format("Fitness Values is {0}", getFitness()));
        for (Integer percentile : getPercentileFitness().keySet()) {
            sb.append(MessageFormat.format("     {0}%={1}", percentile, getPercentileFitness().get(percentile)));
        }
        return sb.toString();
    }
}
