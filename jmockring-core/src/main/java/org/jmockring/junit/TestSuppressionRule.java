/*
 * Copyright (c) 2013 by Cisco Systems, Inc.
 * All rights reserved.
 */

package org.jmockring.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @since 07/08/13
 */
public class TestSuppressionRule implements TestRule {

    private static final Logger log = LoggerFactory.getLogger(TestSuppressionRule.class);

    private SuppressionEvaluator evaluator;

    public TestSuppressionRule(SuppressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public Statement apply(Statement base, final Description description) {

        if (evaluator.isSuppressed()) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    description.addChild(Description.EMPTY);
                    log.info("LOG02060: Skipping test {}", description.getTestClass());
                }
            };
        }
        return base;
    }

}
