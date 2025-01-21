/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package io.openliberty.org.testcontainers.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * Allows external contributors a convenient way to build the custom images we
 * use in our build
 */
public class CustomImages {

	// The --build-arg necessary to overwrite the default BASE_IMAGE in the
	// Dockerfile
	// with the mirrored image in artifactory
	public static final String BASE_IMAGE = "BASE_IMAGE";

	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		if (args == null || args[0] == null || args.length > 1) {
			throw new RuntimeException(
					"CustomImages expects a single argument (projectPath) which is the path to the io.openliberty.org.testcontainers project.");
		}

		// Get data from calling script
		String projectPath = args[0];

		// Where to find instructions to build images
		Path commonPath = Paths.get(projectPath, "resources", "openliberty", "testcontainers");

		// Construct a list of Dockerfiles
		findDockerfiles(commonPath).stream()
				.map(location -> new Dockerfile(location))
				.forEach(dockerfile -> {
					// Find or build all images
					if(dockerfile.isCached()) {
						System.out.println("Skipping build: " + dockerfile.imageName.asCanonicalNameString());
						System.out.println("-----");
						return;
					}
					
					ImageFromDockerfile img = new ImageFromDockerfile(dockerfile.imageName.asCanonicalNameString(), false)
							.withDockerfile(dockerfile.location)
							.withBuildArg(BASE_IMAGE, dockerfile.baseName.asCanonicalNameString());

					try {
						System.out.println("Building image: " + dockerfile.imageName.asCanonicalNameString());
						img.get();
						System.out.println("Built image successfully: " + dockerfile.imageName.asCanonicalNameString());
					} catch (Exception e) {
						throw new RuntimeException("Could not build or find image " + dockerfile.imageName.asCanonicalNameString(), e);
					} finally {
						System.out.println("-----");
					}
				});

		long end = System.currentTimeMillis();
		System.out.println("Execution time in ms: " + (end - start));
	}

	/**
	 * Walk through all files nested within a shared path and find every Dockerfile.
	 * 
	 * @param commonPath the shared path within which all Dockerfiles are nested
	 *                   within
	 * @return A list of paths to every Dockerfile
	 */
	private static List<Path> findDockerfiles(Path commonPath) {
		final String FILE_NAME = "Dockerfile";
		final List<Path> Dockerfiles = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(commonPath)) {
			paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(FILE_NAME))
					.forEach(Dockerfiles::add);
		} catch (IOException e) {
			throw new RuntimeException("Error searching files: " + e.getMessage());
		}

		return Dockerfiles;
	}
	
	/**
	 * A record of a Dockerfile
	 */
	private static class Dockerfile {
		public final Path location;
		public final DockerImageName imageName;
		public final DockerImageName baseName;
		
		public Dockerfile(Path location) {
			this.location = location;
			this.imageName = constructImageName(location);
			this.baseName = findBaseImageFrom(location);
		}
		
		public boolean isCached() {
	        if (PullPolicy.defaultPolicy().shouldPull(imageName)) {
	        	System.out.println("Did not find image locally: " + imageName.asCanonicalNameString());
	            return false;
	        } else {
	        	System.out.println("Found image locally: " + imageName.asCanonicalNameString());
	            return true;
	        }
		}
		
		/**
		 * Using the Dockerfile's path, parse the directory structure to construct a
		 * fully qualified DockerImageName to be associated with this Dockerfile.
		 * 
		 * @param location of Dockerfile to be built
		 * @return The DockerImageName for this Dockerfile
		 */
		private static DockerImageName constructImageName(Path location) {

			// io.openliberty.org.testcontainers/resources/openliberty/testcontainers/[repository]/[version]/Dockerfile
			final String fullPath = location.toString();

			// Find version (between the last two backslash / characters)
			int end = fullPath.lastIndexOf('/');
			int start = fullPath.substring(0, end).lastIndexOf('/') + 1;
			final String version = fullPath.substring(start, end);

			// Find repository (between "resources/" and version)
			start = fullPath.lastIndexOf("resources/") + 10;
			end = fullPath.indexOf(version) - 1;
			final String repository = fullPath.substring(start, end);

			// Construct and return name
			DockerImageName name = ImageBuilderSubstitutor.instance()
					.apply(DockerImageName.parse(repository).withTag(version));
			System.out.println("DockerImageName from path: " + name.asCanonicalNameString());
			return name;
		}

		/**
		 * Similar logic to ImageBuilder.findBaseImageFrom(resource)
		 * 
		 * However, in this case we can only use the ArtifactoryMirrorSubstitutor so we
		 * have to manually put in the Artifactory registry (when available)
		 * 
		 * @param location of Dockerfile the resource path of the Dockerfile
		 * @return The substituted docker image of the BASE_IMAGE argument
		 */
		private DockerImageName findBaseImageFrom(Path location) {

			final String BASE_IMAGE_PREFIX = "ARG BASE_IMAGE=\"";
			final String ARTIFACTORY_REGISTRY = System.getenv("ARTIFACTORY_REGISTRY");

			Stream<String> dockerfileLines;
			try {
				dockerfileLines = Files.readAllLines(location).stream();
			} catch (IOException e) {
				throw new RuntimeException("Could not read or find Dockerfile in " + location.toString(), e);
			}

			String errorMessage = "The Dockerfile did not contain a BASE_IMAGE argument declaration. "
					+ "This is required to allow us to pull and substitute the BASE_IMAGE using the ImageNameSubstitutor.";

			String baseImageLine = dockerfileLines.filter(line -> line.startsWith(BASE_IMAGE_PREFIX)).findFirst()
					.orElseThrow(() -> new IllegalStateException(errorMessage));

			String baseImageName = baseImageLine.substring(BASE_IMAGE_PREFIX.length(), baseImageLine.lastIndexOf('"'));

			DockerImageName original = DockerImageName.parse(baseImageName);
			DockerImageName substituted = ImageNameSubstitutor.instance().apply(original);

			if (original.equals(substituted)) {
				System.out.println("Keep original BASE_IMAGE: " + original.asCanonicalNameString());
				return original;
			} else {
				// Substitutor was used, also prepend the registry.
				System.out.println("Use substituted BASE_IMAGE: " + substituted.asCanonicalNameString());
				return substituted.withRegistry(ARTIFACTORY_REGISTRY);
			}
		}
	}

	/**
	 * A ImageNameSubstitutor for images built by this outer class.
	 */
	private static class ImageBuilderSubstitutor extends ImageNameSubstitutor {

		// TODO replace with the finalized property expected on our build systems
		private static final String INTERNAL_REGISTRY_ENV = "INTERNAL_REGISTRY";

		// Ensures when we look for cached images Docker only attempt to find images
		// locally or from an internally configured registry.
		private static final String REGISTRY = System.getenv(INTERNAL_REGISTRY_ENV) == null 
				? "localhost"
				: System.getenv(INTERNAL_REGISTRY_ENV);

		// The repository where all Open Liberty images will be cached
		private static final String REPOSITORY_PREFIX = "openliberty/testcontainers/";

		@Override
		public DockerImageName apply(final DockerImageName original) {
			Objects.requireNonNull(original);

			if (!original.getRegistry().isEmpty()) {
				throw new IllegalArgumentException("DockerImageName with the registry " + original.getRegistry()
						+ " cannot be substituted with registry " + REGISTRY);
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
		
        // Hide instance method from parent class.
        // which will choose the ImageNameSubstitutor based on environment.
        private static ImageBuilderSubstitutor instance;

        public static synchronized ImageNameSubstitutor instance() {
            if (Objects.isNull(instance)) {
                instance = new ImageBuilderSubstitutor();
            }
            return instance;
        }
	}
}
