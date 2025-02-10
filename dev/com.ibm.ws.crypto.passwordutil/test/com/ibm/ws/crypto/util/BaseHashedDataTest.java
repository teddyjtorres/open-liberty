/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
package com.ibm.ws.crypto.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;

public class BaseHashedDataTest {

    public void testConstructorDefault(char[] plain, byte salt[], String algorithm, int iteration, int length, byte[] expectedOutput) {
        try {
            HashedData hd = new HashedData(plain, algorithm, salt, iteration, length, null);
            System.out.println("Adrian Kalafut -- testConstructorDefault " + formatToBytes(hd.toBytes()));
            assertTrue(Arrays.equals(hd.getPlain(), plain));
            assertTrue(Arrays.equals(hd.getSalt(), salt));
            assertEquals(algorithm, hd.getAlgorithm());
            assertEquals(iteration, hd.getIteration());
            assertEquals(length, hd.getOutputLength());
            assertTrue(Arrays.equals(hd.toBytes(), expectedOutput));
        } catch (Exception e) {
            fail("Unexpected exception : " + e.getMessage());
        }
    }

    public void testConstructorDigest(byte salt[], String algorithm, int iteration, int length, byte[] hashedDataInput) {
        try {
            HashedData hd = new HashedData(hashedDataInput);
            assertTrue(Arrays.equals(hd.getSalt(), salt));
            assertEquals(algorithm, hd.getAlgorithm());
            assertEquals(iteration, hd.getIteration());
            assertEquals(length, hd.getOutputLength());
        } catch (Exception e) {
            fail("Unexpected exception : " + e.getMessage());
        }
    }

    public void testConstructorException(char[] plain, byte salt[], String algorithm, int iteration, int length) {
        try {
            HashedData hd = new HashedData(plain, algorithm, salt, iteration, length, null);
            assertTrue(Arrays.equals(hd.getPlain(), plain));
            assertTrue(Arrays.equals(hd.getSalt(), salt));
            assertEquals(algorithm, hd.getAlgorithm());
            assertEquals(iteration, hd.getIteration());
            assertEquals(length, hd.getOutputLength());
            try {
                hd.toBytes();
            } catch (InvalidPasswordCipherException ipce) {
                // normal
            }
        } catch (Exception e) {
            fail("Unexpected exception : " + e.getMessage());
        }
    }

    public String formatToBytes(byte[] byteArray) {
        StringBuilder sb = new StringBuilder("{ ");

        for (int i = 0; i < byteArray.length; i++) {
            byte b = byteArray[i];
            if (b >= 0) {
                sb.append(String.format("0x%02X", b)); // Format positive byte as 0xHH
            } else {
                sb.append(String.format("(byte) (0x%02X & 0xFF)", b)); // Format negative byte explicitly
            }
            if (i < byteArray.length - 1) {
                sb.append(", ");
            }
        }

        sb.append(" }");
        return sb.toString();
    }
}