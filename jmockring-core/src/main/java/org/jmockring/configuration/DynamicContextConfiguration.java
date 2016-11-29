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

import org.apache.commons.lang3.StringUtils;

import org.jmockring.annotation.DynamicContext;
import org.jmockring.annotation.Server;


/**
 * Assemble web application without the web.xml file.
 * <p/>
 * The servlet context is created programatically and Spring contexts bootstrapped from a list of XML configuration files.
 *
 * @author Pavel Lechev
 * @date 31/12/12
 */
public class DynamicContextConfiguration extends BaseContextConfiguration<DynamicContext> {

    private DynamicContext dynamicContext;

    public DynamicContextConfiguration(DynamicContext dynamicContext, Server server) {
        super(server);
        this.dynamicContext = dynamicContext;
    }

    /**
     * Return a list of all configured context files (comma-separated),
     * including the <code>securitySpringContextLocations</code> if <code><b>secured</b></code> is true.
     *
     * @return
     * @should aggregate configured context locations as string
     * @should aggregate configured context and security context locations as string
     */
    public String getAllContextLocationsAsString() {
        StringBuilder all = new StringBuilder(StringUtils.join(dynamicContext.springContextLocations(), ","));
        if (dynamicContext.securityContextLocations().length > 0) {
            all.append(StringUtils.join(dynamicContext.securityContextLocations(), ","));
        }
        return all.toString();
    }

    @Override
    public String getPropertiesLocation() {
        return dynamicContext.propertiesLocation();
    }

    @Override
    public String[] getExcludedContextLocationPatterns() {
        return dynamicContext.excludedConfigLocations();
    }

    @Override
    public Class[] getForcedMockTypes() {
        return dynamicContext.forcedMockTypes();
    }

    @Override
    public String getContextPath() {
        return dynamicContext.contextPath();
    }

    @Override
    public boolean isEnableAutoMocks() {
        return dynamicContext.autoMocks();
    }

    @Override
    public DynamicContext getConfig() {
        return dynamicContext;
    }

}
