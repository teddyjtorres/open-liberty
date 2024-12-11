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
package componenttest.topology.database.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.testcontainers.utility.DockerImageName;

/**
 * This class is a replacement for the regular <code>org.testcontainers.containers.PostgreSQLContainer</code> class.
 * This custom class is needed to fix the ordering of configure() so that we can set config options such as max_connections=2
 */
public class PostgreSQLContainer extends org.testcontainers.containers.PostgreSQLContainer<PostgreSQLContainer> {

    private final Map<String, String> options = new HashMap<>();

    public PostgreSQLContainer(final String dockerImageName) {
        super(dockerImageName);
    }

    public PostgreSQLContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    /**
     * Add additional configuration options that should be used for this container.
     *
     * @param  key   The PostgreSQL configuration option key. For example: "max_connections"
     * @param  value The PostgreSQL configuration option value. For example: "200"
     * @return       this
     */
    public PostgreSQLContainer withConfigOption(String key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        options.put(key, value);
        return this;
    }

    @Override
    protected void configure() {
        super.configure();

        if (!options.containsKey("fsync"))
            withConfigOption("fsync", "off");
        if (!options.containsKey("max_prepared_transactions"))
            withConfigOption("max_prepared_transactions", "2");

        String command = "postgres -c " + options.entrySet()
                        .stream()
                        .map(e -> e.getKey() + '=' + e.getValue())
                        .collect(Collectors.joining(" -c "));

        this.setCommand(command);
    }

    /**
     * Sets the necessary config options for enabling SSL for the container. Assumes there is
     * a server.crt and server.key file under /var/lib/postgresql/ in the container.
     * An easy way to use this is to combine it with the <code>kyleaure/postgres-ssl:1.0</code>
     * or similar base image
     */
    public PostgreSQLContainer withSSL() {
        withConfigOption("ssl", "on");
        withConfigOption("ssl_cert_file", "/var/lib/postgresql/server.crt");
        withConfigOption("ssl_key_file", "/var/lib/postgresql/server.key");
        return this;
    }

}
