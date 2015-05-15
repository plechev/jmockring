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

import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author Pavel Lechev
 * @date 27/01/13
 */
public class SecurityUtils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * This must be called by the main thread that bootstrapped the tests execution.
     *
     * @param securityPolicy
     */
    public static void enableSecurity(String securityPolicy) {
        ClassLoader classLoader = SecurityUtils.class.getClassLoader();
        enableSecurityManager(classLoader, securityPolicy);
    }


    /**
     * @param classLoader
     * @param securityPolicy
     */
    @SuppressWarnings("all")
    private static void enableSecurityManager(ClassLoader classLoader, String securityPolicy) {
        if (System.getSecurityManager() != null) {
            throw new IllegalStateException("Security manager is already in effect. Will not override!");
        }

        URL policyResource = classLoader.getResource(securityPolicy);
        if (policyResource != null) {
            System.setProperty("java.security.policy", policyResource.getPath());
            System.setProperty("java.security.debug", "all"); // ?? no effect ?
        } else {
            throw new IllegalArgumentException("Invalid path to security policy file: " + securityPolicy);
        }
        log.info("Enabling Java2 Security with policy `{}` in thread `{}`", securityPolicy, Thread.currentThread().getName());

        final List<ProtectionDomain> baseStackDomains = getBaseStackDomains(classLoader);

        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(ProtectionDomain protectionDomain) {
                PermissionCollection permissions = super.getPermissions(protectionDomain);
                if (baseStackDomains.contains(protectionDomain)) {
                    permissions.add(new AllPermission());
                    log.info("[BASE STACK] Granting <AllPermission> to code source {}.", protectionDomain.getCodeSource());
                } else {
                    augmentPermissionsForAllowedCodeSources(permissions, protectionDomain.getCodeSource());
                }
                return permissions;
            }

            @Override
            public PermissionCollection getPermissions(CodeSource codeSource) {
                PermissionCollection permissions = super.getPermissions(codeSource);
                boolean isOnBaseStack = false;
                for (ProtectionDomain baseStackDomain : baseStackDomains) {
                    if (baseStackDomain.getCodeSource().equals(codeSource)) {
                        permissions.add(new AllPermission());
                        log.info("[BASE STACK] Granting <AllPermission> to code source location {}.", codeSource.getLocation());
                        isOnBaseStack = true;
                        break;
                    }
                }
                if (!isOnBaseStack) {
                    augmentPermissionsForAllowedCodeSources(permissions, codeSource);
                }
                return permissions;
            }
        });

        // Debug.Help();
        Policy.getPolicy().refresh();
        System.out.println(Policy.getPolicy().getParameters());
        System.setSecurityManager(new SecurityManager());
    }

    /**
     * @param permissions
     * @param codeSource
     */
    private static void augmentPermissionsForAllowedCodeSources(PermissionCollection permissions, CodeSource codeSource) {
        String fullPath = codeSource.getLocation().getPath();
        Set<Permission> foundPermissions = GrantedPermissions.getPermissionsForPath(fullPath);
        if (foundPermissions.size() > 0) {
            for (Permission permission : foundPermissions) {
                log.info("[SERVER STACK] Adding {} permission for location {}", permission, fullPath);
                permissions.add(permission);
            }
        }
    }

    /**
     * @param originalClassLoader
     * @return
     */
    private static List<ProtectionDomain> getBaseStackDomains(ClassLoader originalClassLoader) {
        List<ProtectionDomain> baseStackClasses = new ArrayList<ProtectionDomain>();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        ClassLoader classLoader = originalClassLoader;

        System.out.println(">>>>>>>>>>>>>>>>> USING CLASSLOADER : " + classLoader);

        for (StackTraceElement elt : stack) {
            classLoader = originalClassLoader;
            System.out.print(elt.getFileName());
            System.out.print(" -> ");
            while (true) {
                try {
                    Class<?> eltClass = classLoader.loadClass(elt.getClassName());
                    baseStackClasses.add(eltClass.getProtectionDomain());
                    break;
                } catch (ClassNotFoundException e) {
                    classLoader = classLoader.getParent();
                    if (classLoader == null) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
        return baseStackClasses;
    }
}
