/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
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
package com.ibm.ws.cache.config;

public class Field {
    public String name;
    public Method method;
    public Field field;
    public int index = -1;

    //implementation methods
    public java.lang.reflect.Field fieldImpl;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(name);
        if (method != null) {
            sb.append(".").append(method);
        } else if (field != null) {
            sb.append(".").append(field);
        }
        if (index != -1) {
            sb.append(".").append(index);
        }
        return sb.toString();
    }

    @Override
    public Object clone() {
        Field m = new Field();
        m.name = name;
        if (method != null)
            m.method = (Method) method.clone();
        if (field != null)
            m.field = (Field) field.clone();
        m.index = index;
        return m;
    }
}
