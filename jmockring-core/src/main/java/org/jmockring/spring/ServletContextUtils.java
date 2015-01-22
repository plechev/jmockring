package org.jmockring.spring;

import javax.servlet.ServletContext;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.configuration.ConfigurationConstants;
import org.jmockring.configuration.ServerConfiguration;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @since 15/07/13
 */
public class ServletContextUtils {

    /**
     * @param context
     *
     * @return
     */
    public static ServerConfiguration getConfigurationFromContext(ServletContext context) {
        return (ServerConfiguration) context.getAttribute(ConfigurationConstants.SERVER_CONFIGURATION_KEY);
    }

    /**
     * @param context
     *
     * @return
     */
    public static BaseContextConfiguration getContextConfigurationFromContext(ServletContext context) {
        return (BaseContextConfiguration) context.getAttribute(ConfigurationConstants.CONTEXT_CONFIGURATION_KEY);
    }
}
