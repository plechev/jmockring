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

package org.jmockring.webserver.jetty;

import static org.jmockring.configuration.ConfigurationConstants.CONTEXT_CONFIG_LOCATION;
import static org.jmockring.configuration.ConfigurationConstants.PORT_CONFIG_KEY;
import static org.jmockring.configuration.ConfigurationConstants.SPRING_SECURITY_FILTER_CHAIN;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.filter.DelegatingFilterProxy;

import org.jmockring.annotation.DynamicContext;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.Servlet;
import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.DynamicContextConfiguration;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.WebAppContextConfiguration;
import org.jmockring.spring.ServerConfigurationAwareContextLoaderListener;
import org.jmockring.webserver.WebServer;
import org.jmockring.webserver.callback.CallbackRequestEventListener;

/**
 * A concrete implementation of the base Jetty WebServer.
 *
 * @author Pavel Lechev
 * @date 20/07/12
 */
public class JettyWebServer implements WebServer {

    private Logger log = LoggerFactory.getLogger(JettyWebServer.class);

    private int port;

    private String secret;

    protected ServerConfiguration configuration;

    /**
     * @return
     */
    protected List<ServletContextHandler> initialiseAndCreateContexts() {

        List<ServletContextHandler> contexts = new ArrayList<ServletContextHandler>();

        // create WAR/web.xml dynamicContexts
        contexts.addAll(createWebAppContexts());

        // create dynamic dynamicContexts (no web.xml)
        contexts.addAll(createDynamicContexts());

        return contexts;
    }


    private List<? extends ServletContextHandler> createWebAppContexts() {
        List<WebAppContext> webAppContexts = new ArrayList<WebAppContext>();

        for (WebAppContextConfiguration webAppConfiguration : configuration.getWebAppContexts()) {
            final ServerConfigurationAwareContextLoaderListener myContextLoaderListener = new ServerConfigurationAwareContextLoaderListener(this.configuration, webAppConfiguration, this.getClass());
            WebAppContext context = new WebAppContext() {
                @Override
                public void setEventListeners(EventListener[] eventListeners) {
                    // Our listener will have to be at the end of the list
                    // so that the real Spring context listener gets to execute first.
                    LinkedList<EventListener> reorderedEventListeners = new LinkedList<EventListener>();
                    if (eventListeners != null) {
                        Collections.addAll(reorderedEventListeners, eventListeners);
                    }

                    int pos = reorderedEventListeners.indexOf(myContextLoaderListener);
                    if (pos < 0) {
                        // not inserted yet
                        reorderedEventListeners.addLast(myContextLoaderListener);
                    } else if (pos < reorderedEventListeners.size() - 1) {
                        // the listener is not at the bottom, need to shift it
                        reorderedEventListeners.remove(pos);
                        reorderedEventListeners.addLast(myContextLoaderListener);
                    }
                    super.setEventListeners(reorderedEventListeners.toArray(new EventListener[reorderedEventListeners.size()]));
                }
            };

            String executionPath = "";
            try {
                executionPath = FileResource.newResource(".").getName();
                System.out.println(String.format("[Jetty bootstrap] - found project execution path: |%s|", executionPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            context.setExtraClasspath(executionPath);
            context.setParentLoaderPriority(true);
            context.setResourceBase(executionPath + webAppConfiguration.getConfig().webApp());

            context.setContextPath(webAppConfiguration.getContextPath());
            if (!webAppConfiguration.getConfig().descriptor().isEmpty()) {
                context.setDescriptor(executionPath + webAppConfiguration.getConfig().descriptor());
            }
            addClasspathEntriesFromParent(context);

            //
            attachRequestListener(context, webAppConfiguration);
            webAppContexts.add(context);
        }

        return webAppContexts;
    }

    private void addClasspathEntriesFromParent(WebAppContext context) {
        URL[] urls = ((URLClassLoader) getClass().getClassLoader()).getURLs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < urls.length; ++i) {
            sb.append(urls[i].toString());
            if (i < urls.length - 1) {
                sb.append(";");
            }
        }
        // context.setExtraClasspath(sb.toString());
        // log.info("Added extra classpath ({} entries): {}", urls.length, sb);
    }

    private List<? extends ServletContextHandler> createDynamicContexts() {
        List<ServletContextHandler> dynamicContexts = new ArrayList<ServletContextHandler>();
        for (DynamicContextConfiguration contextConfiguration : configuration.getDynamicContexts()) {
            DynamicContext dc = contextConfiguration.getConfig();
            ServletContextHandler context = new ServletContextHandler(null, contextConfiguration.getContextPath(), ServletContextHandler.SESSIONS);
            context.setInitParameter(CONTEXT_CONFIG_LOCATION, contextConfiguration.getAllContextLocationsAsString());

            // add filters
            for (org.jmockring.annotation.Filter filter : dc.filters()) {
                FilterHolder fh = new FilterHolder(filter.filterClass());
                fh.setName(filter.name());
                for (Param initParam : filter.params()) {
                    fh.setInitParameter(initParam.name(), initParam.value());
                }
                context.addFilter(fh, filter.mapping(), EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
                log.info("LOG00050: Adding filter: {}", fh);
            }

            // add servlets
            for (Servlet servlet : dc.servlets()) {
                ServletHolder sh;
                if (servlet.name().length() > 0) {
                    sh = new ServletHolder(servlet.name(), servlet.servletClass());
                } else {
                    sh = new ServletHolder(servlet.servletClass());
                }
                for (Param initParam : servlet.params()) {
                    sh.setInitParameter(initParam.name(), initParam.value());
                }
                context.addServlet(sh, servlet.mapping());
                log.info("LOG00060: Adding servlet: {}", sh);
            }

            // add listeners
            for (Class<? extends EventListener> listenerClass : dc.listeners()) {
                if (ContextLoaderListener.class.isAssignableFrom(listenerClass)) {
                    throw new IllegalArgumentException("Found another ContextLoaderListener. Note that Spring will be bootstrapped by " + ServerConfigurationAwareContextLoaderListener.class.getSimpleName());
                }
                try {
                    context.addEventListener(listenerClass.newInstance());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // add context parameters
            if (dc.contextParams().length > 0) {
                for (Param param : dc.contextParams()) {
                    context.setInitParameter(param.name(), param.value());
                }
            }

            // add the Spring configuration aware listener
            context.addEventListener(new ServerConfigurationAwareContextLoaderListener(this.configuration, contextConfiguration, this.getClass()));

            // enable spring security filter chain
            if (dc.securityContextLocations().length > 0) {
                addSpringSecurityFilter(context);
            }

            // add request callback listener:
            attachRequestListener(context, contextConfiguration);

            // set base resource:
            if (!dc.staticResourcesPath().isEmpty()) {
                try {
                    context.setBaseResource(FileResource.newResource(dc.staticResourcesPath()));
                    context.setResourceBase(dc.staticResourcesPath());
                    System.out.println(String.format("[Jetty bootstrap] context: %s, baseResource: |%s|", dc.contextPath(), context.getBaseResource()));
                } catch (IOException e) {
                    log.error("Failed to set base resource for context: " + dc.contextPath(), e);
                }
            }
            dynamicContexts.add(context);
        }
        return dynamicContexts;
    }


    private void attachRequestListener(ServletContextHandler context, BaseContextConfiguration contextConfiguration) {
        CallbackRequestEventListener requestEventListener = new CallbackRequestEventListener(configuration);
        context.addEventListener(requestEventListener);
        contextConfiguration.setRequestEventListener(requestEventListener);
    }


    /**
     * Enable spring security filter chain.
     * The actual security configuration must be supplied as XML file.
     *
     * @param context
     */
    private void addSpringSecurityFilter(ServletContextHandler context) {
        FilterHolder filterHolder = new FilterHolder(DelegatingFilterProxy.class);
        filterHolder.setFilter(new DelegatingFilterProxy());
        filterHolder.setName(SPRING_SECURITY_FILTER_CHAIN);
        context.addFilter(filterHolder, "/*", EnumSet.allOf(DispatcherType.class));
    }

    @Override
    public String getName() {
        return String.format("Jetty Web Server [version: %s]", Server.getVersion());
    }

    @Override
    public void waitForInitialisation() {
        // Do not wait ...
    }

    @Override
    public void initialise(ServerConfiguration configuration) {
        this.configuration = configuration;
        this.port = configuration.getPort(PORT_CONFIG_KEY);
        this.secret = configuration.getProperties().getProperty("jetty.secret", "6ee03119e0b4b0db921b609c18d4d4f1");
    }

    /**
     * Returns a default handler collection that calls
     * {@link #getHandlerList(org.eclipse.jetty.server.Server, java.util.List)} to populate its
     * default handlers.
     *
     * @param server
     * @param contexts
     * @return
     */
    public HandlerCollection getHandlerCollection(Server server, List<ServletContextHandler> contexts) {
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.addHandler(getHandlerList(server, contexts));
        // TODO log is disabled: create external config to enable it conditionally
        // String logDir = configuration.getProperties().getProperty("log.dir", getClass().getClassLoader().getResource(".").getPath());
        // handlerCollection.addHandler(LogHandlerFactory.createLogHandler(logDir));
        return handlerCollection;
    }

    /**
     * Returns the default handler list.  These are wired in to the handler
     * collection returned by {@link #getHandlerCollection(org.eclipse.jetty.server.Server, java.util.List)}
     * so don't need to be done by the caller.
     *
     * @param server
     * @param contexts
     * @return
     */
    public HandlerList getHandlerList(Server server, List<ServletContextHandler> contexts) {
        HandlerList handlers = new HandlerList();
        handlers.addHandler(new ShutdownHandler(server, configuration.getProperties().getProperty("jetty.secret", "6ee03119e0b4b0db921b609c18d4d4f1")));
        // add the dynamicContexts
        for (ServletContextHandler context : contexts) {
            handlers.addHandler(context);
        }

        return handlers;
    }

    /**
     * To be called internally by the subclasses.
     *
     * @param args
     */
    public void execute(String[] args) {
        if (args.length != 1) {
            startServer();
        } else if ("stop".equals(args[0])) {
            stopServer();
        } else if ("start".equals(args[0])) {
            startServer();
        } else {
            usage();
        }
    }

    /**
     * Start Up the server
     */
    private void startServer() {
        Server srv = new Server();
        srv.setStopAtShutdown(true);
        srv.setGracefulShutdown(5000);
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(Integer.valueOf(configuration.getProperties().getProperty("jetty.threads.max", "250")));
        srv.setThreadPool(threadPool);
        Connector connector = new SelectChannelConnector();
        connector.setPort(port);
        connector.setMaxIdleTime(30000);
        srv.setConnectors(new Connector[]{connector});

        try {
            List<ServletContextHandler> contexts = initialiseAndCreateContexts();
            HandlerCollection handlerCollection = getHandlerCollection(srv, contexts);
            srv.setHandler(handlerCollection);
            srv.start();
            srv.join();
        } catch (Exception e) {
            log.error("Exception: ", e);
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void stopServer() {
        HttpClient client = new HttpClient();
        try {
            final HttpExchange exchange = new HttpExchange();
            exchange.setMethod("POST");
            exchange.setURL(String.format("http://127.0.0.1:%s/shutdown?secret=%s", port, secret));
            client.send(exchange);
            log.info("LOG00010: shutdown status {}", exchange.getStatus());
        } catch (IOException e) {
            log.error("LOG00020: Can't stop server gracefully", e);
        }
    }

    /**
     *
     */
    private void usage() {
        log.info("Usage: java -jar <file.jar> [start|stop]\n\t" + "start    Start the server (default)\n\t" + "stop     Stop the server gracefully\n");
        System.exit(-1);
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void start() {
        startServer();
    }

    @Override
    public void shutdown() {
        stopServer();
    }
}
