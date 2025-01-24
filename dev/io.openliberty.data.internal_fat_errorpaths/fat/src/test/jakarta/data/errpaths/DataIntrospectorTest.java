/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.errpaths;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;

/**
 * Contains tests that make assertions on the Jakarta Data introspector output.
 * The introspector runs at the end of DataErrPathsTest before stopping the server
 * so that it contains the state of a running server, and guaranteeing that it is
 * available to these tests.
 */
@RunWith(FATRunner.class)
public class DataIntrospectorTest extends FATServletClient {
    /**
     * Jakarta Data introspector output is captured during DataErrPathsTest.tearDown
     */
    static final List<String> introspectorOutput = new ArrayList<>();

    /**
     * Verify that introspector output was obtained.
     */
    @Test
    public void testIntrospectorOutputObtained() {
        assertEquals(false, introspectorOutput.isEmpty());

        // To view output, from the test results page, follow the System.out link
        // and search for "testIntrospectorOutputObtained"
        for (String line : introspectorOutput)
            System.out.println(line);
    }
}
