/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.singleton.ann.ejb;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.BasicSingleton;

@Singleton(name = "FailedInjection")
@LocalBean
public class FailedInjectionBean implements BasicSingleton {
    private boolean ivValue;

    @Resource
    private void setSessionContext(SessionContext ejbContext) {
        throw new UnsupportedOperationException("Expected Injection Exception.");
    }

    @Override
    public boolean getBoolean() {
        return ivValue;
    }

    @Override
    public void setBoolean(boolean value) {
        ivValue = value;
    }
}
