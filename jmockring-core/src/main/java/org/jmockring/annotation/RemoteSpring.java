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
import org.springframework.context.ApplicationContext;

/**
 * Inject the Spring context directly into the test class.
 * <p/>
 * The type of the target field can be anything that extends {@link org.springframework.context.ApplicationContext},
 * however it must be compatible with the actual context class retrieved from the remote execution.
 *
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 02/01/13
 * @see org.jmockring.junit.PoshTestPostProcessor
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteSpring {

    /**
     * @return
     * @see Server#name()
     */
    String executionName() default Server.DEFAULT_EXECUTION_NAME;

    /**
     * REQUIRED: The context path for the web app whose Spring context we are injecting.
     * <p/>
     * This will always inject the topmost context (the parent of all other Spring contexts)
     *
     * @return
     */
    String contextPath() default "";

    /** @return  */
    Class<? extends WebServer> bootstrap() default WebServer.class;

    /**
     * Annotation classes do not mix well with static fields so we find a backdoor ...
     * <p/>
     * Ohhh ... Java ...
     */
    static interface ContextSupertype {

        /**
         * The target fields annotated with this annotation must be assignable to this type.
         * <p/>
         * Currently this is any {@link ApplicationContext} but could be narrowed to something else if needed in the future.
         */
        Class EXPECTED_CONTEXT_SUPERTYPE = ApplicationContext.class;

    }
}
