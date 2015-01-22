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

package org.jmockring.webserver;

import org.jmockring.configuration.ServerConfiguration;

/**
 * Web Server abstraction.
 * <p/>
 * The concrete implementation can be anything that can run WAR applications.
 * <p/>
 * All implementing classes must have a public constructor accepting a single argument of type {@link org.jmockring.configuration.ServerConfiguration}
 *
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 25/01/13
 */
public interface WebServer {


    /**
     * @param configuration
     */
    void initialise(ServerConfiguration configuration);

    /**
     * Start the server.
     * <p/>
     * This method must block the caller thread for as long as the server is running.
     */
    void start();

    /**
     * Graceful server shutdown.
     * <p/>
     * The task executor should assume that after calling this method the server thread may continue to run for some time
     * until all shutdown procedures have completed.
     */
    void shutdown();

    /**
     * Get the TCP/IP port on which the server is listening.
     * Currently only one listener is started per-server, so we only have one port.
     *
     * @return
     */
    int getPort();

    /**
     * Return a human readable server name.
     *
     * @return
     */
    String getName();

    /**
     * This method will block the current thread until the server is initialised.
     * <p/>
     * In most cases the server connectors will start and will listen to the specific port before the web application is fully initialised.
     * This method allows the server to block the caller until the applications are ready to use.
     */
    void waitForInitialisation();
}
