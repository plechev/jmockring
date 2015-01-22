package org.jmockring.provider;/*
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

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jmockring.annotation.DynamicContext;
import org.jmockring.annotation.Server;
import org.jmockring.annotation.Servers;
import org.jmockring.annotation.Servlet;
import org.jmockring.junit.ExternalServerJUnitSuiteRunner;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Reference implementation of the {@link org.jmockring.junit.ExternalServerJUnitSuiteRunner} usage to write configuration suites.
 * Can be used for:
 * 1) Deploy the services with mocked layers (i.e. mocked service layer, mocked repository layer, etc ...)
 * 2) Conditionally deploy the services in SECURE or NON-SECURE mode
 * (see {@link org.jmockring.annotation.Server#dynamicContexts()}).
 * 3) Full automated integration testing
 * <p/>
 * <b>NOTE:</b>
 * <p/>
 * If tests are included in a suite, do not include the words 'Test' or 'IT' in their name or maven's surefire/failsafe plugins will
 * run them both as part of the suite and individually.
 * TODO:: come up with a solution to this limitation imposed by surefire/failsafe plugins .
 *
 * @author Pavel Lechev
 * @date 20/07/12
 */
@RunWith(ExternalServerJUnitSuiteRunner.class)
// [Not ready] @Security("security/example.policy")
@Servers(value = {
        @Server(
                dynamicContexts = @DynamicContext(
                        springContextLocations = "classpath:/spring/application-context.xml",
                        contextPath = "/context1",
                        servlets = {@Servlet(servletClass = HttpServletDispatcher.class)}
                ),
                testClass = SuiteIT.class
        )
})
@Suite.SuiteClasses({
        CheckServiceProviders.class
})
public class SuiteIT {

}
