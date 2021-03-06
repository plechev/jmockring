package org.jmockring.configuration;

import org.springframework.context.ApplicationContext;

/**
 * @author Pavel Lechev
 * @since 15/07/13
 */
public interface ServerConfigurationAware {

    /**
     *
     * @return
     */
    ServerConfiguration getServerConfiguration();

    /**
     *
     * @return
     */
    BaseContextConfiguration getApplicationContextConfiguration();

    /**
     *
     * @return
     */
    ApplicationContext getSpringContext();
}
