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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.jmockring.annotation.ContextDefaults;
import org.jmockring.annotation.PartOfSuite;
import org.jmockring.annotation.RemoteMock;
import org.jmockring.annotation.RequestClient;
import org.jmockring.junit.ExternalServerJUnitRunner;
import org.jmockring.ri.repository.TestConstructorAutowiredRepository;
import org.jmockring.ri.repository.TestRepository;
import org.jmockring.ri.repository.TestSetterAutowiredRepository;
import org.jmockring.spi.client.RestAssuredClient;
import org.jmockring.webserver.jetty.JettyWebServer;

/**
 * @author Pavel Lechev
 * @date 24/07/12
 */
@RunWith(ExternalServerJUnitRunner.class)
@PartOfSuite(JettyServerSuiteIT.class)
@ContextDefaults(bootstrap = JettyWebServer.class, contextPath = "/context3", executionName = "ex3")
public class WorkWithRemoteMocks {

    @RemoteMock
    private TestSetterAutowiredRepository mockedRepo1;

    @RemoteMock
    private TestRepository mockedRepo2;

    @RemoteMock
    private TestConstructorAutowiredRepository mockedRepo3;

    @RequestClient
    private RestAssuredClient client;

    @Test
    public void shouldRetrieveAndVerifyRemoteMock() throws Exception {

        // RECORD REMOTE MOCKS BEHAVIOUR >>
        Mockito.when(mockedRepo1.getString()).thenReturn("Remote mock repository TestSetterAutowiredRepository");
        Mockito.when(mockedRepo2.getString()).thenReturn("Remote mock repository TestRepository");
        Mockito.when(mockedRepo3.getString()).thenReturn("Remote mock repository TestConstructorAutowiredRepository");

        client.newRequest()
            .request().log().all(true)
            .response().log().all(true)
            .expect()
            .statusCode(200)
            .content("value", is("Remote mock repository TestSetterAutowiredRepository"))
            .when()
            .get("/mocked-repos/{className}", TestSetterAutowiredRepository.class.getName().replace(".", "#"));

        client.newRequest()
            .expect()
            .statusCode(200)
            .body("value", is("Remote mock repository TestRepository"))
            .when()
            .get("/mocked-repos/{className}", TestRepository.class.getName().replace(".", "#"));

        client.newRequest()
            .expect()
            .statusCode(200)
            .body("value", is("Remote mock repository TestConstructorAutowiredRepository"))
            .when()
            .get("/mocked-repos/{className}", TestConstructorAutowiredRepository.class.getName().replace(".", "#"));

    }

}
