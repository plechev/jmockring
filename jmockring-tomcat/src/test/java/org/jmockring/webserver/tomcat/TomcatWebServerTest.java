package org.jmockring.webserver.tomcat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;

import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.jmockring.annotation.BootstrapConfig;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.WebContext;
import org.jmockring.configuration.ServerConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class TomcatWebServerTest {

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

    /**
     * @verifies return tomcat server name from distribution
     * @see TomcatWebServer#getName()
     */
    @Test
    public void getName_shouldReturnTomcatServerNameFromDistribution() throws Exception {
        assertThat(underTest.getName(), is("Apache Tomcat/7.0.42"));
    }

    /**
     * @verifies return port in use
     * @see TomcatWebServer#getPort()
     */
    @Test
    public void getPort_shouldReturnPortInUse() throws Exception {
        when(configurationMock.getPort()).thenReturn(1088);
        assertThat(underTest.getPort(), is(1088));
    }

    /**
     * @verifies not release initialisation latch if event is not AFTER_START_EVENT
     * @see TomcatWebServer#lifecycleEvent(LifecycleEvent)
     */
    @Test
    public void lifecycleEvent_shouldNotReleaseInitialisationLatchIfEventIsNotAFTER_START_EVENT() throws Exception {

        underTest.lifecycleEvent(new LifecycleEvent(new StandardEngine(), Lifecycle.AFTER_INIT_EVENT, new Object()));

        verifyZeroInteractions(latchOnAfterStartMock);
    }

    /**
     * @verifies release initialisation latch on AFTER_START_EVENT
     * @see TomcatWebServer#lifecycleEvent(org.apache.catalina.LifecycleEvent)
     */
    @Test
    public void lifecycleEvent_shouldReleaseInitialisationLatchOnAFTER_START_EVENT() throws Exception {

        underTest.lifecycleEvent(new LifecycleEvent(new StandardEngine(), Lifecycle.AFTER_START_EVENT, new Object()));

        verify(latchOnAfterStartMock).countDown();

    }

    /**
     * @verifies execute server shutdown and release blocking latch
     * @see TomcatWebServer#shutdown()
     */
    @Test
    public void shutdown_shouldExecuteServerShutdownAndReleaseBlockingLatch() throws Exception {

        underTest.shutdown();

        verify(tomcatMock).stop();
        verify(blockingLatchMock).countDown();

        verifyNoMoreInteractions(tomcatMock, blockingLatchMock);
    }

    /**
     * @verifies start tomcat instance and activate blocking latch
     * @see TomcatWebServer#start()
     */
    @Test
    public void start_shouldStartTomcatInstanceAndActivateBlockingLatch() throws Exception {

        underTest.start();

        verify(tomcatMock).start();
        verify(blockingLatchMock).await();

        verifyNoMoreInteractions(tomcatMock, blockingLatchMock);
    }

    /**
     * @verifies block until server initialised
     * @see TomcatWebServer#waitForInitialisation()
     */
    @Test
    public void waitForInitialisation_shouldBlockUntilServerInitialised() throws Exception {

        underTest.waitForInitialisation();

        verify(latchOnAfterStartMock).await();

        verifyZeroInteractions(tomcatMock, blockingLatchMock);

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
