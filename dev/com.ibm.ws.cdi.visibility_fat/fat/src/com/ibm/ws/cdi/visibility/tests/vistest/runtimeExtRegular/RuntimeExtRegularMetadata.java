/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.vistest.runtimeExtRegular;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class RuntimeExtRegularMetadata implements CDIExtensionMetadata {

    /** {@inheritDoc} */
    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> result = new HashSet<>();
        result.add(RuntimeExtRegularTargetBean.class);
        result.add(RuntimeExtRegularTestingBean.class);
        return result;
    }

}
