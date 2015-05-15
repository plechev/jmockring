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

package org.jmockring.configuration;

import org.jmockring.annotation.Server;
import org.jmockring.webserver.WebServer;

/**
 * Specification of a running server with web-app context.
 * <p/>
 * This is used as a pointer to locate specific server configuration / context in the {@link ServerExecutionRegistry}
 *
 * @author Pavel Lechev
 * @date 03/01/13
 */
public class ServerSpec {

    private Class<? extends WebServer> targetServer;

    private String executionName = Server.DEFAULT_EXECUTION_NAME;

    private String contextPath = Server.DEFAULT_CONTEXT_PATH;

    private ServerSpec() {
    }

    public ServerSpec withServer(Class<? extends WebServer> targetServer) {
        this.targetServer = targetServer;
        return this;
    }

    public ServerSpec withName(String executionName) {
        this.executionName = executionName;
        return this;
    }

    public ServerSpec withContext(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public Class<? extends WebServer> getTargetServer() {
        return targetServer;
    }

    public String getExecutionName() {
        return executionName;
    }

    public String getContextPath() {
        return contextPath;
    }

    public static ServerSpec forContext(String contextPath) {
        return new ServerSpec().withContext(contextPath);
    }

    public static ServerSpec forServer(Class<? extends WebServer> targetServer) {
        return new ServerSpec().withServer(targetServer);
    }

    public static ServerSpec forName(String executionName) {
        return new ServerSpec().withName(executionName);
    }


    @Override
    public String toString() {
        return new StringBuilder()
                .append("ServerSpec[")
                .append("targetServer='").append(targetServer).append('\'')
                .append(", executionName='").append(executionName).append('\'')
                .append(", contextPath='").append(contextPath).append('\'')
                .append(']')
                .toString();
    }
}
