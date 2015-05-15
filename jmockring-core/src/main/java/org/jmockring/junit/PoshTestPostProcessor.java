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

package org.jmockring.junit;

import static com.google.common.collect.Lists.newArrayList;
import static org.jmockring.utils.Functions.ifEmpty;
import static org.jmockring.utils.Functions.ifNot;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;

import org.jmockring.annotation.ContextDefaults;
import org.jmockring.annotation.ExecutionConfiguration;
import org.jmockring.annotation.RemoteBean;
import org.jmockring.annotation.RemoteMock;
import org.jmockring.annotation.RemoteRequestListener;
import org.jmockring.annotation.RemoteSpring;
import org.jmockring.annotation.RequestClient;
import org.jmockring.annotation.Server;
import org.jmockring.configuration.ServerExecutionConfiguration;
import org.jmockring.configuration.ServerExecutionRegistry;
import org.jmockring.configuration.ServerSpec;
import org.jmockring.spi.MockProviderSPI;
import org.jmockring.spi.PluggableServiceLoader;
import org.jmockring.spring.event.SpringEventSnooper;
import org.jmockring.spring.mock.BeanAutoMockPostProcessor;
import org.jmockring.webserver.WebServer;
import org.jmockring.webserver.callback.CallbackRequestEventListener;

/**
 * Post-process test classes to inject annotated fields with various remote context data.
 * <p/>
 *
 * @author Pavel Lechev
 * @date 04/01/13
 * @see org.jmockring.annotation.RemoteBean
 * @see org.jmockring.annotation.RemoteMock
 * @see org.jmockring.annotation.ExecutionConfiguration
 * @see org.jmockring.annotation.RequestClient
 * @see org.jmockring.annotation.RemoteSpring
 * @see ExternalServerJUnitRunner
 */
class PoshTestPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PoshTestPostProcessor.class);

    private ConfigurableTargetRunner runner;

    private MockProviderSPI mockingProvider;

    public PoshTestPostProcessor(ConfigurableTargetRunner runner) {
        if (!(runner instanceof ExternalServerJUnitRunner) && !(runner instanceof ExternalServerJUnitSuiteRunner)) {
            throw new IllegalArgumentException("Unsupported JUnit runner!");
        }
        this.runner = runner;
        this.mockingProvider = PluggableServiceLoader.loadMockingProvider(false);
    }

    /**
     *
     */
    void postProcessTestClass() {
        Class<? extends Object> testClass = runner.getTestInstance().getClass();
        ContextDefaults defaults = testClass.getAnnotation(ContextDefaults.class);
        if (defaults == null) {
            // just to ensure we don't get NPEs
            defaults = ContextDefaults.DEFAULTS.get();
        }
        Field[] fields = testClass.getDeclaredFields();
        for (Field f : fields) {
            boolean res = injectSpringContext(runner.getTestInstance(), f, defaults)
                    || injectConfiguration(runner.getTestInstance(), f, defaults)
                    || injectMock(runner.getTestInstance(), f, defaults)
                    || injectBean(runner.getTestInstance(), f, defaults)
                    || injectHttpClient(runner.getTestInstance(), f, defaults)
                    || injectRequestListener(runner.getTestInstance(), f, defaults);
            if (res) {
                LOG.info("Auto-injected field '{}' in class '{}'", f.getName(), testClass.getName());
            }
        }
    }

    /**
     * Must be called before each test method invocation.
     * <p/>
     */

    void executeMockingProviderTestInstanceProcessing() {
        if (mockingProvider != null) {
            mockingProvider.processTestClass(runner.getRunner().getTestClass().getJavaClass(), runner.getTestInstance());
        }
    }

    /**
     * @param testInstance
     * @param field
     * @return
     */
    private boolean injectBean(Object testInstance, Field field, ContextDefaults defaults) {
        RemoteBean ba = field.getAnnotation(RemoteBean.class);
        if (ba != null) {
            ServerSpec serverSpec = ServerSpec
                    .forServer(ifNot(WebServer.class, ba.bootstrap(), defaults.bootstrap()))
                    .withName(ifNot(Server.DEFAULT_EXECUTION_NAME, ba.executionName(), defaults.executionName()))
                    .withContext(ifEmpty(ba.contextPath(), defaults.contextPath()));
            Class requiredType = ba.type() != RemoteBean.class ? ba.type() : field.getType();
            Object bean = null;
            if (requiredType == SpringEventSnooper.class) {
                bean = doGetListenerFromContext(serverSpec, requiredType);
                this.runner.addSnooper((SpringEventSnooper) bean);
            } else {
                bean = doGetBeanFromContext(serverSpec, requiredType, ba.beanName(), false);
            }
            field.setAccessible(true);
            try {
                field.set(testInstance, bean);
                return true;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }


    /**
     * @param testInstance
     * @param field
     * @return
     */
    private boolean injectMock(Object testInstance, Field field, ContextDefaults defaults) {
        RemoteMock ba = field.getAnnotation(RemoteMock.class);
        if (ba != null) {
            ServerSpec serverSpec = ServerSpec
                    .forServer(ifNot(WebServer.class, ba.bootstrap(), defaults.bootstrap()))
                    .withName(ifNot(Server.DEFAULT_EXECUTION_NAME, ba.executionName(), defaults.executionName()))
                    .withContext(ifEmpty(ba.contextPath(), defaults.contextPath()));
            Class requiredType = ba.type() != RemoteMock.class ? ba.type() : field.getType();
            Object mock = doGetBeanFromContext(serverSpec, requiredType, ba.beanName(), true); // require mock.
            mockingProvider.resetMocks(newArrayList(mock));
            field.setAccessible(true);
            try {
                field.set(testInstance, mock);
                this.runner.addUsedMock(mock);
                return true;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * @param testInstance
     * @param field
     * @return
     */
    private boolean injectConfiguration(Object testInstance, Field field, ContextDefaults defaults) {
        ExecutionConfiguration exc = field.getAnnotation(ExecutionConfiguration.class);
        if (exc != null) {
            // check the type is compatible
            if (!ServerExecutionConfiguration.class.isAssignableFrom(field.getType())) {
                throw new IllegalStateException(String.format(
                        "The field '%s.%s' is of type '%s' which is not compatible with '%s'.",
                        testInstance.getClass().getName(),
                        field.getName(),
                        field.getType().getName(),
                        ServerExecutionConfiguration.class.getName()));
            }
            ServerSpec serverSpec = ServerSpec
                    .forServer(ifNot(WebServer.class, exc.bootstrap(), defaults.bootstrap()))
                    .withName(ifNot(Server.DEFAULT_EXECUTION_NAME, exc.executionName(), defaults.executionName()))
                    .withContext(ifEmpty(exc.contextPath(), defaults.contextPath()));
            ServerExecutionConfiguration configuration = ServerExecutionRegistry.getConfiguration(serverSpec);
            if (configuration == null) {
                throw new IllegalStateException("Can't find configuration for specification " + serverSpec);
            }
            field.setAccessible(true);
            try {
                field.set(testInstance, configuration);
                return true;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * @param testInstance
     * @param field
     */
    private boolean injectSpringContext(Object testInstance, Field field, ContextDefaults defaults) {
        RemoteSpring sc = field.getAnnotation(RemoteSpring.class);
        if (sc != null) {
            // check the type is compatible
            if (!RemoteSpring.ContextSupertype.EXPECTED_CONTEXT_SUPERTYPE.isAssignableFrom(field.getType())) {
                throw new IllegalStateException(String.format(
                        "The field '%s.%s' is of type '%s' which is not compatible with '%s'.",
                        testInstance.getClass().getName(),
                        field.getName(),
                        field.getType().getName(),
                        RemoteSpring.ContextSupertype.EXPECTED_CONTEXT_SUPERTYPE.getName()
                ));
            }
            ServerSpec serverSpec = ServerSpec
                    .forServer(ifNot(WebServer.class, sc.bootstrap(), defaults.bootstrap()))
                    .withName(ifNot(Server.DEFAULT_EXECUTION_NAME, sc.executionName(), defaults.executionName()))
                    .withContext(ifEmpty(sc.contextPath(), defaults.contextPath()));
            ServerExecutionConfiguration configuration = ServerExecutionRegistry.getConfiguration(serverSpec);
            if (configuration == null) {
                throw new IllegalStateException("Can't find configuration for specification " + serverSpec);
            }
            field.setAccessible(true);
            try {
                field.set(testInstance, configuration.getSpringContext());
                return true;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * @param testInstance
     * @param field
     * @return
     */
    private boolean injectHttpClient(Object testInstance, Field field, ContextDefaults defaults) {
        RequestClient rc = field.getAnnotation(RequestClient.class);
        if (rc != null) {
            ServerSpec serverSpec = ServerSpec
                    .forServer(ifNot(WebServer.class, rc.bootstrap(), defaults.bootstrap()))
                    .withName(ifNot(Server.DEFAULT_EXECUTION_NAME, rc.executionName(), defaults.executionName()))
                    .withContext(ifEmpty(rc.contextPath(), defaults.contextPath()));
            ServerExecutionConfiguration configuration = ServerExecutionRegistry.getConfiguration(serverSpec);
            Object client = PluggableServiceLoader.loadRequestClient(
                    field.getType(),
                    configuration != null ? configuration.getConfiguration() : null,
                    configuration != null ? configuration.getContextConfiguration() : null);
            if (client != null) {
                try {
                    field.setAccessible(true);
                    field.set(testInstance, client);
                    return true;
                } catch (IllegalAccessException e) {
                    LOG.error("Can't inject spi executor", e);
                }
            } else {
                throw new IllegalStateException("Can not locate provider for client of type " + field.getType());
            }
        }
        return false;
    }

    /**
     * @param testInstance
     * @param field
     * @return
     */
    private boolean injectRequestListener(Object testInstance, Field field, ContextDefaults defaults) {
        RemoteRequestListener rl = field.getAnnotation(RemoteRequestListener.class);
        if (rl != null) {
            // check the type is compatible
            if (CallbackRequestEventListener.class != field.getType()) {
                throw new IllegalStateException(String.format(
                        "The field '%s.%s' is of type '%s' which is not compatible with '%s'.",
                        testInstance.getClass().getName(),
                        field.getName(),
                        field.getType().getName(),
                        CallbackRequestEventListener.class.getName()
                ));
            }
            ServerSpec serverSpec = ServerSpec
                    .forServer(ifNot(WebServer.class, rl.bootstrap(), defaults.bootstrap()))
                    .withName(ifNot(Server.DEFAULT_EXECUTION_NAME, rl.executionName(), defaults.executionName()))
                    .withContext(ifEmpty(rl.contextPath(), defaults.contextPath()));
            ServerExecutionConfiguration configuration = ServerExecutionRegistry.getConfiguration(serverSpec);
            if (configuration == null) {
                throw new IllegalStateException("Can't find configuration for specification " + serverSpec);
            }
            field.setAccessible(true);
            try {
                field.set(testInstance, configuration.getContextConfiguration().getRequestEventListener());
                return true;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }


    /**
     * @param serverSpec
     * @param beanType
     * @param beanName    if null if will be ignored
     * @param lookForMock
     * @param <T>
     * @return
     */
    private <T extends Object> T doGetBeanFromContext(ServerSpec serverSpec,
                                                      Class<T> beanType,
                                                      String beanName,
                                                      boolean lookForMock) {
        ServerExecutionConfiguration serverConfig = ServerExecutionRegistry.getConfiguration(serverSpec);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Can't find server config for specification: " + serverSpec + "\n Check the correct contextPath, execution name and server are used in annotations.");
        }

        ApplicationContext remoteContext = serverConfig.getSpringContext();
        T bean = beanName != null && beanName.length() > 0 ? remoteContext.getBean(beanName, beanType) : remoteContext.getBean(beanType);
        if (bean == null) {
            throw new IllegalArgumentException(String.format("No bean of type [%s] was found in context.", beanType.getName()));
        }
        boolean isMock = mockingProvider.isMock(bean);
        if (isMock && !lookForMock) {
            writeMockedBeans(serverSpec);
            throw new IllegalArgumentException(String.format("Type [%s] is a mock, but real bean was requested. Did you mean to use @%s instead?", beanType.getName(), RemoteMock.class.getSimpleName()));
        } else if (!isMock && lookForMock) {
            writeMockedBeans(serverSpec);
            throw new IllegalArgumentException(String.format("Type [%s] is not a valid mock. Did you mean to use @%s instead?", beanType.getName(), RemoteBean.class.getSimpleName()));
        }
        return bean;
    }

    /**
     * @param serverSpec
     * @param listenerType
     * @param <T>
     * @return
     */
    private <T extends Object> T doGetListenerFromContext(ServerSpec serverSpec, Class<T> listenerType) {

        ServerExecutionConfiguration serverConfig = ServerExecutionRegistry.getConfiguration(serverSpec);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Can't find server config for specification: " + serverSpec + "\n Check the correct contextPath, execution name and server are used in annotations.");
        }
        AbstractApplicationContext remoteContext = (AbstractApplicationContext) serverConfig.getSpringContext();
        // check statically defined listeners
        T listener = null;
        for (ApplicationListener ls : remoteContext.getApplicationListeners()) {
            if (listenerType.isAssignableFrom(listener.getClass())) {
                listener = (T) ls;
            }
        }
        if (listener == null) {
            // check beans:
            listener = remoteContext.getBean(listenerType);
            if (listener == null) {
                throw new IllegalArgumentException(String.format("No listener of type [%s] was found in context.", listenerType.getName()));
            }
        }
        return listener;
    }


    /**
     * @param serverSpec
     */
    private void writeMockedBeans(ServerSpec serverSpec) {
        Map<String, Class> mocks = getMockedBeans(serverSpec);
        System.out.println(" =================================================================== ");
        System.out.println("Mocked beans in context " + serverSpec + " :");
        for (Map.Entry<String, Class> mockEntry : mocks.entrySet()) {
            System.out.println(String.format("    MOCK: %s [%s]", mockEntry.getKey(), mockEntry.getValue().getName()));
        }
        System.out.println(" =================================================================== ");
    }

    private Map<String, Class> getMockedBeans(ServerSpec serverSpec) {
        ServerExecutionConfiguration serverConfig = ServerExecutionRegistry.getConfiguration(serverSpec);
        AbstractApplicationContext remoteContext = (AbstractApplicationContext) serverConfig.getSpringContext();
        List<BeanFactoryPostProcessor> postProcessors = remoteContext.getBeanFactoryPostProcessors();
        for (BeanFactoryPostProcessor processor : postProcessors) {
            if (processor.getClass() == BeanAutoMockPostProcessor.class) {
                return ((BeanAutoMockPostProcessor) processor).getMockedBeans();
            }
        }
        LOG.warn("No BeanAutoMockPostProcessor found in context " + serverSpec);
        return new HashMap<String, Class>();
    }

    public MockProviderSPI getMockingProvider() {
        return mockingProvider;
    }
}
