/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxb.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

@RunWith(FATRunner.class)
public class LibertyJAXBSpecTest {

    private static final String APP_NAME = "jaxbApp";

    @Server("jaxbspec_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jaxb.web", "jaxb.xmlnsform.unqualified", "jaxb.xmlnsform.qualified");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @After
    public void afterTest() throws Exception {
        server.stopServer();
    }

    @Test
    public void testBackupWithParentNamespaceTrue() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("com.sun.xml.bind.backupWithParentNamespace", "true");
//        server.setJvmOptions(map);
        server.addBootstrapProperties(map);
        server.startServer();

        File personXml = new File(server.pathToAutoFVTTestFiles + "/Person.xml");
        if (personXml.exists()) {
            System.out.println("Person.xml path: " + personXml.getPath());
        } else {
            Assert.fail("Person.xml does not exist - " + personXml.getPath());
        }

        JAXBContext jaxbContextQualified = JAXBContext.newInstance(jaxb.xmlnsform.qualified.Person.class);
        Unmarshaller unmarshallerQualified = jaxbContextQualified.createUnmarshaller();
        jaxb.xmlnsform.qualified.Person qualifiedPerson = (jaxb.xmlnsform.qualified.Person) unmarshallerQualified.unmarshal(personXml);
        System.out.println("~qualifiedPerson: " + qualifiedPerson);
        assertNull("1", qualifiedPerson.getFirstNameWithPrefix());
        assertNotNull("2", qualifiedPerson.getLastNameWithOutPrefix());

        JAXBContext jaxbContextUnQualified = JAXBContext.newInstance(jaxb.xmlnsform.unqualified.Person.class);
        Unmarshaller unmarshallerUnQualified = jaxbContextUnQualified.createUnmarshaller();
        jaxb.xmlnsform.unqualified.Person unQualifiedPerson = (jaxb.xmlnsform.unqualified.Person) unmarshallerUnQualified.unmarshal(personXml);
        System.out.println("~unQualifiedPerson: " + unQualifiedPerson);
        assertNotNull("3", unQualifiedPerson.getFirstNameWithPrefix());
        assertNull("4", unQualifiedPerson.getLastNameWithOutPrefix());
    }

    @Test
    public void testBackupWithParentNamespaceFalse() throws Exception {
//        server.setJvmOptions(new HashMap<String, String>());
//        server.addBootstrapProperties(new HashMap<String, String>());
        Map map = (Map) server.getBootstrapProperties().remove("com.sun.xml.bind.backupWithParentNamespace");

        server.startServer();

        File personXml = new File(server.pathToAutoFVTTestFiles + "/Person.xml");
        if (personXml.exists()) {
            System.out.println("Person.xml path: " + personXml.getPath());
        } else {
            Assert.fail("Person.xml does not exist - " + personXml.getPath());
        }

        JAXBContext jaxbContextQualified = JAXBContext.newInstance(jaxb.xmlnsform.qualified.Person.class);
        Unmarshaller unmarshallerQualified = jaxbContextQualified.createUnmarshaller();
        jaxb.xmlnsform.qualified.Person qualifiedPerson = (jaxb.xmlnsform.qualified.Person) unmarshallerQualified.unmarshal(personXml);
        System.out.println("~qualifiedPerson: " + qualifiedPerson);
        assertNotNull("Violation of JAXB spec, an element with a prefix from a XmlNsForm.QUALIFIED package should not be null", qualifiedPerson.getFirstNameWithPrefix());
        assertNull("Violation of JAXB spec, an element with a prefix from a XmlNsForm.QUALIFIED package should not be null", qualifiedPerson.getLastNameWithOutPrefix());

        JAXBContext jaxbContextUnQualified = JAXBContext.newInstance(jaxb.xmlnsform.unqualified.Person.class);
        Unmarshaller unmarshallerUnQualified = jaxbContextUnQualified.createUnmarshaller();
        jaxb.xmlnsform.unqualified.Person unQualifiedPerson = (jaxb.xmlnsform.unqualified.Person) unmarshallerUnQualified.unmarshal(personXml);
        System.out.println("~unQualifiedPerson: " + unQualifiedPerson);
        assertNull("Violation of JAXB spec, an element with a prefix from a XmlNsForm.UNQUALIFIED package should be null", unQualifiedPerson.getFirstNameWithPrefix());
        assertNotNull("Violation of JAXB spec, an element with a prefix from a XmlNsForm.UNQUALIFIED package should not be null", unQualifiedPerson.getLastNameWithOutPrefix());
    }
}
