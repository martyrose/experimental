package com.accertify.regression;

import com.accertify.genetic.OptimizerConstants;
import com.accertify.genetic.RulesManager;
import com.accertify.genetic.model.RulesTripped;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;

// TODO Cleanup and merge this with main genetic package
public class RulesStats {
    protected static transient Log log = LogFactory.getLog(RulesStats.class);



    public static void main(String[] args) {
        if( args.length != 2 ) {
            System.err.println("PROGGIE ordersFile rulesFile");
            System.exit(2);
        }
        String orderFile = args[0];
        String rulesFile = args[1];
        long start = System.currentTimeMillis();

        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(true); intFormat.setMaximumFractionDigits(0);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setGroupingUsed(true); currencyFormat.setMaximumFractionDigits(0);
        NumberFormat decimal2Format = NumberFormat.getNumberInstance();
        decimal2Format.setGroupingUsed(true); decimal2Format.setMaximumFractionDigits(2); decimal2Format.setMinimumFractionDigits(2);
        NumberFormat decimal6Format = NumberFormat.getNumberInstance();
        decimal6Format.setGroupingUsed(true); decimal6Format.setMaximumFractionDigits(6); decimal6Format.setMinimumFractionDigits(6);
        NumberFormat percent2Format = NumberFormat.getPercentInstance();
        percent2Format.setGroupingUsed(true); percent2Format.setMaximumFractionDigits(2); percent2Format.setMinimumFractionDigits(2); percent2Format.setMinimumIntegerDigits(1);
        NumberFormat percent6Format = NumberFormat.getPercentInstance();
        percent6Format.setGroupingUsed(true); percent6Format.setMaximumFractionDigits(6); percent6Format.setMinimumFractionDigits(6); percent6Format.setMinimumIntegerDigits(1);

        try {
            // This fetches all the rules that tripped from the orders file really fast
            RuleExistenceProcessor rep = new RuleExistenceProcessor();
            Files.readLines(new File(orderFile), Charset.defaultCharset(), rep);
            Set<Long> trippedRules = rep.getResult();
            log.warn("Tripping Rules: " + trippedRules.size());
//            log.warn("Tripping Rules: " + trippedRules);

            // Now process over it
            RuleStatsProcessor lp = new RuleStatsProcessor(trippedRules);
            Files.readLines(new File(orderFile), Charset.defaultCharset(), lp);
            final RuleStatsResult rsr = lp.getResult();

            Long totalCount = rsr.fraudCount+rsr.notFraudCount;
            Long totalValue = rsr.fraudValue+rsr.notFraudValue;

            log.warn("Total Count: " + intFormat.format(totalCount));
            log.warn("Total Value: " + currencyFormat.format(totalValue));

            log.warn("Fraud Count: " + intFormat.format(rsr.fraudCount));
            log.warn("Fraud Value: " + currencyFormat.format(rsr.fraudValue));

            log.warn("Not Fraud Count: " + intFormat.format(rsr.notFraudCount));
            log.warn("Not Fraud Value: " + currencyFormat.format(rsr.notFraudValue));

            log.warn("Fraud Rate By Count: " + percent2Format.format(divide(rsr.fraudCount, totalCount)));
            log.warn("Fraud Rate By Value: " + percent2Format.format(divide(rsr.fraudValue, totalValue)));

            log.warn("Average Fraud Value: " + currencyFormat.format(divide(rsr.fraudValue,rsr.fraudCount)));
            log.warn("Average Not Fraud Value: " + currencyFormat.format(divide(rsr.notFraudValue,rsr.notFraudCount)));


            final double fraudRate = divide(rsr.fraudCount, totalCount);
            log.warn("");
            log.warn("");
            log.warn("Fraud Rate By Count: " + percent2Format.format(fraudRate));
            log.warn("Skill: trippedFraudCount/trippedCount");
            log.warn("Effecitveness: (skill/fraudRate)-1");
            log.warn("");
            log.warn("");

            Map<Long,String> mm = RulesManager.getRuleDescriptions(rulesFile);

            double findMaxEffectivenessTripsOften = Double.MIN_VALUE;
            double closeEnough = 1.0d;
            while (findMaxEffectivenessTripsOften != Double.MIN_NORMAL && closeEnough > 0.0d) {
                for (Long rid : trippedRules) {
                    Long trippedCount = rsr.trippedFraudCount.get(rid) + rsr.trippedNotFraudCount.get(rid);
                    double tripRate = divide(trippedCount, totalCount);

                    double trippedFraudSkill = divide(rsr.trippedFraudCount.get(rid), rsr.trippedFraudCount.get(rid) + rsr.trippedNotFraudCount.get(rid));
                    double effectivedNess = (trippedFraudSkill / fraudRate) - 1.0d;

                    if (tripRate*closeEnough > fraudRate) {
                        if (findMaxEffectivenessTripsOften < effectivedNess) {
                            findMaxEffectivenessTripsOften = effectivedNess;
                        }
                    }
                }
                closeEnough = closeEnough - .05d;
            }

            if( findMaxEffectivenessTripsOften == Double.MIN_VALUE ) {
                log.warn("Your rules stink. Try again.");
                return;
            }
            log.warn("Max Effectiveness: " + decimal2Format.format(findMaxEffectivenessTripsOften));
            log.warn("");


            SortedSet<Long> sortedByEffectiveNess = new TreeSet<Long>(new Comparator<Long>() {
                @Override
                public int compare(Long r1, Long r2) {
                    double trippedFraudSkill1 = divide(rsr.trippedFraudCount.get(r1), rsr.trippedFraudCount.get(r1) + rsr.trippedNotFraudCount.get(r1));
                    double trippedFraudSkill2 = divide(rsr.trippedFraudCount.get(r2), rsr.trippedFraudCount.get(r2) + rsr.trippedNotFraudCount.get(r2));

                    double effectivedNess1 = trippedFraudSkill1 / fraudRate;
                    double effectivedNess2 = trippedFraudSkill2 / fraudRate;
                    if (effectivedNess1 == effectivedNess2) {
                        return (int) (r1 - r2);
                    }
                    return effectivedNess1 > effectivedNess2 ? 1 : -1;
                }
            });
            sortedByEffectiveNess.addAll(trippedRules);

            Map<Long, Integer> ruleToScores = new HashMap<Long, Integer>();
            int HIGH_SCORE = 1000;
            int MAX_SCORE = OptimizerConstants.MAX_RULE_SCORE;
            int MIN_SCORE = OptimizerConstants.MIN_RULE_SCORE;
            for(Long rid:sortedByEffectiveNess) {
                String ruleName = mm.get(rid);

                Long trippedCount = rsr.trippedFraudCount.get(rid) + rsr.trippedNotFraudCount.get(rid);

                double tripRate = divide(trippedCount, totalCount);
                double trippedFraudSkill = divide(rsr.trippedFraudCount.get(rid), rsr.trippedFraudCount.get(rid) + rsr.trippedNotFraudCount.get(rid));
                double effectiveNess = (trippedFraudSkill / fraudRate) - 1.0d;
                double effectiveNessMultiplier = effectiveNess/findMaxEffectivenessTripsOften;

                int score = (int)Math.round(Math.min(Math.max(HIGH_SCORE*effectiveNessMultiplier,MIN_SCORE), MAX_SCORE));

                ruleToScores.put(rid, score);

                log.warn("Rule: {0} - score {6} tripped {1} skill {4} effectiveness {5} (f : nf) ({2} : {3})",
                        StringUtils.leftPad(StringUtils.left(ruleName, 50), 50),
                        StringUtils.leftPad(percent2Format.format(divide(trippedCount, totalCount)), 7, " "),
                        StringUtils.leftPad(intFormat.format(rsr.trippedFraudCount.get(rid)), 10, " "),
                        StringUtils.leftPad(intFormat.format(rsr.trippedNotFraudCount.get(rid)), 10, " "),
                        StringUtils.leftPad(percent2Format.format(divide(rsr.trippedFraudCount.get(rid), rsr.trippedFraudCount.get(rid)+rsr.trippedNotFraudCount.get(rid))), 7, " "),
                        StringUtils.leftPad(decimal2Format.format(effectiveNess), 7, " "),
                        StringUtils.leftPad(intFormat.format(score), 6, " ")
                );

            }
            log.warn("");
            log.warn("");
            log.warn("Score Profile: " + RulesTripped.format(ruleToScores));
        } catch (IOException e) {
            log.error(e, e.getMessage());
        } finally {
            long end= System.currentTimeMillis();
            log.warn("Took: " + DurationFormatUtils.formatDurationHMS(end-start));
        }
    }

    private static double divide(long v1, long v2) {
        return ((double)v1)/((double)v2);
    }

    private static final Long ZERO = new Long(0l);
    private static final Long massage(Long l) {
        if( l == null ) {
            return ZERO;
        }
        return l;
    }
}
