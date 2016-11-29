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

package org.jmockring.spi.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.specification.RequestSpecification;

import org.jmockring.annotation.RequestClient;
import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ServerConfiguration;

/**
 * A wrapper around {@link RestAssured} REST testing client which is automatically pre-configured
 * with the correct host/port and context path and can be injected in the test class with {@link RequestClient}.
 *
 * @author Pavel Lechev
 * @date 17/02/13
 */
public class RestAssuredClient {

    private static final Logger log = LoggerFactory.getLogger(RestAssuredClient.class);

    private final ServerConfiguration serverConfiguration;

    private final BaseContextConfiguration contextConfiguration;

    private RestAssuredConfig restAssuredConfig;

    public RestAssuredClient(ServerConfiguration serverConfiguration, BaseContextConfiguration contextConfiguration) {
        this.serverConfiguration = serverConfiguration;
        this.contextConfiguration = contextConfiguration;
        this.restAssuredConfig = new RestAssuredConfig();
    }

    /**
     * @return
     */
    public RequestSpecification newRequest() {

        String hostname = serverConfiguration.getHost();
        String scheme = serverConfiguration.getScheme();

        // setup
        RestAssured.baseURI = String.format("%s://%s", scheme, hostname);
        RestAssured.port = serverConfiguration.getPort();
        if (contextConfiguration != null) {
            RestAssured.basePath = contextConfiguration.getContextPath();
        } else {
            RestAssured.basePath = "";
        }
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config = this.restAssuredConfig;

        // create request spec:
        RequestSpecification specification = RestAssured.with();
        log.info(String.format("LOG00300: Initialising RestEasy::RequestSpecification for %s:%d%s", RestAssured.baseURI, RestAssured.port, RestAssured.basePath));
        RestAssured.reset(); // clear the parameters after the specification is created;
        return specification;
    }


    /**
     * Allow further configuration in the setup/test init phase.
     *
     * @return
     */
    public RestAssuredConfig getConfig() {
        return restAssuredConfig;
    }

    /**
     * @return
     */
    public ServerConfiguration getServerConfiguration() {
        return serverConfiguration;
    }

}
