/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import org.testcontainers.utility.DockerImageName;

import componenttest.containers.registry.InternalRegistry;

/**
 * TODO javadoc and test
 */
public class ImageHelper {
    public static boolean isSyntheticImage(DockerImageName dockerImage) {
        return dockerImage.getRegistry().equals("localhost") && //
               dockerImage.getRepository().split("/")[0].equals("testcontainers") && //
               dockerImage.getVersionPart().equals("latest");
    }

    public static boolean isCommittedImage(DockerImageName dockerImage) {
        return dockerImage.getRepository().equals("sha256") ||
               dockerImage.getRepository().equals("aes") ||
               dockerImage.getRepository().equals("xor");
    }

    public static boolean isBuiltImage(DockerImageName dockerImage) {
        boolean matchesRegistry = !dockerImage.getRegistry().isEmpty() &&
                                  (dockerImage.getRegistry().equals("localhost") ||
                                   dockerImage.getRegistry().equals(InternalRegistry.instance().getRegistry()));
        boolean matchesRepository = dockerImage.getRepository().contains("openliberty/testcontainers");
        boolean nonLatestTag = !dockerImage.getVersionPart().equals("latest");
        return matchesRegistry && matchesRepository && nonLatestTag;
    }
}
