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

package org.jmockring.webserver.callback;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jmockring.configuration.ServerConfiguration;

/**
 * Provide thread synchronisation for HTTP requests handled by the bootstrapped server.
 *
 * @author Pavel Lechev
 * @date 31/01/13
 * @see org.jmockring.configuration.BaseContextConfiguration#getRequestEventListener()
 * @see org.jmockring.annotation.RemoteRequestListener
 */
public final class CallbackRequestEventListener implements ServletRequestListener {

    private static final Logger log = LoggerFactory.getLogger(CallbackRequestEventListener.class);

    /**
     * Max number of seconds allowed for the timeout parameters.
     * <p/>
     * This is a safety net preventing blocking of the test execution for unreasonable amount of time.
     */
    private final static int ABSOLUTE_MAX_TIMEOUT = 300;

    /**
     * Sync blocking queue used to handoff the request when it is initiated (before any filters or servlets are called)
     */
    private SynchronousQueue<Request> startRendezvous = new SynchronousQueue<Request>(true);

    /**
     * Sync blocking queue used to handoff the request when it has completed (after all filters and servlets have been invoked)
     */
    private SynchronousQueue<Request> endRendezvous = new SynchronousQueue<Request>(true);

    private final ServerConfiguration serverConfiguration;

    public CallbackRequestEventListener(ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        log.info("LOG00000: requestDestroyed: {}", request);
        endRendezvous.offer(new Request(
                Request.Method.valueOf(request.getMethod().toUpperCase()),
                request.getRequestURI(),
                request.getParameterMap()));
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        log.info("LOG00010: requestInitialized: {}", request);
        startRendezvous.offer(new Request(
                Request.Method.valueOf(request.getMethod().toUpperCase()),
                request.getRequestURI(),
                request.getParameterMap()));
    }

    /**
     * Suspend the calling thread until a request is received or timeout is reached.
     *
     * @param timeout max time in seconds to wait for the request.
     *
     * @return Request details or null if the timeout was reached
     */
    public Request waitForRequestStart(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be more than 0.");
        }
        if (timeout > ABSOLUTE_MAX_TIMEOUT) {
            throw new IllegalArgumentException("Maximum timeout " + timeout + " exceeds " + ABSOLUTE_MAX_TIMEOUT + " seconds.");
        }
        try {
            log.info("LOG00020: waitForRequestStart()");
            return startRendezvous.poll(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted", e);
        }
    }

    /**
     * Same as #waitForRequestStart but will only return the request if its URI contains the pattern.
     * <p/>
     * May potentially block for longer than <code>timeout</code> as it will wait for specific request arriving and discard the non-matching ones.
     *
     * @param uriPattern
     * @param timeout
     *
     * @return
     */
    public Request waitForRequestStart(String uriPattern, int timeout) {
        while (true) {
            Request request = waitForRequestStart(timeout);
            if (request == null) {
                return null;
            }
            if (request.getRequestUri().contains(uriPattern)) {
                return request;
            }
        }
    }

    /**
     * Same as #waitForRequestEnd but will only return the request if its URI contains the pattern.
     * <p/>
     * May potentially block for longer than <code>timeout</code> as it will wait for specific request arriving and discard the non-matching ones.
     *
     * @param uriPattern
     * @param timeout
     *
     * @return
     */
    public Request waitForRequestEnd(String uriPattern, int timeout) {
        while (true) {
            Request request = waitForRequestEnd(timeout);
            if (request == null) {
                return null;
            }
            if (request.getRequestUri().contains(uriPattern)) {
                return request;
            }
        }
    }


    /**
     * Suspend the calling thread until a request was processed or timeout is reached.
     *
     * @param timeout max time in seconds to wait for the request.
     *
     * @return Request details or null if the timeout was reached
     */
    public Request waitForRequestEnd(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be more than 0.");
        }
        if (timeout > ABSOLUTE_MAX_TIMEOUT) {
            throw new IllegalArgumentException("Maximum timeout " + timeout + " exceeds " + ABSOLUTE_MAX_TIMEOUT + " seconds.");
        }
        try {
            log.info("LOG00020: waitForRequestEnd()");
            return endRendezvous.poll(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted", e);
        }
    }

}
