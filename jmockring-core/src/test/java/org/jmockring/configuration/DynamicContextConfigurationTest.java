package org.jmockring.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URLConnection;

import org.junit.Test;

import org.jmockring.annotation.DynamicContext;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.Server;

public class DynamicContextConfigurationTest {

    private DynamicContextConfiguration underTest;

   /**
     * @verifies aggregate configured context and security context locations as string
     * @see DynamicContextConfiguration#getAllContextLocationsAsString()
     */
    @Test
    public void getAllContextLocationsAsString_shouldAggregateConfiguredContextAndSecurityContextLocationsAsString() throws Exception {
        final Server server = Dummy.class.getAnnotation(Server.class);
        underTest = new DynamicContextConfiguration(server.dynamicContexts()[1], server);

        final String locations = underTest.getAllContextLocationsAsString();
        assertThat(locations, is("/spring/context-loader1.xml,/spring/context-loader2.xml/spring/security-loader1.xml,/spring/security-loader2.xml"));
    }

    /**
     * @verifies aggregate configured context locations as string
     * @see DynamicContextConfiguration#getAllContextLocationsAsString()
     */
    @Test
    public void getAllContextLocationsAsString_shouldAggregateConfiguredContextLocationsAsString() throws Exception {
        final Server server = Dummy.class.getAnnotation(Server.class);
        underTest = new DynamicContextConfiguration(server.dynamicContexts()[0], server);

        final String locations = underTest.getAllContextLocationsAsString();
        assertThat(locations, is("/spring/context-loader1.xml,/spring/context-loader2.xml"));
    }


    @Server(
        testClass = Dummy.class,
        dynamicContexts = {
            @DynamicContext(
                springContextLocations = {"/spring/context-loader1.xml", "/spring/context-loader2.xml"},
                autoMocks = true,
                contextParams = @Param(name = "param1", value = "value1"),
                contextPath = "/context1",
                excludedConfigLocations = {"/exclude/this-file.xml"},
                forcedMockTypes = {URLConnection.class}
            ),
            @DynamicContext(
                springContextLocations = {"/spring/context-loader1.xml", "/spring/context-loader2.xml"},
                securityContextLocations = {"/spring/security-loader1.xml", "/spring/security-loader2.xml"},
                autoMocks = false,
                contextParams = @Param(name = "param1", value = "value1"),
                contextPath = "/context2"
            )
        })
    private static class Dummy {

    }

}
