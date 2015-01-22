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

package org.jmockring.utils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavel Lechev
 * @date 19/07/12
 */
public final class PortChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PortChecker.class);

    public static final int MIN_PORT_NUMBER = 1025; // do not ever use ports under 1024

    public static final int MAX_PORT_NUMBER = 65000;

    private PortChecker() {
    }

    /**
     * @param port
     *
     * @return
     */
    public static boolean available(int port) {
        return available(port, false);
    }

    /**
     * @param port
     * @param strict
     *
     * @return
     */
    public static boolean available(int port, boolean strict) {
        if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            if (strict && portTaken(port)) {
                return false;
            }
            return true;
        } catch (IOException e) {
            LOG.error("Connection error: {}", e.getMessage());
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    LOG.error("Connection error: {}", e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * Perform an extra check ...
     *
     * @param portNumber
     *
     * @return
     * @throws java.io.IOException
     */
    private static boolean portTaken(int portNumber) throws IOException {
        try {
            LOG.debug("{}: Checking if port open by trying to connect as a spi", portNumber);
            Socket sock = new Socket("localhost", portNumber);
            sock.close();
            LOG.debug("{}: Someone responding on port - seems not open", portNumber);
            return false;
        } catch (IOException e) {
            if (e.getMessage().contains("refused")) {
                return true;
            }
            LOG.error("Troubles checking if port is open", e);
            throw e;
        }
    }


}
