package org.jmockring;

import java.util.concurrent.Semaphore;

import org.jmockring.configuration.ServerConfiguration;
import org.jmockring.configuration.ServerExecutionConfiguration;
import org.jmockring.configuration.ServerExecutionRegistry;
import org.jmockring.spi.MockProviderSPI;
import org.jmockring.spi.PluggableServiceLoader;

/**
 * General utilities for making JMockring more useful.
 *
 * @author Pavel Lechev <plechev@cisco.com>
 * @since 29/07/13
 */
public final class JMockring {


    private static final MockProviderSPI mockingProvider = PluggableServiceLoader.loadMockingProvider(false);


    /**
     * <u>!!! WARNING !!!</u>
     * <p/>
     * Extreme care should be exercised when using this method to ensure <b>no usages</b> are committed to versioned code.
     * If such calls leak into the CI environment, it will cause the build to hang on forever.
     * <p/>
     * This is a convenience method for suspending the test execution indefinitely.
     * The call to it will typically be either in the test #setUp() method or at the very beginning of the test method itself.
     * <p/>
     * Once this is called, it suspends the test execution thread and <b>the only</b> way to continue is to forcefully shut down the test.
     * <p/>
     * It may be used for experimenting when we need to connect to the running server from outside our test.
     * For example: using tools like SoapUI, Postman, RESTCLient, etc ...
     * <p/>
     * Effectively this gives us a running web server with our application deployed on it and allows us to
     * experiment with different request payloads and monitor the responses (headers, status codes, cookies, etc ...) or simply debug the application.
     * <p/>
     * <p/>
     */
    public static void suspend() {
        ServerExecutionConfiguration[] servers = ServerExecutionRegistry.getAllServers();
        StringBuilder hosts = new StringBuilder();
        for (ServerExecutionConfiguration server : servers) {
            ServerConfiguration serverConfiguration = server.getConfiguration();
            hosts.append(String.format("\n\t%s://%s:%d",
                            server.getConfiguration().getScheme(),
                            server.getConfiguration().getServerConfig().host(),
                            server.getConfiguration().getPort())
            );
        }
        try {
            System.err.println(String.format("Suspending test execution. You can now connect to the running server on: %s", hosts));
            new Semaphore(0).acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Determine if the supplied instance is a mock.
     * The evaluation will be delegated to the available mocking provider (currently only Mockito is being discovered) if one is available on the classpath.
     * <p/>
     * If no provider is available this call will throw {@link IllegalStateException}.
     *
     * @param instance
     * @return
     * @throws IllegalStateException if no mocking provider is available
     * @should call mocking provider to determine mock state
     * @should call mocking provider to determine non mock state
     */
    public static boolean isMock(Object instance) {
        if (mockingProvider == null) {
            throw new IllegalStateException("Mocking provider is not available. Can not test `isMock` condition!");
        }
        return mockingProvider.isMock(instance);
    }

}
