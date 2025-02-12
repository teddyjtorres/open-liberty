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
package concurrent.cdi.web;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A bean that has a method with a signature that looks like it could be
 * an Asynchronous method, but doesn't have the annotation. A CDI extension
 * adds Asynchronous to it instead.
 */
@ApplicationScoped
public class TestBean {
    /**
     * Do not put @Asynchronous on this method.
     * We want to test that a CDI extension can do it.
     */
    public CompletableFuture<Thread> asyncByExtension() {
        return Asynchronous.Result.complete(Thread.currentThread());
    }
}
