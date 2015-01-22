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

package org.jmockring.configuration;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.jmockring.spring.ServletContextUtils;
import org.jmockring.spring.event.SpringEventSnooper;
import org.jmockring.webserver.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Pavel Lechev
 * @date 20/07/12
 * @see org.jmockring.junit.ServerBootstrap
 */
@HandlesTypes(WebApplicationInitializer.class)
public class ServerExecutionRegistry implements ApplicationContextAware, ServletContainerInitializer {

    private static final Logger log = LoggerFactory.getLogger(ServerExecutionRegistry.class);

    private static final Map<String, ServerExecutionConfiguration> serverExecutionConfigurations = new ConcurrentHashMap<String, ServerExecutionConfiguration>();

    private static final Collection<ServerConfigurationAware> remoteApplicationContexts = new ConcurrentLinkedQueue<ServerConfigurationAware>();

    /**
     * @param serverSpec
     * @return
     */
    public static ServerExecutionConfiguration getConfiguration(ServerSpec serverSpec) {
        return serverExecutionConfigurations.get(uniqueName(serverSpec.getTargetServer(), serverSpec.getExecutionName(), serverSpec.getContextPath()));
    }

    /**
     * @return
     */
    public static ServerExecutionConfiguration[] getAllServers() {
        ServerExecutionConfiguration[] allServers = new ServerExecutionConfiguration[serverExecutionConfigurations.size()];
        allServers = serverExecutionConfigurations.values().toArray(allServers);
        return allServers;
    }

    /**
     * @param applicationContext
     * @throws org.springframework.beans.BeansException
     */
    @Override
    public synchronized void setApplicationContext(ApplicationContext applicationContext) {
        //  collect all application dynamicContexts
        remoteApplicationContexts.add((ServerConfigurationAware) applicationContext);
    }

    /**
     * To be called once all servers are up and running, but before the tests execution commences.
     * <p/>
     * Loop the app dynamicContexts and initialise the `serverExecutionConfigurations`
     */
    public static synchronized void initialise() {

        if (remoteApplicationContexts.size() > 0) {
            log.info("Commence initialising registry for {} contexts ...", remoteApplicationContexts.size());
            for (ServerConfigurationAware configurationAware : remoteApplicationContexts) {
                ServerConfiguration configuration = configurationAware.getServerConfiguration();
                log.info("Getting configurations for execution name '{}'", configuration.getExecutionName());
                if (configuration == null) {
                    throw new IllegalStateException("Can't find server execution configuration in context.\n Is `ServerConfigurationAwareContextLoaderListener` enabled?");
                }
                BaseContextConfiguration contextConfiguration = configurationAware.getApplicationContextConfiguration();
                if (contextConfiguration == null) {
                    throw new IllegalStateException("Can't find context configuration.\n Is `ServerConfigurationAwareContextLoaderListener` enabled?");
                }
                Class<? extends WebServer> bootstrap = configuration.getServerConfig().bootstrap();
                if (bootstrap == null) {
                    throw new IllegalStateException("Can't find server execution class type in context.\n Is `ServerConfigurationAwareContextLoaderListener` enabled?");
                }
                ConfigurableWebApplicationContext springContext = (ConfigurableWebApplicationContext) configurationAware.getSpringContext();
                postProcessSpringContext(springContext);
                serverExecutionConfigurations.put(
                        uniqueName(bootstrap, configuration.getExecutionName(), contextConfiguration.getContextPath()),
                        new ServerExecutionConfiguration(configuration, contextConfiguration, springContext));
                // TODO debugWriteout(context, contextConfiguration);
            }
            remoteApplicationContexts.clear(); // clear it once initialised
        } else {
            log.warn("LOG00180: `initialise()` called but `remoteApplicationContexts` is empty.\n Either no servers have been run yet, or initialisation has already been performed.");
        }
    }

    private static void postProcessSpringContext(ConfigurableWebApplicationContext springContext) {
        if (springContext.getBean("TheSnooper") == null) {
            springContext.addApplicationListener(new SpringEventSnooper<ApplicationEvent>());
        }
    }

    /**
     * To be called when all configuration have been executed.
     */
    public static synchronized void cleanup() {
        log.info("Cleaning up the ServerExecutionRegistry ...");
        remoteApplicationContexts.clear();
        serverExecutionConfigurations.clear();
    }

    /**
     * Create unique property name for the executing configuration, the target server, the given execution name and the web app context.
     *
     * @param serverClass
     * @param executionName
     * @param contextPath   the web app context path as per {@link org.jmockring.annotation.WebContext#contextPath()} or {@link org.jmockring.annotation.DynamicContext#contextPath()}
     * @return
     */
    private static String uniqueName(Class<? extends WebServer> serverClass, String executionName, String contextPath) {
        return new StringBuilder()
                .append(serverClass.getSimpleName()).append("~")
                .append(executionName).append("~")
                .append(contextPath)
                .toString();
    }


    /**
     * @param c
     * @param ctx
     * @throws ServletException
     */
    @Override
    public void onStartup(Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        log.info("LOG00190: Initialised context: {}" + ctx);
        remoteApplicationContexts.add(new ServerConfigurationAware() {
            @Override
            public ServerConfiguration getServerConfiguration() {
                return ServletContextUtils.getConfigurationFromContext(ctx);
            }

            @Override
            public BaseContextConfiguration getApplicationContextConfiguration() {
                return ServletContextUtils.getContextConfigurationFromContext(ctx);
            }

            @Override
            public ApplicationContext getSpringContext() {
                return WebApplicationContextUtils.getWebApplicationContext(ctx);
            }
        });
    }
}
