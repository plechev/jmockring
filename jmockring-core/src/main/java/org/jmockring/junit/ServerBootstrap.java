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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jmockring.annotation.BootstrapConfig;
import org.jmockring.annotation.DynamicContext;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.Security;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.Servers;
import org.jmockring.annotation.WebContext;
import org.jmockring.configuration.DynamicContextConfiguration;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.ServerExecutionRegistry;
import org.jmockring.configuration.WebAppContextConfiguration;
import org.jmockring.security.SecurityUtils;
import org.jmockring.spi.PostShutdownHook;
import org.jmockring.spi.PreStartupHook;
import org.jmockring.utils.PortChecker;
import org.jmockring.webserver.WebServer;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap services based on information in Server annotations placed on either a test class or test suite.
 * <p/>
 * This class takes care of the dynamic, on-demand port allocation, thus preventing port clashes when multiple tests are executed
 * in parallel on the same build box. Allocated pots are saved as JVM-wide unique properties and can be retrieved from inside the tests
 * to configure the correct HTTP requests. See {@link org.jmockring.configuration.ServerExecutionRegistry} for more detail.
 * <p/>
 * By playing around with the Server annotations and passing different Spring context files, we can deploy the
 * service in either <b>real-life</b> or <b>fake</b> mode. Services can be also, conditionally started up with security enabled or disabled.
 * <p/>
 * For more detail see {@link org.jmockring.annotation.Server} annotation options.
 *
 * @author Pavel Lechev
 * @date 20/07/12
 * @see ExternalServerJUnitRunner
 * @see ExternalServerJUnitSuiteRunner
 * @see org.jmockring.annotation.Servers
 * @see org.jmockring.annotation.Server
 * @see org.jmockring.configuration.ServerExecutionRegistry
 */
public class ServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    private ExecutorService executorService;

    private final List<WebServer> servers;

    private CountDownLatch startUpLatch;

    private CountDownLatch shutDownLatch;

    /**
     * To prevent an infinite block, for whatever reason, this is the absolute maximum of time (in seconds per-server)
     * for which the configuration executor thread will wait before giving up and proceeding to executing the tests.
     * Obviously, if this happens the tests will fail as there won't be anything to connect to, which is preferable to hanging on forever.
     */
    private static final long ABSOLUTE_MAX_WAIT_FOR_STARTUP = 10;

    /**
     * Similar to  #ABSOLUTE_MAX_WAIT_FOR_STARTUP but used for winding up the servers.
     */
    private static final long ABSOLUTE_MAX_WAIT_FOR_SHUTDOWN = 2;

    private final ConfigurableTargetRunner<?> runner;

    ServerBootstrap(ConfigurableTargetRunner<?> runner) {
        this.servers = new ArrayList<WebServer>();
        this.runner = runner;
    }

    ServersRunstateListener runAll() {
        // startup hook
        Class<? extends PreStartupHook> startupHookClass = getBootstrapConfig().startupHook();
        if (startupHookClass != PreStartupHook.class) {
            log.info("LOG00400: Calling shutdown cleanup: {}", startupHookClass);
            try {
                startupHookClass.newInstance().beforeTestsCommence();
            } catch (Exception e) {
                log.error("LOG00410: Failed to execute startup hook!", e);
                throw new IllegalStateException(e);
            }
        }


        String securityPolicy = getSecurityPolicy();
        if (securityPolicy != null) {
            log.info("Proceed to enabling Java2 security manager with policy '{}'", securityPolicy);
            SecurityUtils.enableSecurity(securityPolicy);
        }

        ServersRunstateListener listener = new ServersRunstateListener();
        log.debug("Added <ServersRunstateListener> listener");
        try {
            startServers();
        } catch (NoSuchMethodException e) {
            log.error("LOG00120:", e);
            throw new IllegalStateException("Can't bootstrap external server", e);
        } catch (InterruptedException e) {
            log.error("LOG00120:", e);
            throw new IllegalStateException("Can't bootstrap external server", e);
        }
        log.debug("Removing <ServersRunstateListener> listener");
        return listener;
    }

    /**
     * @throws NoSuchMethodException
     * @throws java.lang.reflect.InvocationTargetException
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void startServers() throws NoSuchMethodException, InterruptedException {
        Server[] servers = getServerAnnotations();
        BootstrapConfig bootstrapConfig = getBootstrapConfig();
        setSystemProperties(bootstrapConfig.systemProperties());
        initialiseExecutorService(servers.length);

        // start the servers:
        log.info("Initiating servers startup (>>)");
        for (Server context : servers) {
            startSingleServer(context, bootstrapConfig);
        }
        // wait for all servers to start
        this.startUpLatch.await(ABSOLUTE_MAX_WAIT_FOR_STARTUP * servers.length, TimeUnit.SECONDS);
        log.info("All servers are up and running: proceed to tests (>>)");

    }

    /**
     * @param params
     */
    private void setSystemProperties(Param[] params) {
        for (Param param : params) {
            System.setProperty(param.name(), param.value());
        }
    }

    private BootstrapConfig getBootstrapConfig() {
        TestClass testClass = runner.getConfiguredTestClass();
        Annotation[] annotations = testClass.getAnnotations();
        BootstrapConfig bootstrapConfig = null;
        for (Annotation a : annotations) {
            if (a.annotationType() == BootstrapConfig.class) {
                bootstrapConfig = (BootstrapConfig) a;
                log.info("Using configured bootstrapConfig = {}", bootstrapConfig);
                break;
            }
        }
        if (bootstrapConfig == null) {
            bootstrapConfig = BootstrapConfig.DEFAULT.getConfig();
            log.info("Using default bootstrapConfig = {}", bootstrapConfig);
        }
        return bootstrapConfig;
    }

    /**
     * Configure the thread pool executor.
     *
     * @param serversNum
     */
    private void initialiseExecutorService(int serversNum) {
        int threadPoolSize = (int) (Runtime.getRuntime().availableProcessors() * 0.5 * (serversNum + 1 / 2));
        if (threadPoolSize == 0) {
            // on some OS `availableProcessors` is 0
            threadPoolSize = 2;
        }
        log.info("Configuring Executor service with pool size {} for {} servers. ", threadPoolSize, serversNum);
        this.executorService = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory() {
            ThreadFactory wrappedFactory = Executors.defaultThreadFactory();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = wrappedFactory.newThread(r);
                t.setName("SRVExec:" + t.getName());
                t.setUncaughtExceptionHandler(new BootstrapUncaughtExceptionHandler());
                return t;
            }
        });
        this.startUpLatch = new CountDownLatch(serversNum);
        this.shutDownLatch = new CountDownLatch(serversNum);
    }

    /**
     * @param serverConfig
     *
     * @throws NoSuchMethodException
     * @throws java.lang.reflect.InvocationTargetException
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void startSingleServer(final Server serverConfig, final BootstrapConfig bootstrapConfig) throws InterruptedException {
        final ServerConfiguration serverConfiguration = createConfiguration(serverConfig, bootstrapConfig);
        try {
            final WebServer server = serverConfig.bootstrap().newInstance();
            server.initialise(serverConfiguration);
            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    log.info("Starting the server {} on port {}", server.getName(), server.getPort());
                    server.start(); // block here ....
                    shutDownLatch.countDown();
                    log.info("Prepare to shut down the server '{}' on port {}", server.getName(), server.getPort());
                }
            });
            this.servers.add(server);
            checkServerRunning(server.getPort(), bootstrapConfig.numberOfAttempts());
            server.waitForInitialisation();

        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * Create server configuration from the annotations data.
     *
     * @param serverConfig
     * @param bootstrapConfig
     *
     * @return
     */
    private ServerConfiguration createConfiguration(Server serverConfig, BootstrapConfig bootstrapConfig) {
        ServerConfiguration configuration;
        DynamicContext[] dynamicContexts = serverConfig.dynamicContexts();
        WebContext[] webContexts = serverConfig.webContexts();
        if (dynamicContexts.length == 0 && webContexts.length == 0) {
            throw new UnsupportedOperationException("Default configuration is not implemented - use at least one of @DynamicContext or @WebContext.");
        } else {
            configuration = new ServerConfiguration(serverConfig, bootstrapConfig);
            // create web app configurations:
            for (WebContext webContext : webContexts) {
                WebAppContextConfiguration webAppConfiguration = new WebAppContextConfiguration(webContext, serverConfig);
                configuration.addWebAppContext(webAppConfiguration);
            }
            // create dynamic configurations:
            for (DynamicContext dynamicContext : dynamicContexts) {
                DynamicContextConfiguration dynamicContextConfiguration = new DynamicContextConfiguration(dynamicContext, serverConfig);
                configuration.addDynamicContext(dynamicContextConfiguration);
            }
            // server-wide elements
            configuration.setPropertiesLocation(serverConfig.propertiesLocation());
            log.info("Creating custom configuration for server [{}] ", serverConfig.bootstrap().getName());
        }

        if (serverConfig.port() <= 0) {  // port nto specified, use dynamic ports
            configuration.setPort(allocateAvailablePort(serverConfig));
            log.info("Allocated dynamic port [{}] for server [{}]", configuration.getPort(), serverConfig.bootstrap().getName());
        } else {
            configuration.setPort(serverConfig.port());
            log.info("Set pre-configured port [{}] for server [{}]", configuration.getPort(), serverConfig.bootstrap().getName());
        }
        configuration.setExecutionName(serverConfig.name());
        return configuration;
    }


    /**
     * While this provides a level of certainty, it is still not 100% failure-proof.
     * Albeit highly unlikely. an allocated port may become unavailable
     * in the short interval between this call and the startup of the server.
     * If this becomes an issue, try playing with port ranges via {@link org.jmockring.annotation.Server#startAtPort()},
     *
     * @param serverContext
     *
     * @return
     */
    private int allocateAvailablePort(Server serverContext) {
        for (int portToCheck = serverContext.startAtPort(); portToCheck <= PortChecker.MAX_PORT_NUMBER; ++portToCheck) {
            if (PortChecker.available(portToCheck)) {
                return portToCheck;
            }
        }
        throw new IllegalStateException(String.format("Can not allocate port. Attempted range [%s, %s]",
                serverContext.startAtPort(),
                PortChecker.MAX_PORT_NUMBER)
        );
    }

    /**
     * @return
     */
    private String getSecurityPolicy() {
        for (Annotation a : runner.getConfiguredTestClass().getAnnotations()) {
            if (a.annotationType() == Security.class) {
                return ((Security) a).value();
            }
        }
        return null;
    }


    /**
     * @return
     */
    private Server[] getServerAnnotations() {
        TestClass testClass = runner.getConfiguredTestClass();
        Annotation[] annotations = testClass.getAnnotations();
        Server singleContext = null;
        Server[] allContexts = null;
        for (Annotation a : annotations) {
            if (a.annotationType() == Server.class) {
                singleContext = (Server) a;
            } else if (a.annotationType() == Servers.class) {
                allContexts = ((Servers) a).value();
            }
        }
        if (allContexts != null && singleContext != null) {
            throw new IllegalStateException(
                    String.format("Illegal usage of @Server and @Servers on the same class %s",
                            testClass.getName())
            );
        } else if (allContexts == null && singleContext == null) {
            throw new IllegalStateException(
                    String.format("Can not configure dynamicContexts: neither @Server nor @Servers annotations are present on %s",
                            testClass.getName())
            );
        } else if (allContexts == null) {
            allContexts = new Server[]{singleContext};
        }
        validateContextsConfiguration(allContexts);
        return allContexts;
    }

    /**
     * @param allServers
     *
     * @throws IllegalArgumentException if any of the context configurations are inconsistent
     */
    private void validateContextsConfiguration(Server[] allServers) {
        MultiMap bootstraps = new MultiValueMap();
        for (Server serverConfig : allServers) {
            // 1. Validate Class<->Name uniqueness
            if (bootstraps.containsKey(serverConfig.bootstrap())) {
                // server already defined in another context: check name
                Collection allNames = (Collection) bootstraps.get(serverConfig.bootstrap());
                if (allNames.contains(serverConfig.name())) {
                    // same class & same name : not good
                    throw new IllegalArgumentException(
                            String.format("Duplicate server context definition for [%s] and name [%s]. Consider using @Server#name()",
                                    serverConfig.bootstrap().getName(),
                                    serverConfig.name())
                    );
                }
            }
            bootstraps.put(serverConfig.bootstrap(), serverConfig.name());

            // 2. Check if context is configured
            if (serverConfig.dynamicContexts().length == 0 && serverConfig.webContexts().length == 0) {
                throw new IllegalArgumentException("No context configurations found for execution of class " + serverConfig.bootstrap().getName());
            }
        }
    }

    /**
     * Verify that the server is up and running so we can un-latch the waiting (tests executor) thread.
     *
     * @param portNumber
     * @param tryAttempts
     *
     * @throws InterruptedException
     */
    private void checkServerRunning(int portNumber, int tryAttempts) throws InterruptedException {

        int pauseFor = 1000; // a sec is good enough
        int attempts = 0;

        while (true) {
            boolean isRunning = !PortChecker.available(portNumber);
            attempts++;
            if (isRunning) {
                startUpLatch.countDown();
                log.info(String.format("Good-to-go for server on port %s after %s attempts. Total startup wait %s sec",
                        portNumber,
                        attempts,
                        (pauseFor * attempts) / 1000)
                        );
                return;
            }
            if (attempts > tryAttempts) {
                startUpLatch.countDown(); // un-latch before exception
                shutDownLatch.countDown();
                executorService.shutdown(); // kill all executing threads
                throw new IllegalStateException(
                        String.format(
                                "Can't connect to server on port %s after %s attempts. " +
                                        "When debugging make sure no breakpoints exist anywhere in the bootstrap call stack. " +
                                        "If this problem persists, use @BootstrapConfig to increase the number of allowed attempts before giving up to connect.",
                                portNumber,
                                attempts));
            }
            Thread.sleep(pauseFor);
        }

    }

    /**
     *
     */
    class ServersRunstateListener extends DefaultTestLifecycleListener {

        @Override
        public void beforeClass() {
            // startup hook
            Class<? extends PreStartupHook> startupHookClass = getBootstrapConfig().startupHook();
            if (startupHookClass != PreStartupHook.class) {
                log.info("LOG00400: Calling shutdown cleanup: {}", startupHookClass);
                try {
                    startupHookClass.newInstance().beforeTestsCommence();
                } catch (Exception e) {
                    log.error("LOG00410: Failed to execute startup hook!", e);
                    throw new IllegalStateException(e);
                }
            }
            log.info(" ================= All tests STARTING ... ================= ");
            // initialise registry  before the tests start.
            ServerExecutionRegistry.initialise();
        }


        @Override
        public void afterClass() {
            log.info(" ================= All tests COMPLETE: SHUTTING DOWN ... ================= ");
            // System.err.println("All tests finished - doing clean-up...");
            ServerExecutionRegistry.cleanup(); // don't need these anymore, so we cleanup ready for other tests executed in this JVM instance.
            log.debug("ServerExecutionRegistry cleaned - shutting down the servers...");
            if (executorService != null) {
                if (executorService.isTerminated() || executorService.isShutdown()) {
                    log.warn("Executor service has already been terminated. Perhaps uncaught exception shut down the server thread prematurely - see console/log output for details.");
                    return;
                }
                // System.err.println(String.format("executorService: isShutdown=%s, isTerminated=%s", executorService.isShutdown(), executorService.isTerminated()));
                // System.err.println(String.format("shutDownLatch.count: %s", shutDownLatch.getCount()));
                for (final WebServer server : servers) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            log.info("Sending 'stop' signal to server on port {} ...", server.getPort());
                            server.shutdown();
                        }
                    });
                }
                log.info("Initiating executor service shutdown (>>)");
                try {
                    shutDownLatch.await(ABSOLUTE_MAX_WAIT_FOR_SHUTDOWN, TimeUnit.SECONDS); // give each server some time to stop, else kill the threads forcefully.
                    executorService.shutdown();
                    executorService.awaitTermination(servers.size() * ABSOLUTE_MAX_WAIT_FOR_SHUTDOWN, TimeUnit.SECONDS);
                    log.info("Executor service shutdown complete - graceful servers wind-up in progress ... (<<)");

                    // shutdown hook
                    Class<? extends PostShutdownHook> shutdownHookClass = getBootstrapConfig().shutdownHook();
                    if (shutdownHookClass != PostShutdownHook.class) {
                        log.info("LOG00400: Calling shutdown cleanup: {}", shutdownHookClass);
                        try {
                            shutdownHookClass.newInstance().onTestsComplete();
                        } catch (Exception e) {
                            log.error("LOG00410: Failed to execute shutdown cleanup!", e);
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("Unexpected InterruptedException", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static class BootstrapUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private static Logger logB = LoggerFactory.getLogger(BootstrapUncaughtExceptionHandler.class);

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logB.error("Thread died with exception", e);
        }
    }


}
