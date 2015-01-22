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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jmockring.annotation.PartOfSuite;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.Servers;
import org.jmockring.spring.event.SpringEventSnooper;
import org.junit.internal.builders.IgnoredClassRunner;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the JUnit {@link Suite} runner allowing grouping of tests which require the same server executions.
 * <p/>
 * Does <u>exactly</u> the same job as {@link ExternalServerJUnitRunner} but for a suite of tests.
 * <p/>
 * This allows starting up all servers, run a set of integration tests against them, then shut the servers down at the end.
 * <p/>
 *
 * @author Pavel Lechev
 * @date 20/07/12
 * @see ServerBootstrap
 * @see ExternalServerJUnitRunner
 * @see PartOfSuite
 * @see org.jmockring.annotation.Servers
 * @see org.jmockring.annotation.Server
 */
public class ExternalServerJUnitSuiteRunner extends Suite implements ConfigurableTargetRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalServerJUnitSuiteRunner.class);

    private Set<Object> usedMocks = new HashSet<Object>();

    private List<TestLifecycleListener> listeners;

    private SpringEventSnooper snooper;

    public ExternalServerJUnitSuiteRunner(Class<?> suiteClass, RunnerBuilder builder) throws InitializationError {
        super(suiteClass, builder);
        validateSuiteConfiguration(suiteClass);
        this.listeners = new ArrayList<TestLifecycleListener>();
    }

    /**
     * Perform validation of the suite class configuration and return list of the test classes part of this suite.
     *
     * @param suiteClass the configuration suite class, annotated with {@link Suite.SuiteClasses}
     *
     * @return the suite member classes
     * @throws IllegalStateException if the suite configuration or any of the suite members aren't configured correctly.
     */
    static Class[] validateSuiteConfiguration(Class<?> suiteClass) {
        // check the suite class has the correct runner.
        RunWith rw = suiteClass.getAnnotation(RunWith.class);
        Class<?> foundRunner = null;
        if (rw == null
                || ((foundRunner = rw.value()) != ExternalServerJUnitSuiteRunner.class)) {
            throw new IllegalStateException(format("The suite class '%s' doesn't have the required runner. Expected '%s', but found '%s'.",
                    suiteClass.getName(),
                    ExternalServerJUnitSuiteRunner.class.getSimpleName(),
                    foundRunner != null ? foundRunner.getSimpleName() : "[none]"
            ));
        }
        // check for available Server configurations
        if (suiteClass.getAnnotation(Server.class) == null && suiteClass.getAnnotation(Servers.class) == null) {
            throw new IllegalStateException(format("The suite class '%s' doesn't have any '@%s' or '@%s' configurations.",
                    suiteClass.getName(),
                    Server.class.getSimpleName(),
                    Servers.class.getSimpleName()
            ));
        }
        // has this suite have any test classes in it ?
        Suite.SuiteClasses scs = suiteClass.getAnnotation(Suite.SuiteClasses.class);
        Class<?>[] suiteTestClasses;
        if (scs == null || (((suiteTestClasses = scs.value()).length == 0))) {
            throw new IllegalStateException(format("The suite class '%s' doesn't have any test classes. Configure '@%s'.",
                    suiteClass.getName(),
                    Suite.SuiteClasses.class.getName()
            ));
        }
        // it has classes -> check if they all have @PartOfSuite and ExternalServerJUnitRunner configured
        for (Class<?> suiteMemberClass : suiteTestClasses) {
            RunWith mrw = suiteMemberClass.getAnnotation(RunWith.class);
            if (mrw == null || mrw.value() != ExternalServerJUnitRunner.class) {
                throw new IllegalStateException(format(
                        "Suite member class '%s' does not have the runner '%s' configured. Consider using '@RunWith(%s.class)'.",
                        suiteMemberClass.getName(),
                        ExternalServerJUnitRunner.class.getName(),
                        ExternalServerJUnitRunner.class.getSimpleName()
                ));
            }
            PartOfSuite psa = suiteMemberClass.getAnnotation(PartOfSuite.class);
            if (psa == null || psa.value() != suiteClass) {
                throw new IllegalStateException(format(
                        "Suite member class '%1$s' does not have the correct @%2$s annotation. Consider using '@%2$s(%3$s.class)'.",
                        suiteMemberClass.getName(),
                        PartOfSuite.class.getSimpleName(),
                        suiteClass.getSimpleName()
                ));
            }
        }
        LOG.info("Configuration is OK for suite class {}", suiteClass.getName());
        return suiteTestClasses;
    }

    @Override
    public void run(RunNotifier notifier) {
        LOG.info("[BOOTSTRAP] Initiate configuration suite servers bootstrap for suite '{}'.", getTestClass().getName());
        ServerBootstrap.ServersRunstateListener runstateListener = new ServerBootstrap(this).runAll();
        // This listener must be added only when executing outside the suite.
        listeners.add(runstateListener);
        super.run(notifier);
    }

    @Override
    public TestClass getConfiguredTestClass() {
        return getTestClass();
    }

    /**
     * Get a chance to customise the runners before they kick in.
     *
     * @return
     */
    @Override
    protected List<Runner> getChildren() {
        List<Runner> children = super.getChildren();
        for (Runner runner : children) {
            if (runner.getClass() == ExternalServerJUnitRunner.class) {
                ((ExternalServerJUnitRunner) runner).setSuiteRunning(true);
            } else if (runner.getClass() == IgnoredClassRunner.class
                    || runner.getClass() == ErrorReportingRunner.class) {
                continue; // test class is annotated with @Ignored or test startup error
            } else {
                throw new IllegalStateException("Not a valid runner in children collection. Expected ExternalServerJUnitRunner");
            }
        }
        return children;
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

    @Override
    public void addSnooper(SpringEventSnooper snooper) {
        this.snooper = snooper;
    }

    @Override
    public Object getTestInstance() {
        throw new IllegalStateException("Method not implemented by the test suite runner!");
    }
}
