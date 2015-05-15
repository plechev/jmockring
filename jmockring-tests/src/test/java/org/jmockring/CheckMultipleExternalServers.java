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

package org.jmockring;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;

import org.jmockring.annotation.ContextDefaults;
import org.jmockring.annotation.ExecutionConfiguration;
import org.jmockring.annotation.PartOfSuite;
import org.jmockring.annotation.RemoteBean;
import org.jmockring.annotation.RemoteMock;
import org.jmockring.annotation.RemoteSpring;
import org.jmockring.annotation.RequestClient;
import org.jmockring.configuration.ServerExecutionConfiguration;
import org.jmockring.junit.ExternalServerJUnitRunner;
import org.jmockring.ri.repository.TestRepository;
import org.jmockring.ri.service.TestService;
import org.jmockring.spi.client.RestAssuredClient;
import org.jmockring.webserver.jetty.JettyWebServer;

/**
 * This demonstrates the usage of multiple external services running
 * and accessing their dynamically allocated ports from inside the configuration....
 *
 * @author Pavel Lechev
 * @date 19/07/12
 */
@RunWith(ExternalServerJUnitRunner.class)
@PartOfSuite(JettyServerSuiteIT.class)
@ContextDefaults(bootstrap = JettyWebServer.class, contextPath = "/context1")
public class CheckMultipleExternalServers {

    @ExecutionConfiguration(executionName = "ex1", contextPath = "/context1")
    private ServerExecutionConfiguration config1;

    @ExecutionConfiguration(executionName = "ex2", contextPath = "/context2")
    private ServerExecutionConfiguration config2;

    @ExecutionConfiguration(executionName = "ex3", contextPath = "/context3")
    private ServerExecutionConfiguration config3;

    @RemoteSpring(executionName = "ex1", contextPath = "/context1")
    private ApplicationContext sContext1;

    @RemoteBean(executionName = "ex3", contextPath = "/context3")
    private TestService testService;

    @RemoteMock(executionName = "ex3", contextPath = "/context3")
    private TestRepository testRepository;

    @RequestClient(executionName = "ex1", contextPath = "/context1")
    private RestAssuredClient client1;

    @RequestClient(executionName = "ex2", contextPath = "/context2")
    private RestAssuredClient client2;

    @RequestClient(executionName = "ex3", contextPath = "/context3")
    private RestAssuredClient client3;


    @Test
    public void shouldAccessMultipleExternalServers() throws Exception {

        when(testRepository.getString()).thenReturn("BLAH ...");

        client1
            .newRequest()
            .request().log().all(true)
            .response().log().all(true)
            .expect()
            .statusCode(200)
            .content("key1_111", is("value1"))
            .content("key2_111", is("value2"))
            .content("key3_111", is("value3"))
            .when()
            .get("/default/111");


        client2
            .newRequest()
            .request().log().all(true)
            .response().log().all(true)
            .expect()
            .statusCode(200)
            .content("key_Default", is("value_Default"))
            .when()
            .get("/default");

        client3
            .newRequest()
            .request().log().all(true)
            .response().log().all(true)
            .expect()
            .statusCode(200)
            .content("key1_333", is("value1"))
            .content("key2_333", is("value2"))
            .content("key3_333", is("value3"))
            .when()
            .get("/default/333");

    }

}
