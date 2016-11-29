package org.jmockring.webserver.tomcat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.jmockring.annotation.BootstrapConfig;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.WebContext;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.ServerExecutionRegistry;
import org.jmockring.configuration.WebAppContextConfiguration;
import org.jmockring.webserver.callback.CallbackRequestEventListener;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TomcatWebServer.class)
public class TomcatWebServerPowerMockTest {

    @Mock(name = "blockingLatch")
    private CountDownLatch blockingLatchMock;

    @Mock(name = "latchOnAfterStart")
    private CountDownLatch latchOnAfterStartMock;

    @Mock
    private Connector connectorMock;

    @Mock
    private StandardContext containerMock;

    @Mock
    private Engine engineMock;

    @Mock
    private ServerConfiguration configurationMock;

    @Mock
    private org.apache.catalina.Server serverMock;

    @Mock
    private Tomcat tomcatMock;

    @Captor
    private ArgumentCaptor<LifecycleListener> listenerCaptor;

    @InjectMocks
    private TomcatWebServer underTest;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(Tomcat.class).withNoArguments().thenReturn(tomcatMock);

        PowerMockito.whenNew(CountDownLatch.class).withArguments(1)
                .thenReturn(latchOnAfterStartMock)
                .thenReturn(blockingLatchMock);

        when(tomcatMock.getEngine()).thenReturn(engineMock);
        when(tomcatMock.getConnector()).thenReturn(connectorMock);
        when(tomcatMock.getServer()).thenReturn(serverMock);
        final Host hostMock = mock(Host.class);
        when(tomcatMock.getHost()).thenReturn(hostMock);
        when(hostMock.findChildren()).thenReturn(new StandardContext[]{containerMock});
    }


    /**
     * @verifies initialise server configuration
     * @see TomcatWebServer#initialise(ServerConfiguration)
     */
    @Test
    public void initialise_shouldInitialiseServerConfiguration() throws Exception {

        final String loc = ".";
        final Server serverConfig = DummyTest.class.getAnnotation(Server.class);
        ServerConfiguration configuration = new ServerConfiguration(serverConfig, DummyTest.class.getAnnotation(BootstrapConfig.class));
        configuration.setPort(1008);
        final WebAppContextConfiguration cxtConfMock = mock(WebAppContextConfiguration.class);
        when(cxtConfMock.getConfig()).thenReturn(serverConfig.webContexts()[0]);
        final Context cxtMock = mock(Context.class);
        when(tomcatMock.addWebapp("/context1", FilenameUtils.normalize(loc + "../../src/main/webapp"))).thenReturn(cxtMock);
        final ApplicationContextFacade contextFacade = mock(ApplicationContextFacade.class);
        when(cxtMock.getServletContext()).thenReturn(contextFacade);

        configuration.addWebAppContext(cxtConfMock);

        underTest.initialise(configuration);

        verify(tomcatMock).noDefaultWebXmlPath();
        verify(tomcatMock).setBaseDir(FilenameUtils.normalize(loc + "/../tomcat-work"));
        verify(tomcatMock).setHostname("host.net");
        verify(engineMock).setDefaultHost("host.net");
        verify(connectorMock).setScheme("https");
        verify(connectorMock).setPort(1008);
        verify(connectorMock).setSecure(true);
        verify(tomcatMock).setSilent(false);
        verify(serverMock).addLifecycleListener(underTest);

        PowerMockito.verifyNew(CountDownLatch.class, times(2)).withArguments(1);

        verify(tomcatMock).init();
        verify(containerMock).addServletContainerInitializer(any(ServerExecutionRegistry.class), Matchers.<Set<Class<?>>>eq(null));
        verify(cxtMock).setAltDDName(FilenameUtils.normalize(loc + "../../path/to/custom/web.xml"));
        verify(contextFacade).setAttribute(ConfigurationConstants.SERVER_CONFIGURATION_KEY, configuration);
        verify(contextFacade).setAttribute(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY, cxtConfMock);

        // verify the context was added correctly to the listener callback by simulating event execution and verify interactions:
        verify(serverMock, times(2)).addLifecycleListener(listenerCaptor.capture());
        listenerCaptor.getValue().lifecycleEvent(new LifecycleEvent(new StandardServer(), Lifecycle.BEFORE_START_EVENT, this));

        ArgumentCaptor<CallbackRequestEventListener> crel = ArgumentCaptor.forClass(CallbackRequestEventListener.class);
        verify(contextFacade).addListener(crel.capture());
        verify(cxtConfMock).setRequestEventListener(crel.getValue()); // assert that the same instance is passed as above
    }


    @Server(
            port = 6060,
            name = "TomcatWebServerContext",
            webContexts = {
                    @WebContext(
                            webApp = "../../src/main/webapp",
                            contextPath = "/context1",
                            descriptor = "../../path/to/custom/web.xml"
                    )
            },
            propertiesLocation = "/mock/base-webserver-ri.properties",
            bootstrap = TomcatWebServer.class,
            testClass = DummyTest.class,
            host = "host.net",
            scheme = "https"
    )
    @BootstrapConfig(host = "host-alt.net", numberOfAttempts = 20, scheme = "https", systemProperties = {@Param(name = "sys.prop1", value = "prop1.value")})
    public static class DummyTest {

    }


}
