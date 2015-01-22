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

import java.lang.reflect.Method;
import java.util.Collection;

import org.jmockring.spi.ExecutionHandler;
import org.jmockring.spi.MockProviderSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @version 0.0.1
 * @date 28/02/13
 */
public abstract class AbstractMockingProvider implements MockProviderSPI {

    private static final Logger log = LoggerFactory.getLogger(AbstractMockingProvider.class);

    private ExecutionHandler mockingExecution;

    private ExecutionHandler checkMockExecution;

    private ExecutionHandler resetMocksExecution;

    private ExecutionHandler processTestClassExecution;

    protected AbstractMockingProvider() {
        try {
            mockingExecution = createMockingExecution();
            checkMockExecution = createCheckMockExecution();
            processTestClassExecution = createProcessTestClassExecution();
            resetMocksExecution = createResetMocksExecution();
        } catch (RuntimeException e) {
            log.error("LOG00000: Mocking Provider '{}' not available: {}", providerName(), e.getMessage());
        }
    }

    @Override
    public <T extends Object> T createMock(Class<T> typeToMock) {
        return (T) mockingExecution.execute(typeToMock);
    }

    @Override
    public <T extends Object> boolean isMock(T instance) {
        return Boolean.TRUE.equals(checkMockExecution.execute(instance));
    }

    @Override
    public void processTestClass(Class testClass, Object testInstance) {
        processTestClassExecution.execute(testClass, testInstance);
    }

    @Override
    public void resetMocks(Collection<Object> usedMocks) {
        Object[] arr = new Object[usedMocks.size()];
        resetMocksExecution.execute(usedMocks.toArray(arr));
    }

    @Override
    public boolean isActive() {
        return mockingExecution != null && checkMockExecution != null
                && processTestClassExecution != null && resetMocksExecution != null;
    }

    protected abstract ExecutionHandler createMockingExecution();

    protected abstract ExecutionHandler createCheckMockExecution();

    protected abstract ExecutionHandler createProcessTestClassExecution();

    protected abstract ExecutionHandler createResetMocksExecution();


    /**
     * @param className
     * @param method
     * @param args
     *
     * @return
     */
    protected Method getMethod(String className, String method, Class<?>... args) {
        try {
            Class clazz = loadClass(className);
            return clazz.getMethod(method, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Can't find method in class: " + className, e);
        }
    }

    /**
     * @param className
     *
     * @return
     */
    protected Class loadClass(String className) {
        ClassLoader loader = this.getClass().getClassLoader();
        try {
            return loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't load class", e);
        }
    }

    /**
     * @param classToInstantiate
     * @param <T>
     *
     * @return
     */
    protected <T extends Object> T newInstance(Class<T> classToInstantiate) {
        try {
            return classToInstantiate.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
