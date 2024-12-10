/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Substitutes a known registry for a known remote mirror repository within Artifactory.
 * Example: public.ecr.aws/docker/library/postgres:17.0-alpine --> wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine
 */
public class ArtifactoryMirrorSubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = ArtifactoryMirrorSubstitutor.class;

    private static final Map<String, String> REGISTRY_MAP = new HashMap<>();
    static {
        REGISTRY_MAP.put("NONE", "wasliberty-infrastructure-docker");
        REGISTRY_MAP.put("docker.io", "wasliberty-docker-remote"); //Only for verified images
//        REGISTRY_MAP.put("ghcr.io", "TODO");
//        REGISTRY_MAP.put("icr.io", "TODO");
//        REGISTRY_MAP.put("mcr.microsoft.com", "TODO");
        REGISTRY_MAP.put("public.ecr.aws", "wasliberty-aws-docker-remote");
    }

    @Override
    public DockerImageName apply(final DockerImageName original) {

        final String repository;
        final DockerImageName result;

        if (original.getRegistry().isEmpty()) {
            repository = REGISTRY_MAP.get("NONE") + "/" + original.getRepository();
            result = DockerImageName.parse(original.asCanonicalNameString())
                            .withRepository(repository);
            Log.info(c, "apply", original.asCanonicalNameString() + " --> " + result.asCanonicalNameString());
            return result;
        }

        if (REGISTRY_MAP.containsKey(original.getRegistry())) {
            repository = REGISTRY_MAP.get(original.getRegistry()) + "/" + original.getRepository();
            result = DockerImageName.parse(original.asCanonicalNameString())
                            .withRepository(repository);
            Log.info(c, "apply", original.asCanonicalNameString() + " --> " + result.asCanonicalNameString());
            return result;
        }

        Log.info(c, "apply", original.asCanonicalNameString() + " --> [UNCHANGED]");
        return original;
    }

    @Override
    protected String getDescription() {
        return "Substitutes a known registry for a known remote mirror repository within Artifactory.";
    }
}
