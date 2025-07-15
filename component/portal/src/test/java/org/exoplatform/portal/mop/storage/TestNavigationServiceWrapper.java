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
package org.exoplatform.portal.mop.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.Node;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml")})
public class TestNavigationServiceWrapper extends AbstractKernelTest {

    /** . */
    private NavigationService navigationService;

    private SiteStorage modelStorage;

    /** . */
    private ListenerService listenerService;


    @Override
    protected void setUp() throws Exception {
        PortalContainer container = getContainer();

        //
        listenerService = container.getComponentInstanceOfType(ListenerService.class);
        navigationService = container.getComponentInstanceOfType(NavigationService.class);
        modelStorage = container.getComponentInstanceOfType(SiteStorage.class);

        //
        super.setUp();
        begin();
    }

    protected void createSite(SiteType type, String siteName) throws Exception {
      ContainerData container = new ContainerData(null,
                                                  "testcontainer_" + siteName,
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  "",
                                                  null,
                                                  null,
                                                  Collections.emptyList(),
                                                  Collections.emptyList());
      PortalData portal = new PortalData(null,
                                         siteName,
                                         type.getName(),
                                         null,
                                         null,
                                         null,
                                         new ArrayList<>(),
                                         null,
                                         null,
                                         null,
                                         container,
                                         true,
                                         88,
                                         0);
      this.modelStorage.create(portal);
  
      NavigationContext nav = new NavigationContext(type.key(siteName), new NavigationState(1));
      this.navigationService.saveNavigation(nav);
  
      restartTransaction();
    }

    protected void createNavigation(SiteType siteType, String siteName) throws Exception {
        createSite(siteType, siteName);
        navigationService.saveNavigation(new NavigationContext(new SiteKey(siteType, siteName), new NavigationState(1)));

        restartTransaction();
    }

    public void testNotification() throws Exception {
        class ListenerImpl extends Listener<NavigationService, SiteKey> {

            /** . */
            private final LinkedList<Event> events = new LinkedList<Event>();

            @Override
            public void onEvent(Event event) throws Exception {
                events.addLast(event);
            }
        }

        //
        ListenerImpl createListener = new ListenerImpl();
        ListenerImpl updateListener = new ListenerImpl();
        ListenerImpl destroyListener = new ListenerImpl();

        //
        listenerService.addListener(EventType.NAVIGATION_CREATED, createListener);
        listenerService.addListener(EventType.NAVIGATION_UPDATED, updateListener);
        listenerService.addListener(EventType.NAVIGATION_DESTROYED, destroyListener);

        // Create
        createSite(SiteType.PORTAL, "notification");

        assertEquals(0, updateListener.events.size());
        assertEquals(0, destroyListener.events.size());
        assertEquals(1, createListener.events.size());
        Event event = createListener.events.removeFirst();
        assertEquals(SiteKey.portal("notification"), event.getData());
        assertEquals(EventType.NAVIGATION_CREATED, event.getEventName());

        // Update
        restartTransaction();
        NavigationContext navigation = new NavigationContext(SiteKey.portal("notification"), new NavigationState(3));
        navigationService.saveNavigation(navigation);
        assertSame(navigationService, event.getSource());
        assertEquals(0, createListener.events.size());
        assertEquals(1, updateListener.events.size());
        assertEquals(0, destroyListener.events.size());
        event = updateListener.events.removeFirst();
        assertEquals(SiteKey.portal("notification"), event.getData());
        assertEquals(EventType.NAVIGATION_UPDATED, event.getEventName());
        assertSame(navigationService, event.getSource());

        // Update
        navigation = navigationService.loadNavigation(SiteKey.portal("notification"));
        Node root = navigationService.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
        root.setState(new NodeState.Builder(root.getState()).label("foo").build());
        navigationService.saveNode(root.getContext(), null);
        assertEquals(0, createListener.events.size());
        assertEquals(0, updateListener.events.size());
        assertEquals(0, destroyListener.events.size());

        // Destroy
        navigationService.destroyNavigation(navigation);
        assertEquals(0, createListener.events.size());
        assertEquals(0, updateListener.events.size());
        assertEquals(1, destroyListener.events.size());
        event = destroyListener.events.removeFirst();
        assertEquals(SiteKey.portal("notification"), event.getData());
        assertEquals(EventType.NAVIGATION_DESTROYED, event.getEventName());
        assertSame(navigationService, event.getSource());
    }

    public void testCacheInvalidation() throws Exception {
        SiteKey key = SiteKey.portal("wrapper_cache_invalidation");

        //
        createNavigation(SiteType.PORTAL, "wrapper_cache_invalidation");
        restartTransaction();
        navigationService.saveNavigation(new NavigationContext(key, new NavigationState(0)));
        restartTransaction();

        //
        NavigationContext nav = navigationService.loadNavigation(key);
        assertNotNull(nav);
        NodeContext<Node> root = navigationService.loadNode(Node.MODEL, nav, Scope.ALL, null);
        assertNotNull(root);
    }

    public void testCachingInMultiThreading() throws Exception {
        final SiteKey foo = SiteKey.portal("test_caching_in_multi_threading");
        assertNull(navigationService.loadNavigation(foo));
        createSite(SiteType.PORTAL, "test_caching_in_multi_threading");

        navigationService.saveNavigation(new NavigationContext(foo, new NavigationState(0)));

        // Start a new thread to work with navigations in parallels
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            ExoContainerContext.setCurrentContainer(getContainer());
            begin();
            try {
              // Loading the foo navigation and update into the cache if any
              assertNotNull(navigationService.loadNavigation(foo));
            } finally {
              end();
            }
          }
        });
        t.start();
        t.join();

        assertNotNull(navigationService.loadNavigation(foo));

        restartTransaction();

        assertNotNull(navigationService.loadNavigation(foo));
    }
}
