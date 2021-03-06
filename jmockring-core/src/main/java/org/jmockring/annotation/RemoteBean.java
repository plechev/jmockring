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
 * Inject a real bean from the remote Spring context.
 * <p/>
 * Performs validation to ensure the object is NOT a Mockito mock, but real instance.
 * <p/>
 * <b>Notice:</b>
 * <p/>
 * This annotation is typically used to retrieve real remote beans whose behaviour we are testing.
 * Since some of these beans may be proxied by Spring, the declaring type of the injected field should be an interface,
 * not concrete implementation.
 *
 * @author Pavel Lechev
 * @date 02/01/13
 * @see org.jmockring.junit.PoshTestPostProcessor
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteBean {

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
     * The type of the bean to inject.
     * <p/>
     * Optional - the type will be determined by the target field type, but can be specified explicitly here.
     *
     * @return
     */
    Class<?> type() default RemoteBean.class;

    /**
     * The name of the bean to inject (optional).
     *
     * @return
     */
    String beanName() default "";

}
