/*
 * Copyright (c) 2013, Pavel Lechev
 *    All rights reserved.
 *
 *    Redistribution and use in source and binary forms, with or without modification,
 *    are permitted provided that the following conditions are met:
 *
 *     1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     2) Redistributions in binary form must reproduce the above copyright notice,
 *        this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     3) Neither the name of the Pavel Lechev nor the names of its contributors may be used to endorse or promote
 *        products derived from this software without specific prior written permission.
 *
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 *    INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *    IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jmockring.spi.provider.mockring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jmockring.spi.ExecutionHandler;

/**
 * Powermock provider - uses Mockito to create mocks and adds extra functionality around static method mocking, etc ...
 * This provider will be active only if Powermock and Mockito libs are both on the classpath.
 * <p/>
 * <p/>
 * WARNING:
 * In the list of providers keep this prover above the {@link MockitoMockingProvider} so it
 * can be discovered and activated before the Mockito lib is picked up.
 * <p/>
 * <p/>
 * Notes on bootstrapping PowerMock framework:
 * <p/>
 * In order to activate PowerMock without using the PowerMockRunner we use the PowerMockAgent facility, introduced in v 1.4.9.
 * To activate the agent the test class must declare a field for the JUnit rule of type <code>org.powermock.modules.junit4.rule.PowerMockRule</code>, annotate
 * it with {@link Rule} and initialise it. For example:
 * <p/>
 * <pre>
 * {@literal @Rule
 *   public PowerMockRule rule = new PowerMockRule();
 * }
 *   ...
 * </pre>
 * ^^ It is a JUnit requirement for the rule fields to be made <code>public</code>. ^^
 *
 * @author Pavel Lechev
 * @version 0.0.1
 * @date 28/02/13
 */
public class PowerMockitoMockingProvider extends MockitoMockingProvider {

    private static final Logger log = LoggerFactory.getLogger(PowerMockitoMockingProvider.class);

    private static final String POWERMOCKITO_CORE_CLASS_NAME = "org.powermock.api.mockito.PowerMockito";

    private static final String POWERMOCK_RULE_CLASS_NAME = "org.powermock.modules.junit4.rule.PowerMockRule";

    private static final String POWERMOCK_RUNNER_CLASS_NAME = "org.powermock.modules.junit4.PowerMockRunner";

    private static final String POWERMOCK_PREPAREFORTEST_ANNOTATION_CLASS_NAME = "org.powermock.core.classloader.annotations.PrepareForTest";

    private static final String POWERMOCK_AGENT_CLASS_NAME = "org.powermock.modules.agent.PowerMockAgent";

    @Override
    protected ExecutionHandler createMockingExecution() {
        return new ExecutionHandler(null, getMethod(POWERMOCKITO_CORE_CLASS_NAME, "mock", Class.class));
    }

    @Override
    public String providerName() {
        return "PowerMockito Framework";
    }

    @Override
    public boolean isActive() {
        try {
            return super.isActive() && loadClass(POWERMOCK_AGENT_CLASS_NAME) != null;
        } catch (RuntimeException e) {
            log.info("LOG00040: PowerMock agent is unavailable", e);
        }
        return false;
    }

    @Override
    public void processTestClass(Class testClass, Object testInstance) {
        super.processTestClass(testClass, testInstance);
        // check PowerMock status and see if we are ready to bootstrap the Java Agent via JUnit rules.
        if (enforcePowerMockRuleDeclared(testClass, testInstance)) {
            ExecutionHandler execution = new ExecutionHandler(null, getMethod(POWERMOCK_AGENT_CLASS_NAME, "initializeIfNeeded"));
            execution.execute();
            log.info("LOG00090: Calling PowerMock agent initialisation: {}", execution);
        }
    }


    /**
     * Check if the test class has the powermock PrepareForTest annotation and if so,
     * check and enforce the existence of the PowerMockRule.
     *
     * @param testClass
     *
     * @return TRUE if the rule is correctly defines, FALSE - if the rule is not required.
     * @throws IllegalStateException if the JUnit rule is not defined
     */
    private boolean enforcePowerMockRuleDeclared(Class testClass, Object testInstance) {
        Annotation prepForTest = testClass.getAnnotation(loadClass(POWERMOCK_PREPAREFORTEST_ANNOTATION_CLASS_NAME));
        if (prepForTest != null) {
            if (powerMockRunnerPresent(testClass)) {
                return false;
            }

            Class ruleClass = loadClass(POWERMOCK_RULE_CLASS_NAME);
            for (Field f : testClass.getDeclaredFields()) {
                if (f.getType().isAssignableFrom(ruleClass)) {
                    if (f.getAnnotation(Rule.class) != null) {
                        try {
                            if (f.get(testInstance) == null) {
                                throw new IllegalStateException(String.format("Rule field %s#%s is not initialised. See javadoc for %s", testClass.getName(), f.getName(), PowerMockitoMockingProvider.class.getName()));
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(String.format("Rule field %s#%s is not can not be accessed.", testClass.getName(), f.getName()), e);
                        }
                        log.info("LOG00070: Verified existence of rule '{}' in test class '{}'", ruleClass.getSimpleName(), testClass.getSimpleName());
                        return true;
                    }
                    throw new IllegalStateException(String.format("Missing @Rule annotation on field %s#%s. See javadoc for %s", testClass.getName(), f.getName(), PowerMockitoMockingProvider.class.getName()));
                }
            }
            throw new IllegalStateException(String.format("Missing %s rule in test class %s. See javadoc for %s", ruleClass.getName(), testClass.getName(), PowerMockitoMockingProvider.class.getName()));
        }
        return false;
    }

    /**
     * @param testClass
     *
     * @return
     */
    private boolean powerMockRunnerPresent(Class testClass) {
        Class superClass = testClass.getSuperclass();
        Class<? extends Runner> runnerClass = loadClass(POWERMOCK_RUNNER_CLASS_NAME);
        while (superClass != null) {
            if (superClass.getAnnotation(RunWith.class) != null
                    && ((RunWith) superClass.getAnnotation(RunWith.class)).value().isAssignableFrom(runnerClass)) {
                log.info("LOG00100: Found PowerMock runner is superclass {} ", superClass.getSimpleName());
                return true;
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }



}
