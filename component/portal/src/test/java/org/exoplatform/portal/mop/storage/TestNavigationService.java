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
import java.util.Iterator;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.Node;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test.navigation.configuration.xml") })
public class TestNavigationService extends AbstractKernelTest {
  
    /** . */
    protected NavigationService  service;

    private SiteStorage modelStorage;

    protected void setUp() throws Exception {
        super.setUp();
        PortalContainer container = PortalContainer.getInstance();
        this.service = container.getComponentInstanceOfType(NavigationService.class);
        this.modelStorage = container.getComponentInstanceOfType(SiteStorage.class);
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
                                           9,
                                           0);
        this.modelStorage.create(portal);

        NavigationContext nav = new NavigationContext(type.key(siteName), new NavigationState(1));
        this.service.saveNavigation(nav);
        restartTransaction();
    }

    public void testHiddenNode() throws Exception {
        this.createSite(SiteType.PORTAL, "hidden_node");

        NavigationContext nav = this.service.loadNavigation(SiteKey.portal("hidden_node"));
        NodeContext node = this.service.loadNode(Node.MODEL, nav, Scope.ALL, null);
        node.add(0, "a");
        node.add(1, "b");
        node.add(2, "c");
        this.service.saveNode(node, null);

        //
        restartTransaction();

        //
        nav = service.loadNavigation(SiteKey.portal("hidden_node"));

        //
        Node root;
        Node a;
        Node b;
        Node c;

        //
        root = service.loadNode(Node.MODEL, nav, Scope.GRANDCHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        a.setHidden(true);
        assertEquals(2, root.getChildren().size());
        assertNull(root.getChild("a"));
        assertEquals("b", root.getChild(0).getName());
        try {
            root.getChild(2);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }
        assertFalse(root.removeChild("a"));
        try {
            b.setName("a");
            fail();
        } catch (IllegalArgumentException ignore) {
        }

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        assertSame(a, root.getChild(0));
        assertSame(c, root.getChild(1));
        try {
            root.getChild(2);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        a.setHidden(true);
        c.setHidden(true);
        assertSame(b, root.getChild(0));
        try {
            root.getChild(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testHiddenInsert1() throws Exception {
        this.createSite(SiteType.PORTAL, "hidden_insert_1");
        NavigationContext defaultNav = service.loadNavigation(SiteKey.portal("hidden_insert_1"));
        NodeContext node = this.service.loadNode(Node.MODEL, defaultNav, Scope.ALL, null);
        node.add(0, "a");
        this.service.saveNode(node, null);

        //
        restartTransaction();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_1"));

        //
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        Node a = root.getChild("a");
        a.setHidden(true);
        Node b = root.addChild("b");
        assertEquals(1, root.getChildren().size());
        assertSame(b, root.getChildren().iterator().next());
        a.setHidden(false);
        assertEquals(2, root.getChildren().size());
        Iterator<Node> it = root.getChildren().iterator();
        assertSame(b, it.next());
        assertSame(a, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        a.setHidden(true);
        b = root.addChild(0, "b");
        assertEquals(1, root.getChildren().size());
        assertSame(b, root.getChildren().iterator().next());
        a.setHidden(false);
        assertEquals(2, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(b, it.next());
        assertSame(a, it.next());
    }

    public void testHiddenInsert2() throws Exception {
        this.createSite(SiteType.PORTAL, "hidden_insert_2");
        NavigationContext defaultNav = service.loadNavigation(SiteKey.portal("hidden_insert_2"));
        NodeContext node = this.service.loadNode(Node.MODEL, defaultNav, Scope.ALL, null);
        node.add(0, "a");
        node.add(1, "b");
        this.service.saveNode(node, null);

        //
        restartTransaction();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_2"));

        //
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        Node a = root.getChild("a");
        Node b = root.getChild("b");
        b.setHidden(true);
        Node c = root.addChild(0, "c");
        assertEquals(2, root.getChildren().size());
        Iterator<Node> it = root.getChildren().iterator();
        assertSame(c, it.next());
        assertSame(a, it.next());
        b.setHidden(false);
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(c, it.next());
        assertSame(a, it.next());
        assertSame(b, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        b.setHidden(true);
        c = root.addChild(1, "c");
        assertEquals(2, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(b, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        b.setHidden(true);
        c = root.addChild("c");
        assertEquals(2, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(b, it.next());
    }

    public void testHiddenInsert3() throws Exception {

        this.createSite(SiteType.PORTAL, "hidden_insert_3");
        NavigationContext defaultNav = service.loadNavigation(SiteKey.portal("hidden_insert_3"));
        NodeContext node = this.service.loadNode(Node.MODEL, defaultNav, Scope.ALL, null);
        node.add(0, "a");
        node.add(1, "b");
        node.add(2, "c");
        this.service.saveNode(node, null);

        //
        restartTransaction();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_3"));

        //
        Node root, a, b, c, d;
        Iterator<Node> it;

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild(0, "d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(d, it.next());
        assertSame(a, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(d, it.next());
        assertSame(a, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild(1, "d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(d, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(d, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild(2, "d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild("d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());
    }

    public void testCount() throws Exception {
        this.createSite(SiteType.PORTAL, "count");

        //
        restartTransaction();

        //
        NavigationContext navigation = service.loadNavigation(SiteKey.portal("count"));
        Node root;

        //
        root = service.loadNode(Node.MODEL, navigation, Scope.SINGLE, null).getNode();
        assertEquals(0, root.getNodeCount());
        // assertEquals(-1, root.getSize());

        //
        root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
        assertEquals(0, root.getNodeCount());
        assertEquals(0, root.getSize());
        Node a = root.addChild("a");
        assertEquals(1, root.getNodeCount());
        assertEquals(1, root.getSize());
        a.setHidden(true);
        assertEquals(0, root.getNodeCount());
        assertEquals(1, root.getSize());
    }

    public void testInsertDuplicate() {
        try {
            this.createSite(SiteType.PORTAL, "insert_duplicate");

            NavigationContext defaultNav = service.loadNavigation(SiteKey.portal("insert_duplicate"));
            NodeContext node = this.service.loadNode(Node.MODEL, defaultNav, Scope.ALL, null);
            node.add(0, "a");
            this.service.saveNode(node, null);

        } catch (Exception ex) {
            fail(ex);
        }

        //
        restartTransaction();

        //
        NavigationContext navigation = service.loadNavigation(SiteKey.portal("insert_duplicate"));
        Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
        try {
            root.addChild("a");
            fail("Exception should be thrown due to the duplicate insert");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testLoadNodeById() throws Exception {
        this.createSite(SiteType.PORTAL, "test_site");
        NavigationContext defaultNav = service.loadNavigation(SiteKey.portal("test_site"));
        NodeContext<Node> rootNode = this.service.loadNode(Node.MODEL, defaultNav, Scope.ALL, null);
        rootNode.add(0, "child1").getNode();
        rootNode.add(1, "child2").getNode();
        this.service.saveNode(rootNode, null);

        NodeContext<Node> node = service.loadNodeById(Node.MODEL, rootNode.getId(), Scope.ALL, null);
        assertNotNull(node.getNode());
        assertEquals(2, node.getNode().getChildren().size());
    }

}
