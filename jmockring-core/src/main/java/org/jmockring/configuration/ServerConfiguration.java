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

package org.jmockring.configuration;

import static org.jmockring.utils.Functions.ifEmpty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jmockring.annotation.BootstrapConfig;
import org.jmockring.annotation.Server;
import org.jmockring.utils.PropertyFileReader;

/**
 * Aggregate configuration of all contexts on this serverConfig + serverConfig global settings.
 *
 * @author Pavel Lechev
 * @date 19/07/12
 */
public class ServerConfiguration {

    private Set<DynamicContextConfiguration> dynamicContexts = new HashSet<DynamicContextConfiguration>();

    private Set<WebAppContextConfiguration> webAppContexts = new HashSet<WebAppContextConfiguration>();

    private BootstrapConfig bootstrapConfig;

    private String propertiesLocation;

    private int port;

    private String executionName;

    private int defaultPort;

    private Properties cachedProperties;

    private Server serverConfig;

    public ServerConfiguration(Server serverConfig, BootstrapConfig bootstrapConfig) {
        this.serverConfig = serverConfig;
        this.bootstrapConfig = bootstrapConfig;
    }

    public Set<DynamicContextConfiguration> getDynamicContexts() {
        return Collections.unmodifiableSet(dynamicContexts);
    }

    public Set<WebAppContextConfiguration> getWebAppContexts() {
        return Collections.unmodifiableSet(webAppContexts);
    }

    public void addDynamicContext(DynamicContextConfiguration dynamicContextConfiguration) {
        this.dynamicContexts.add(dynamicContextConfiguration);
    }

    public void addWebAppContext(WebAppContextConfiguration webAppConfiguration) {
        this.webAppContexts.add(webAppConfiguration);
    }

    public String getPropertiesLocation() {
        return propertiesLocation;
    }

    public void setPropertiesLocation(String propertiesLocation) {
        this.propertiesLocation = propertiesLocation;
    }

    public String getExecutionName() {
        return executionName;
    }

    public void setExecutionName(String executionName) {
        this.executionName = executionName;
    }

    /**
     * Will return the port if it is explicitly configured here or try to
     * locate it in the available properties for the specified key.
     *
     * @param portPropertyKey
     * @return
     * @throws IllegalArgumentException if we don't have explicit configuration and the key is not passed
     *                                  or it doesn't point to a valid port
     * @should return explicitly configured port
     * @should throw exception if not explicitly configured port and portPropertyKey is null
     * @should return port from properties if available
     * @should return default port if port not available in properties
     */
    public int getPort(String portPropertyKey) {
        if (this.port > 0) {
            return port;
        }
        if (portPropertyKey == null) {
            throw new IllegalArgumentException("No explicit port configuration and null port property key");
        }
        try {
            this.port = Integer.parseInt(getProperties().getProperty(portPropertyKey));
            return this.port;
        } catch (NumberFormatException e) {
            if (defaultPort > 0) {
                return defaultPort;
            }
            throw new IllegalArgumentException(
                String.format("Can't determine externally configured port for property [%s]", portPropertyKey), e);
        }
    }

    /**
     * Convenience version of #getPort(key). to be used when port is explicitly configured here (this.port > 0)
     *
     * @return
     */
    public int getPort() {
        return getPort(null);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    public String getScheme() {
        return ifEmpty(serverConfig.scheme(), bootstrapConfig.scheme());
    }

    public String getHost() {
        return ifEmpty(serverConfig.host(), bootstrapConfig.host());
    }

    /**
     * @return
     * @should return cached properties
     */
    public Properties getProperties() {
        if (cachedProperties == null) {
            refresh();
        }
        return cachedProperties;
    }

    /**
     * Read all properties from the configured file and the system location.
     * @should preload properties from
     */
    public void refresh() {
        final String configFile = System.getProperty(ConfigurationConstants.EXTERNAL_CONFIG_SYSTEM_KEY);
        // load external properties, if any:
        Properties externalProperties = (configFile == null || configFile.isEmpty()) ? new Properties() : new PropertyFileReader(configFile).load();
        cachedProperties = propertiesLocation != null ? PropertyFileReader.fromClasspath(propertiesLocation).override(externalProperties) : externalProperties;
    }

    public void setBootstrapConfig(BootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
    }

    public BootstrapConfig getBootstrapConfig() {
        return bootstrapConfig;
    }

    public Server getServerConfig() {
        return serverConfig;
    }

    @Override
    public String toString() {
        return String.format("%s -> [%s://%s:%d]",
            getClass().getSimpleName(),
            getScheme(),
            getHost(),
            getPort());
    }
}
