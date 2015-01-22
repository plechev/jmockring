package org.jmockring.test.mvcsupport;

import org.jmockring.annotation.DynamicContext;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.RequestClient;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.Servlet;
import org.jmockring.junit.ExternalServerJUnitRunner;
import org.jmockring.ri.servlet.DefaultServlet;
import org.jmockring.spi.client.RestAssuredClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.servlet.DispatcherServlet;

import com.jayway.restassured.response.Response;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 03/04/13
 */

@RunWith(ExternalServerJUnitRunner.class)
@Server(
        enableDebug = true,
        dynamicContexts = @DynamicContext(
                springContextLocations = "classpath:spring/application-context.xml",
                contextPath = "/mvc",
                servlets = {
                        @Servlet(
                                servletClass = DispatcherServlet.class,
                                params = {@Param(name = "contextConfigLocation", value = "classpath:spring/web-mvc-context.xml")},
                                mapping = "/spring/*"
                        ),
                        @Servlet(
                                servletClass = DefaultServlet.class,
                                mapping = "/default"
                        )
                },
                //  staticResourcesPath = "C:\\Works\\projects\\jmockring-all\\jmockring-ri\\src\\main\\webapp\\",
                excludedConfigLocations = "repository-context.xml"
        ),
        testClass = SpringMVCSupportIT.class
)
@Ignore("Not ready for MVC support")
public class SpringMVCSupportIT {

    @RequestClient(contextPath = "/mvc")
    private RestAssuredClient client;

    @Test
    public void shouldPing() throws Exception {
        client.newRequest().expect().statusCode(200).when().get("/default");

        Response res = client.newRequest().expect().statusCode(200).when().get("/spring/controller/test");
        System.out.println(String.format("JSP[%s] (test): %s", res.getHeader("Content-Length"), res.getBody().asString()));

        res = client.newRequest().expect().statusCode(200).when().get("/spring/controller/raw");
        System.out.println(String.format("JSP[%s] (raw): %s", res.getHeader("Content-Length"), res.getBody().asString()));

        res = client.newRequest().expect().statusCode(200).when().get("/spring/controller/direct");
        System.out.println(String.format("JSP[%s] (direct): %s", res.getHeader("Content-Length"), res.getBody().asString()));
    }

}
