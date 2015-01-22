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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.utils.PropertyFileReader;
import org.jmockring.webserver.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Hook into the Spring context to inject the web server configuration ({@link org.jmockring.configuration.ServerConfiguration})
 * and the executing class {@link org.jmockring.webserver.WebServer} into the Spring environment.
 * <p/>
 * This class is designed to have minimum side effects on the Spring context.
 * The context itself is not modified in any way.
 * A new property source is added to the environment, which will only make sense to code which looks for it
 * and will be silently ignored by anything else...
 *
 * @author Pavel Lechev
 * @date 23/07/12
 * @requires Spring 3.1
 * @requires Servlet 3.0 API
 */
public class ServerConfigurationAwareContextLoaderListener extends ContextLoaderListener {

    private static final Logger log = LoggerFactory.getLogger(ServerConfigurationAwareContextLoaderListener.class);

    private final ServerConfiguration serverConfiguration;

    private final Class<? extends WebServer> bootstrap;

    private BaseContextConfiguration baseContextConfiguration;

    public ServerConfigurationAwareContextLoaderListener(ServerConfiguration serverConfiguration,
                                                         BaseContextConfiguration baseContextConfiguration,
                                                         Class<? extends WebServer> bootstrap) {
        this.serverConfiguration = serverConfiguration;
        this.baseContextConfiguration = baseContextConfiguration;
        this.bootstrap = bootstrap;
    }

    @Override
    protected WebApplicationContext createWebApplicationContext(ServletContext sc) {

        Class<?> contextClass = determineContextClass(sc);
        if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
            throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
                    "] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
        }
        CusomizableXMLWebApplicationContext ctx = (CusomizableXMLWebApplicationContext) BeanUtils.instantiateClass(contextClass);

        // 1) This injects the configuration in the Spring execution environment.
        //    !!! REQUIRED BY THE `BeanAutoMockPostProcessor` when in use, else are just ignored
        MutablePropertySources sources = ((StandardServletEnvironment) ctx.getEnvironment()).getPropertySources();
        if (baseContextConfiguration.getPropertiesLocation() != null) {
            Properties properties = PropertyFileReader.fromClasspath(baseContextConfiguration.getPropertiesLocation()).load();
            sources.addFirst(new PropertiesPropertySource("extra_properties_added_for_testing", properties));
            log.info("Added extra properties {} for context `{}`", properties, baseContextConfiguration.getContextPath());
        }

        Map<String, Object> configMap = new HashMap<String, Object>() {
            {
                put(ConfigurationConstants.EXECUTION_PROPERTIES_BOOTSTRAP, bootstrap);
                put(ConfigurationConstants.SERVER_CONFIGURATION_KEY, serverConfiguration);
                put(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY, baseContextConfiguration);
            }
        };
        sources.addLast(new MapPropertySource(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY, configMap));

        // 2) Essential :: DO NOT REMOVE
        ctx.setBaseContextConfiguration(baseContextConfiguration);

        return ctx;
    }

    @Override
    protected Class<?> determineContextClass(ServletContext servletContext) {
        return CusomizableXMLWebApplicationContext.class;
    }


}
