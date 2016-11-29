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

package org.jmockring.annotation;

/**
 * @author Pavel Lechev
 * @since 01/01/13
 */
public @interface WebContext {

    /**
     * The path to WAR file or the root folder of the web application.
     *
     * @return
     */
    String webApp();

    /**
     * Context path for the web application. Default "/".
     *
     * @return
     */
    String contextPath() default Server.DEFAULT_CONTEXT_PATH;

    /**
     * Disable auto-mocking
     *
     * @return
     */
    boolean autoMocks() default true;

    /**
     * Path to web.xml. If empty the default is used ({@link #webApp()}/WEB-INF/web.xml)
     *
     * @return
     */
    String descriptor() default "";

    /**
     * List of patterns specifying the XML context files to <u>exclude</u> from the created {@link org.springframework.context.ApplicationContext}.
     * <p/>
     * This allows to load partial Spring context, which will trigger the auto-mocking of the missing beans thus allowing layered testing,
     * e.g. skip the repository context to test MVC+services without the need to connect to database.
     *
     * @return
     */
    String[] excludedConfigLocations() default {};

    /**
     * Specify classes or interfaces whose beans should always be mocked even if
     * real bean definition exists in the context.
     * <p/>
     * While generally we use auto-mocking for the omitted bean definitions, it is sometimes useful to
     * force mocks on certain beans which would otherwise be part of the context in their real form.
     *
     * @return
     */
    Class[] forcedMockTypes() default {};

    /**
     * Add additional properties.
     * <p/>
     * The properties will be at the top of the Spring environment properties source so they can override anything in the production config.
     * <p/>
     * When specifying the location <b>do not use</b> Spring's `classpath:` prefix - simply use the classpath relative location, e.g. `/path/to/test-config.properties`
     *
     * @return
     */
    String propertiesLocation() default "";

}
