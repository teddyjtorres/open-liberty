/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package componenttest.containers;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * An image name substituter is configured in testcontainers.properties and will transform docker image names.
 * Here we use it to apply a private mirror registry and repository prefix so that in remote builds we use an internal
 * Artifactory mirror for a number of supported Docker image registries.
 */
@SuppressWarnings("deprecation")
public class ArtifactoryImageNameSubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = ArtifactoryImageNameSubstitutor.class;

    private static final ArtifactoryMirrorSubstitutor MIRROR = new ArtifactoryMirrorSubstitutor();
    private static final ArtifactoryRegistrySubstitutor REGISTRY = new ArtifactoryRegistrySubstitutor();

    /**
     * Manual override that will allow builds or users to pull from the default registry instead of Artifactory.
     */
    private static final String forceExternal = "fat.test.artifactory.force.external.repo";

    @Override
    public DockerImageName apply(final DockerImageName original) {
        final DockerImageName result;
        final boolean collect;
        final String reason;

        do {
            // Priority 1: If we are using a synthetic image do not substitute nor cache
            if (isSyntheticImage(original)) {
                result = original;
                collect = false;
                reason = "Image name is known to be synthetic, cannot use Artifactory registry.";
                break;
            }

            // Priority 2a: If the image is known to only exist in an Artifactory organization
            if (original.getRepository().contains("wasliberty-")) {
                result = REGISTRY.apply(original);
                collect = true;
                reason = "This image only exists in Artifactory, must use Artifactory registry.";
                break;
            }

            // Priority 2b: If the image is known to only exist in an Artifactory registry
            if (original.getRegistry() != null && original.getRegistry().contains("artifactory.swg-devops.com")) {
                throw new RuntimeException("Not all developers of Open Liberty have access to artifactory, must use a public registry.");
            }

            // Priority 3: If a public registry was explicitly set on an image, do not substitute
            // This is now handled directly by the MIRROR substitutor

            // Priority 4: Always use Artifactory if using remote docker host.
            if (DockerClientFactory.instance().isUsing(EnvironmentAndSystemPropertyClientProviderStrategy.class)) {
                result = REGISTRY.apply(MIRROR.apply(original));
                collect = true;
                reason = "Using a remote docker host, must use Artifactory registry";
                break;
            }

            // Priority 5: System property artifactory.force.external.repo
            // (NOTE: only honor this property if set to true)
            if (Boolean.getBoolean(forceExternal)) {
                result = original;
                collect = true;
                reason = "System property [ fat.test.artifactory.force.external.repo ] was set to true, must use original image name.";
                break;
            }

            // Priority 6: If Artifactory registry is available use it to avoid rate limits on other registries
            if (ArtifactoryRegistry.instance().isArtifactoryAvailable()) {
                result = REGISTRY.apply(MIRROR.apply(original));
                collect = true;
                reason = "Artifactory was available.";
                break;
            }

            //default - use original
            result = original;
            collect = true;
            reason = "Default behavior: use default docker registry.";
        } while (false);

        // Alert user that we either added the Artifactory registry or not.
        if (original == result) {
            Log.info(c, "apply", "Keeping original image name: " + original.asCanonicalNameString()
                                 + System.lineSeparator() + "Reason: " + reason);
        } else {
            Log.info(c, "apply", "Swapping docker image name " + original.asCanonicalNameString() + " --> " + result.asCanonicalNameString()
                                 + System.lineSeparator() + "Reason: " + reason);
        }

        // Collect image data for verification after testing
        if (collect) {
            return ImageVerifier.collectImage(original, result);
        } else {
            return original;
        }
    }

    @Override
    protected String getDescription() {
        return "ArtifactoryImageNameSubstitutor: Chained subsitutor of ArtifactoryMirrorSubstitutor and ArtifactoryRegistrySubstitutor";
    }

    /**
     * Docker images that are programmatically constructed at runtime (usually with ImageFromDockerfile)
     * will error out with a 404 if we attempt to do a docker pull from an Artifactory mirror registry.
     * To work around this issue, we will avoid image name substitution for image names that appear to be programmatically
     * generated by Testcontainers. FATs that use ImageFromDockerfile should consider using dedicated images
     * instead of programmatic construction (see com.ibm.ws.cloudant_fat/publish/files/couchdb-ssl/ for an example)
     */
    private static boolean isSyntheticImage(DockerImageName dockerImage) {
        String name = dockerImage.asCanonicalNameString();
        boolean isSynthetic = dockerImage.getRegistry().equals("localhost") && //
                              dockerImage.getRepository().split("/")[0].equals("testcontainers") && //
                              dockerImage.getVersionPart().equals("latest");
        boolean isCommittedImage = dockerImage.getRepository().equals("sha256");
        if (isSynthetic || isCommittedImage) {
            Log.warning(c, "WARNING: Cannot use private registry for programmatically built or committed image " + name +
                           ". Consider using a pre-built image instead.");
        }
        return isSynthetic || isCommittedImage;
    }
}
