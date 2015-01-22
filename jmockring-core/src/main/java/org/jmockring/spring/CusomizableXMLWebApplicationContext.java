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

import java.io.IOException;
import java.util.Map;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.ServerConfigurationAware;
import org.jmockring.configuration.ServerExecutionRegistry;
import org.jmockring.spring.event.SpringEventSnooper;
import org.jmockring.spring.mock.BeanAutoMockPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 03/01/13
 */
public class CusomizableXMLWebApplicationContext extends XmlWebApplicationContext implements ServerConfigurationAware {

    private static final Logger log = LoggerFactory.getLogger(CusomizableXMLWebApplicationContext.class);

    private BaseContextConfiguration baseContextConfiguration;


    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (baseContextConfiguration == null) {
            throw new IllegalStateException("Context configuration is not set. See ServerConfigurationAwareContextLoaderListener.");
        }
        super.prepareBeanFactory(beanFactory);

        // add auto-mock factory post processor
        addBeanFactoryPostProcessor(new BeanAutoMockPostProcessor(baseContextConfiguration));

        // add the registry bean
        beanFactory.createBean(ServerExecutionRegistry.class);

        // add the snooper
        ((DefaultListableBeanFactory) beanFactory).registerBeanDefinition("TheSnooper", new RootBeanDefinition(SpringEventSnooper.class));
        log.info("LOG00040: Added snooper bean definition");

    }

    /**
     * copied from org.springframework.web.context.support.XmlWebApplicationContext#loadBeanDefinitions(org.springframework.beans.factory.support.DefaultListableBeanFactory)
     * <p/>
     * just adds custom document reader
     *
     * @param beanFactory
     *
     * @throws BeansException
     * @throws IOException
     */
    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory) {
            @Override
            protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
                return new ImportFilteringBeanDefinitionDocumentReader(baseContextConfiguration);
            }
        };

        // Configure the bean definition reader with this context's
        // resource loading environment.
        beanDefinitionReader.setEnvironment(this.getEnvironment());
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        // Allow a subclass to provide custom initialization of the reader,
        // then proceed with actually loading the bean definitions.
        super.initBeanDefinitionReader(beanDefinitionReader);
        super.loadBeanDefinitions(beanDefinitionReader);
    }


    void setBaseContextConfiguration(BaseContextConfiguration baseContextConfiguration) {
        this.baseContextConfiguration = baseContextConfiguration;
    }

    BaseContextConfiguration getBaseContextConfiguration() {
        return baseContextConfiguration;
    }

    @Override
    public ServerConfiguration getServerConfiguration() {
        Map configMap = (Map) getEnvironment().getPropertySources().get(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY).getSource();
        return (ServerConfiguration) configMap.get(ConfigurationConstants.SERVER_CONFIGURATION_KEY);
    }

    @Override
    public BaseContextConfiguration getApplicationContextConfiguration() {
        Map configMap = (Map) getEnvironment().getPropertySources().get(ConfigurationConstants.EXECUTION_ENVIRONMENT_KEY).getSource();
        return (BaseContextConfiguration) configMap.get(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY);
    }

    @Override
    public ApplicationContext getSpringContext() {
        return this;
    }
}
