/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.filter;

public interface IValue {

    /**
     * Are the two values equal?
     * @param str
     * @return
     * @throws FilterException
     */
    public boolean equals(IValue str) throws FilterException;

    /**
     * Is the Value (this) greater than the input (str)?
     * 
     * @param str
     * @return
     * @throws FilterException
     */
    public boolean greaterThan(IValue str) throws FilterException;

    /**
     * Is the Value (this) less than the input (str)?
     * 
     * @param str
     * @return
     * @throws FilterException
     */
    public boolean lessThan(IValue str) throws FilterException;

    /**
     * Does the Value (this) contain the input (str)?
     * Or, more properly, is the value of this contained by the str being compared?
     * 
     * @param str
     * @return
     * @throws FilterException
     */
    public boolean containedBy(IValue str) throws FilterException;
}
