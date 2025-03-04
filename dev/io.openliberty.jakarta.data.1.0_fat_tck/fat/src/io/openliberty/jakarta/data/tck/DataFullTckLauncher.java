/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.jakarta.data.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataFullTckLauncher {

    @Server("io.openliberty.jakarta.data.1.0.full")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, FATSuite.relationalDatabase);
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(FATSuite.relationalDatabase).getDriverName());
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKD0202E", // TODO : https://github.com/OpenLiberty/open-liberty/issues/30155
                          "CWWKD1054E", // tested error path
                          "CWWKD1080E", // TODO : https://github.com/OpenLiberty/open-liberty/issues/30155
                          "CWWKE0955E" //websphere.java.security java 18+
        );
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckFullPersistence() throws Exception {
        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jimage.dir", server.getServerSharedPath() + "jimage/output/");
        additionalProps.put("tck_protocol", "servlet");
        additionalProps.put("jakarta.profile", "full");
        additionalProps.put("jakarta.tck.database.type", "relational");
        additionalProps.put("jakarta.tck.database.name", FATSuite.relationalDatabase.getClass().getSimpleName());

        //Always skip signature tests on full profile (already tested in core profile)
        additionalProps.put("included.groups", "platform & persistence & !signature");

        additionalProps.put("excluded.tests", FATSuite.getExcludedTestByDatabase(DatabaseContainerType.valueOf(FATSuite.relationalDatabase)));

        //Comment out to use SNAPSHOT
        additionalProps.put("jakarta.data.groupid", "jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.1");

        TCKRunner.build(server, Type.JAKARTA, "Data")
                        .withPlatfromVersion("11")
                        .withQualifiers("full", "persistence")
                        .withRelativeTCKRunner("publish/tckRunner/platform/")
                        .withAdditionalMvnProps(additionalProps)
                        .withLogging(FATSuite.getLoggingConfig())
                        .runTCK();
    }

    // Cannot test NoSQL database on Full profile since the persistence feature is automatically included
}