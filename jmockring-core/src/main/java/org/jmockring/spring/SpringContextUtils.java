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

import static java.lang.String.format;
import static java.lang.System.out;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.webserver.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;


/**
 * @author Pavel Lechev
 * @date 26/07/12
 */
public final class SpringContextUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SpringContextUtils.class);

    private SpringContextUtils() {
    }

    /**
     * @param factory
     *
     * @return
     */
    public static ServerConfiguration getConfigurationFromContext(DefaultListableBeanFactory factory) {
        WebApplicationContext context = (WebApplicationContext) factory.resolveDependency(new CustomDependencyDescriptor(ResourceLoader.class), "");
        return getConfigurationFromContext(context);
    }

    /**
     * @param context
     *
     * @return
     */
    public static ServerConfiguration getConfigurationFromContext(WebApplicationContext context) {
        Map configMap = (Map) ((StandardServletEnvironment) context.getEnvironment()).getPropertySources()
                .get(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY).getSource();
        return (ServerConfiguration) configMap.get(ConfigurationConstants.SERVER_CONFIGURATION_KEY);
    }

    /**
     * @param factory
     *
     * @return
     */
    public static Class<? extends WebServer> getBootstrapClassFromContext(DefaultListableBeanFactory factory) {
        WebApplicationContext context = (WebApplicationContext) factory.resolveDependency(new CustomDependencyDescriptor(ResourceLoader.class), "");
        return getBootstrapClassFromContext(context);
    }

    /**
     * @param context
     *
     * @return
     */
    public static Class<? extends WebServer> getBootstrapClassFromContext(WebApplicationContext context) {
        Map configMap = (Map) ((StandardServletEnvironment) context.getEnvironment()).getPropertySources()
                .get(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY).getSource();
        return (Class<? extends WebServer>) configMap.get(ConfigurationConstants.EXECUTION_PROPERTIES_BOOTSTRAP);
    }

    /**
     * @param factory
     *
     * @return
     */
    public static Properties getConfiguredPropertiesFromContext(DefaultListableBeanFactory factory) {
        WebApplicationContext context = (WebApplicationContext) factory.resolveDependency(new CustomDependencyDescriptor(ResourceLoader.class), "");
        return getConfiguredPropertiesFromContext(context);
    }

    /**
     * @param context
     *
     * @return
     */
    public static Properties getConfiguredPropertiesFromContext(WebApplicationContext context) {
        ServerConfiguration configuration = getConfigurationFromContext(context);
        Properties endeavourProperties = new Properties();
        try {
            endeavourProperties.load(context.getClass().getResourceAsStream(configuration.getPropertiesLocation()));
        } catch (IOException e) {
            LOG.error("LOG00270: can't load properties", e);
        }
        return endeavourProperties;
    }

    /**
     * @param context
     *
     * @return
     */
    public static BaseContextConfiguration getContextConfigurationFromContext(WebApplicationContext context) {
        Map configMap = (Map) ((StandardServletEnvironment) context.getEnvironment()).getPropertySources()
                .get(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY).getSource();
        return (BaseContextConfiguration) configMap.get(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY);
    }


    public static void debugWriteout(WebApplicationContext ctx, BaseContextConfiguration baseContextConfiguration) {
        out.println("================================================ DEBUG START ================================================");
        if (baseContextConfiguration.isEnableDebug()) {
            Map<String, Object> controllers = ctx.getBeansWithAnnotation(Controller.class);
            if (controllers != null && controllers.size() > 0) {
                out.println("Annotated MVC controllers:");
                for (Map.Entry<String, Object> entry : controllers.entrySet()) {
                    RequestMapping mapping = entry.getValue().getClass().getAnnotation(RequestMapping.class);
                    String path = null;
                    if (mapping != null) {
                        path = mapping.value()[0];
                    }
                    out.println(format("Class: %s, bean name: %s, mapping: %s", entry.getValue().getClass(), entry.getKey(), path));
                }
            }
        }
        out.println("================================================  DEBUG END  ================================================");
    }


    /** just a shortcut skipping unnecessary details */
    private static class CustomDependencyDescriptor extends DependencyDescriptor {

        private static Field field;

        private static final long serialVersionUID = 4350218247499857949L;

        static {
            try {
                field = CustomDependencyDescriptor.class.getDeclaredField("lookupType");
            } catch (NoSuchFieldException e) {
                LOG.error("Can't find field", e);
            }
        }

        private final Class<?> lookupType;

        public CustomDependencyDescriptor(Class<?> lookupType) {
            super(field, true);
            this.lookupType = lookupType;
        }

        @Override
        public Class<?> getDependencyType() {
            return this.lookupType;
        }
    }
}
