/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.logging.internal;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MpTelemetryAuditEventMappingUtils {

    private static final Map<String, String> auditEventToOtelFieldMap = new HashMap<String, String>();

    static {
        // Add all mapped audit events...
        auditEventToOtelFieldMap.put(null, null);
    }

    public static String getOTelMappedAuditFieldKey(String key) {
        return auditEventToOtelFieldMap.get(key);
    }

}
