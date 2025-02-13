/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat;

import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.matchers.Matchers;
import componenttest.topology.impl.LibertyServer;

/**
 * Helper class for building and testing apps that should fail to start in a particular way
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * AppValidator.validateAppOn(SHARED_SERVER)
 *                 .withClass(MyInvalidClass.class)
 *                 .failsWith("CWMFT5001E")
 *                 .run();
 * }
 * </pre>
 *
 * <p>
 * This creates a web application containing just {@code MyInvalidClass}, deploys it, checks that it either succeeds or fails to start. In the case of failure, it checks that a
 * message containing {@code CWMFT5001E} is found in the
 * log, and then undeploys it.
 */
public class AppValidator {
    private final LibertyServer server;
    private final List<Class<?>> classes;
    private final Map<URL, String> manifestResources;
    private final List<String> stringsToFind;
    private final List<File[]> libraries;
    private PropertiesAsset appConfig;
    private String appName;

    private static String APP_START_CODE = "CWWKZ000[13]I";
    private static String APP_FAIL_CODE = "CWWKZ000[24]E";
    private static int appCount = 0;

    /**
     * Begin setting up an app validation to run on the given liberty server
     */
    public static AppValidator validateAppOn(LibertyServer server) {
        return new AppValidator(server);
    }

    private AppValidator(LibertyServer server) {
        this.server = server;
        classes = new ArrayList<>();
        stringsToFind = new ArrayList<>();
        manifestResources = new HashMap<>();
        libraries = new ArrayList<>();
    }

    /**
     * Include the given class in the application to validate
     * <p>
     * Usually the app should include only the classes needed to provoke the validation failure under test
     */
    public AppValidator withClass(Class<?> clazz) {
        classes.add(clazz);
        return this;
    }

    /**
     * Specify that the app should fail to start, and that the given failure message should be seen during app startup
     */
    public AppValidator failsWith(String expectedFailure) {
        stringsToFind.add(expectedFailure);
        stringsToFind.add(APP_FAIL_CODE);
        server.addIgnoredErrors(Collections.singletonList(expectedFailure));
        return this;
    }

    /**
     * Specify that the app should start, but that the given message should be seen during app startup
     * <p>
     * This can be used to ensure that warning messages are emitted
     */
    public AppValidator succeedsWith(String expectedMessage) {
        stringsToFind.add(expectedMessage);
        stringsToFind.add(APP_START_CODE);
        server.addIgnoredErrors(Collections.singletonList(expectedMessage));
        return this;
    }

    public AppValidator withAppConfig(PropertiesAsset appConfig) throws Exception {
        this.appConfig = appConfig;
        return this;
    }

    public AppValidator withAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public AppValidator withManifestResource(URL url, String target) {
        manifestResources.put(url, target);
        return this;
    }

    public AppValidator withLibrary(File[] library) {
        libraries.add(library);
        return this;
    }

    /**
     * Specify that the app should start successfully
     * <p>
     * This can be used to ensure that valid edge cases do not cause a validation failure
     */
    public AppValidator succeeds() {
        stringsToFind.add(APP_START_CODE);
        return this;
    }

    /**
     * Run the validation
     * <p>
     * This consists of the following steps
     * <ul>
     * <li>Build the app archive from the classes given using {@link #withClass(Class)}</li>
     * <li>Deploy the app to the server passed to {@link #validateAppOn(LibertyServer)}</li>
     * <li>if {@link #failsWith(String)} was called, validate that any expected messages are found in the log and that the app fails to start</li>
     * <li>if {@link #succeedsWith(String)} was called, validate that any expected messages are found in the log and that the app starts successfully</li>
     * <li>if {@link #succeeds()} was called, validate that the app starts successfully</li>
     * <li>Remove the app from the server</li>
     * </ul>
     */
    public void run() {
        try {
            String archiveName;

            if (appName != null) {
                archiveName = appName + ".war";
            } else {
                archiveName = "testApp-" + (++appCount) + ".war";
            }

            try {
                WebArchive app = ShrinkWrap.create(WebArchive.class, archiveName);

                for (Class<?> clazz : classes) {
                    app = app.addClass(clazz);
                }
                if (appConfig != null) {
                    app = app.addAsResource(appConfig, "META-INF/microprofile-config.properties");
                }

                for (File[] library : libraries) {
                    app = app.addAsLibraries(library);
                }

                for (URL url : manifestResources.keySet()) {
                    app = app.addAsManifestResource(url, manifestResources.get(url));
                }

                RemoteFile logFile = server.getDefaultLogFile();
                server.setMarkToEndOfLog(logFile);

                ShrinkHelper.exportArtifact(app, "tmp/apps", true/* print contents */, true/* overwrite */);
                server.copyFileToLibertyServerRoot("tmp/apps", "dropins", archiveName);

                for (String stringToFind : stringsToFind) {
                    String logLine = server.waitForStringInLogUsingMark(stringToFind + "|" + APP_START_CODE + "|" + APP_FAIL_CODE);
                    assertThat(logLine, Matchers.containsPattern(stringToFind));
                }
            } finally {
                server.deleteFileFromLibertyServerRoot("dropins/" + archiveName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
