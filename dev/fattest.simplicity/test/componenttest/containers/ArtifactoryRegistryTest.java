/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

public class ArtifactoryRegistryTest {

    private static final String nl = System.lineSeparator();

    @Test
    public void testArtifactoryRegistry() throws Exception {
        Method findRegistry = getFindRegistry();

        // Unset
        System.clearProperty(ArtifactoryRegistry.artifactoryRegistryKey);
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.artifactoryRegistryKey + " was unset");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Empty
        System.setProperty(ArtifactoryRegistry.artifactoryRegistryKey, "");
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.artifactoryRegistryKey + " was empty");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Missing
        System.setProperty(ArtifactoryRegistry.artifactoryRegistryKey, "${" + ArtifactoryRegistry.artifactoryRegistryKey.substring(9) + "}");
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.artifactoryRegistryKey + " was missing from gradle");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Null
        System.setProperty(ArtifactoryRegistry.artifactoryRegistryKey, "null");
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.artifactoryRegistryKey + " was null");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Valid
        String expected = "docker-na-public.artifactory.swg-devops.com";
        System.setProperty(ArtifactoryRegistry.artifactoryRegistryKey, expected);
        String actual = (String) findRegistry.invoke(null);

        assertEquals(expected, actual);
    }

    @Test
    public void testArtifactoryAuthToken() throws Exception {
        String testUsername = "test.email@example.com";
        String testToken = "aToTallyFakeTokenThaTWouldNeverBeUsed";
        String expectedAuthToken = "dGVzdC5lbWFpbEBleGFtcGxlLmNvbTphVG9UYWxseUZha2VUb2tlblRoYVRXb3VsZE5ldmVyQmVVc2Vk";

        Method generateAuthToken = getGenerateAuthToken();

        // Ensure failure path
        System.clearProperty(ArtifactoryRegistry.artifactoryRegistryUser);
        System.clearProperty(ArtifactoryRegistry.artifactoryRegistryToken);

        try {
            generateAuthToken.invoke(null);
            fail("Should not have generated authToken when property "
                 + ArtifactoryRegistry.artifactoryRegistryUser + " and " + ArtifactoryRegistry.artifactoryRegistryToken + " were unset");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Ensure successful path
        System.setProperty(ArtifactoryRegistry.artifactoryRegistryUser, testUsername);
        System.setProperty(ArtifactoryRegistry.artifactoryRegistryToken, testToken);

        String actualAuthToken = (String) generateAuthToken.invoke(null);

        assertEquals(expectedAuthToken, actualAuthToken);

        String actualAuthData = new String(Base64.getDecoder().decode(actualAuthToken.getBytes()), StandardCharsets.UTF_8);
        String actualUsername = actualAuthData.split(":")[0];
        String actualToken = actualAuthData.split(":")[1];

        assertEquals(testUsername, actualUsername);
        assertEquals(testToken, actualToken);
    }

    private static Method getFindRegistry() throws Exception {
        Method method = ArtifactoryRegistry.class.getDeclaredMethod("findRegistry");
        method.setAccessible(true);
        return method;
    }

    private static Method getGenerateAuthToken() throws Exception {
        Method method = ArtifactoryRegistry.class.getDeclaredMethod("generateAuthToken");
        method.setAccessible(true);
        return method;
    }

}
