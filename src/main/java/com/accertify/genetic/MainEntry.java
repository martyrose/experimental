package com.accertify.genetic;

import com.accertify.genetic.fitness.FitnessResult;
import com.accertify.genetic.fitness.PercentileFitnessFunction;
import com.accertify.genetic.model.OrdersCollection;
import com.accertify.genetic.model.RuleChangeProfile;
import com.accertify.genetic.model.RuleIdTranslator;
import com.accertify.genetic.model.RulesTripped;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 6/22/11
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class MainEntry {
    protected static transient Log log = LogFactory.getLog(MainEntry.class);

    private static final String getArg(String[] args, int arg) {
        if( arg < args.length ) {
            return args[arg];
        }
        return null;
    }

    /**
     * Main method. A single command-line argument is expected, which is the
     * amount of change to create (in other words, 75 would be equal to 75
     * cents).
     *
     * @param args amount of change in cents to create
     * @throws Exception
     * @author Neil Rotstan
     * @author Klaus Meffert
     * @since 1.0
     */
    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();

        if( args.length < 7) {
            System.err.println("PROGGIE evolveTimeMin populationSize trainFile rulesFile parallelism percentiles existingSolutionFile");
            System.err.println("PROGGIE 60 50 /tmp/tesco.txt /tmp/rules.txt 6 99:95:90");
            System.exit(-1);
        }
        int evolveTimeMin = Integer.parseInt(getArg(args, 0));
        int maxPopulationSize = Integer.parseInt(getArg(args, 1));
        String trainFile = getArg(args, 2);
        String rulesFile = getArg(args, 3);

        int threads = Integer.parseInt(getArg(args, 4));
        String[] percentiles1 = StringUtils.split(getArg(args, 5), ":", 8);
        Set<Integer> temp = new HashSet<Integer>(percentiles1.length);
        for(int i=0; i<percentiles1.length; i++) {
            temp.add(Integer.parseInt(percentiles1[i]));
        }
        Set<Integer> percentiles = Collections.unmodifiableSet(temp);
        String existingSolutionFile = getArg(args, 6);

        OrdersOptimizer oz = new OrdersOptimizer();

        RuleIdTranslator ridTranslator = new RuleIdTranslator(200);
        OrdersCollection oc = OrdersManager.getOrdersFromFile(trainFile, ridTranslator);

        if(oc == null || oc.size() == 0) {
            System.err.println("No Orders Found");
            System.exit(-1);
        }

        log.warn("Population Size " + maxPopulationSize);
        log.warn("Evolving for: " + DurationFormatUtils.formatDurationHMS(evolveTimeMin * DateUtils.MILLIS_PER_MINUTE));
        log.warn("Training Against " + trainFile);
        log.warn("Rules Definition " + rulesFile);
        SortedSet<Integer> ss = new TreeSet<Integer>(percentiles);
        log.warn("Optimization Percentiles: " + Arrays.asList(ss));

        Map<Long, Integer> preExistingSolution = Collections.emptyMap();
        Map<Integer, Integer> mappedExistingSolution = Collections.emptyMap();

        // TODO This should be a method call
        // TODO Check if file exists
        if (StringUtils.isNotBlank(existingSolutionFile)) {
            String rulesTrippedStr = FileUtils.readFileToString(new File(existingSolutionFile), "UTF-8");
            preExistingSolution = RulesTripped.parse(rulesTrippedStr);
            log.warn("Loading Pre-Existing Solution From {0} Entries {1}", existingSolutionFile, preExistingSolution.size());

            if( preExistingSolution.size() != ridTranslator.size() ) {
                if( preExistingSolution.size() < ridTranslator.size()) {
                    for(Long ruleId: ridTranslator.getRuleKeys()) {
                        if(!preExistingSolution.containsKey(ruleId) ) {
                            log.warn("Missing Rule {0}", String.valueOf(ruleId));
                            preExistingSolution.put(ruleId, 0);
                        }
                    }
                }
                if( preExistingSolution.size() > ridTranslator.size() ) {
                    Set<Long> knownKeys = ridTranslator.getRuleKeys();
                    for(Long ruleId: preExistingSolution.keySet()) {
                        if( !knownKeys.contains(ruleId)) {
                            log.warn("PreExisting Solution has defintion for rule {0} which isn't in the data", String.valueOf(ruleId));
                        }
                    }
                }
            }
        }

        if (!preExistingSolution.isEmpty()) {
            mappedExistingSolution = ridTranslator.toIntIds(preExistingSolution);
        }

        // TODO Push all this other intersting stuff into an async process that works off a temp set
        // TODO Move reporting out of the orders optimize path
        long start = System.currentTimeMillis();
        Map<Integer, Integer> intRuleIdMapping = oz.findFitSolution(oc, percentiles, rulesFile, maxPopulationSize, evolveTimeMin, threads, mappedExistingSolution);
        long end = System.currentTimeMillis();

        log.warn("Evolution Took " + DurationFormatUtils.formatDurationHMS(end-start));

        printCurrentBestSolutionSummary(intRuleIdMapping, oc, percentiles);
        printCurrentBestSolutionDetail(rulesFile, intRuleIdMapping, oc);

        oc.clear();

        Map<Long, Integer> longRuleIdMapping = ridTranslator.toLongIds(intRuleIdMapping);
        log.warn("Solution: " + RulesTripped.format(longRuleIdMapping));
    }

    private static void printCurrentBestSolutionSummary(Map<Integer, Integer> fitSolution, OrdersCollection oc, Set<Integer> percentiles) {
        RuleIdTranslator ridTranslator = oc.getRuleIdTranslator();
        OrdersCollectionRuleSetConfiguration originalRuleSetConfiguration = OrdersCollectionRuleSetConfiguration.calculateInPlaceRuleSetConfiguration(oc);
        Map<Integer, Integer> existingRulesSolution = originalRuleSetConfiguration.getSampleScoreProfile();

        Set<Integer> missingKeysInFitSolution = new HashSet<Integer>(existingRulesSolution.keySet());
        missingKeysInFitSolution.removeAll(fitSolution.keySet());

        if( !missingKeysInFitSolution.isEmpty() ) {
            for(Integer missingKey:missingKeysInFitSolution) {
                log.warn("Adding in missing key " + ridTranslator.getLongRuleId(missingKey) + " score = " + existingRulesSolution.get(missingKey) + " -> 0");
                fitSolution.put(missingKey, existingRulesSolution.get(missingKey));

                // If there are *new* keys, neither we get to use it nor the existing profile
                fitSolution.put(missingKey, 0);
                existingRulesSolution.put(missingKey, 0);
            }
        }

        PercentileFitnessFunction fitness = new PercentileFitnessFunction(oc, percentiles, 1);
        FitnessResult oldFitnessResult = fitness.evaluateForFullFitness(existingRulesSolution);
        FitnessResult newFitnessResult = fitness.evaluateForFullFitness(fitSolution);

        log.warn("==========================================");
        log.warn("Existing " + oldFitnessResult.renderFitness());
        log.warn("Proposed " + newFitnessResult.renderFitness());
        log.warn("==========================================");

        for(Integer percentile:percentiles) {
            double percentLess = 1- ((double)newFitnessResult.getPercentileFitness().get(percentile)) / ((double)oldFitnessResult.getPercentileFitness().get(percentile));
            log.warn(percentile + "% went from {0} to {1} we will review {2} % less", oldFitnessResult.getPercentileFitness().get(percentile), newFitnessResult.getPercentileFitness().get(percentile), percentLess*100d);
        }

    }

    public static void printCurrentBestSolutionDetail(String rulesFileLocation, Map<Integer, Integer> fitSolution, OrdersCollection oc) {
        OrdersCollectionRuleSetConfiguration originalRuleSetConfiguration = OrdersCollectionRuleSetConfiguration.calculateInPlaceRuleSetConfiguration(oc);
        List<RuleChangeProfile> lrcp = generateRuleChangeProfiles(fitSolution, originalRuleSetConfiguration, oc.getRuleIdTranslator());

        Map<Long, String> ruleTranslation = RulesManager.getRuleDescriptions(rulesFileLocation);

        // Now sor those entries by the absolute change in value
        Collections.sort(lrcp, new Comparator<RuleChangeProfile>() {
            public int compare(RuleChangeProfile o1, RuleChangeProfile o2) {
                return o1.newScore.compareTo(o2.newScore);
            }
        });

        log.warn("==========================================");
        log.warn("Ordered by new score");
        for(RuleChangeProfile entry: lrcp) {
            Long ruleId = entry.ruleId;
            log.warn("\tRule {0} {1}    Original Score {2}     New Score {3}     Diff {4}", String.valueOf(ruleId), StringUtils.rightPad(StringUtils.left(ruleTranslation.get(ruleId), 50), 50), StringUtils.leftPad(String.valueOf(entry.oldScore), 6), StringUtils.leftPad(String.valueOf(entry.newScore), 6), StringUtils.leftPad(String.valueOf(entry.diff()), 6));
        }
        log.warn("==========================================");
    }

    private static List<RuleChangeProfile> generateRuleChangeProfiles(Map<Integer, Integer> fitSolution, OrdersCollectionRuleSetConfiguration originalRuleSetConfiguration, RuleIdTranslator ridTranslator) {
        Map<Integer, Integer> originalSolution = originalRuleSetConfiguration.getSampleScoreProfile();

        List<RuleChangeProfile> lrcp = new ArrayList<RuleChangeProfile>(fitSolution.size());
        for( Integer ruleId:fitSolution.keySet()) {
            RuleChangeProfile rcp = new RuleChangeProfile();
            rcp.ruleId = ridTranslator.getLongRuleId(ruleId);
            rcp.oldScore = originalSolution.get(ruleId);
            rcp.newScore = fitSolution.get(ruleId);
            lrcp.add(rcp);
        }
        return lrcp;
    }
}
