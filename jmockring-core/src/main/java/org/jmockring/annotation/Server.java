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

import org.jmockring.webserver.WebServer;

/**
 * Configure a single server execution.
 * <p/>
 * Contexts can be started in either "real-life" or "partial" mode, depending on what Spring dynamicContexts are
 * passed to {@code #springContextLocations()}.
 * <p/>
 * In addition, security can be enabled/disabled on a service (if the bootstrap class is written correctly), simply by
 * providing {@code #securityContextLocations} or not.
 * <p/>
 * This allows greater degree of flexibility when writing tests.
 * <p/>
 *
 * @author Pavel Lechev
 * @date 19/07/12
 * @see #bootstrap()
 * @see org.jmockring.junit.ExternalServerJUnitRunner
 * @see org.jmockring.junit.ExternalServerJUnitSuiteRunner
 * @see org.jmockring.junit.ServerBootstrap
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Server {

    String DEFAULT_EXECUTION_NAME = "[default]";

    /**
     *
     */
    String DEFAULT_CONTEXT_PATH = "/";

    /**
     * External properties file location (classpath relative, but should not(!) be prepended with `classpath:`).
     * <p/>
     * This will be merged in Spring 3.1 {@link org.springframework.core.env.Environment} sub-system
     * as the first {@link org.springframework.core.env.PropertySource} ensuring that any settings here will be resolved first,
     * thus allowing to override anything that is configured via properties sources from inside the test execution.
     *
     * @return
     */
    String propertiesLocation() default "";

    /**
     * The Main class used to bootstrap the server.
     * <p/>
     * <p/>
     * <b>IMPORTANT:</b>
     * <p/>
     * It is a requirement that this class has a constructor that accepts a single argument of type
     * {@link org.jmockring.configuration.ServerConfiguration} and all server configuration is embedded in that object.
     *
     * @see DynamicContext#springContextLocations()
     */
    Class<? extends WebServer> bootstrap() default WebServer.class;

    /**
     * Dynamic port allocation:
     * <p/>
     * To be able to run multiple servers in parallel on the same box,
     * we need to ensure ports are dynamically allocated based on availability.
     * <p/>
     * This specifies the starting point where to check for port availability and will run until
     * it finds available port or reaches the max allowed.
     * <p/>
     * In most cases it should be ok to leave this as default and let the {@link org.jmockring.junit.ServerBootstrap}
     * allocate ports dynamically. The clash-avoidance routine should provide good enough results.
     * If it fails, the starting range can be adjusted here for different tests which should further
     * reduce the chance of bumping on an already allocated port.
     *
     * @return
     * @see org.jmockring.junit.ServerBootstrap#allocateAvailablePort(Server)
     */
    int startAtPort() default 30000;

    /**
     * By default ports will be allocated dynamically.
     * This can be changed here - just specify a port number and ensure it does not clash with anything else on the same machine.
     *
     * @return
     */
    int port() default -1;

    /**
     * Name this execution.
     * <p/>
     * If we bootstrap two or more instances of the same server (bootstrap() is same), its good to
     * include this name, so we can distinguish them,
     * <p/>
     * This is not likely to happen very often (if ever), so in most cases we can work with the default and do not ask for it anywhere.
     * <p/>
     *
     * @return
     */
    String name() default DEFAULT_EXECUTION_NAME;

    /**
     * Define dynamically constructed Spring contexts.
     *
     * @return
     */
    DynamicContext[] dynamicContexts() default {};

    /**
     * @return
     */
    WebContext[] webContexts() default {};

    /**
     * Write out Spring context debug info.
     *
     * @return
     */
    boolean enableDebug() default false;

    /**
     * @return
     */
    String host() default BootstrapConfig.HOST;

    /**
     * @return
     */
    String scheme() default BootstrapConfig.SCHEME_HTTP;

    /**
     * Specify the test class or the suite where this annotation is used.
     * <p/>
     * This is used internally to identify the code source location of the running tests.
     *
     * @return
     * @see org.jmockring.annotation.WebContext#webApp()
     */
    Class<?> testClass();

}
