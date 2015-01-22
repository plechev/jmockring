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

import java.util.ServiceLoader;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to load SPI providers.
 *
 * @author Pavel Lechev <pavel@jmockring.org>
 * @version 0.0.1
 * @date 12/02/13
 */
public final class PluggableServiceLoader {

    private static final Logger log = LoggerFactory.getLogger(PluggableServiceLoader.class);

    private static ServiceLoader<RequestClientSPI> requestClientLoader = ServiceLoader.load(RequestClientSPI.class);

    private static ServiceLoader<MockProviderSPI> mockringLoader = ServiceLoader.load(MockProviderSPI.class);

    private PluggableServiceLoader() {
    }

    /**
     * @param requiredType
     * @param serverConfiguration
     * @param contextConfiguration
     * @param <T>
     *
     * @return
     */
    public static <T extends Object> T loadRequestClient(Class<?> requiredType,
                                                         ServerConfiguration serverConfiguration,
                                                         BaseContextConfiguration contextConfiguration) {
        for (RequestClientSPI<T> clientProvider : requestClientLoader) {
            if (clientProvider.clientClass() == requiredType) {
                return clientProvider.createClient(serverConfiguration, contextConfiguration);
            }
        }
        return null;
    }

    /**
     * Load the first available mocking provider or throw exception, if none is found on the classpath.
     *
     * @param isRequired true if failure to find one should result in an error. Can be `false` if auto-mocking is not enabled,
     *                   which means we don't care if a mocking framework is being used.
     *
     * @return
     */
    public static MockProviderSPI loadMockingProvider(boolean isRequired) {
        StringBuilder registered = new StringBuilder();
        for (MockProviderSPI mockProvider : mockringLoader) {
            registered.append(mockProvider.providerName()).append(" ");
            if (mockProvider.isActive()) {
                log.info("LOG00020: Using mocking provider: {}", mockProvider.providerName());
                return mockProvider;
            }
        }
        if (isRequired) {
            throw new IllegalStateException("Can't find active mocking provider on the classpath. Scanned providers are: " + registered);
        }
        return null;
    }


}
