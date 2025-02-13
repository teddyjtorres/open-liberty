/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.beanvalidation.v20.cdi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.BootstrapConfiguration;
import javax.validation.ClockProvider;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.internal.engine.valueextraction.ValueExtractorDescriptor;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.BVNLSConstants;
import com.ibm.ws.beanvalidation.service.BvalManagedObjectBuilder;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * An implementation that is CDI aware.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "type=BvalMOBuilder" })
public class BvalManagedObjectBuilderImpl implements BvalManagedObjectBuilder {

    private static final TraceComponent tc = Tr.register(BvalManagedObjectBuilderImpl.class);
    private static TraceNLS nls = TraceNLS.getTraceNLS(BvalManagedObjectBuilderImpl.class, BVNLSConstants.BV_RESOURCE_BUNDLE);
    private static final String REFERENCE_CDI_SERVICE = "cdiService";
    private static final String REFERENCE_MANAGED_OBJECT_SERVICE = "managedObjectService";

    private final AtomicServiceReference<CDIService> cdiService = new AtomicServiceReference<CDIService>(REFERENCE_CDI_SERVICE);
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<ManagedObjectService>(REFERENCE_MANAGED_OBJECT_SERVICE);

    private static class GetInstancesFromServiceLoader implements PrivilegedAction<List<ValueExtractor>> {

        //private static final Log LOG = LoggerFactory.make(MethodHandles.lookup());

        private GetInstancesFromServiceLoader() {
        }

        public static <T> List<T> action(ClassLoader primaryClassLoader, Class<T> serviceClass) {
            // Option #1: try the primary class loader first (either the thread context class loader or the external class
            // loader that has been defined)
            List<T> instances = loadInstances(primaryClassLoader, serviceClass);

            // Option #2: if we cannot find any service files within the primary class loader, use the current class loader
            if (instances.isEmpty() && GetInstancesFromServiceLoader.class.getClassLoader() != primaryClassLoader) {
                instances = loadInstances(GetInstancesFromServiceLoader.class.getClassLoader(), serviceClass);
            }

            return instances;
        }

        private static <T> List<T> loadInstances(ClassLoader classloader, Class<T> clazz) {
            ServiceLoader<T> loader = ServiceLoader.load(clazz, classloader);
            Iterator<T> iterator = loader.iterator();
            List<T> instances = new ArrayList<T>();
            while (iterator.hasNext()) {
                try {
                    instances.add(iterator.next());
                } catch (ServiceConfigurationError e) {
                    // ignore, because it can happen when multiple
                    // services are present and some of them are not class loader
                    // compatible with our API.
                    // log an error still as it can hide a legitimate issue (see HV-1689)
                    //LOG.unableToLoadInstanceOfService(loader.getClass().getName(), e);
                    Tr.debug(tc, "unableToLoadInstanceOfService");
                }
            }
            return instances;
        }

        @Override
        public List<ValueExtractor> run() {

            return action(Thread.currentThread().getContextClassLoader(),
                          ValueExtractor.class);
        }
    }

    @Override
    public ValidatorFactory injectValidatorFactoryResources(Configuration<?> config, ClassLoader appClassLoader, Version runtimeVersion) {
        if (cdiService.getServiceWithException().isCurrentModuleCDIEnabled()) {
            createManagedConstraintValidatorFactory(config, appClassLoader, runtimeVersion);
            createManagedMessageInterpolator(config, appClassLoader);
            createManagedTraversableResolver(config, appClassLoader);
            createManagedParameterNameProvider(config, appClassLoader);
            createManagedClockProvider(config, appClassLoader);
            addValueExtractorBeans(config, appClassLoader);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Current module is not CDI enabled, skipping creating CDI enhanced objects.");
        }
        return config.buildValidatorFactory();
    }

    @Activate
    protected void activate(ComponentContext cc) {
        cdiService.activate(cc);
        managedObjectServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        cdiService.deactivate(cc);
        managedObjectServiceRef.deactivate(cc);
    }

    @Reference(name = REFERENCE_CDI_SERVICE, service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref) {
        cdiService.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref) {
        cdiService.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MANAGED_OBJECT_SERVICE,
               service = ManagedObjectService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        managedObjectServiceRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        managedObjectServiceRef.unsetReference(ref);
    }

    private void createManagedMessageInterpolator(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String messageInterpolatorClassName = bootstrapConfiguration.getMessageInterpolatorClassName();
        MessageInterpolator mi = null;

        if (messageInterpolatorClassName == null) {
            mi = config.getDefaultMessageInterpolator();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends MessageInterpolator> messageInterpolatorClass = (Class<? extends MessageInterpolator>) loadClass(messageInterpolatorClassName, appClassLoader);
            mi = createManagedObject(messageInterpolatorClass);
        }

        config.messageInterpolator(mi);
    }

    private void createManagedTraversableResolver(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String traversableResolverClassName = bootstrapConfiguration.getTraversableResolverClassName();
        TraversableResolver tr = null;

        if (traversableResolverClassName == null) {
            tr = config.getDefaultTraversableResolver();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends TraversableResolver> traversableResolverClass = (Class<? extends TraversableResolver>) loadClass(traversableResolverClassName, appClassLoader);
            tr = createManagedObject(traversableResolverClass);
        }

        config.traversableResolver(tr);
    }

    private void createManagedParameterNameProvider(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String parameterNameProviderClassName = bootstrapConfiguration.getParameterNameProviderClassName();
        ParameterNameProvider pnp = null;

        if (parameterNameProviderClassName == null) {
            pnp = config.getDefaultParameterNameProvider();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends ParameterNameProvider> parameterNameProviderClass = (Class<? extends ParameterNameProvider>) loadClass(parameterNameProviderClassName, appClassLoader);
            pnp = createManagedObject(parameterNameProviderClass);
        }

        config.parameterNameProvider(pnp);
    }

    private void createManagedClockProvider(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String clockProviderClassName = bootstrapConfiguration.getClockProviderClassName();
        ClockProvider clockProvider = null;

        if (clockProviderClassName == null) {
            clockProvider = config.getDefaultClockProvider();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends ClockProvider> clockProviderClass = (Class<? extends ClockProvider>) loadClass(clockProviderClassName, appClassLoader);
            clockProvider = createManagedObject(clockProviderClass);
        }

        config.clockProvider(clockProvider);
    }

    private void createManagedConstraintValidatorFactory(Configuration<?> config, ClassLoader appClassLoader, Version runtimeVersion) {
        BootstrapConfiguration configSource = config.getBootstrapConfiguration();
        String constraintValidatorFactoryClassName = configSource.getConstraintValidatorFactoryClassName();
        ConstraintValidatorFactory cvf = null;
        if (constraintValidatorFactoryClassName == null) {
            // use default
            if (isBeanValidationVersion31(runtimeVersion)) {
                //Hibernate Validator 9.0+ package location for InjectingConstraintValidatorFactory
                cvf = (ConstraintValidatorFactory) createManagedObject(org.hibernate.validator.cdi.spi.InjectingConstraintValidatorFactory.class);
            } else {
                cvf = createManagedObject(org.hibernate.validator.cdi.internal.InjectingConstraintValidatorFactory.class);
            }

        } else {
            @SuppressWarnings("unchecked")
            Class<? extends ConstraintValidatorFactory> constraintValidatorFactoryClass = (Class<? extends ConstraintValidatorFactory>) loadClass(constraintValidatorFactoryClassName,
                                                                                                                                                  appClassLoader);

            cvf = createManagedObject(constraintValidatorFactoryClass);
        }

        config.constraintValidatorFactory(cvf);
    }

    private void addValueExtractorBeans(Configuration<?> config, ClassLoader appClassLoader) {
        Map<ValueExtractorDescriptor.Key, ValueExtractorDescriptor> valueExtractorDescriptors = createValidationXmlValueExtractors(config,
                                                                                                                                   appClassLoader).stream().collect(Collectors.toMap(ValueExtractorDescriptor::getKey,
                                                                                                                                                                                     Function.identity()));

        for (ValueExtractorDescriptor serviceLoaderValueExtractorDescriptor : createServiceLoaderValueExtractors()) {
            valueExtractorDescriptors.putIfAbsent(serviceLoaderValueExtractorDescriptor.getKey(), serviceLoaderValueExtractorDescriptor);
        }

        for (ValueExtractorDescriptor valueExtractorDescriptor : valueExtractorDescriptors.values()) {
            config.addValueExtractor(valueExtractorDescriptor.getValueExtractor());
        }
    }

    private Set<ValueExtractorDescriptor> createValidationXmlValueExtractors(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        Set<String> valueExtractorClassNames = bootstrapConfiguration.getValueExtractorClassNames();

        @SuppressWarnings("unchecked")
        Set<ValueExtractorDescriptor> valueExtractorDescriptors = valueExtractorClassNames.stream() //
                        .map(fqcn -> createManagedObject((Class<? extends ValueExtractor<?>>) loadClass(fqcn, appClassLoader))) //
                        .map(valueExtractor -> new ValueExtractorDescriptor(valueExtractor)) //
                        .collect(Collectors.toSet());

        return valueExtractorDescriptors;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Set<ValueExtractorDescriptor> createServiceLoaderValueExtractors() {
        Set<ValueExtractorDescriptor> valueExtractorDescriptors = new HashSet<>();

        List<ValueExtractor> valueExtractors;

        valueExtractors = AccessController.doPrivileged((PrivilegedAction<List<ValueExtractor>>) () -> new GetInstancesFromServiceLoader().run());
        for (ValueExtractor<?> valueExtractor : valueExtractors) {
            valueExtractorDescriptors.add(new ValueExtractorDescriptor(createManagedObject((Class<? extends ValueExtractor<?>>) valueExtractor.getClass())));
        }
        return valueExtractorDescriptors;
    }

    private <T> ManagedObjectFactory<T> getManagedBeanManagedObjectFactory(Class<T> clazz) {
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        ManagedObjectService managedObjectService = managedObjectServiceRef.getServiceWithException();
        try {
            ManagedObjectFactory<T> factory = managedObjectService.createManagedObjectFactory(mmd, clazz, true);
            return factory;
        } catch (ManagedObjectException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create a ManagedObjectFactory for " + clazz.getName(), e);
            throw new ValidationException(nls.getString("BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY", "BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY"), e);
        }
    }

    private <T> T createManagedObject(Class<T> clazz) {
        // The mof handles calling produce, inject, and postConstruct.
        ManagedObjectFactory<T> mof = getManagedBeanManagedObjectFactory(clazz);

        ManagedObject<T> mo;
        try {
            mo = mof.createManagedObject();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create a ManagedObject using a ManagedObjectFactory for class type " + mof.getManagedObjectClass(), e);
            throw new ValidationException(nls.getString("BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY", "BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY"), e);
        }
        return mo.getObject();
    }

    private Class<?> loadClass(String className, ClassLoader appClassLoader) {
        try {
            return Class.forName(className, true, appClassLoader);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Class " + className + " not found during CDI enablement of the ValidatorFactory.", e);
            throw new ValidationException(nls.getString("BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY", "BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY"), e);
        }
    }

    private boolean isBeanValidationVersion31(Version runtimeVersion) {
        return runtimeVersion.compareTo(new Version(3, 1, 0)) == 0;
    }
}
