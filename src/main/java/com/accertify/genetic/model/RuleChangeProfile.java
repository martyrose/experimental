package com.accertify.genetic.model;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 5/11/11
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class RuleChangeProfile {
    public Long ruleId;
    public Integer oldScore;
    public Integer newScore;

    public Integer diff() {
        return newScore - oldScore;
    }
}
