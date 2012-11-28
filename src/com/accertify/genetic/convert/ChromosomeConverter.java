package com.accertify.genetic.convert;

import org.jgap.Chromosome;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.IntegerGene;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/11/11
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChromosomeConverter {

    public static final Map<Integer, Integer> convert(IChromosome chromo) {
        // Now build up a scoring evaluation set based off the chromosome
        Map<Integer, Integer> scoreMapping = new HashMap<Integer, Integer>(chromo.getGenes().length);

        Gene[] genes = chromo.getGenes();
        for( int ruleId=0; ruleId<genes.length; ruleId++) {
            scoreMapping.put(ruleId, (Integer)genes[ruleId].getAllele());
        }
        return scoreMapping;
    }

    public static final int[] convertToArray(IChromosome chromo) {
        Gene[] genes = chromo.getGenes();
        // CACHE THESE?!?!?
        int[] scores = new int[genes.length];
        for( int i=0; i<genes.length; i++) {
            scores[i] = (Integer)genes[i].getAllele();
        }

        return scores;
    }

    public static final void populate(Map<Integer, Integer> scoreProfile, IChromosome chromo) {
        Gene[] genes = chromo.getGenes();
        for( int ruleId=0; ruleId<genes.length; ruleId++) {
            ((IntegerGene)genes[ruleId]).setAllele(scoreProfile.get(ruleId));
        }
    }
}
