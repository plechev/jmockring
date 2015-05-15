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

package org.jmockring.security;

import java.security.AllPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Pavel Lechev
 * @date 27/01/13
 */
public class GrantedPermissions {

    private static final Map<Permission, String[]> permissions = new LinkedHashMap<Permission, String[]>();

    static {
        permissions.put(new AllPermission(), new String[]{
                "/org/mockito/",
                "/org/eclipse/jetty/",
                "/org/objenesis/",
                "/junit",
                "/junit-jmockring",
        });
        permissions.put(new RuntimePermission("accessClassInPackage.sun.reflect"), new String[]{
                "/org/springframework/",
        });
        permissions.put(new RuntimePermission("reflectionFactoryAccess"), new String[]{
                "/org/springframework/",
        });
    }

    public static Set<Permission> getPermissionsForPath(String fullPath) {
        Set<Permission> foundPermissions = new HashSet<Permission>();
        for (Map.Entry<Permission, String[]> entry : permissions.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (fullPath.contains(pattern)) {
                    foundPermissions.add(entry.getKey());
                }
            }
        }
        return foundPermissions;
    }

}
