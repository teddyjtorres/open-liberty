/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryAuditTest extends FATServletClient {

    private static Class<?> c = TelemetryAuditTest.class;

    public static final String APP_NAME = "MpTelemetryLogApp";

    public static final String SERVER_NAME = "TelemetryAudit";

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static final String SERVER_XML_ALL_SOURCES_WITH_AUDIT = "allSourcesWithAudit.xml";
    public static final String SERVER_XML_AUDIT_SOURCE_FEATURE = "auditServer.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    @BeforeClass
    public static void initialSetup() throws Exception {
        server.saveServerConfiguration();
    }

    @Before
    public void testSetup() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY },
                                "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        server.startServer();
    }

    @After
    public void testTearDown() throws Exception {
        server.stopServer(EXPECTED_FAILURES);

        // Restore the server configuration, after each test case.
        server.restoreServerConfiguration();
    }

    /**
     * Ensures Audit messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryAuditLogs() throws Exception {
        String line = server.waitForStringInLog("AuditService", server.getConsoleLogFile());

        assertNotNull("The AuditService audit event was not not found.", line);

        Map<String, String> expectedAuditFieldsMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_audit");
                put("io.openliberty.audit.event_name", "SECURITY_AUDIT_MGMT");
                put("io.openliberty.audit.event_sequence_number", "");
                put("io.openliberty.audit.observer.id", "");
                put("io.openliberty.audit.observer.name", "AuditService");

                put("io.openliberty.audit.observer.type_uri", "service/server");

                put("io.openliberty.audit.outcome", "success");

                put("io.openliberty.audit.target.id", "");

                put("io.openliberty.audit.target.type_uri", "service/audit/start");

                put("thread.id", ""); // since, the thread.id can be random, have to make sure the thread.id field is still present.
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        TestUtils.checkJsonMessage(line, expectedAuditFieldsMap);
    }

    /*
     * Test a server with all MPTelemetry sources enabled with audit and ensure all message, trace, ffdc, and audit logs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message, trace, ffdc, audit"/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryAuditLogsWithAllSourcesEnabled() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_ALL_SOURCES_WITH_AUDIT, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");

        String auditLine = server.waitForStringInLog("SECURITY_AUTHN", server.getConsoleLogFile());

        //Ensure audit log is bridged over, that is generated from an app.
        assertNotNull("Audit logs could not be found.", auditLine);
        Map<String, String> expectedAuditFieldsMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_audit");

                put("io.openliberty.audit.event_name", "SECURITY_AUTHN");

                put("io.openliberty.audit.observer.name", "SecurityService");
                put("io.openliberty.audit.observer.type_uri", "service/server");

                put("io.openliberty.audit.outcome", "success");

                put("io.openliberty.audit.reason.reason_code", "200");
                put("io.openliberty.audit.reason.reason_type", "HTTP");

                put("io.openliberty.audit.target.appname", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.LogServlet");
                put("io.openliberty.audit.target.method", "GET");
                put("io.openliberty.audit.target.name", "/MpTelemetryLogApp/LogURL");
                put("io.openliberty.audit.target.realm", "defaultRealm");

                put("io.openliberty.audit.target.type_uri", "service/application/web");
            }
        };
        TestUtils.checkJsonMessage(auditLine, expectedAuditFieldsMap);

        //Ensure the other sources - message, trace, and ffdc logs are bridged.
        TestUtils.runApp(server, "ffdc1");
        String messageLine = server.waitForStringInLog("info message", server.getConsoleLogFile());
        String traceLine = server.waitForStringInLog("finest trace", server.getConsoleLogFile());
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", server.getConsoleLogFile());

        assertNotNull("Info message could not be found.", messageLine);
        assertNotNull("Trace message could not be found.", traceLine);
        assertNotNull("FFDC message could not be found.", ffdcLine);

    }

    private static void setConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        server.waitForStringInLogUsingMark("CWWKG0017I"); // wait for server config update
        server.waitForStringInLogUsingMark("CWWKF0008I"); // wait for feature config update
        server.waitForStringInLogUsingMark("CWWKZ0003I"); // wait for app started update
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

}