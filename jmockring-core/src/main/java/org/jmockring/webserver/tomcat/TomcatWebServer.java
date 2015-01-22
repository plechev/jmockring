package org.jmockring.webserver.tomcat;

import java.util.concurrent.CountDownLatch;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;
import org.apache.commons.io.FilenameUtils;
import org.jmockring.annotation.WebContext;
import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.ServerExecutionRegistry;
import org.jmockring.configuration.WebAppContextConfiguration;
import org.jmockring.webserver.WebServer;
import org.jmockring.webserver.callback.CallbackRequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @since 12/07/13
 */
public class TomcatWebServer implements WebServer, LifecycleListener {


    private static final Logger log = LoggerFactory.getLogger(TomcatWebServer.class);

    private Tomcat tomcat;

    private ServerConfiguration configuration;

    private CountDownLatch blockingLatch;

    private CountDownLatch latchOnAfterStart;

    @Override
    public void initialise(ServerConfiguration configuration) {

        this.configuration = configuration;
        // location is typically the base folder where the executing test class is located.
        String location = configuration.getServerConfig().testClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        String hostname = configuration.getHost();
        String scheme = configuration.getScheme();

        tomcat = new Tomcat();
        tomcat.noDefaultWebXmlPath();
        tomcat.setBaseDir(FilenameUtils.normalize(location + "/../tomcat-work"));
        tomcat.setHostname(hostname);
        tomcat.getEngine().setDefaultHost(hostname);
        if ("https".equalsIgnoreCase(scheme)) {
            tomcat.getConnector().setScheme(scheme);
            tomcat.getConnector().setPort(configuration.getPort());
            tomcat.getConnector().setSecure(true);
        } else {
            tomcat.setPort(configuration.getPort());
        }
        tomcat.setSilent(false);
        tomcat.getServer().addLifecycleListener(this);

        latchOnAfterStart = new CountDownLatch(1);
        blockingLatch = new CountDownLatch(1);

        /* There needs to be a symlink to the current dir named 'webapps' */
        try {
            // addDynamicContexts(configuration, location);
            addWebAppContexts(configuration, location);
            tomcat.init();
            addListeners();
        } catch (LifecycleException e) {
            log.error("LOG00140: Failed to initialise Tomcat ", e);
        } catch (ServletException e) {
            log.error("LOG00180: Failed to add web application to Tomcat", e);
        }
    }

    /*
    private void addDynamicContexts(ServerConfiguration configuration, String location) {
        for (DynamicContextConfiguration contextConfiguration : configuration.getDynamicContexts()) {

            tomcat.addContext(contextConfiguration.getContextPath())


            ServletContextHandler context = new ServletContextHandler(null, contextConfiguration.getContextPath(), ServletContextHandler.SESSIONS);
            context.setInitParameter(CONTEXT_CONFIG_LOCATION, contextConfiguration.getAllContextLocationsAsString());

            // add filters
            for (org.jmockring.annotation.Filter filter : contextConfiguration.getConfig().filters()) {
                FilterHolder fh = new FilterHolder(filter.filterClass());
                fh.setName(filter.name());
                for (Param initParam : filter.params()) {
                    fh.setInitParameter(initParam.name(), initParam.value());
                }
                context.addFilter(fh, filter.mapping(), EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
                log.info("LOG00050: Adding filter: {}", fh);
            }

            // add servlets
            for (Servlet servlet : contextConfiguration.getConfig().servlets()) {
                ServletHolder sh = new ServletHolder(servlet.servletClass());
                for (Param initParam : servlet.params()) {
                    sh.setInitParameter(initParam.name(), initParam.value());
                }
                context.addServlet(sh, servlet.mapping());
                log.info("LOG00060: Adding servlet: {}", sh);
            }

            // add listeners
            for (Class<? extends EventListener> listenerClass : contextConfiguration.getConfig().listeners()) {
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
            if (contextConfiguration.getConfig().contextParams().length > 0) {
                for (Param param : contextConfiguration.getConfig().contextParams()) {
                    context.setInitParameter(param.name(), param.value());
                }
            }

            // add the Spring configuration aware listener
            context.addEventListener(new ServerConfigurationAwareContextLoaderListener(this.configuration, contextConfiguration, this.getClass()));

            // enable spring security filter chain
            if (contextConfiguration.getConfig().securityContextLocations().length > 0) {
                addSpringSecurityFilter(context);
            }

            // add request callback listener:
            attachRequestListener(context, contextConfiguration);

            // set base resource:
            if (!contextConfiguration.getConfig().staticResourcesPath().isEmpty()) {
                try {
                    context.setBaseResource(FileResource.newResource(contextConfiguration.getConfig().staticResourcesPath()));
                    context.setResourceBase(contextConfiguration.getConfig().staticResourcesPath());
                    System.out.println(String.format("[Jetty bootstrap] context: %s, baseResource: |%s|", context.getContextPath(), context.getBaseResource()));
                } catch (IOException e) {
                    log.error("Failed to set base resource for context: " + context.getContextPath(), e);
                }
            }
            dynamicContexts.add(context);
        }




    }

*/

    /**
     * @param configuration
     * @param location      typically the base folder where the executing test class is located. For Maven projects this is `/target/test-classes/`
     *                      We use it in conjunction with the relative path supplied in the annotated test class to obtain the absolute path to the
     *                      web application document root (where /WEB-INF folder is).
     * @throws ServletException
     */
    private void addWebAppContexts(ServerConfiguration configuration, String location) throws ServletException {
        for (WebAppContextConfiguration webAppContext : configuration.getWebAppContexts()) {
            WebContext webContextConfig = webAppContext.getConfig();
            String webAppPath = FilenameUtils.normalize(location + webContextConfig.webApp());
            Context webContext = tomcat.addWebapp(webContextConfig.contextPath(), webAppPath);
            if (webContextConfig.descriptor().length() > 0) {
                String webXmlPath = FilenameUtils.normalize(location + webContextConfig.descriptor());
                webContext.setAltDDName(webXmlPath);
                log.info(String.format("LOG00330: Using custom WEB descriptor '%s' for server=%s, context=%s", webXmlPath, configuration, webContextConfig.contextPath()));
            }
            ApplicationContextFacade servletContext = (ApplicationContextFacade) webContext.getServletContext();
            servletContext.setAttribute(ConfigurationConstants.SERVER_CONFIGURATION_KEY, configuration);
            servletContext.setAttribute(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY, webAppContext);
            attachRequestListener(servletContext, webAppContext);
            log.info("LOG00150: Adding web application for context [{}]", servletContext);
        }
    }

    private void addListeners() {
        for (Container container : tomcat.getHost().findChildren()) {
            ((Context) container).addServletContainerInitializer(new ServerExecutionRegistry(), null);
            log.info("LOG00230: Adding registry as listener ...");
        }
    }

    private void attachRequestListener(final ApplicationContextFacade context, final BaseContextConfiguration contextConfiguration) {
        tomcat.getServer().addLifecycleListener(new LifecycleListener() {
            @Override
            public void lifecycleEvent(LifecycleEvent event) {
                log.info("LOG00420: event.type={}", event.getType());
                if (event.getType().equals(Lifecycle.BEFORE_START_EVENT)) {
                    CallbackRequestEventListener requestEventListener = new CallbackRequestEventListener(configuration);
                    context.addListener(requestEventListener);
                    contextConfiguration.setRequestEventListener(requestEventListener);
                }
            }
        });
    }

    @Override
    public void start() {
        try {
            log.info("LOG00170: Starting '{}' on port {} ... ", getName(), getPort());
            tomcat.start();
            blockingLatch.await();  // block here until the #shutdown() is called.
        } catch (LifecycleException e) {
            log.error("LOG00120: Failed to start Tomcat", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            log.info("LOG00160: Stopping '{}' on port {} ...", getName(), getPort());
            tomcat.stop();
            blockingLatch.countDown();  // release the block.
        } catch (LifecycleException e) {
            log.error("LOG00120: Failed to stop Tomcat", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getPort() {
        return configuration.getPort();
    }

    @Override
    public String getName() {
        return ServerInfo.getServerInfo();
    }

    @Override
    public void waitForInitialisation() {
        try {
            log.info("LOG00260: Waiting for '{}' to initialise ... ", getName());
            latchOnAfterStart.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        log.info("LOG00250: [TOMCAT EVENT RECEIVED {}]: ", event.getType());
        if (event.getType().equals(Lifecycle.AFTER_START_EVENT)) {
            log.info("LOG00251: ... unlatching initialisation ... ");
            latchOnAfterStart.countDown();
        }
    }
}
