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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EventListener;
import javax.servlet.http.HttpServlet;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @version 0.0.1
 * @date 31/12/12
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicContext {

    /**
     * REQUIRED:
     * <p/>
     * Comma separated locations of .xml Spring configuration files.
     * <p/>
     * <b>IMPORTANT Maven build notice:</b>
     * <p/>
     * In Maven, the execution will happen inside a test context, all resources will be provisioned giving priority to the `test/resources` folders.
     *
     * @return
     * @see Server#bootstrap()
     */
    String[] springContextLocations();

    /**
     * If provided, the platform.webserver.configuration.ServerConfiguration#secured will be set to TRUE
     * <p/>
     * This allows to conditionally run the server in secure or non-secure mode, depending on the needs of the tests.
     *
     * @return
     */
    String[] securityContextLocations() default {};

    /**
     * Context path for the web application.  Default "/"
     *
     * @return
     */
    String contextPath() default Server.DEFAULT_CONTEXT_PATH;

    /**
     * Default is true.
     * <p/>
     * Set to false to disable the {@link org.jmockring.spring.mock.BeanAutoMockPostProcessor}.
     * <p/>
     * Note that this post-processor will not be configured in the context, so if there are missing bean definitions
     * the context startup will fail.
     *
     * @return
     */
    boolean autoMocks() default true;

    /**
     * Representation of <b>web.xml's</b> &lt;servlet&gt; and &lt;servlet-mapping&gt; tags.
     *
     * @return
     */
    Servlet[] servlets() default {@Servlet(servletClass = HttpServlet.class)};

    /**
     * Representation of <b>web.xml's</b> &lt;filter&gt; and &lt;filter-mapping&gt; tags.
     * <p/>
     * Ordering of the filters here is significant - similarly to doing it in web.xml
     *
     * @return
     */
    Filter[] filters() default {};


    /**
     * Representation of <b>web.xml's</b> &lt;context-param&gt; tag.
     *
     * @return
     */
    Param[] contextParams() default {};

    /**
     * Representation of <b>web.xml's</b> &lt;listener&gt; tag.
     * <p/>
     * Allows defining additional listeners to bootstrap frameworks upon servlet initialisation.
     *
     * @return
     */
    Class<? extends EventListener>[] listeners() default {};

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
     * Add additional properties.
     * <p/>
     * The properties will be at the top of the Spring environment properties source so they can override anything in the production config.
     * <p/>
     * When specifying the location <b>do not use</b> Spring's `classpath:` prefix - simply use the classpath relative location, e.g. `/path/to/test-config.properties`
     *
     * @return
     */
    String propertiesLocation() default "";

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
     * For web apps this points to the static resources folder.
     *
     * @return
     */
    String staticResourcesPath() default "";


    /**
     * The path to WAR file or the root folder of the web application.
     *
     * @return
     */
    String webApp() default "";

}
