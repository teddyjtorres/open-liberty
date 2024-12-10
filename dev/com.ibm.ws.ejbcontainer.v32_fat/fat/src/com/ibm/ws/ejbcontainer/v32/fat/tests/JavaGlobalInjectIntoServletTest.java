/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.ejb32.javaglobal.web.BasicServlet;

@RunWith(FATRunner.class)
public class JavaGlobalInjectIntoServletTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.v32.fat.basic")
    @TestServlets({ @TestServlet(servlet = BasicServlet.class, contextRoot = "JavaGlobalInjectIntoServletTestApp") })
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### JavaGlobalInjectIntoServletApp.ear
        JavaArchive JavaGlobalInjectIntoServletEJB = ShrinkHelper.buildJavaArchive("JavaGlobalInjectIntoServletEJB.jar", "io.openliberty.ejb32.javaglobal.ejb.");
        WebArchive JavaGlobalInjectIntoServletWeb = ShrinkHelper.buildDefaultApp("JavaGlobalInjectIntoServletWeb.war", "io.openliberty.ejb32.javaglobal.web.");
        EnterpriseArchive JavaGlobalInjectIntoServletApp = ShrinkWrap.create(EnterpriseArchive.class, "JavaGlobalInjectIntoServletApp.ear");
        JavaGlobalInjectIntoServletApp.addAsModule(JavaGlobalInjectIntoServletEJB).addAsModule(JavaGlobalInjectIntoServletWeb);
        JavaGlobalInjectIntoServletApp = (EnterpriseArchive) ShrinkHelper.addDirectory(JavaGlobalInjectIntoServletApp,
                                                                                       "test-applications/JavaGlobalInjectIntoServletApp.ear/resources/");

        ShrinkHelper.exportDropinAppToServer(server, JavaGlobalInjectIntoServletApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
