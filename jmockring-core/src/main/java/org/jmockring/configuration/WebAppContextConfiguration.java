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

import org.jmockring.annotation.Server;
import org.jmockring.annotation.WebContext;

/**
 * Deploy application with `web.xml`.
 * <p/>
 * Partial Spring contexts with auto-mocking can be supported only if the `web.xml` is customised for testing only.
 *
 * @author Pavel Lechev
 * @version 0.0.1
 * @date 01/01/13
 */
public class WebAppContextConfiguration extends BaseContextConfiguration<WebContext> {

    private final WebContext webContext;

    public WebAppContextConfiguration(WebContext webContext, Server server) {
        super(server);
        this.webContext = webContext;
    }

    @Override
    public WebContext getConfig() {
        return webContext;
    }

    @Override
    public String getPropertiesLocation() {
        return webContext.propertiesLocation();
    }

    @Override
    public String[] getExcludedContextLocationPatterns() {
        return webContext.excludedConfigLocations();
    }

    @Override
    public Class[] getForcedMockTypes() {
        return webContext.forcedMockTypes();
    }

    @Override
    public String getContextPath() {
        return webContext.contextPath();
    }

    @Override
    public boolean isEnableAutoMocks() {
        return webContext.autoMocks();
    }
}
