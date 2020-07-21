/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class AccessDecision {

    private static final TraceComponent tc = Tr.register(AccessDecision.class);

    public static boolean isGranted(Method method, Principal principal, Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {
        if (isDenyAll(method)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found @DenyAll for method: " + method.getName());
            }
            return false;
        }

        if (isPermitAll(method)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found @PermitAll for method: " + method.getName());
            }
            return true;
        }

        RolesAllowed rolesAllowed = getRolesAllowed(method);
        if (rolesAllowed != null) {
            return isGrantedAnyRole(principal, rolesAllowed, "method", method.getName(), isUserInRoleFunction);
        } else {
            return isGrantedAtClassLevel(method.getDeclaringClass(), principal, isUserInRoleFunction);
        }

    }

    private static boolean isGrantedAtClassLevel(Class<?> cls, Principal principal, Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {
        if (isDenyAll(cls)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found @DenyAll for class: " + cls.getName());
            }
            return false;
        }

        if (isPermitAll(cls)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found @PermitAll for class: " + cls.getName());
            }
            return true;
        }

        RolesAllowed rolesAllowed = getRolesAllowed(cls);
        if (rolesAllowed != null) {
            return isGrantedAnyRole(principal, rolesAllowed, "class", cls.getName(), isUserInRoleFunction);
        } else {
            // If no annotations, return true.
            return true;
        }
    }

    private static RolesAllowed getRolesAllowed(AnnotatedElement element) {
        return element.getAnnotation(RolesAllowed.class);
    }

    private static boolean isPermitAll(AnnotatedElement element) {
        return element.isAnnotationPresent(PermitAll.class);
    }

    private static boolean isDenyAll(AnnotatedElement element) {
        return element.isAnnotationPresent(DenyAll.class);
    }

    private static boolean isGrantedAnyRole(Principal principal, RolesAllowed rolesAllowed, String type, String name,
                                            Predicate<String> isUserInRoleFunction) throws UnauthenticatedException {
        String[] roles = rolesAllowed.value();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Found @RolesAllowed for " + type + ": " + name, new Object[] { roles });
        }

        if (Stream.of(roles).anyMatch(isUserInRoleFunction)) {
            return true;
        } else {
            checkAuthentication(principal);
        }
        return false;
    }

    private static void checkAuthentication(Principal principal) throws UnauthenticatedException {
        if (principal == null) {
            throw new UnauthenticatedException("principal is null");
        }

        if ("UNAUTHENTICATED".equals(principal.getName())) {
            throw new UnauthenticatedException("principal is UNAUTHENTICATED");
        }
    }

}
