package com.mrose.jenetics;

import com.google.common.collect.Range;

import org.jenetics.BitChromosome;
import org.jenetics.BitGene;
import org.jenetics.GeneticAlgorithm;
import org.jenetics.Genotype;
import org.jenetics.Mutator;
import org.jenetics.NumberStatistics;
import org.jenetics.Optimize;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.SinglePointCrossover;
import org.jenetics.util.Factory;
import org.jenetics.util.Function;

/**
 * Created by martinrose on 11/4/14.
 */
public class OnesCounting {
  public static void main(String[] args) {
    Range<Long> r = Range.closed(0L, 0L);
    long l = r.lowerEndpoint();
    boolean val = r.contains(l);
    System.out.println("Contains: " + val);
  }

}

