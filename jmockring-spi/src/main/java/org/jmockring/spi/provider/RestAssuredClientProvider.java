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

package org.jmockring.spi.provider;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.spi.RequestClientSPI;
import org.jmockring.spi.client.RestAssuredClient;

/**
 * @author Pavel Lechev
 * @version 0.0.1
 * @date 16/02/13
 */
public class RestAssuredClientProvider implements RequestClientSPI<RestAssuredClient> {


    @Override
    public Class<RestAssuredClient> clientClass() {
        return RestAssuredClient.class;
    }

    @Override
    public RestAssuredClient createClient(ServerConfiguration serverConfiguration,
                                          BaseContextConfiguration contextConfiguration) {
        if (serverConfiguration == null) {
            throw new IllegalArgumentException("ServerConfiguration is required to create the RestAssured RequestSpecification.");
        }
        return new RestAssuredClient(serverConfiguration, contextConfiguration);
    }

}
