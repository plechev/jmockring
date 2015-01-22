/*
 * Copyright (c) 2013, Pavel Lechev
 *    All rights reserved.
 *
 *    Redistribution and use in source and binary forms, with or without modification,
 *    are permitted provided that the following conditions are met:
 *
 *     1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     2) Redistributions in binary form must reproduce the above copyright notice,
 *        this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     3) Neither the name of the Pavel Lechev nor the names of its contributors may be used to endorse or promote
 *        products derived from this software without specific prior written permission.
 *
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 *    INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *    IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jmockring.spring.mock;

import static java.lang.String.format;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import org.jmockring.configuration.BaseContextConfiguration;
import org.jmockring.spring.SpringContextUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Create Mockito mocks for all unavailable beans ...
 *
 * @author Pavel Lechev
 * @date 23/07/12
 */
public class BeanAutoMockPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BeanAutoMockPostProcessor.class);

    private final Map<String, Class> mockedBeans = new ConcurrentHashMap<String, Class>();

    private ApplicationContext applicationContext;

    private Set<Class> availableFactoredTypes = null; // this must be null initially

    private Set<MockedBeanDescriptor> createdForcedMocks = new HashSet<MockedBeanDescriptor>();

    private final BaseContextConfiguration contextConfiguration;

    public BeanAutoMockPostProcessor(BaseContextConfiguration contextConfiguration) {
        this.contextConfiguration = contextConfiguration;
    }


    /**
     * This kick-starts the auto-mocking bean definition processing.
     * <p/>
     * Reset/Initialise any per-context state in this method.
     *
     * @param registry
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) registry;
        if (contextConfiguration.getForcedMockTypes().length > 0) {
            processForcedMocks(factory);
        }

        Set<String> overriddenForcedMocks = new HashSet<String>();
        for (String beanName : factory.getBeanDefinitionNames()) {
            BeanDefinition owningBeanDefinition = registry.getBeanDefinition(beanName);
            String beanClassName = owningBeanDefinition.getBeanClassName();
            if (beanClassName == null) {
                log.warn("LOG00240: Class name not found for beanName: {}", beanName);
                continue;
            } else if (isSpringInternalBean(beanClassName)) {
                log.debug("LOG00241: Skipping Spring internal bean: {}", beanName);
                continue;
            } else if (isJmockringMock(beanClassName)) {
                log.debug("LOG00242: Skipping Jmockring Mocking factory bean: {}", beanName);
                continue;
            }

            try {
                Class beanClass = Class.forName(beanClassName);
                String forceMockedBeanName = getForceMockedBeanName(beanClass);
                if (forceMockedBeanName != null) {
                    registry.removeBeanDefinition(beanName);
                    registerMockedBean(factory, beanName, beanClass);
                    overriddenForcedMocks.add(forceMockedBeanName);
                    log.info("LOG00380: Overriding bean definition factory name for bean [{}={}] as its type is already force-mocked", beanName, beanClass.getName());
                    // this bean will be mocked, no need to rocess its dependencies, so we skip the rest of its processing
                    continue;
                }
                if (contextConfiguration.isEnableAutoMocks()) {
                    // System.err.println(">>>>>>>>> Check class for creating referenced mocks: " + beanClass);
                    processAnnotatedDependencies(factory, beanClass);
                    processExtraDependencies(factory, beanClass, owningBeanDefinition);
                }
            } catch (Exception e) {
                log.error("LOG00160:", e);
            }
        }

        for (String overriddenForcedMockName : overriddenForcedMocks) {
            registry.removeBeanDefinition(overriddenForcedMockName);
            mockedBeans.remove(overriddenForcedMockName);
            log.info("LOG00390: Removing overridden forced-mock definition for name = {}", overriddenForcedMockName);
        }
    }

    private boolean isJmockringMock(String beanClassName) {
        return SpringMockitoFactory.class.getName().equals(beanClassName);
    }

    /**
     * @param type
     *
     * @return
     */
    private String getForceMockedBeanName(Class type) {
        for (MockedBeanDescriptor mockedDescriptor : createdForcedMocks) {
            if (mockedDescriptor.getType().isAssignableFrom(type)) {
                return mockedDescriptor.getName();
            }
        }
        return null;
    }


    private static boolean isSpringInternalBean(String beanClassName) {
        return beanClassName.startsWith("org.springframework.");
    }


    /**
     * @param factory
     *
     * @return
     */
    private void processForcedMocks(DefaultListableBeanFactory factory) {
        for (Class<?> typeToForceMock : contextConfiguration.getForcedMockTypes()) {
            String beanName = getSpringCompatibleBeanName(typeToForceMock);
            MockedBeanDescriptor descriptor = registerMockedBean(factory, beanName, typeToForceMock);
            if (descriptor != null) {
                log.info("LOG00360: Created forced mock for bean [{}={}]", beanName, typeToForceMock.getName());
                createdForcedMocks.add(descriptor);
                if (typeToForceMock.isInterface() || Modifier.isAbstract(typeToForceMock.getModifiers())) {
                    // raise warning, so user can add the types to the #forcedMockTypes()
                    log.warn("Forced mock is created for an interface/abstract {}; Make sure all implementing classes are also included in #forcedMockTypes()");
                }
            }
        }
    }

    /**
     * @param factory
     * @param beanClass
     *
     * @throws ClassNotFoundException
     */
    private void processAnnotatedDependencies(DefaultListableBeanFactory factory, Class beanClass) throws ClassNotFoundException, NoSuchMethodException {
        WiringOperation mockOperation = null;
        List<AccessibleObject> annotationWiredMembers = getWiredMembers(beanClass);
        if (annotationWiredMembers.size() > 0) {
            for (final AccessibleObject accessibleObject : annotationWiredMembers) {
                if (accessibleObject.getClass() == Field.class) {
                    mockOperation = WiringOperation.FIELD;
                } else if (accessibleObject.getClass() == Method.class) {
                    mockOperation = WiringOperation.METHOD;
                } else if (accessibleObject.getClass() == Constructor.class) {
                    mockOperation = WiringOperation.CONSTRUCTOR;
                }
                if (mockOperation != null) {
                    mockOperation.mock(this, factory, accessibleObject);
                }
            }
        }
    }

    /**
     * @param factory
     * @param beanClass
     * @param beanDefinition
     */
    private void processExtraDependencies(DefaultListableBeanFactory factory, Class beanClass, BeanDefinition beanDefinition) throws NoSuchMethodException {
        List<PropertyDescriptor> xmlWiredMembers = getXmlWiredMembers(beanClass, beanDefinition, factory);
        if (xmlWiredMembers.size() > 0) {
            // log.info("Processing XML defined properties for {} [{}]", beanClass.getName(), xmlWiredMembers);
            for (PropertyDescriptor pd : xmlWiredMembers) {
                String beanNameToMock = (String) pd.getValue("referencedBeanName");
                Class beanTypeToMock = pd.getPropertyType();
                registerMockedBean(factory, beanNameToMock, beanTypeToMock);

            }
        }
    }

    /**
     * This checks for any dependencies not identified via annotaiton wiring (e.g. from XMLs).
     *
     * @param beanClass
     * @param beanDefinition
     * @param factory
     *
     * @return
     */
    private List<PropertyDescriptor> getXmlWiredMembers(Class beanClass, BeanDefinition beanDefinition, DefaultListableBeanFactory factory) throws NoSuchMethodException {

        Map<String, RuntimeBeanReference> beanReferences = new HashMap<String, RuntimeBeanReference>();
        // check properties:
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<PropertyDescriptor>();
        MutablePropertyValues pv = beanDefinition.getPropertyValues();
        if (pv.getPropertyValueList().size() > 0) {
            for (PropertyValue prop : pv.getPropertyValueList()) {
                String propertyName = prop.getName();
                Object propertyValue = prop.getValue();
                if (propertyValue instanceof RuntimeBeanReference) {
                    beanReferences.put(propertyName, (RuntimeBeanReference) propertyValue);
                }
            }
        }
        // check constructor injection:
        ConstructorArgumentValues constrArgValues = beanDefinition.getConstructorArgumentValues();
        Map<Integer, ConstructorArgumentValues.ValueHolder> idxConstrValues = constrArgValues.getIndexedArgumentValues();
        if (idxConstrValues.size() > 0) {
            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> holder : idxConstrValues.entrySet()) {
                ConstructorArgumentValues.ValueHolder arg = holder.getValue();
                if (arg.getValue() instanceof RuntimeBeanReference) {
                    beanReferences.put(((RuntimeBeanReference) arg.getValue()).getBeanName(), (RuntimeBeanReference) arg.getValue());
                }
            }
        } else {
            List<ConstructorArgumentValues.ValueHolder> genConstrValues = constrArgValues.getGenericArgumentValues();
            for (ConstructorArgumentValues.ValueHolder arg : genConstrValues) {
                if (arg.getValue() instanceof RuntimeBeanReference) {
                    beanReferences.put(((RuntimeBeanReference) arg.getValue()).getBeanName(), (RuntimeBeanReference) arg.getValue());
                }
            }
        }

        // process dependent beans
        for (Map.Entry<String, RuntimeBeanReference> beanRef : beanReferences.entrySet()) {
            String referencedBeanName = beanRef.getValue().getBeanName();
            String propertyName = beanRef.getKey();
            if (!factory.isBeanNameInUse(referencedBeanName)) {
                // check if there is a factory bean declared for this type


                // bean is not available
                log.warn(">>> Referenced bean {} not available for {}: ", referencedBeanName, beanDefinition + "." + propertyName);
                PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(beanClass, propertyName);
                if (pd == null) {
                    // pd not found: check the fields:
                    for (final Field field : beanClass.getDeclaredFields()) {
                        if (field.getName().equals(propertyName)) {
                            try {
                                pd = new PropertyDescriptor(propertyName, beanClass, null, null) {
                                    @Override
                                    public synchronized Class<?> getPropertyType() {
                                        return field.getType();
                                    }
                                };
                            } catch (IntrospectionException e) {
                                log.error("RemoteBean introspection error", e);
                            }
                            break;
                        }
                    }
                }
                /*
                   TODO: need cleaning up
                 */
                if (pd == null && (propertyName.equals("configuration") || propertyName.equals("properties"))) {
                    // this is just a lucky dip : obviously will not work all the times
                    try {
                        log.warn("Bypass type checks for bean dependency name [{}] in class [{}]", propertyName, beanClass.getName());
                        pd = new PropertyDescriptor(propertyName, beanClass, null, null) {
                            @Override
                            public synchronized Class<?> getPropertyType() {
                                return Properties.class;
                            }
                        };
                    } catch (IntrospectionException e) {
                        log.error("RemoteBean introspection error", e);
                    }
                }
                if (pd == null) {
                    throw new IllegalStateException(
                            format("Can't find property descriptor for class [%s] and property [%s]", beanClass.getName(), referencedBeanName));
                }
                pd.setValue("referencedBeanName", referencedBeanName);
                if (!isFactoryBeanAvailable(factory, pd.getPropertyType())) {
                    // only add it for processing iof there is no factory bean for this type
                    propertyDescriptors.add(pd);
                }
            }
        }
        return propertyDescriptors;
    }


    /**
     * The relies on all factory beans being parameterised with the relevant bean type they create.
     *
     * @param factory
     * @param beanClassToCheck
     *
     * @return
     */
    private boolean isFactoryBeanAvailable(DefaultListableBeanFactory factory, Class<?> beanClassToCheck) throws NoSuchMethodException {
        if (availableFactoredTypes == null) {
            obtainFactoredTypes(factory);
        }
        for (Class factoredBeanClass : availableFactoredTypes) {
            if (beanClassToCheck.isAssignableFrom(factoredBeanClass)) {
                log.info("Found factory for bean type '{}' ", beanClassToCheck.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * @param factory
     */
    private void obtainFactoredTypes(DefaultListableBeanFactory factory) throws NoSuchMethodException {
        if (availableFactoredTypes != null) {
            throw new IllegalStateException("obtainFactoredTypes() was already called - there should not be a need to call more than once!");
        }
        availableFactoredTypes = new HashSet<Class>();
        String[] allBdNames = factory.getBeanDefinitionNames();
        for (String bdName : allBdNames) {
            BeanDefinition bd = factory.getBeanDefinition(bdName);
            try {
                Class fClass = Class.forName(bd.getBeanClassName());
                if (FactoryBean.class.isAssignableFrom(fClass)) {
                    Class factoredBeanClass = null;
                    if (fClass.getGenericInterfaces() != null && fClass.getGenericInterfaces().length > 0) {
                        if (fClass.getGenericInterfaces()[0] instanceof ParameterizedType) {
                            Type[] genericTypes = ((ParameterizedType) fClass.getGenericInterfaces()[0]).getActualTypeArguments();
                            if (genericTypes.length > 0) {
                                factoredBeanClass = (Class) genericTypes[0];
                            }
                        }
                    }
                    if (factoredBeanClass == null) {
                        // check the return type of #getObject()
                        Method factoryMethod = fClass.getMethod("getObjectType", null);
                        Class<?> factoryReturnType = factoryMethod.getReturnType();
                        if (factoryReturnType.getGenericInterfaces() != null && factoryReturnType.getGenericInterfaces().length > 0) {
                            if (factoryReturnType.getGenericInterfaces()[0] instanceof ParameterizedType) {
                                Type[] genericTypes = ((ParameterizedType) factoryReturnType.getGenericInterfaces()[0]).getActualTypeArguments();
                                if (genericTypes.length > 0) {
                                    factoredBeanClass = (Class) genericTypes[0];
                                }
                            }
                        }
                    }
                    if (factoredBeanClass != null) {
                        log.info("Found factory bean '{}' for type '{}'", fClass.getName(), factoredBeanClass.getName());
                        availableFactoredTypes.add(factoredBeanClass);
                    } else {
                        // the alternative to find out the type is to create instance of the factory, call the #getObject and inspect the returned type,
                        // however this can cause problems, so we are not doing it here ... instead alert the user.
                        log.warn("Found FactoryBean class {} which does not define generic interface for the target bean. Factory auto-mocking is likely to fail.", fClass.getName());
                    }
                }
            } catch (ClassNotFoundException e) {
                log.warn("An attempt to load bean class failed", e);
            }
        }
    }


    /**
     * Use to verify mocks are created OK
     *
     * @param beanFactory
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // verify that all mocked beans are present in the factory
        for (Map.Entry<String, Class> mockedBean : mockedBeans.entrySet()) {
            if (!beanFactory.containsBean(mockedBean.getKey())) {
                // this must be present at this point - perhaps even throw an exception ?
                log.error("RemoteBean not found {}", mockedBean.getKey());
            }
        }
    }

    /**
     * Return all fields with Autowired or Inject annotations
     *
     * @param targetBean
     *
     * @return
     */
    private List<AccessibleObject> getWiredMembers(Class targetBean) {
        List<AccessibleObject> accessibles = new ArrayList<AccessibleObject>();
        // fields
        accessibles.addAll(Collections2.filter(Arrays.asList(targetBean.getDeclaredFields()), new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return input.isAnnotationPresent(Autowired.class) || input.isAnnotationPresent(Inject.class) || input.isAnnotationPresent(PersistenceContext.class);
            }
        }));
        // methods
        accessibles.addAll(Collections2.filter(Arrays.asList(targetBean.getDeclaredMethods()), new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
                return input.isAnnotationPresent(Autowired.class) || input.isAnnotationPresent(Inject.class) || input.isAnnotationPresent(PersistenceContext.class);
            }
        }));
        // methods
        accessibles.addAll(Collections2.filter(Arrays.asList(targetBean.getDeclaredConstructors()), new Predicate<Constructor>() {
            @Override
            public boolean apply(Constructor input) {
                return input.isAnnotationPresent(Autowired.class) || input.isAnnotationPresent(Inject.class) || input.isAnnotationPresent(PersistenceContext.class);
            }
        }));
        return accessibles;
    }

    /**
     * Retrieve mock from the context
     *
     * @param name
     * @param type
     * @param <T>
     *
     * @return
     */
    public <T extends Object> T getMock(String name, Class<T> type) {
        T bean = applicationContext.getBean(name, type);
        if (bean != null && mockedBeans.containsKey(name)) {
            throw new IllegalArgumentException(
                    format("The bean for name '%s' and type '%s' is not a mock.", name, type));
        }
        return bean;
    }

    /**
     * @param applicationContext
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     *
     */
    private enum WiringOperation {

        FIELD {
            @Override
            MockedBeanDescriptor[] mock(BeanAutoMockPostProcessor autoMockProcessor, DefaultListableBeanFactory factory, AccessibleObject target) throws ClassNotFoundException, NoSuchMethodException {
                Field field = (Field) target;
                Class propertyType = field.getType();
                MockedBeanDescriptor descriptor = doRegisterMock(autoMockProcessor, factory, propertyType, field.getAnnotations());
                if (descriptor != null) {
                    return new MockedBeanDescriptor[]{descriptor};
                }
                return new MockedBeanDescriptor[]{};
            }
        },
        METHOD {
            @Override
            MockedBeanDescriptor[] mock(BeanAutoMockPostProcessor autoMockProcessor, DefaultListableBeanFactory factory, AccessibleObject target) throws ClassNotFoundException, NoSuchMethodException {
                Method method = (Method) target;
                if (method.getParameterTypes().length != 1) {
                    throw new IllegalArgumentException(
                            format("Setter method with single argument expected: [%s.%s]: " + method.getDeclaringClass().getName(), method.getName())
                    );
                }
                Class propertyType = method.getParameterTypes()[0];
                MockedBeanDescriptor descriptor = doRegisterMock(autoMockProcessor, factory, propertyType, method.getAnnotations());
                if (descriptor != null) {
                    return new MockedBeanDescriptor[]{descriptor};
                }
                return new MockedBeanDescriptor[]{};
            }
        },
        CONSTRUCTOR {
            @Override
            MockedBeanDescriptor[] mock(BeanAutoMockPostProcessor autoMockProcessor, DefaultListableBeanFactory factory, AccessibleObject target) throws ClassNotFoundException, NoSuchMethodException {
                Constructor constructor = (Constructor) target;
                Class[] parameters = constructor.getParameterTypes();
                List<MockedBeanDescriptor> mockedBeanDescriptors = new ArrayList<MockedBeanDescriptor>();
                Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
                int paramIdx = 0;
                for (Class typeToMock : parameters) {
                    MockedBeanDescriptor descriptor = doRegisterMock(autoMockProcessor, factory, typeToMock, parameterAnnotations[paramIdx]);
                    if (descriptor != null) {
                        mockedBeanDescriptors.add(descriptor);
                    }
                    paramIdx++;
                }
                MockedBeanDescriptor[] arr = new MockedBeanDescriptor[mockedBeanDescriptors.size()];
                return mockedBeanDescriptors.toArray(arr);
            }
        };


        /**
         * @param factory
         * @param beanTypeToMock
         * @param annotations
         *
         * @return
         * @throws ClassNotFoundException
         */
        private static MockedBeanDescriptor doRegisterMock(BeanAutoMockPostProcessor autoMockProcessor,
                                                           DefaultListableBeanFactory factory,
                                                           Class beanTypeToMock,
                                                           Annotation[] annotations) throws ClassNotFoundException, NoSuchMethodException {
            if (ApplicationEventPublisher.class.isAssignableFrom(beanTypeToMock)) {
                // Beans may choose to inject the Application context itself. (check from top of the hierarchy `ApplicationEventPublisher`)
                // Spring handles this as a special inject operation - obviously no bean definition is required here.
                // In our case the real ApplicationContext will be injected regardless whether we create mock or not (which makes no sense anyway)
                return null;
            } else if (autoMockProcessor.isFactoryBeanAvailable(factory, beanTypeToMock)) {
                log.info("LOG00370: Dependent type '{}' has available factory bean. Skip mocking ...", beanTypeToMock.getName());
                // this type has a factory bean already created, so we don't need to mock it.
                // the factory will create real instances as required.
                return null;
            }

            String beanName = getAnnotatedBeanName(annotations);
            if (beanName == null) {
                beanName = beanNameForClass(beanTypeToMock, false, factory);
            }
            if (autoMockProcessor.getForceMockedBeanName(beanTypeToMock) != null) {
                log.info("LOG00371: Dependent type '{}' is already force mocked. Skip mocking ...", beanTypeToMock.getName());
                return null;
            }
            if (!factory.isBeanNameInUse(beanName)) {
                // not yet created
                return autoMockProcessor.registerMockedBean(factory, beanName, beanTypeToMock);
            }
            return null;
        }

        /**
         * @param factory
         * @param target
         *
         * @return
         * @throws ClassNotFoundException
         */
        abstract MockedBeanDescriptor[] mock(BeanAutoMockPostProcessor autoMockProcessor,
                                             DefaultListableBeanFactory factory,
                                             AccessibleObject target) throws ClassNotFoundException, NoSuchMethodException;


        /**
         * @param annotations
         *
         * @return
         */
        private static String getAnnotatedBeanName(Annotation[] annotations) {
            String beanName = null;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == Qualifier.class) {
                    beanName = ((Qualifier) annotation).value();
                } else if (annotation.annotationType() == Resource.class) {
                    beanName = ((Resource) annotation).name();
                } else if (annotation.annotationType() == Named.class) {
                    beanName = ((Named) annotation).value();
                }
            }
            return beanName;
        }


        /**
         * Check if definition already exists for a bean of the type <code>type</code> and return its name,
         * else, create a bean name from the class name.
         *
         * @param type
         * @param isNamed
         * @param factory
         *
         * @return `
         * @throws ClassNotFoundException
         */
        private static String beanNameForClass(Class type, boolean isNamed, DefaultListableBeanFactory factory) throws ClassNotFoundException {
            // check if definition already exists for class
            String[] names = factory.getBeanNamesForType(type, true, false);
            if (names.length > 1 && !isNamed) {
                log.warn(format("More than one bean names detected for type to be mocked, [%s]", Arrays.toString(names)));
                return names[0];
            }
            if (names.length == 1) {
                return names[0];
            }

            String name = getSpringCompatibleBeanName(type);
            if (type.isInterface() && !isNamed) {
                // the target wired type is an interface: check if there are concrete implementations which aren't mocked.
                for (String defName : factory.getBeanDefinitionNames()) {
                    BeanDefinition bd = factory.getBeanDefinition(defName);
                    if (bd.getBeanClassName() != null) {
                        Class existingClass = Class.forName(bd.getBeanClassName());
                        if (type.isAssignableFrom(existingClass)) {
                            name = defName;
                            break;
                        }
                    }
                }
            }
            return name;
        }

    }

    /**
     * This is what Spring uses when name isn't supplied
     *
     * @param type
     *
     * @return
     */
    private static String getSpringCompatibleBeanName(Class type) {
        String name = type.getSimpleName(); //
        name = name.replaceFirst("^\\w", name.substring(0, 1).toLowerCase());
        return name;
    }

    /**
     * Create & register bean definition for the type to mock which uses the {@link SpringMockitoFactory} to create bean instances.
     * When the context is refreshed, the bean factory will create Mockito mocks for such bean definitions and wire them accordingly.
     *
     * @param factory
     * @param beanName
     * @param typeToMock
     */
    private MockedBeanDescriptor registerMockedBean(DefaultListableBeanFactory factory, String beanName, Class typeToMock) {
        if (isSpringInternalBean(typeToMock.getName())) {
            throw new IllegalArgumentException("Illegal attempt to create mock for internal Spring bean. Aborting!");
        }

        MockedBeanDescriptor descriptor;
        if (Properties.class.isAssignableFrom(typeToMock)) {
            // retrieve properties previously injected by `ServerConfigurationAwareContextLoaderListener`
            Properties executionProperties = SpringContextUtils.getConfiguredPropertiesFromContext(factory);
            RootBeanDefinition definition = new RootBeanDefinition(CustomPropertiesFactory.class);
            MutablePropertyValues values = new MutablePropertyValues();
            values.addPropertyValue(new PropertyValue("properties", executionProperties));
            definition.setPropertyValues(values);
            factory.registerBeanDefinition(beanName, definition);
            descriptor = new MockedBeanDescriptor(beanName, typeToMock);
        } else {
            Class internalType = typeToMock;
            // TODO :: auto-mocking EM is bit flaky, needs more thought !!
            if (internalType == EntityManager.class) {
                internalType = EntityManagerFactory.class;
                if (factory.isBeanNameInUse("entityManagerFactory")) {
                    beanName = "entityManagerFactory";
                }
                log.info("Mocking EntityManager via factory mock for {}", EntityManagerFactory.class.getSimpleName());
                if (factory.getBeansOfType(PersistenceExceptionTranslationPostProcessor.class, false, false).size() == 0) {
                    registerMockedBean(factory, "persistenceExceptionTranslationPostProcessor", PersistenceExceptionTranslationPostProcessor.class);
                }
            }
            RootBeanDefinition definition = new RootBeanDefinition(SpringMockitoFactory.class);
            MutablePropertyValues values = new MutablePropertyValues();
            values.addPropertyValue(new PropertyValue("type", internalType));
            definition.setPropertyValues(values);
            factory.registerBeanDefinition(beanName, definition);
            descriptor = new MockedBeanDescriptor(beanName, internalType);
        }
        mockedBeans.put(descriptor.getName(), descriptor.getType());
        return descriptor;
    }

    public Map<String, Class> getMockedBeans() {
        return mockedBeans;
    }


}

