package org.jmockring.spi;

/**
 * Callback interface to setup before any tests in the suite have started.
 * <p/>
 * This is invoked before the servers are started.
 * <p/>
 * Implementations must provide no-arg constructor.
 *
 * @author Pavel Lechev <plechev@cisco.com>
 * @since 08/11/13
 */
public interface PreStartupHook {

    void beforeTestsCommence();

}
