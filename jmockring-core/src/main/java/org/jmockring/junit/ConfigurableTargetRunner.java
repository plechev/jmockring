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

import org.jmockring.spring.event.SpringEventSnooper;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.TestClass;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 04/01/13
 */
public interface ConfigurableTargetRunner<T> {

    /**
     * This is primarily used to get hold of the class which is annotated with the
     * {@link org.jmockring.annotation.Server} or {@link org.jmockring.annotation.Servers}
     * annotations, which are then used to create boostrap configurations.
     * <p/>
     * Do not use the returned class to manage tests execution in any way as it may not always reflect the real configuration
     * class currently being executed by JUnit (see {@link ExternalServerJUnitRunner#getConfiguredTestClass()}).
     *
     * @return
     * @see ServerBootstrap#startServers()
     * @see org.jmockring.configuration.ServerConfiguration
     */
    TestClass getConfiguredTestClass();

    /**
     * Convenience method to allow polymorphic access to the implementing runner.
     * (The {@link org.junit.runner.Runner} is a class so can not be extended by this interface)
     *
     * @return
     */
    ParentRunner getRunner();

    /**
     * Callback method to pass any mocks used in the tests.
     * <p/>
     * This allows automatic management of mocks state - reset/clear after each configuration method execution.
     * <p/>
     * This call does not validate whether the object passed is a real mock - the caller must have done that prior to this.
     *
     * @param mock
     */
    void addUsedMock(Object mock);

    /**
     * Return the instance of the currently executing test class.
     * Available only after JUnit calls {@link org.junit.runners.BlockJUnit4ClassRunner#createTest()}.
     *
     * @return
     * @throws IllegalStateException if the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest()} hasn't been called yet.
     */
    Object getTestInstance();

    /**
     * Add snooper so we can later clear all delegates.
     *
     * @param snooper
     */
    void addSnooper(SpringEventSnooper snooper);
}
