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

package org.jmockring.spi;

import java.util.Collection;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @version 0.0.1
 * @date 28/02/13
 */
public interface MockProviderSPI {


    /**
     * Symbolic name for the mocking provider, e.g. "Mockito", "PowerMock", "EasyMock" ...
     *
     * @return
     */
    String providerName();

    /**
     * Create a mock for the given type.
     *
     * @param typeToMock
     * @param <T>
     *
     * @return
     */
    <T extends Object> T createMock(Class<T> typeToMock);

    /**
     * Determine if the passed instance is a mock.
     *
     * @param instance
     *
     * @return
     */
    <T extends Object> boolean isMock(T instance);


    /**
     * Is this provider active, i.e. can be used to create mocks, etc ...
     * <p/>
     * This method <b>must</b> be called by the service loader {@link java.util.ServiceLoader}
     * before it decides to use the provider.
     *
     * @return
     */
    boolean isActive();

    /**
     * Process any annotations, etc .. in the test class instance - simply delegate to the mocking engine.
     *
     * @param testClass
     * @param testInstance
     */
    void processTestClass(Class testClass, Object testInstance);

    /**
     * Clears the state of any used mocks.
     *
     * @param usedMocks
     */
    void resetMocks(Collection<Object> usedMocks);
}
