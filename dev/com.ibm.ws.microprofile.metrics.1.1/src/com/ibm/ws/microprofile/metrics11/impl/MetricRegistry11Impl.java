/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.metrics11.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * A registry of metric instances.
 */
@Vetoed
public class MetricRegistry11Impl extends MetricRegistryImpl {

    private final ConfigProviderResolver configResolver;

    /**
     * Creates a new {@link MetricRegistry}.
     *
     * @param configResolver
     */
    public MetricRegistry11Impl(ConfigProviderResolver configResolver) {
        super();
        this.configResolver = configResolver;
    }

    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        // For MP Metrics 1.0, MetricType.from(Class in) does not support lambdas or proxy classes
        return register(newMetadata(name, from(metric)), metric);
    }

    @Override
    @Deprecated
    public <T extends Metric> T register(String name, T metric, Metadata metadata) throws IllegalArgumentException {
        return register(metadata, metric);
    }

    @Override
    @FFDCIgnore({ NoSuchElementException.class })
    public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
        //Create Copy of Metadata object so it can't be changed after its registered
        Metadata metadataCopy = newMetadata(metadata);
        for (String tag : metadata.getTags().keySet()) {
            metadataCopy.getTags().put(tag, metadata.getTags().get(tag));
        }
        metadataCopy.setReusable(metadata.isReusable());

        //Append global tags to the metric
        Config config = configResolver.getConfig(getThreadContextClassLoader());
        try {
            Optional<String> val = config.getOptionalValue("MP_METRICS_TAGS", String.class);
            if (val != null && val.isPresent()) {
                String[] globaltags = val.get().split(",");
                String currentTags = metadataCopy.getTagsAsString();
                for (String tag : globaltags) {
                    if (!(tag == null || tag.isEmpty() || !tag.contains("="))) {
                        if (!currentTags.contains(tag.split("=")[0])) {
                            metadataCopy.addTag(tag);
                        }
                    }
                }
            }
        } catch (NoSuchElementException e) {
            //Continue if there is no global tags
        }

        final Metric existing = metrics.putIfAbsent(metadata.getName(), metric);
        this.metadata.putIfAbsent(metadata.getName(), metadataCopy);
        if (existing == null) {
        } else {
            throw new IllegalArgumentException("A metric named " + metadata.getName() + " already exists");
        }

        /*
         * This is the method used by monitor metrics to register metrics.
         * Previously, connectionpool metrics will be associated with an application
         * as the initial creation of a connection pool occurs under an application context thread.
         *
         * We must avoid associating connection pool metrics to an application
         * so that it is not deregistered. The metric is to remain until the datasource
         * is removed via mbean deregistration (i.e., server shut down or jbc-x.x is removed or thee datasource
         * element in sever.xml is removed.
         *
         * mpMetrics-1.1 uses connecitonpool.%s.<rest_of_metric> so we'll check for startsWith and endsWith
         */
        String metricName = metadata.getName();
        if (metricName.startsWith("connectionpool.")
            && (metricName.endsWith(".create.total") ||
                metricName.endsWith(".destroy.total") ||
                metricName.endsWith(".managedConnections") ||
                metricName.endsWith(".connectionHandles") ||
                metricName.endsWith(".freeConnections") ||
                metricName.endsWith(".waitTime.total") ||
                metricName.endsWith(".inUseTime.total") ||
                metricName.endsWith(".queuedRequests.total") ||
                metricName.endsWith(".usedConnections.total"))) {
            return metric;
        }

        addNameToApplicationMap(metadata.getName());
        return metric;
    }

    private static ClassLoader getThreadContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            return Thread.currentThread().getContextClassLoader();
        });
    }

    private static Metadata newMetadata(String name, MetricType type) {
        if (System.getSecurityManager() == null) {
            return new Metadata(name, type);
        }
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Metadata>) () -> {
                return new Metadata(name, type);
            });
        } catch (PrivilegedActionException pae) {
            throw new IllegalArgumentException(pae.getCause());
        }
    }

    private static Metadata newMetadata(Metadata metadata) {
        if (System.getSecurityManager() == null) {
            return new Metadata(metadata.getName(), metadata.getDisplayName(), metadata.getDescription(), metadata.getTypeRaw(), metadata.getUnit());
        }
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Metadata>) () -> {
                return new Metadata(metadata.getName(), metadata.getDisplayName(), metadata.getDescription(), metadata.getTypeRaw(), metadata.getUnit());
            });
        } catch (PrivilegedActionException pae) {
            throw new IllegalArgumentException(pae.getCause());
        }
    }
}
