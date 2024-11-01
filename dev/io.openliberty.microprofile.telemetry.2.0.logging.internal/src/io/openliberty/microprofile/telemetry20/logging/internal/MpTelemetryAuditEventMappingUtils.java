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

/**
 * This class contains helper methods to correctly format and map the audit event fields to
 * OpenTelemetry, using the OpenTelemetry semantic naming conventions.
 */
public class MpTelemetryAuditEventMappingUtils {

    /*
     * The following are Liberty Audit event field types that have camelCase field names, that need to be flatten
     * and formatted into snake_case. The other audit fields do not need to be formatted,
     * they can just be directly mapped, with the "io.openliberty.*" prefix prepended to it.
     *
     * e.g.
     * target.typeURI => io.openliberty.target.type_uri
     * target.id => io.openliberty.target.id
     *
     */
    private final static String[][] auditEventTable = {
                                                        //{ MpTelemetryLogFieldConstants.AUDIT_EVENT_NAME, "event_name" }, - Not needed, as the OpenTelemetry Semnatic Attribute Convention name "event.name" is used.
                                                        { MpTelemetryLogFieldConstants.AUDIT_EVENT_SEQUENCE_NUMBER, "event.sequence_number" },
                                                        { MpTelemetryLogFieldConstants.AUDIT_EVENT_TIME, "event_time" }
    };

    private final static String[][] auditObserverTable = {
                                                           { MpTelemetryLogFieldConstants.AUDIT_OBSERVER_TYPEURI, "observer.type_uri" }

    };

    private final static String[][] auditTargetTable = {
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_TYPEURI, "target.type_uri" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_REPOSITORY_ID, "target.repository_id" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_UNIQUENAME, "target.unique_name" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_ENTITY_TYPE, "target.entity_type" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_EXTENDED_PROPERTIES, "target.extended_properties" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_APPLICATION_ID, "target.application_id" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_TOKEN_ID, "target.token_id" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_CLIENT_ID, "target.client_id" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_INITIATOR_ROLE, "target.initiator_role" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_NUMBER_REVOKED, "target.number_revoked" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_TARGET_USERID, "target.user_id" }

    };

    private final static String[][] auditTargetMessagingTable = {
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_LOGIN_TYPE, "target.messaging.login_type" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_REMOTE_CHAIN_NAME, "target.messaging.remote.chain_name" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_USER_NAME, "target.messaging.user_name" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_OPERATIONTYPE, "target.messaging.operation_type" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_CALLTYPE, "target.messaging.call_type" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_RESOURCE, "target.messaging.jms_resource" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_ACTIONS, "target.messaging.jms_actions" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_ROLES, "target.messaging.jms_roles" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_REQUESTOR_TYPE, "target.messaging.jms_requestor_type" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_QUEUE_PERMISSIONS,
                                                                    "target.messaging.queue_permissions" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_TOPIC_PERMISSIONS,
                                                                    "target.messaging.topic_permissions" },
                                                                  { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_TEMPORARY_DESTINATION_PERMISSIONS,
                                                                    "target.messaging.temp_destination_permissions" }
    };

    private final static String[][] auditTargetJmxTable = {
                                                            { MpTelemetryLogFieldConstants.AUDIT_TARGET_JMX_MBEAN_QUERYEXP, "target.jmx.mbean.query_exp" }
    };

    private final static String[][] auditInitiatorTable = {
                                                            { MpTelemetryLogFieldConstants.AUDIT_INITIATOR_TYPEURI, "initiator.type_uri" }
    };

    private final static String[][] auditReasonTable = {
                                                         { MpTelemetryLogFieldConstants.AUDIT_REASON_CODE, "reason.reason_code" },
                                                         { MpTelemetryLogFieldConstants.AUDIT_REASON_TYPE, "reason.reason_type" }
    };

    public static String getOTelFormattedAuditEventKey(String auditKey) {
    }

}
