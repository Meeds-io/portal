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
package org.exoplatform.portal.pc;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.pc.aspects.PortletLifecyclePhaseInterceptor;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.federation.FederatingPortletInvoker;
import org.gatein.pc.federation.impl.FederatingPortletInvokerService;
import org.gatein.pc.portlet.PortletInvokerInterceptor;
import org.gatein.pc.portlet.aspects.CCPPInterceptor;
import org.gatein.pc.portlet.aspects.ConsumerCacheInterceptor;
import org.gatein.pc.portlet.aspects.ContextDispatcherInterceptor;
import org.gatein.pc.portlet.aspects.EventPayloadInterceptor;
import org.gatein.pc.portlet.aspects.PortletCustomizationInterceptor;
import org.gatein.pc.portlet.aspects.ProducerCacheInterceptor;
import org.gatein.pc.portlet.aspects.RequestAttributeConversationInterceptor;
import org.gatein.pc.portlet.aspects.SecureTransportInterceptor;
import org.gatein.pc.portlet.aspects.SessionInvalidatorInterceptor;
import org.gatein.pc.portlet.aspects.ValveInterceptor;
import org.gatein.pc.portlet.container.ContainerPortletDispatcher;
import org.gatein.pc.portlet.container.ContainerPortletInvoker;
import org.gatein.pc.portlet.impl.deployment.DeploymentException;
import org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployer;
import org.gatein.pc.portlet.impl.state.StateManagementPolicyService;
import org.gatein.pc.portlet.impl.state.producer.PortletStatePersistenceManagerService;
import org.gatein.pc.portlet.state.StateConverter;
import org.gatein.pc.portlet.state.producer.ProducerPortletInvoker;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebAppEvent;
import org.gatein.wci.WebAppLifeCycleEvent;
import org.gatein.wci.WebAppListener;
import org.picocontainer.Startable;

public class ExoKernelIntegration implements Startable, WebAppListener {

    /** . */
    protected PortletApplicationDeployer portletApplicationRegistry;

    /** Exo Context */
    private final ExoContainer container;

    /** DO NOT REMOVE ME, OTHERWISE YOU'LL BREAK THINGS. */
    @SuppressWarnings("unused")
    private final ResourceBundleService resourceBundleService;

    /**
     * Indicates whether or not the producerPortletInvoker has been registered from this class
     */
    private boolean producerPortletInvokerSet;

    /** . */
    private Log                        log = ExoLogger.getLogger(ExoKernelIntegration.class);

    /**
     * We enforce the dependency with the ResourceBundleService since it must be stared before the
     * <code>portletApplicationRegistry</code>
     *
     * @param context the exo container context
     * @param resourceBundleService the resource bundle service that is here for the sake of creating a dependency
     */
    public ExoKernelIntegration(ExoContainerContext context, ResourceBundleService resourceBundleService) {
        this.container = context.getContainer();
        this.resourceBundleService = resourceBundleService;
    }

    public void start() {

        // The portlet container invoker used by producer to dispatch to portlets
        ContainerPortletInvoker containerPortletInvoker = new ContainerPortletInvoker();

        // The portlet application deployer
        portletApplicationRegistry = new ExoPortletApplicationDeployer();
        portletApplicationRegistry.setContainerPortletInvoker(containerPortletInvoker);

        // activate schema validation for portlet.xml if needed
        String validation = PropertyManager.getProperty("gatein.portlet.validation");
        boolean validated = validation == null || "true".equals(validation.trim().toLowerCase());
        log.debug("portlet xml validation is " + (validated ? "enabled" : " disabled"));
        portletApplicationRegistry.setSchemaValidated(validated);

        // Container Stack
        ContainerPortletDispatcher portletContainerDispatcher = new ContainerPortletDispatcher();

        FederatingPortletInvoker federatingPortletInvoker = new FederatingPortletInvokerService();

        EventPayloadInterceptor eventPayloadInterceptor = new EventPayloadInterceptor();
        eventPayloadInterceptor.setNext(portletContainerDispatcher);
        RequestAttributeConversationInterceptor requestAttributeConversationInterceptor = new RequestAttributeConversationInterceptor();
        requestAttributeConversationInterceptor.setNext(eventPayloadInterceptor);
        CCPPInterceptor ccppInterceptor = new CCPPInterceptor();
        ccppInterceptor.setNext(requestAttributeConversationInterceptor);
        ProducerCacheInterceptor producerCacheInterceptor = new ProducerCacheInterceptor();
        producerCacheInterceptor.setNext(ccppInterceptor);
        SessionInvalidatorInterceptor sessionInvalidatorInterceptor = new SessionInvalidatorInterceptor();
        sessionInvalidatorInterceptor.setNext(producerCacheInterceptor);
        ContextDispatcherInterceptor contextDispatcherInterceptor = new ContextDispatcherInterceptor();
        contextDispatcherInterceptor.setNext(sessionInvalidatorInterceptor);
        PortletLifecyclePhaseInterceptor portletLifecyclePhaseInterceptor = new PortletLifecyclePhaseInterceptor();
        portletLifecyclePhaseInterceptor.setNext(contextDispatcherInterceptor);
        SecureTransportInterceptor secureTransportInterceptor = new SecureTransportInterceptor();
        secureTransportInterceptor.setNext(portletLifecyclePhaseInterceptor);
        ValveInterceptor valveInterceptor = new ValveInterceptor();
        valveInterceptor.setPortletApplicationRegistry(portletApplicationRegistry);
        valveInterceptor.setNext(secureTransportInterceptor);

        // The portlet container invoker continued
        containerPortletInvoker.setNext(valveInterceptor);

        // register container invoker so that WSRP can use it, WSRP uses its own ProducerPortletInvoker
        container.registerComponentInstance(ContainerPortletInvoker.class, containerPortletInvoker);

        // The producer persistence manager
        PortletStatePersistenceManagerService producerPersistenceManager = new PortletStatePersistenceManagerService();

        // The producer state management policy
        StateManagementPolicyService producerStateManagementPolicy = new StateManagementPolicyService();
        producerStateManagementPolicy.setPersistLocally(false);

        // The producer state converter
        StateConverter producerStateConverter = new ExoStateConverter();

        // The producer portlet invoker
        ProducerPortletInvoker producerPortletInvoker = new ProducerPortletInvoker();
        producerPortletInvoker.setNext(containerPortletInvoker);
        producerPortletInvoker.setPersistenceManager(producerPersistenceManager);
        producerPortletInvoker.setStateManagementPolicy(producerStateManagementPolicy);
        producerPortletInvoker.setStateConverter(producerStateConverter);

        // register the producer portlet invoker if it hasn't been already
        if (!federatingPortletInvoker.isResolved(PortletInvoker.LOCAL_PORTLET_INVOKER_ID)) {
            federatingPortletInvoker.registerInvoker(PortletInvoker.LOCAL_PORTLET_INVOKER_ID, producerPortletInvoker);
            producerPortletInvokerSet = true;
        }

        // The consumer portlet invoker
        PortletCustomizationInterceptor portletCustomizationInterceptor = new PortletCustomizationInterceptor();
        portletCustomizationInterceptor.setNext(federatingPortletInvoker);
        ConsumerCacheInterceptor consumerCacheInterceptor = new ConsumerCacheInterceptor();
        consumerCacheInterceptor.setNext(portletCustomizationInterceptor);
        PortletInvokerInterceptor consumerPortletInvoker = new PortletInvokerInterceptor();
        consumerPortletInvoker.setNext(consumerCacheInterceptor);

        // register federating portlet and consumerPortletInvoker invoker with container
        container.registerComponentInstance(PortletInvoker.class, consumerPortletInvoker);
        container.registerComponentInstance(FederatingPortletInvoker.class, federatingPortletInvoker);

        //
        ServletContainerFactory.getServletContainer().addWebAppListener(this);
    }

    public void stop() {
        ServletContainerFactory.getServletContainer().removeWebAppListener(this);
        if (!producerPortletInvokerSet || !PropertyManager.isDevelopping()) {
           // The current instance did not register the producerPortletInvoker so we have nothing to do or we are not in
           // developing mode
           return;
        }
        final ExoContainer topContainer = ExoContainerContext.getTopContainer();
        FederatingPortletInvoker federatingPortletInvoker = (FederatingPortletInvoker) topContainer
                .getComponentInstanceOfType(FederatingPortletInvoker.class);
        if (federatingPortletInvoker != null && federatingPortletInvoker.isResolved(PortletInvoker.LOCAL_PORTLET_INVOKER_ID)) {
           // The current instance registered the producerPortletInvoker so we have to unregister it
           federatingPortletInvoker.unregisterInvoker(PortletInvoker.LOCAL_PORTLET_INVOKER_ID);
        }
    }

    public PortletApplicationDeployer getPortletApplicationRegistry() {
        return portletApplicationRegistry;
    }

    public void onEvent(WebAppEvent event) {
        if (event instanceof WebAppLifeCycleEvent) {
            WebAppLifeCycleEvent lifeCycleEvent = (WebAppLifeCycleEvent) event;
            int type = lifeCycleEvent.getType();
            if (type == WebAppLifeCycleEvent.ADDED) {
                try {
                    portletApplicationRegistry.add(lifeCycleEvent.getWebApp().getServletContext());
                } catch (DeploymentException e) {
                    log.error("Portlet deployment failed", e);
                }
            } else if (type == WebAppLifeCycleEvent.REMOVED) {
                portletApplicationRegistry.remove(lifeCycleEvent.getWebApp().getServletContext());
            }
        }
    }
}
