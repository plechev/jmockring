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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.jmockring.annotation.ContextDefaults;
import org.jmockring.annotation.RequestClient;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.Servers;
import org.jmockring.annotation.WebContext;
import org.jmockring.junit.ExternalServerJUnitRunner;
import org.jmockring.spi.client.RestAssuredClient;
import org.jmockring.webserver.tomcat.TomcatWebServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.response.Response;

/**
 * @author Pavel Lechev
 * @date 20/07/12
 */
@RunWith(ExternalServerJUnitRunner.class)
@Servers({
        @Server(
                port = 6060,
                name = "TomcatWebServerContext",
                webContexts = {
                        @WebContext(
                                webApp = "../../src/main/webapp",
                                contextPath = "/context1"
                        )
                },
                propertiesLocation = "/mock/base-webserver-ri.properties",
                bootstrap = TomcatWebServer.class,
                testClass = WebAppContextIT.class
        ),
        @Server(
                port = 6063,
                name = "SecuredTomcatWebServerContext",
                webContexts = {
                        @WebContext(
                                webApp = "../../src/main/webapp",
                                descriptor = "../../src/test/webapp/test-web.xml",
                                contextPath = "/context2"
                        )
                },
                propertiesLocation = "/mock/base-webserver-ri.properties",
                bootstrap = TomcatWebServer.class,
                testClass = WebAppContextIT.class
        )

}
)
@ContextDefaults(contextPath = "/context1", bootstrap = TomcatWebServer.class)
public class WebAppContextIT {

    private static final Logger log = LoggerFactory.getLogger(WebAppContextIT.class);

    @RequestClient(executionName = "TomcatWebServerContext", contextPath = "/context1")
    private RestAssuredClient plainClient;

    @RequestClient(executionName = "SecuredTomcatWebServerContext", contextPath = "/context2")
    private RestAssuredClient customDescriptorClient;

    @Test
    public void shouldStartTomcatAndAccessWebAppResources() throws Exception {

        Response response = plainClient.newRequest()
                .expect()
                .statusCode(200)
                .when()
                .get("/jsp/test.jsp");
        String resText = response.asString();
        assertThat(resText, containsString("TEST JSP PAGE (1):"));
        assertThat(resText, containsString("TEST JSP PAGE (2):"));

        response = plainClient.newRequest()
                .expect()
                .statusCode(200)
                .when()
                .get("/js/test-script.js");
        assertThat(response.asString(), containsString("testJavaScript = \"TEST\";"));

        response = plainClient.newRequest()
                .expect()
                .statusCode(200)
                .when()
                .get("/test-servlet");
        assertThat(response.asString(), containsString("This is TEST SERVLET RESPONSE"));
    }


    @Test
    public void shouldStartTomcatAndAccessWebAppResourcesViaHTTPS() throws Exception {

        Response response = customDescriptorClient.newRequest()
                .expect()
                .statusCode(200)
                .when()
                .get("/jsp/test.jsp");
        String resText = response.asString();
        assertThat(resText, containsString("TEST JSP PAGE (1):"));
        assertThat(resText, containsString("TEST JSP PAGE (2):"));

        response = customDescriptorClient.newRequest()
                .expect()
                .statusCode(200)
                .when()
                .get("/js/test-script.js");
        assertThat(response.asString(), containsString("testJavaScript = \"TEST\";"));

        response = customDescriptorClient.newRequest()
                .expect()
                .statusCode(200)
                .when()
                .get("/test-mapping");
        assertThat(response.asString(), containsString("This is TEST SERVLET RESPONSE"));
    }

}
