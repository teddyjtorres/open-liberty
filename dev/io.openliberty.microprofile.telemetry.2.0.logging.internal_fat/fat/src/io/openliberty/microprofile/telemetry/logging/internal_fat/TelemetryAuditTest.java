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
import static org.junit.Assert.assertNull;

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
import com.ibm.websphere.simplicity.log.Log;

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

    // Test server configurations
    public static final String SERVER_XML_AUDIT_SOURCE_FEATURE = "auditServer.xml";
    public static final String SERVER_XML_ALL_SOURCES_WITH_AUDIT = "allSourcesWithAudit.xml";
    public static final String SERVER_XML_ONLY_AUDIT_FEATURE = "onlyAuditFeature.xml";
    public static final String SERVER_XML_ONLY_AUDIT_SOURCE = "onlyAuditSource.xml";
    public static final String SERVER_XML_NO_AUDIT_SOURCE_FEATURE = "noAuditSourceFeature.xml";

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
     * Tests whether Audit messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryAuditLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure audit feature and audit source
        setConfig(server, messageLogFile, SERVER_XML_AUDIT_SOURCE_FEATURE);

        // Wait for the audit security management event that occurs at audit service startup to be bridged over.
        String line = server.waitForStringInLog("AuditService", consoleLogFile);
        assertNotNull("The AuditService audit event was not found.", line);

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
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
        TestUtils.checkJsonMessage(line, expectedAuditFieldsMap);
    }

    /*
     * Test a server with all MPTelemetry sources enabled with audit and ensure all message, trace, ffdc, and audit logs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message, trace, ffdc, audit"/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryAuditLogsWithAllSourcesEnabled() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure all sources
        setConfig(server, messageLogFile, SERVER_XML_ALL_SOURCES_WITH_AUDIT);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event
        TestUtils.runApp(server, "logServlet");

        //Ensure audit log is bridged over, that is generated from an app.
        String auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("The Security Authentication audit event was not found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);

        //Ensure the other sources - message, trace, and ffdc logs are bridged, as well.
        String messageLine = server.waitForStringInLog("info message", consoleLogFile);
        assertNotNull("Info message could not be found.", messageLine);

        String traceLine = server.waitForStringInLog("finest trace", consoleLogFile);
        assertNotNull("Trace message could not be found.", traceLine);

        TestUtils.runApp(server, "ffdc1");
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", consoleLogFile);
        assertNotNull("FFDC message could not be found.", ffdcLine);

    }

    /*
     * Tests when the audit source is dynamically added to the server.xml, with the audit feature already present.
     */
    @Test
    public void testDynamicAuditSourceAddition() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure audit feature only
        setConfig(server, messageLogFile, SERVER_XML_ONLY_AUDIT_FEATURE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event
        TestUtils.runApp(server, "logServlet");

        // Ensure audit log is NOT bridged over, that is generated from an app.
        String auditLine = server.waitForStringInLog("liberty_audit", consoleLogFile);
        assertNull("Audit logs could be found.", auditLine);

        // Configure <mpTelemetry source="audit"/>
        setConfig(server, messageLogFile, SERVER_XML_AUDIT_SOURCE_FEATURE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event.
        TestUtils.runApp(server, "logServlet");

        //Ensure audit log is bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("Audit logs could not be found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);
    }

    /*
     * Tests when the audit source is dynamically removed to the server.xml, with the audit feature already present.
     */
    @Test
    public void testDynamicAuditSourceRemoval() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure audit feature and audit source
        setConfig(server, messageLogFile, SERVER_XML_AUDIT_SOURCE_FEATURE);

        // Wait for the audit security management event that occurs at audit service startup to be bridged over.
        String auditLine = server.waitForStringInLog("AuditService", consoleLogFile);
        assertNotNull("The AuditService audit event was not found.", auditLine);

        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event
        TestUtils.runApp(server, "logServlet");

        //Ensure audit log is bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("Audit logs could not be found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);

        // Remove only audit source
        setConfig(server, messageLogFile, SERVER_XML_ONLY_AUDIT_FEATURE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event.
        TestUtils.runApp(server, "logServlet");

        // Ensure audit log is NOT bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("liberty_audit", consoleLogFile);
        assertNull("Audit logs could be found.", auditLine);
    }

    /*
     * Tests when the audit feature is dynamically added to the server.xml, with the audit source already present.
     */
    @Test
    public void testDynamicAuditFeatureAddition() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure audit source only
        setConfig(server, messageLogFile, SERVER_XML_ONLY_AUDIT_SOURCE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event
        TestUtils.runApp(server, "logServlet");

        // Ensure audit log is NOT bridged over, that is generated from an app.
        String auditLine = server.waitForStringInLog("liberty_audit", consoleLogFile);
        assertNull("Audit logs could be found.", auditLine);

        // Configure audit feature
        setConfig(server, messageLogFile, SERVER_XML_AUDIT_SOURCE_FEATURE);

        // Wait for the audit service startup audit event to be bridged over.
        String line = server.waitForStringInLog("AuditService", consoleLogFile);
        assertNotNull("The AuditService audit event was not found.", line);

        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event.
        TestUtils.runApp(server, "logServlet");

        //Ensure audit log is bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("Audit logs could not be found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);
    }

    /*
     * Tests when the audit feature is dynamically removed in the server.xml, with the audit source already present.
     */
    @Test
    public void testDynamicAuditFeatureRemoval() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure audit feature and audit source
        setConfig(server, messageLogFile, SERVER_XML_AUDIT_SOURCE_FEATURE);

        // Wait for the audit security management event that occurs at audit service startup to be bridged over.
        String auditLine = server.waitForStringInLog("AuditService", consoleLogFile);
        assertNotNull("The AuditService audit event was not found.", auditLine);

        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event
        TestUtils.runApp(server, "logServlet");

        //Ensure audit log is bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("Audit logs could not be found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);

        // Remove only audit feature
        setConfig(server, messageLogFile, SERVER_XML_ONLY_AUDIT_SOURCE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event.
        TestUtils.runApp(server, "logServlet");

        // Ensure audit log is NOT bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("liberty_audit", consoleLogFile);
        assertNull("Audit logs could be found.", auditLine);
    }

    /*
     * Tests when the audit feature and source are dynamically removed in the server.xml.
     */
    @Test
    public void testDynamicAuditFeatureSourceRemoval() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure audit feature and audit source
        setConfig(server, messageLogFile, SERVER_XML_AUDIT_SOURCE_FEATURE);

        // Wait for the audit security management event that occurs at audit service startup to be bridged over.
        String auditLine = server.waitForStringInLog("AuditService", consoleLogFile);
        assertNotNull("The AuditService audit event was not found.", auditLine);

        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event
        TestUtils.runApp(server, "logServlet");

        //Ensure audit log is bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("Audit logs could not be found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);

        // Remove audit feature and source
        setConfig(server, messageLogFile, SERVER_XML_NO_AUDIT_SOURCE_FEATURE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an audit event.
        TestUtils.runApp(server, "logServlet");

        // Ensure audit logs is NOT bridged over, that is generated from an app.
        auditLine = server.waitForStringInLog("liberty_audit", consoleLogFile);
        assertNull("Audit logs could be found.", auditLine);
    }

    private static void setConfig(LibertyServer server, RemoteFile logFile, String fileName) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);

        String configUpdate = server.waitForStringInLogUsingMark("CWWKG0017I"); // wait for server config update
        Log.info(c, "setConfig", "Config Update Message Found : " + configUpdate);

        String featureUpdate = server.waitForStringInLogUsingMark("CWWKF0008I"); // wait for feature config update
        Log.info(c, "setConfig", "Feature Update Message Found : " + featureUpdate);

        String appStartedUpdate = server.waitForStringInLogUsingMark("CWWKZ0003I"); // wait for app started update
        Log.info(c, "setConfig", "App Started Updated Message Found :  " + appStartedUpdate);
    }

    private static void checkAuditOTelAttributeMapping(String auditLine) {
        // Ensures the triggered application audit security event is mapped correctly.
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
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

}