/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.wsspi.config.internal;

public final class ConfigTypeConstants {

    public static final String TR_GROUP = "MetatypeHelper";
    public static final String NLS_PROPS = "com.ibm.ws.kernel.metatype.helper.internal.resources.ConfigTypeMessages";

    public enum FilesetAttribute {
        dir, caseSensitive, includes, excludes, scanInterval;
    }

}
