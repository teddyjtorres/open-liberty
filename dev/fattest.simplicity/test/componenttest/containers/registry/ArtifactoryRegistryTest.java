/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

public class ArtifactoryRegistryTest {

    @Test
    public void testArtifactoryRegistry() throws Exception {
        Method findRegistry = getFindRegistry();

        // Unset
        System.clearProperty(ArtifactoryRegistry.REGISTRY);
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.REGISTRY + " was unset");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Empty
        System.setProperty(ArtifactoryRegistry.REGISTRY, "");
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.REGISTRY + " was empty");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Missing
        System.setProperty(ArtifactoryRegistry.REGISTRY, "${" + ArtifactoryRegistry.REGISTRY.substring(9) + "}");
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.REGISTRY + " was missing from gradle");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Null
        System.setProperty(ArtifactoryRegistry.REGISTRY, "null");
        try {
            findRegistry.invoke(null);
            fail("Should not have found registry when property " + ArtifactoryRegistry.REGISTRY + " was null");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Valid
        String expected = "docker-na-public.artifactory.swg-devops.com";
        System.setProperty(ArtifactoryRegistry.REGISTRY, expected);
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
        System.clearProperty(ArtifactoryRegistry.REGISTRY_USER);
        System.clearProperty(ArtifactoryRegistry.REGISTRY_PASSWORD);

        try {
            generateAuthToken.invoke(null);
            fail("Should not have generated authToken when property "
                 + ArtifactoryRegistry.REGISTRY_USER + " and " + ArtifactoryRegistry.REGISTRY_PASSWORD + " were unset");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Ensure successful path
        System.setProperty(ArtifactoryRegistry.REGISTRY_USER, testUsername);
        System.setProperty(ArtifactoryRegistry.REGISTRY_PASSWORD, testToken);

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
