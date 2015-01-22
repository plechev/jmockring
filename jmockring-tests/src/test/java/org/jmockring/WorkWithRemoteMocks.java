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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientResponse;
import org.jmockring.annotation.ExecutionConfiguration;
import org.jmockring.annotation.PartOfSuite;
import org.jmockring.annotation.RemoteMock;
import org.jmockring.annotation.RequestClient;
import org.jmockring.configuration.ServerExecutionConfiguration;
import org.jmockring.junit.ExternalServerJUnitRunner;
import org.jmockring.ri.repository.TestConstructorAutowiredRepository;
import org.jmockring.ri.repository.TestRepository;
import org.jmockring.ri.repository.TestSetterAutowiredRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * @author Pavel Lechev
 * @date 24/07/12
 */
@RunWith(ExternalServerJUnitRunner.class)
@PartOfSuite(ExternalServerSuiteIT.class)
public class WorkWithRemoteMocks {

    @RemoteMock(executionName = "ex3", contextPath = "/context3")
    private TestSetterAutowiredRepository mockedRepo1;

    @RemoteMock(executionName = "ex3", contextPath = "/context3")
    private TestRepository mockedRepo2;

    @RemoteMock(executionName = "ex3", contextPath = "/context3")
    private TestConstructorAutowiredRepository mockedRepo3;

    @RequestClient
    private ClientExecutor executor;

    @ExecutionConfiguration(executionName = "ex3", contextPath = "/context3")
    private ServerExecutionConfiguration configuration;

    @Test
    @Ignore("Need to work on clients")
    public void shouldRetrieveAndVerifyRemoteMock() throws Exception {

        // RECORD REMOTE MOCKS BEHAVIOUR >>
        Mockito.when(mockedRepo1.getString()).thenReturn("Remote mock repository TestSetterAutowiredRepository");
        Mockito.when(mockedRepo2.getString()).thenReturn("Remote mock repository TestRepository");
        Mockito.when(mockedRepo3.getString()).thenReturn("Remote mock repository TestConstructorAutowiredRepository");

        // MAKE THE REQUESTS >>
        ClientResponse response1 = executor.createRequest(configuration.getRequestURL("/mocked-repos/" + TestSetterAutowiredRepository.class.getName()))
                .accept(MediaType.APPLICATION_JSON)
                .get();
        Map<String, String> value1 = (Map) response1.getEntity(Map.class);
        assertThat(value1.get("value"), is("Remote mock repository TestSetterAutowiredRepository"));

        ClientResponse response2 = executor.createRequest(configuration.getRequestURL("/mocked-repos/" + TestRepository.class.getName()))
                .accept(MediaType.APPLICATION_JSON)
                .get();
        Map<String, String> value2 = (Map) response2.getEntity(Map.class);
        assertThat(value2.get("value"), is("Remote mock repository TestRepository"));

        ClientResponse response3 = executor.createRequest(configuration.getRequestURL("/mocked-repos/" + TestConstructorAutowiredRepository.class.getName()))
                .accept(MediaType.APPLICATION_JSON)
                .get();
        Map<String, String> value3 = (Map) response3.getEntity(Map.class);
        assertThat(value3.get("value"), is("Remote mock repository TestConstructorAutowiredRepository"));

    }

}
