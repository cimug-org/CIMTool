/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.engine;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;

public class DefaultRuleListener implements RuleListener {

	private String name;
	private int ruleInvocations = 0;
	private int rulesEvaluatedToTrue = 0;
	private int rulesEvaluatedToFalse = 0;
	private int rulesFailedWithExceptions = 0;

	public DefaultRuleListener(String name) {
		this.name = name;
	}

	@Override
	public boolean beforeEvaluate(Rule rule, Facts facts) {
		return RuleListener.super.beforeEvaluate(rule, facts);
	}

	@Override
	public void afterEvaluate(Rule rule, Facts facts, boolean evaluationResult) {
		if (evaluationResult)
			rulesEvaluatedToTrue++;
		else
			rulesEvaluatedToFalse++;
	}

	@Override
	public void onEvaluationError(Rule rule, Facts facts, Exception exception) {
		exception.printStackTrace(System.err);
		rulesFailedWithExceptions++;
	}

	/**
	 * Method invoked before the call to createValidationResult()
	 */
	@Override
	public void beforeExecute(Rule rule, Facts facts) {
		this.ruleInvocations++;
	}

	/**
	 * Method invoked upon a successful call to Execute (i.e. to createValidationResult())
	 */
	@Override
	public void onSuccess(Rule rule, Facts facts) {
		RuleListener.super.onSuccess(rule, facts);
	}

	@Override
	public void onFailure(Rule rule, Facts facts, Exception exception) {
		exception.printStackTrace(System.err);
		rulesFailedWithExceptions++;
	}

	@Override
	public String toString() {
		String msg = String.format("DefaultRuleListener '%s' [\n ruleInvocations=%d,\n rulesEvaluatedToTrue=%d,\n rulesEvaluatedToFalse=%d,\n rulesFailedWithExceptions=%d\n]", name, ruleInvocations, rulesEvaluatedToTrue, rulesEvaluatedToFalse, rulesFailedWithExceptions);
		return msg;
	}

}
