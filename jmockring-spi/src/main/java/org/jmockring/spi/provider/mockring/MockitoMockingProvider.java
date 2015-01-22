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

import org.jmockring.spi.ExecutionHandler;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @version 0.0.1
 * @date 28/02/13
 */
public class MockitoMockingProvider extends AbstractMockingProvider {

    private static final String MOCKITO_UTIL_CLASS_NAME = "org.mockito.internal.util.MockUtil";

    private static final String MOCKITO_CORE_CLASS_NAME = "org.mockito.Mockito";

    private static final String DEFAULT_ANNOTATION_ENGINE_CLASS_NAME = "org.mockito.internal.configuration.DefaultAnnotationEngine";

    @Override
    protected ExecutionHandler createMockingExecution() {
        return new ExecutionHandler(null, getMethod(MOCKITO_CORE_CLASS_NAME, "mock", Class.class));
    }

    @Override
    protected ExecutionHandler createCheckMockExecution() {
        return new ExecutionHandler(newInstance(loadClass(MOCKITO_UTIL_CLASS_NAME)),
                getMethod(MOCKITO_UTIL_CLASS_NAME, "isMock", Object.class));
    }

    @Override
    protected ExecutionHandler createProcessTestClassExecution() {
        return new ExecutionHandler(newInstance(loadClass(DEFAULT_ANNOTATION_ENGINE_CLASS_NAME)),
                getMethod(DEFAULT_ANNOTATION_ENGINE_CLASS_NAME, "process", Class.class, Object.class));
    }

    @Override
    protected ExecutionHandler createResetMocksExecution() {
        return new ExecutionHandler(null, getMethod(MOCKITO_CORE_CLASS_NAME, "reset", Object[].class));
    }

    @Override
    public String providerName() {
        return "Mockito Framework";
    }

    @Override
    public String toString() {
        return providerName();
    }
}
