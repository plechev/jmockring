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

import org.jmockring.spi.PostShutdownHook;
import org.jmockring.spi.PreStartupHook;

/**
 * Global configuration settings for bootstrapping server instances.
 * <p/>
 * All settings here must have default values.
 *
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 30/01/13
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface BootstrapConfig {

    String SCHEME_HTTP = "http";

    String SCHEME_HTTPS = "https";

    String HOST = "localhost";


    /**
     * Max number of connect attempts for confirmation that the server has started.
     * There is a pause of 1s between retries.
     * <p/>
     * Default is 10. Increase on slower or overloaded systems.
     *
     * @return
     */
    int numberOfAttempts() default 10;

    String scheme() default SCHEME_HTTP;

    String host() default HOST;

    /**
     * Set system properties before the web application is started
     *
     * @return
     */
    Param[] systemProperties() default {};

    /**
     * Register shutdown cleanup hook.
     *
     * @return
     * @see org.jmockring.spi.PostShutdownHook
     */
    Class<? extends PostShutdownHook> shutdownHook() default PostShutdownHook.class;


    /**
     * Register shutdown cleanup hook.
     *
     * @return
     * @see org.jmockring.spi.PostShutdownHook
     */
    Class<? extends PreStartupHook> startupHook() default PreStartupHook.class;

    /**
     * Hold the default configuration, if the test class is not annotated with this explicitly.
     */
    @BootstrapConfig
    public static class DEFAULT {

        public static BootstrapConfig getConfig() {
            return (BootstrapConfig) DEFAULT.class.getAnnotations()[0];
        }
    }

}
