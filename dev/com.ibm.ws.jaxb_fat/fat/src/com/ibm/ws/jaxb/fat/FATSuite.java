/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                LibertyJAXBTest.class,
                LibertyJAXBSpecTest.class,
                ThirdPartyJAXBFromJDKTest.class,
                ThirdPartyJAXBFromAppTest.class,
                JAXBToolsTest.class
})

public class FATSuite {
//    @ClassRule
//    public static RepeatTests r = RepeatTests.withoutModification()
//                    .andWith(new JakartaEE9Action())
//                    .andWith(new JakartaEE10Action());
}
