/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jbatch.joblog.internal.callback;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.callback.IJobExecutionEndCallbackService;
import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.ws.jbatch.joblog.services.IJobLogManagerService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class JobLogExecutionEndCallback implements
                IJobExecutionEndCallbackService {
    private IJobLogManagerService joblogManager;

    @Reference
    protected void setJobLogManagerService(IJobLogManagerService reference) {
        this.joblogManager = reference;
    }

    @Override
    public void jobEnded(WorkUnitDescriptor ctx) {
        if (joblogManager != null) {
            joblogManager.workUnitEnded(ctx);
        }
    }

}
