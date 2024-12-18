/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.tests;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.v32.fat.singletonlifecycletx.SingletonLifecycleTxTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SingletonLifecycleTxTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.v32.fat.basic")
    @TestServlets({ @TestServlet(servlet = SingletonLifecycleTxTestServlet.class, contextRoot = "SingletonLifecycleTx") })
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### SingletonLifecycleTx.war
        WebArchive SingletonLifecycleTx = ShrinkHelper.buildDefaultApp("SingletonLifecycleTx.war", "com.ibm.ws.ejbcontainer.v32.fat.singletonlifecycletx.");
        ShrinkHelper.exportDropinAppToServer(server, SingletonLifecycleTx, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0089E", "CNTR4006E", "CNTR4007E");
        }
    }
}
