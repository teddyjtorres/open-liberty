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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testcontainers.utility.MountableFile;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This builder class is an extension of {@link org.testcontainers.images.builder.ImageFromDockerfile}
 * and is intended to allow developers of Open Liberty the ability to create custom images from a Dockerfile.
 *
 * This class will is optimized to find or build the image using the following priority:
 * - If the image is cached on the docker host, use it.
 * - Otherwise, if the image is cached in the registry, pull it, and use it.
 * - Otherwise, build the image and cache it on the docker host.
 *
 * TODO make builder public once build pipeline is finalized
 */
class ImageBuilder {

    private static final Class<?> c = ImageBuilder.class;

    // The --build-arg necessary to overwrite the default BASE_IMAGE in the Dockerfile
    // with the mirrored image in artifactory
    private static final String BASE_IMAGE = "BASE_IMAGE";

    // Image to build
    private final DockerImageName image;

    // Constructor
    private ImageBuilder(DockerImageName image) {
        //builder class
        this.image = image;
    }

    /**
     * The image to build.
     *
     * The Dockerfile with instructions on how to build this image must be saved in source control in directory
     * io.openliberty.org.testcontainers/resources/openliberty/testcontainers/<image-name>/<image-version>/Dockerfile
     *
     * Note: The resulting image will be cached with name "localhost/openliberty/testcontainers/<image-name>:<image-version>"
     * therefore, you must update the image version whenever a change is made to the corresponding Dockerfile.
     *
     * @param  customImage the image to build in format "<image-name>:<image-version>" or "openliberty/testcontainers/<image-name>:<image-version>"
     *
     * @return             instance of ImageBuilder
     */
    public static ImageBuilder build(String customImage) {
        Objects.requireNonNull(customImage);

        return new ImageBuilder(ImageBuilderSubstitutor.instance().apply(DockerImageName.parse(customImage)));
    }

    // Add future configuration methods here

    /**
     * Termination point of this builder class.
     *
     * We will first attempt to find a cached version of the image,
     * if unsuccessful, we will then build the image from the Dockerfile.
     *
     * @return RemoteDockerImage that points to a cached or built image.
     */
    public RemoteDockerImage get() {
        return getCached().orElse(pullCached().orElse(buildFromDockerfile()));
    }

    /*
     * Helper method, attempts to find a cached version of the image.
     */
    private Optional<RemoteDockerImage> getCached() {
        final String m = "getLocalCache";

        RemoteDockerImage cachedImage = new RemoteDockerImage(image);

        if (PullPolicy.defaultPolicy().shouldPull(image)) {
            Log.info(c, m, "Unable to find cached image " + image.asCanonicalNameString());
            return Optional.empty();
        } else {
            Log.info(c, m, "Found cached image " + image.asCanonicalNameString());
            return Optional.of(cachedImage);
        }
    }

    /*
     * Helper method, attempt to pull a cached version from the a registry.
     */
    private Optional<RemoteDockerImage> pullCached() {
        final String m = "pullCached";

        if (image.getRegistry().equalsIgnoreCase("localhost")) {
            Log.info(c, m, "Assumed we cannot pull image from " + image.asCanonicalNameString());
            return Optional.empty();
        }

        RemoteDockerImage cachedImage = new RemoteDockerImage(image);

        try {
            cachedImage.get();
            Log.info(c, m, "Found pullable image " + image.asCanonicalNameString());
            return Optional.of(cachedImage);
        } catch (Exception e) {
            Log.info(c, m, "Unable to find pullable image " + image.asCanonicalNameString());
            return Optional.empty();
        }
    }

    /*
     * Helper method, constructs an image from a Dockerfile
     */
    private RemoteDockerImage buildFromDockerfile() {
        String resource = constructResource(image);
        String baseImage = findBaseImageFrom(resource).asCanonicalNameString();

        ImageFromDockerfile builtImage = new ImageFromDockerfile(image.asCanonicalNameString(), false)
                        .withFileFromClasspath(".", resource)
                        .withBuildArg(BASE_IMAGE, baseImage);

        return new RemoteDockerImage(builtImage);
    }

    /**
     * Helper method, constructs a resource path to the directory that holds the
     * Dockerfile and supporting files that define this image.
     *
     *
     * @param  image The name of this image in the form: openliberty/testcontainers/<image-name>:<image-version>
     * @return       the resource path in the form: /openliberty/testcontainers/<image-name>/<image-version>/
     */
    private static String constructResource(DockerImageName image) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("/");
        buffer.append(image.getRepository()).append("/");
        buffer.append(image.getVersionPart()).append("/");

        return buffer.toString();
    }

    /**
     * Helper method, searches for the Dockerfile on the classpath,
     * and attempts to find the line:
     * - ARG BASE_IMAGE="[base-image-of-docker-file]"
     *
     * Once found, run the BASE_IMAGE through the ImageNameSubstitutor
     * and return the DockerImageName result.
     *
     * @param  resource the resource path of the Dockerfile
     * @return          The substituted docker image of the BASE_IMAGE argument
     */
    private static DockerImageName findBaseImageFrom(String resource) {
        /*
         * Finds the Dockerfile on classpath and will extract the file to a temporary location so we can read it.
         * This will be done during the image build step anyway so this is just front-loading that work for our benefit.
         */
        String resourceDir = MountableFile.forClasspathResource(resource).getResolvedPath();

        Stream<String> dockerfileLines;
        try {
            dockerfileLines = Files.readAllLines(Paths.get(resourceDir, "Dockerfile")).stream();
        } catch (IOException e) {
            throw new RuntimeException("Could not read or find Dockerfile in " + resourceDir, e);
        }

        String errorMessage = "The Dockerfile did not contain a BASE_IMAGE argument declaration. "
                              + "This is required to allow us to pull and substitute the BASE_IMAGE using the ImageNameSubstitutor.";

        String baseImageName = dockerfileLines.filter(line -> line.startsWith("ARG BASE_IMAGE"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(errorMessage));

        return ImageNameSubstitutor.instance().apply(DockerImageName.parse(baseImageName));
    }

    /**
     * A ImageNameSubstitutor for images built by this outer class
     */
    private static class ImageBuilderSubstitutor extends ImageNameSubstitutor {

        // TODO replace with the finalized property expected on our build systems
        private static final String INTERNAL_REGISTRY_PROP = "io.openliberty.internal.registry";

        // Ensures when we look for cached images Docker only attempt to find images
        // locally or from an internally configured registry.
        private static final String REGISTRY = System.getProperty(INTERNAL_REGISTRY_PROP, "localhost");

        // The repository where all Open Liberty images will be cached
        private static final String REPOSITORY_PREFIX = "openliberty/testcontainers/";

        @Override
        public DockerImageName apply(final DockerImageName original) {
            Objects.requireNonNull(original);

            if (!original.getRegistry().isEmpty()) {
                throw new IllegalArgumentException("DockerImageName with the registry " + original.getRegistry() + " cannot be substituted with registry " + REGISTRY);
            }

            if (original.getRepository().startsWith(REPOSITORY_PREFIX)) {
                return original.withRegistry(REGISTRY);
            } else {
                return original.withRepository(REPOSITORY_PREFIX + original.getRepository()).withRegistry(REGISTRY);
            }
        }

        @Override
        protected String getDescription() {
            return "ImageBuilderSubstitutor with registry " + REGISTRY;
        }

    }

}