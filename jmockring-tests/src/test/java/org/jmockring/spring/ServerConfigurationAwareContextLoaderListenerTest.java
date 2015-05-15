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

package org.jmockring.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import org.jmockring.annotation.Server;
import org.jmockring.annotation.WebContext;
import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.WebAppContextConfiguration;
import org.jmockring.utils.ClassMatcher;
import org.jmockring.webserver.jetty.JettyWebServer;

/**
 * @author Pavel Lechev
 * @date 02/01/13
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerConfigurationAwareContextLoaderListenerTest {

    private ServerConfigurationAwareContextLoaderListener underTest;

    private MockServletContext servletContextMock;

    private ServerConfiguration serverConfiguration;

    private BaseContextConfiguration baseContextConfiguration;

    @Before
    public void setUp() throws Exception {
        serverConfiguration = new ServerConfiguration(null, null);
        baseContextConfiguration = new WebAppContextConfiguration(Dummy.getWebContext(), Dummy.getServer());
        serverConfiguration.addWebAppContext((WebAppContextConfiguration) baseContextConfiguration);
        serverConfiguration.setPropertiesLocation("classpath:/path/to/dummy.properties");
        underTest = new ServerConfigurationAwareContextLoaderListener(serverConfiguration, baseContextConfiguration, JettyWebServer.class);

        servletContextMock = new MockServletContext("base path");
    }

    @Test
    public void shouldCreateWebApplicationContext() throws Exception {

        ConfigurableWebApplicationContext ctx = (ConfigurableWebApplicationContext) underTest.createWebApplicationContext(servletContextMock);
        assertThat(ctx.getClass(), ClassMatcher.isAClass(CusomizableXMLWebApplicationContext.class));
        assertThat(((CusomizableXMLWebApplicationContext) ctx).getBaseContextConfiguration(), CoreMatchers.is(baseContextConfiguration));

        MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
        assertThat(sources.contains(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY), is(true));
        PropertySource<?> source = sources.get(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY);

        Class bootstrapClass = (Class) source.getProperty(ConfigurationConstants.EXECUTION_PROPERTIES_BOOTSTRAP);
        assertThat(bootstrapClass, ClassMatcher.isAClass(JettyWebServer.class));
        assertThat((ServerConfiguration) source.getProperty(ConfigurationConstants.SERVER_CONFIGURATION_KEY), CoreMatchers.is(serverConfiguration));
        assertThat((WebAppContextConfiguration) source.getProperty(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY), CoreMatchers.is(baseContextConfiguration));
    }


    @Server(webContexts = @WebContext(webApp = "/path/to/webapp", contextPath = "/context", descriptor = "blah/web.xml"), testClass = Dummy.class)
    private static class Dummy {

        static Server getServer() {
            return Dummy.class.getAnnotation(Server.class);
        }

        static WebContext getWebContext() {
            return Dummy.class.getAnnotation(Server.class).webContexts()[0];
        }
    }


}
