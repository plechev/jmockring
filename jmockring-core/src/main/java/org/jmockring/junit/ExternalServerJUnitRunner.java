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

package org.jmockring.junit;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import org.jmockring.annotation.PartOfSuite;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.Servers;
import org.jmockring.spring.event.SpringEventSnooper;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Run tests with bootstrapped service environment.
 * <p/>
 * By using Server/Servers annotations we can start up any number of services
 * and execute a set of tests against them.
 * <p/>
 * <p/>
 * Usage of this runner activates the {@link PoshTestPostProcessor} which adds some convenience annotation processing for the test instances.
 *
 * @author Pavel Lechev
 * @see ServerBootstrap
 * @see org.jmockring.annotation.Server
 * @see org.jmockring.annotation.Servers
 * @see ExternalServerJUnitSuiteRunner
 * @see PoshTestPostProcessor
 * @see PartOfSuite
 * @since 19/07/12
 */
public class ExternalServerJUnitRunner extends BlockJUnit4ClassRunner implements ConfigurableTargetRunner {

    private static final Logger log = LoggerFactory.getLogger(ExternalServerJUnitRunner.class);

    private PoshTestPostProcessor postProcessor;

    /**
     * Keep track of all used mocks, so they can be reset when test method completes.
     * This is used for auto-mocked beans from the remote context as well as for
     * locally created mocks with @Mock/@Captor
     *
     * @see PoshTestPostProcessor#executeMockingProviderTestInstanceProcessing()
     */
    private Set<Object> usedMocks = new HashSet<Object>();

    private Class<?> suiteClass;

    private Object testInstance;

    /**
     * This is true if the class is actually being executed as part of a suite.
     */
    private boolean suiteRunning = false;

    private List<TestLifecycleListener> listeners;

    private SpringEventSnooper snooper;

    private List<TestRule> preExecutedRules = new ArrayList<TestRule>();

    /**
     * Creates a ExternalServerJUnitRunner to run {@code testClass}
     *
     * @throws org.junit.runners.model.InitializationError
     *          if the test class is malformed.
     */
    public ExternalServerJUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        this.suiteClass = validateAndReturnSuiteClass(testClass);
        this.postProcessor = new PoshTestPostProcessor(this);
        this.listeners = new ArrayList<TestLifecycleListener>();
        this.listeners.add(new MockManagerListener(this));
    }

    @Override
    public void run(RunNotifier notifier) {
        if (!suiteRunning) {
            // initiate bootstrap ONLY if not running as part of a suite.
            log.info("[BOOTSTRAP] Initiate single test class servers bootstrap for class '{}'.", getTestClass().getName());
            ServerBootstrap.ServersRunstateListener runstateListener = new ServerBootstrap(this).runAll();
            // This listener must be added only when executing outside the suite.
            this.listeners.add(runstateListener); // This will block the current thread until all servers are started
        }
        super.run(notifier);
    }

    @Override
    protected Object createTest() throws Exception {
        this.testInstance = super.createTest();
        return this.testInstance;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        // execute test method
        log.info("LOG00110: Running test method {}#{}", getTestClass().getName(), method.getName());
        super.runChild(method, notifier);
    }


    @Override
    protected List<TestRule> getTestRules(Object target) {
        List<TestRule> rules = super.getTestRules(target);
        for (TestRule preExecuted : preExecutedRules) {

        }

        rules.add(new TestWatcher() {
            @Override
            protected void starting(Description description) {
                LifecycleStatement.Phase.BEFORE.execute(listeners, testInstance);
            }

            @Override
            protected void finished(Description description) {
                LifecycleStatement.Phase.AFTER.execute(listeners, testInstance);
            }
        });
        return rules;
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement neighbour = super.withAfterClasses(statement);
        return new LifecycleStatement(neighbour, listeners, LifecycleStatement.Phase.AFTER_CLASS);
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement neighbour = super.withBeforeClasses(statement);
        return new LifecycleStatement(neighbour, listeners, LifecycleStatement.Phase.BEFORE_CLASS);
    }

    @Override
    public ParentRunner getRunner() {
        return this;
    }

    @Override
    public void addUsedMock(Object mock) {
        this.usedMocks.add(mock);
    }

    /**
     * @param testClass the currently executing test class.
     *
     * @return the suite class if valid one found or null if this class is not part of a suite.
     * @throws IllegalStateException if the suite configuration or any of the suite members aren't configured correctly.
     */
    static Class<?> validateAndReturnSuiteClass(Class<?> testClass) {
        PartOfSuite psa = testClass.getAnnotation(PartOfSuite.class);
        if (psa == null) {
            // the configuration is not declared as being part of a suite,
            // so we check ist own configuration, but abort further validation of the suite.

            // check this class for available Server configurations:
            if (testClass.getAnnotation(Server.class) == null && testClass.getAnnotation(Servers.class) == null) {
                throw new IllegalStateException(format("The test class '%s' is not part of a suite and doesn't have any '@%s' or '@%s' configurations.",
                        testClass.getName(),
                        Server.class.getSimpleName(),
                        Servers.class.getSimpleName()
                                                      ));
            }
            log.info("Configuration is OK for test class {}", testClass.getName());
            return null;
        }

        // proceed to validating the suite config
        Class<?> suiteClass = psa.value();
        Class[] suiteTestClasses = ExternalServerJUnitSuiteRunner.validateSuiteConfiguration(suiteClass);

        // The suite is OK, however just make sure this class is actually part of it.
        // This is not essential for running the configuration, but is included here
        // simply to ensure the whole palava of using the @PartOfSuite makes sense.
        if (!Arrays.asList(suiteTestClasses).contains(testClass)) {
            throw new IllegalStateException(String.format("The test class %s is not part of suite %s. Configure it in @%s.",
                    testClass.getName(),
                    suiteClass.getName(),
                    Suite.SuiteClasses.class.getName()
                                                         ));
        }
        log.info("Test class `{}` is attached to suite configuration from `{}`", testClass.getName(), suiteClass.getName());
        return suiteClass;
    }

    /**
     * Get instance of {@link org.junit.runners.model.TestClass} wrapping the suite class if available,
     * instead of the actual test class which is being executed.
     * <p/>
     * If the configuration is not part of a suite, then return the test class itself.
     *
     * @return the test class whose configuration is used to run the Jmockring tests
     * @see ConfigurableTargetRunner#getConfiguredTestClass()
     */
    @Override
    public TestClass getConfiguredTestClass() {
        return this.suiteClass != null ? new TestClass(this.suiteClass) : getTestClass();
    }

    public void setSuiteRunning(boolean suiteRunning) {
        this.suiteRunning = suiteRunning;
    }

    @Override
    public Object getTestInstance() {
        if (this.testInstance == null) {
            throw new IllegalStateException("JUnit hasn't created the test instance yet!");
        }
        return this.testInstance;
    }

    @Override
    public void addSnooper(SpringEventSnooper snooper) {
        this.snooper = snooper;
    }

    /**
     *
     */
    private static class MockManagerListener extends DefaultTestLifecycleListener {

        private ExternalServerJUnitRunner runner;

        private MockManagerListener(ExternalServerJUnitRunner runner) {
            this.runner = runner;
        }

        @Override
        public void beforeMethod(Object testInstance) {
            runner.postProcessor.postProcessTestClass();  // inject the remote mocks/beans/config only ONCE per class. Remote mocks will be reset automatically.
            runner.postProcessor.executeMockingProviderTestInstanceProcessing(); // inject fresh Mockito mocks & captors before each method call.
            resetRemoteState(); // first reset any remote state left after the previous method execution
        }

        @Override
        public void afterMethod(Object testInstance) {
            resetRemoteState(); // first reset any remote state left after the previous method execution
        }

        private void resetRemoteState() {
            if (runner.usedMocks.size() > 0) {
                log.info("LOG00220: Resetting {} used mocks: {}", runner.usedMocks.size(), Iterables.transform(runner.usedMocks, new Function<Object, Object>() {
                    @Override
                    public Object apply(@Nullable Object input) {
                        if (input != null) {
                            return input.getClass().getSimpleName();
                        }
                        return null;
                    }
                }));
                if (runner.postProcessor.getMockingProvider() != null) {
                    runner.postProcessor.getMockingProvider().resetMocks(runner.usedMocks);
                }
            }
            if (runner.snooper != null) {
                int usedSnooperDelegates = runner.snooper.delegateCount();
                if (usedSnooperDelegates > 0) {
                    log.info("LOG00030: Resetting ''The Snooper'' (clear {} delegate/s) ...", usedSnooperDelegates);
                    runner.snooper.clear();
                }
            }
        }
    }

}
