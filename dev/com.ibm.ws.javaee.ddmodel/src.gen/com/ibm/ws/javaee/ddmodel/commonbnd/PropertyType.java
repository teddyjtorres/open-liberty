/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.commonbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class PropertyType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonbnd.Property {
    public PropertyType() {
        this(false);
    }

    public PropertyType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    com.ibm.ws.javaee.ddmodel.StringType value;
    com.ibm.ws.javaee.ddmodel.StringType description;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.lang.String getValue() {
        return value != null ? value.getValue() : null;
    }

    @Override
    public java.lang.String getDescription() {
        return description != null ? description.getValue() : null;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            // "name" is the same for XML and XMI.
            if ("name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
            // "value" is the same for XML and XMI.
            if ("value".equals(localName)) {
                this.value = parser.parseStringAttributeValue(index);
                return true;
            }
            // "description" is the same for XML and XMI.
            if ("description".equals(localName)) {
                this.description = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("name", name);
        diag.describeIfSet("value", value);
        diag.describeIfSet("description", description);
    }
}
