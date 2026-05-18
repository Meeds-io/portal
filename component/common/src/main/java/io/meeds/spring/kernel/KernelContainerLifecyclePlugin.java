/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.spring.kernel;

import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.PropertyConfigurator;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.spi.ComponentAdapter;
import org.exoplatform.services.rest.impl.StartableApplication;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.spring.kernel.annotation.Exclude;
import io.meeds.spring.kernel.model.SpringBeanComponentAdapter;
import io.meeds.spring.kernel.model.SpringBeanProxyFactoryBean;

import jakarta.servlet.ServletContext;

public class KernelContainerLifecyclePlugin extends BaseContainerLifecyclePlugin {

  private static final Logger                                 LOG                     =
                                                                  LoggerFactory.getLogger(KernelContainerLifecyclePlugin.class);

  private static Map<String, ConfigurableListableBeanFactory> springBeanRegistries    = new ConcurrentHashMap<>();

  private static Map<String, ApplicationContext>              springContexts          = new ConcurrentHashMap<>();

  private static Map<String, Runnable>                        springContextsInitTasks = new ConcurrentHashMap<>();

  private static boolean                                      kernelAlreadyBooted     = false;

  public static void addSpringContext(String servletContextName,
                                      ApplicationContext applicationContext,
                                      ConfigurableListableBeanFactory beanFactory,
                                      Runnable springContextInitTask) {
    if (kernelAlreadyBooted) {
      LOG.warn("Adding Spring context '{}' happened too late in Server startup, spring beans will not be injected for this context",
               servletContextName);
      return;
    }
    LOG.info("Add Spring context '{}' to inject its Beans in Kernel as available components", servletContextName);
    springContexts.put(servletContextName, applicationContext);
    springBeanRegistries.put(servletContextName, beanFactory);
    if (springContextInitTask != null) {
      springContextsInitTasks.put(servletContextName, springContextInitTask);
    }
  }

  @Override
  public void initContainer(ExoContainer container) throws Exception {
    if (!(container instanceof PortalContainer portalContainer) || springContexts.isEmpty()) {
      return;
    }
    kernelAlreadyBooted = true; // NOSONAR
    if (LOG.isInfoEnabled()) {
      LOG.info("Start Spring Bean definitions of contexts [{}] integration with Kernel Container",
               StringUtils.join(springContexts.keySet(), ","));
    }
    long start = System.currentTimeMillis();

    // Delay Spring context finishing startup until the Kernel container is
    // fully started
    PortalContainer.addInitTask(portalContainer.getPortalContext(), new PortalContainerPostInitTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        finishSpringContextStartup(portalContainer);
      }
    }, "portal");

    Collection<ComponentAdapter<?>> containerComponentAdapters = portalContainer.getComponentAdapters();
    Collection<ComponentAdapter<?>> parentComponentAdapters = portalContainer.getParent()
        != null ? portalContainer.getParent().getComponentAdapters() : Collections.emptyList();
    Collection<ComponentAdapter<?>> kernelComponentAdapters = CollectionUtils.union(containerComponentAdapters,
                                                                                    parentComponentAdapters);
    Map<String, Map<String, BeanDefinition>> springBeansByContext = getBeansByServletContext();
    LOG.info("1. Add Kernel Services in all Spring contexts");
    addKernelToSpring(portalContainer, kernelComponentAdapters);
    LOG.info("2. Add Spring Beans of all contexts into Kernel");
    addSpringToKernel(portalContainer, springBeansByContext);
    LOG.info("3. Add Spring Beans into each other context");
    addSpringToEachOther(portalContainer, springBeansByContext);
    LOG.info("End spring integration with Kernel Container within {}ms", System.currentTimeMillis() - start);
  }

  public static void addKernelToSpring(PortalContainer portalContainer, Collection<ComponentAdapter<?>> kernelComponentAdapters) {
    kernelComponentAdapters.forEach(adapter -> {
      Class<?> componentKey = componentToBeanName(adapter);
      if (isIgnoreKernelComponent(portalContainer,
                                  componentKey,
                                  adapter.getComponentImplementation())) {
        return;
      }
      springContexts.forEach((servletContextName, applicationContext) -> {
        ConfigurableListableBeanFactory beanRegistry = springBeanRegistries.get(servletContextName);
        String componentKeyName = componentKey.getName();
        if (!beanRegistry.containsBeanDefinition(componentKeyName)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Add Kernel Component '{}' in Spring context [{}]",
                      componentKey.getName(),
                      servletContextName);
          }
          RootBeanDefinition beanDefinition = createBeanDefinition(componentKey, portalContainer);
          ((BeanDefinitionRegistry) beanRegistry).registerBeanDefinition(componentKeyName, beanDefinition);
        } else if (LOG.isDebugEnabled()) {
          LOG.debug("-- Ignore Adding duplicated Kernel Component '{}' in Spring contexts {}",
                    componentKey.getName(),
                    servletContextName);
        }
      });
    });
  }

  @SuppressWarnings("rawtypes")
  public static void addSpringToKernel(PortalContainer portalContainer,
                                       Map<String, Map<String, BeanDefinition>> springBeansByContext) {
    springContexts.forEach((servletContextName, applicationContext) -> {
      Map<String, BeanDefinition> beansMap = springBeansByContext.get(servletContextName);
      beansMap.forEach((beanName, beanDefinition) -> {
        Class componentKey = beanNameToComponentKey(beanDefinition.getBeanClassName(), beanName);
        ComponentAdapter<?> componentAdapter = createComponentAdapter(servletContextName,
                                                                      portalContainer,
                                                                      componentKey.getName(),
                                                                      beanDefinition.getBeanClassName(),
                                                                      () -> getBeanInstance(applicationContext,
                                                                                            beanName,
                                                                                            portalContainer.getName()));
        if (componentAdapter != null) {
          LOG.debug("Add Spring Bean '{}' from context '{}' as Kernel component '{}'",
                    beanName,
                    servletContextName,
                    componentAdapter.getComponentKey());
          portalContainer.registerComponentAdapter(componentAdapter);
        } else {
          LOG.debug("Ignore Spring bean '{}' injector to kernel, with class '{}'",
                    beanName,
                    beanDefinition.getBeanClassName());
        }
      });
    });
  }

  public static void addSpringToEachOther(PortalContainer portalContainer,
                                          Map<String, Map<String, BeanDefinition>> springBeansByContext) {
    springContexts.forEach((senderServletContextName, senderApplicationContext) -> {
      Map<String, BeanDefinition> beansMap = springBeansByContext.get(senderServletContextName);
      springBeanRegistries.entrySet()
                          .stream()
                          .filter(e -> !Strings.CS.equals(senderServletContextName, e.getKey()))
                          .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
                          .forEach((receiverServletContextName,
                                    receiverBeanRegistry) -> addSpringBeansToEachOtherContexts(portalContainer,
                                                                                               senderServletContextName,
                                                                                               receiverServletContextName,
                                                                                               senderApplicationContext,
                                                                                               receiverBeanRegistry,
                                                                                               beansMap));
    });
  }

  public static Map<String, Map<String, BeanDefinition>> getBeansByServletContext() {
    return springBeanRegistries.entrySet()
                               .stream()
                               .collect(Collectors.toMap(Entry::getKey,
                                                         e -> getEligibleBeans(e.getValue())));
  }

  /**
   * Finish booting Spring contexts switch the order or priority added in Kernel
   * addons definition
   *
   * @param portalContainer
   */
  public static void finishSpringContextStartup(PortalContainer portalContainer) {
    PortalContainerConfig portalContainerConfig = portalContainer.getComponentInstanceOfType(PortalContainerConfig.class);
    List<String> springDependencies = portalContainerConfig.getDependencies(portalContainer.getName())
                                                           .stream()
                                                           .filter(springContextsInitTasks::containsKey)
                                                           .filter(Objects::nonNull)
                                                           .toList();
    if (LOG.isInfoEnabled()) {
      LOG.info("4. Finish booting spring based contexts switch Addons Priority: [{}]",
               StringUtils.join(springDependencies, ", "));
    }
    springDependencies.stream()
                      .map(springContextsInitTasks::get)
                      .forEach(Runnable::run);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static ComponentAdapter<Object> createComponentAdapter(String servletContextName,
                                                                 PortalContainer portalContainer,
                                                                 String beanName,
                                                                 String beanClassName,
                                                                 Supplier<Object> getBeanFunction) {
    Class componentKey = beanNameToComponentKey(beanClassName, beanName);
    if (componentKey != null && portalContainer.getComponentAdapter(componentKey) == null) {
      return new SpringBeanComponentAdapter(servletContextName,
                                            beanName,
                                            componentKey,
                                            getBeanFunction);
    } else {
      return null;
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static void addSpringBeansToEachOtherContexts(PortalContainer portalContainer,
                                                        String senderServletContextName,
                                                        String receiverServletContextName,
                                                        ApplicationContext senderApplicationContext,
                                                        ConfigurableListableBeanFactory receiverBeanRegistry,
                                                        Map<String, BeanDefinition> beansMap) {
    beansMap.forEach((beanName, senderBeanDefinition) -> {
      Class<Object> beanClassName = getClass(senderBeanDefinition.getBeanClassName());
      Class beanClassNameInterface = beanNameToComponentKey(senderBeanDefinition.getBeanClassName());
      if (beanClassName != null
          && portalContainer.getComponentAdapter(senderBeanDefinition.getBeanClassName()) == null
          && !receiverBeanRegistry.containsBeanDefinition(beanName)
          && !receiverBeanRegistry.containsBeanDefinition(senderBeanDefinition.getBeanClassName())
          && !receiverBeanRegistry.containsBeanDefinition(beanClassNameInterface.getTypeName())) {
        LOG.debug("Add Spring Bean '{}' with Class '{}' Interface '{}' from Spring Context '{}' to Spring context '{}'",
                  beanName,
                  beanClassName,
                  beanClassNameInterface,
                  senderServletContextName,
                  receiverServletContextName);
        RootBeanDefinition receiverBeanDefinition = createProxyBeanDefinition(beanClassNameInterface,
                                                                              () -> getBeanInstance(senderApplicationContext,
                                                                                                    portalContainer,
                                                                                                    beanName,
                                                                                                    beanClassNameInterface,
                                                                                                    receiverServletContextName));
        ((BeanDefinitionRegistry) receiverBeanRegistry).registerBeanDefinition(beanName, receiverBeanDefinition);
      }
    });
  }

  private static RootBeanDefinition createProxyBeanDefinition(Class<?> beanClass, Supplier<?> targetSupplier) {
    if (!beanClass.isInterface() && Modifier.isFinal(beanClass.getModifiers())) {
      throw new IllegalStateException("Cannot proxy final class: " + beanClass.getName());
    }
    RootBeanDefinition bd = new RootBeanDefinition(SpringBeanProxyFactoryBean.class);
    bd.getPropertyValues().add("exposedType", beanClass);
    bd.getPropertyValues().add("targetSupplier", targetSupplier);
    bd.setSynthetic(true);
    bd.setLazyInit(true);
    bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    return bd;
  }

  private static boolean isIgnoreKernelComponent(PortalContainer portalContainer,
                                                 Class<?> componentKey,
                                                 Class<?> componentImplementation) {
    return (!componentKey.isInterface() && isComponentDuplicated(portalContainer, componentKey))
           || componentImplementation.getPackageName().startsWith("java")
           || componentImplementation.getPackageName().startsWith("jakarta")
           || componentImplementation.getPackageName().startsWith("sun.")
           || ResourceContainer.class.isAssignableFrom(componentImplementation)
           || PropertyConfigurator.class.isAssignableFrom(componentImplementation)
           || ExoContainerContext.class.isAssignableFrom(componentImplementation)
           || GenericDAO.class.isAssignableFrom(componentImplementation);
  }

  private static <T> RootBeanDefinition createBeanDefinition(Class<T> keyClass,
                                                             PortalContainer portalContainer) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(keyClass,
                                                               () -> getComponentInstance(portalContainer, keyClass));
    beanDefinition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
    beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
    beanDefinition.setAutowireCandidate(true);
    beanDefinition.setSynthetic(true);
    beanDefinition.setLazyInit(true);
    return beanDefinition;
  }

  private static Map<String, BeanDefinition> getEligibleBeans(ConfigurableListableBeanFactory beanRegistry) {
    String[] beanNames = beanRegistry.getBeanDefinitionNames();
    return Stream.of(beanNames)
                 .map(beanName -> {
                   BeanDefinition beanDefinition = beanRegistry.getBeanDefinition(beanName);
                   if (isBeanEligible(beanName, beanDefinition.getBeanClassName())) {
                     return new SimpleEntry<>(beanName, beanDefinition);
                   }
                   return null;
                 })
                 .filter(Objects::nonNull)
                 .collect(Collectors.toMap(Entry::getKey,
                                           Entry::getValue));
  }

  @SuppressWarnings("rawtypes")
  private static Class componentToBeanName(ComponentAdapter adapter) { // NOSONAR
    Object key = adapter.getComponentKey();
    Class componentKeyClass = getComponentKeyClass(key);
    if (componentKeyClass == null || !isComponentClassValid(componentKeyClass)) {
      Class componentImplementation = adapter.getComponentImplementation();
      LOG.debug("Kernel component with key '{}' class {}. Returning implementation class '{}'",
                key,
                componentKeyClass == null ? "wasn't found" : "isn't valid",
                componentImplementation == null ? null : componentImplementation.getName());
      return componentImplementation;
    } else {
      return componentKeyClass;
    }
  }

  private static Class<?> beanNameToComponentKey(String... beanClassNames) {
    Stream<String> componentKeys = Stream.of(beanClassNames);
    List<Class<Object>> componentClasses = componentKeys.filter(Objects::nonNull)
                                                        .map(KernelContainerLifecyclePlugin::getClass)
                                                        .filter(Objects::nonNull)
                                                        .filter(className -> !Objects.equals(className, Object.class))
                                                        .distinct()
                                                        .toList();
    if (componentClasses.isEmpty()
        // Direct rejection of Services in some cases
        || componentClasses.stream().anyMatch(KernelContainerLifecyclePlugin::isServiceBeanRejected)) {
      return null;
    } else {
      Class<Object> beanClassName = componentClasses.stream()
                                                    .filter(KernelContainerLifecyclePlugin::isServiceBean)
                                                    .findFirst()
                                                    .orElse(null);
      Class<?>[] componentKeyInterfaces = beanClassName == null ? null : beanClassName.getInterfaces();
      return componentKeyInterfaces == null ? beanClassName :
                                            Arrays.stream(componentKeyInterfaces)
                                                  .filter(KernelContainerLifecyclePlugin::isServiceBean)
                                                  .findFirst()
                                                  .orElse(beanClassName);
    }
  }

  private static boolean isBeanEligible(String... beanClassNames) {
    Stream<String> componentKeys = Stream.of(beanClassNames);
    List<Class<Object>> componentClasses = componentKeys.filter(Objects::nonNull)
                                                        .map(KernelContainerLifecyclePlugin::getClass)
                                                        .filter(Objects::nonNull)
                                                        .distinct()
                                                        .toList();
    return !componentClasses.isEmpty()
           && componentClasses.stream().noneMatch(KernelContainerLifecyclePlugin::isServiceBeanRejected)
           && componentClasses.stream().anyMatch(KernelContainerLifecyclePlugin::isServiceBean);
  }

  private static boolean isServiceBeanRejected(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Exclude.class)
           || beanClass.isAnnotationPresent(Configuration.class)
           || beanClass.isAnnotationPresent(SpringBootApplication.class);
  }

  private static boolean isServiceBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Service.class)
           && !Strings.CS.contains(beanClass.getName(), "org.springframework")
           && !Strings.CS.startsWith(beanClass.getName(), "java")
           && !Strings.CS.startsWith(beanClass.getName(), "jdk")
           && !Strings.CS.contains(beanClass.getName(), "$");
  }

  private static boolean isComponentClassValid(Class<?> componentClass) {
    return !componentClass.equals(Object.class)
           && !componentClass.isAssignableFrom(PropertyConfigurator.class)
           && !componentClass.equals(Startable.class)
           && !componentClass.equals(StartableApplication.class)
           && !componentClass.equals(ResourceContainer.class)
           && !Strings.CS.startsWith(componentClass.getName(), "jakarta")
           && !Strings.CS.equals(componentClass.getName(), "org.exoplatform.commons.cluster.StartableClusterAware");
  }

  private static Object getBeanInstance(ApplicationContext applicationContext,
                                        PortalContainer portalContainer,
                                        String beanName,
                                        Class<Object> beanClassName,
                                        String contextName) {
    boolean beanExistsInKernel = portalContainer.getComponentAdapter(beanClassName) != null;
    if (beanExistsInKernel) {
      LOG.trace("Retrieve Bean with name '{}' from Kernel Container using class '{}' in detstination to '{}'",
                beanName,
                beanClassName,
                contextName);
      return portalContainer.getComponentInstanceOfType(beanClassName);
    } else {
      return getBeanInstance(applicationContext, beanName, contextName);
    }
  }

  private static Object getBeanInstance(ApplicationContext applicationContext, String beanName, String contextName) {
    LOG.trace("Retrieve Bean with name '{}' from Spring to Kernel using Application context '{}' in detstination to '{}'",
              beanName,
              applicationContext.getApplicationName(),
              contextName);
    return applicationContext.getBean(beanName);
  }

  private static <T> T getComponentInstance(PortalContainer portalContainer, Class<T> keyClass) {
    LOG.trace("Getting Kernel Service '{}' requested by a Spring Bean.",
              keyClass);
    try {
      T instance = portalContainer.getComponentInstanceOfType(keyClass);
      if (instance == null) {
        LOG.trace("Kernel Service '{}' requested by a Spring Bean wasn't",
                  keyClass);
      }
      return instance;
    } catch (Exception e) {
      LOG.warn("Kernel Service '{}' requested by a Spring Bean wasn't found",
               keyClass,
               e);
      return null;
    }
  }

  private static Class<?> getComponentKeyClass(Object key) {
    if (key instanceof Class<?> keyClass) { // NOSONAR
      return keyClass;
    } else if (key instanceof String keyString) {
      return getClass(keyString);
    } else {
      LOG.warn("Ignoring Kernel component with key '{}' which isn't of type *Class* neither *String*.", key);
      return null;
    }
  }

  private static boolean isComponentDuplicated(PortalContainer portalContainer, Class<?> componentKey) {
    Class<?>[] interfaces = componentKey.getInterfaces();
    return ArrayUtils.isNotEmpty(interfaces)
           && Arrays.stream(interfaces)
                    .anyMatch(interfaceClass -> {
                      boolean componentIsDuplicated = portalContainer.getComponentAdapter(interfaceClass) != null;
                      if (componentIsDuplicated) {
                        LOG.debug("Ignore Kernel Component '{}' as it's already injected in Spring context with class '{}'",
                                  componentKey,
                                  interfaceClass);
                      }
                      return componentIsDuplicated;
                    });
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> getClass(String beanClassName) {
    try {
      // Must be found in shared library
      return (Class<T>) KernelContainerLifecyclePlugin.class.getClassLoader().loadClass(beanClassName);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

}
