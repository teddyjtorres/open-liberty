/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Class: InternalLogException
//------------------------------------------------------------------------------
/**
 * This exception indicates that an operation has failed due to an unexpected
 * error condition. The recovery log service is in an undefined state and continued
 * use may be impossible.
 */
public class InternalLogException extends Exception {
    String reason = null;

    public InternalLogException() {
        this((Throwable) null);
    }

    public InternalLogException(String s) {
        super(s);
    }

    public InternalLogException(Throwable cause) {
        super(cause);
    }

    public InternalLogException(String s, Throwable cause) {
        super(s, cause);
        reason = s;
    }

    @Override
    public String toString() {
        if (reason != null)
            return reason + ", " + super.toString();
        else
            return super.toString();
    }
}