/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

/**
 * Common local interface for beans that verify CDI BeanManager
 * injection and lookups.
 **/
public interface BeanManagerLocal {
    /**
     * Verifies that the BeanManager was properly injected per the
     * conifguration of the bean, and that the BeanManager may be
     * looked up.
     **/
    public void verifyBeanMangerInjectionAndLookup();
}
