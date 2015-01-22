package org.jmockring.spi;

/**
 * Callback interface to cleanup after all tests in the suite have completed.
 * <p/>
 * This is invoked after the server is shut down.
 * <p/>
 * Implementations must provide no-arg constructor.
 *
 * @author Pavel Lechev <plechev@cisco.com>
 * @since 08/11/13
 */
public interface PostShutdownHook {

    void onTestsComplete();

}
