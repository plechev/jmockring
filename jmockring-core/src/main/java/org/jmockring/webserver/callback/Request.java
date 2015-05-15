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

import java.util.Map;

/**
 * Hand-off of {@link javax.servlet.http.HttpServletRequest} from the executing server to the test class.
 * <p/>
 * It is safer to create copy of the essential request data and pass
 * it to another thread than to pass the request object itself
 * <p/>
 * Servlet API requests are thread-local instances and tossing them around would be dangerous.
 *
 * @author Pavel Lechev
 * @date 31/01/13
 */
public final class Request {

    private final String requestUri;

    private final Method method;

    final private Map<String, String[]> parameters;


    public Request(Method method, String requestUri, Map<String, String[]> parameters) {
        this.method = method;
        this.requestUri = requestUri;
        this.parameters = parameters;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public Method getMethod() {
        return method;
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }

    public enum Method {
        GET, POST, PUT, HEAD, DELETE, OPTIONS, TRACE
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Request");
        sb.append("{method=").append(method);
        sb.append(", requestUri='").append(requestUri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
