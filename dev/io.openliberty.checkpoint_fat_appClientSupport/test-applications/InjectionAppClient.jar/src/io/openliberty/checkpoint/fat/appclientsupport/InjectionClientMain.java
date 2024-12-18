/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.checkpoint.fat.appclientsupport;

import javax.ejb.EJB;

import io.openliberty.checkpoint.fat.appclientsupport.view.SimpleInjectionBeanRemote;

public class InjectionClientMain {

    private final static String NAME_EJB_GLOBAL = "java:global/InjectionApp/InjectionAppEJB/SimpleInjectionBean!io.openliberty.checkpoint.fat.appclientsupport.view.SimpleInjectionBeanRemote";

    @EJB(lookup = NAME_EJB_GLOBAL)
    public static SimpleInjectionBeanRemote injectedGlobalEjb;

    public static void main(String[] args) {
        System.out.println("main - entry");

        if (checkEJB(injectedGlobalEjb))
            System.out.println("injectGlobal_EJB-PASSED");

        System.out.println("main - exit");
    }

    private static boolean checkEJB(SimpleInjectionBeanRemote ejb) {
        boolean b;
        try {
            b = ejb != null && ejb.add(4, 8) == 12;
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        }
        return b;
    }

}
