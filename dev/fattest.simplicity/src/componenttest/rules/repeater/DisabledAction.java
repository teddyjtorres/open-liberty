/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package componenttest.rules.repeater;

import java.util.function.Predicate;

import componenttest.custom.junit.runner.Mode.TestMode;

//An action that is never enabled
public class DisabledAction implements RepeatTestAction {

    private static final Class<?> c = DisabledAction.class;

    public static final String ID = "DISABLED_ACTION";

    @Override
    public void setup() {}

    @Override
    public boolean isEnabled() {
        return false;
    }

    public RepeatTestAction fullFATOnly() {
        return this;
    }

    public DisabledAction conditionalFullFATOnly(Predicate<DisabledAction> conditional) {
        return this;
    }

    /**
     * Run the {@link DisabledAction} only when the testing mode is {@link TestMode#LITE}.
     *
     * @return this instance.
     */
    public RepeatTestAction liteFATOnly() {
        return this;
    }

    @Override
    public String toString() {
        return "Disabled Action";
    }

    @Override
    public String getID() {
        return ID;
    }

}
