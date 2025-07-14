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
import org.exoplatform.portal.mop.NodeTarget;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.HierarchyError;
import org.exoplatform.portal.mop.navigation.HierarchyException;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.Node;
import org.exoplatform.portal.mop.navigation.NodeChange;
import org.exoplatform.portal.mop.navigation.NodeChangeQueue;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml") })
public class TestNavigationServiceUpdate extends AbstractKernelTest {

  /** . */
  protected NavigationService  service;

  private SiteStorage siteStorage;

  protected void setUp() throws Exception {
    begin();
    this.service = getContainer().getComponentInstanceOfType(NavigationService.class);
    this.siteStorage = getContainer().getComponentInstanceOfType(SiteStorage.class);
  }

  @Override
  protected void tearDown() throws Exception {
    end();
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
                                       5,
                                       0);
    this.siteStorage.create(portal);

    restartTransaction();
  }

  protected void createNavigation(SiteType siteType, String siteName) throws Exception {
    createSite(siteType, siteName);
    service.saveNavigation(new NavigationContext(new SiteKey(siteType, siteName), new NavigationState(1)));
  }

  public void testNoop() throws Exception {
    createNavigation(SiteType.PORTAL, "update_no_op");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_no_op"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    rootContext1.add(null, "c");
    rootContext1.add(null, "d");
    service.saveNode(rootContext1, null);

    //
    restartTransaction();

    NodeContext<Node> root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    Iterator<NodeChange<Node>> it = root.getNode().update(service, null);
    assertFalse(it.hasNext());
  }

  public void testHasChanges() throws Exception {
    createNavigation(SiteType.PORTAL, "update_cannot_save");
    //
    restartTransaction();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_cannot_save"));
    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();

    //
    assertFalse(root.getContext().hasChanges());
    root.addChild("foo");
    assertTrue(root.getContext().hasChanges());

    //
    try {
      root.update(service, null);
    } catch (IllegalArgumentException expected) {
    }

    //
    assertTrue(root.getContext().hasChanges());
    service.saveNode(root.getContext(), null);
    assertFalse(root.getContext().hasChanges());

    //
    Iterator<NodeChange<Node>> it = root.update(service, null);
    assertFalse(it.hasNext());
  }

  public void testAddFirst() throws Exception {
    createNavigation(SiteType.PORTAL, "update_add_first");

    //
    restartTransaction();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_add_first"));
    NodeContext<Node> root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    assertEquals(0, root1.getNodeSize());
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("a");
    service.saveNode(root2.getContext(), null);

    //
    restartTransaction();

    //
    root1.getNode().update(service, null);
    assertEquals(1, root1.getNodeSize());
    Node a = root1.getNode(0);
    assertEquals("a", a.getName());

  }

  public void testAddSecond() throws Exception {
    createNavigation(SiteType.PORTAL, "update_add_second");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_add_second"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    restartTransaction();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a = root1.getChild("a");
    assertEquals(1, root1.getSize());
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("b");
    service.saveNode(root2.getContext(), null);

    //
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = root1.update(service, null);
    NodeChange.Added<Node> added = (NodeChange.Added<Node>) changes.next();
    assertSame(root1, added.getParent());
    assertSame(root1.getChild("b"), added.getTarget());
    assertSame(a, added.getPrevious());
    assertFalse(changes.hasNext());
    assertEquals(2, root1.getSize());
    assertEquals("a", root1.getChild(0).getName());
    assertEquals("b", root1.getChild(1).getName());
  }

  public void testRemove() throws Exception {
    createNavigation(SiteType.PORTAL, "update_remove");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_remove"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    restartTransaction();

    NodeContext<Node> root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    assertEquals(1, root1.getNodeSize());
    Node a = root1.getNode("a");
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = root1.getNode().update(service, null);
    NodeChange.Removed<Node> removed = (NodeChange.Removed<Node>) changes.next();
    assertSame(root1.getNode(), removed.getParent());
    assertSame(a, removed.getTarget());
    assertFalse(changes.hasNext());
    assertEquals(0, root1.getNodeSize());
  }

  public void testMove() throws Exception {
    createNavigation(SiteType.PORTAL, "update_move");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_move"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    restartTransaction();

    NodeContext<Node> root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    assertEquals(2, root1.getNodeSize());
    Node a = root1.getNode("a");
    Node b = a.getChild("b");
    Node c = root1.getNode("c");
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.getChild("c").addChild(root2.getChild("a").getChild("b"));
    service.saveNode(root2.getContext(), null);

    //
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = root1.getNode().update(service, null);
    NodeChange.Moved<Node> moved = (NodeChange.Moved<Node>) changes.next();
    assertSame(a, moved.getFrom());
    assertSame(c, moved.getTo());
    assertSame(b, moved.getTarget());
    assertSame(null, moved.getPrevious());
    assertFalse(changes.hasNext());
    assertEquals(0, root1.getNode("a").getSize());
    assertEquals(1, root1.getNode("c").getSize());
  }

  public void testAddWithSameName() throws Exception {
    createNavigation(SiteType.PORTAL, "update_add_with_same_name");

    //
    restartTransaction();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_add_with_same_name"));
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.addChild("a").addChild("b");
    root1.addChild("c");
    service.saveNode(root1.getContext(), null);

    //
    restartTransaction();

    //
    root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a = root1.getChild("a");
    Node b = a.getChild("b");
    Node c = root1.getChild("c");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.getChild("c").addChild(root2.getChild("a").getChild("b"));
    Node b2 = root2.getChild("a").addChild("b");
    service.saveNode(root2.getContext(), null);

    //
    assertSame(a, root1.getChild("a"));
    assertSame(c, root1.getChild("c"));
  }

  public void testComplex() throws Exception {
    createNavigation(SiteType.PORTAL, "update_complex");

    //
    restartTransaction();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_complex"));
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a1 = root1.addChild("a");
    a1.addChild("c");
    a1.addChild("d");
    a1.addChild("e");
    Node b1 = root1.addChild("b");
    b1.addChild("f");
    b1.addChild("g");
    b1.addChild("h");
    service.saveNode(root1.getContext(), null);

    //
    restartTransaction();

    //
    root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    a1 = root1.getChild("a");
    Node c1 = a1.getChild("c");
    Node d1 = a1.getChild("d");
    Node e1 = a1.getChild("e");
    b1 = root1.getChild("b");
    Node f1 = b1.getChild("f");
    Node g1 = b1.getChild("g");
    Node h1 = b1.getChild("h");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a2 = root2.getChild("a");
    a2.removeChild("e");
    Node b2 = root2.getChild("b");
    b2.addChild(2, a2.getChild("d"));
    a2.addChild(1, "d");
    b2.removeChild("g");
    service.saveNode(root2.getContext(), null);

    //
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = root1.update(service, null);
    NodeChange.Added<Node> added = (NodeChange.Added<Node>) changes.next();
    assertSame(a1, added.getParent());
    assertEquals("d", added.getTarget().getName());
    assertSame(c1, added.getPrevious());
    NodeChange.Removed<Node> removed1 = (NodeChange.Removed<Node>) changes.next();
    assertSame(a1, removed1.getParent());
    assertSame(e1, removed1.getTarget());
    NodeChange.Moved<Node> moved = (NodeChange.Moved<Node>) changes.next();
    assertSame(a1, moved.getFrom());
    assertSame(b1, moved.getTo());
    assertSame(d1, moved.getTarget());
    assertSame(f1, moved.getPrevious());
    NodeChange.Removed<Node> removed2 = (NodeChange.Removed<Node>) changes.next();
    assertSame(b1, removed2.getParent());
    assertSame(g1, removed2.getTarget());
    assertFalse(changes.hasNext());

    //
    assertSame(a1, root1.getChild("a"));
    assertSame(b1, root1.getChild("b"));
    assertEquals(2, a1.getSize());
    assertSame(c1, a1.getChild(0));
    assertNotNull(a1.getChild(1));
    assertEquals("d", a1.getChild(1).getName());
    assertFalse(d1.getId().equals(a1.getChild(1).getId()));
    assertEquals(3, b1.getSize());
    assertSame(f1, b1.getChild(0));
    assertSame(d1, b1.getChild(1));
    assertSame(h1, b1.getChild(2));
  }

  public void testReplaceChild() throws Exception {
    createNavigation(SiteType.PORTAL, "update_replace_child");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_replace_child"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    String foo1Id = root1.getChild("foo").getId();

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    root2.removeChild("foo");
    Node foo = root2.addChild("foo");
    foo.setState(new NodeState.Builder().label("foo2").build());
    service.saveNode(root2.getContext(), null);
    String foo2Id = foo.getId();
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = root1.update(service, null);
    NodeChange.Added<Node> added = (NodeChange.Added<Node>) changes.next();
    assertEquals(foo2Id, added.getTarget().getId());
    NodeChange.Removed<Node> removed = (NodeChange.Removed<Node>) changes.next();
    assertEquals(foo1Id, removed.getTarget().getId());
    assertFalse(changes.hasNext());

    //
    foo = root1.getChild("foo");
    assertEquals(foo2Id, foo.getId());
    assertEquals("foo2", root1.getChild("foo").getState().getLabel());
  }

  public void testRename() throws Exception {
    createNavigation(SiteType.PORTAL, "update_rename");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_rename"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    root2.getChild("foo").setName("bar");
    service.saveNode(root2.getContext(), null);
    restartTransaction();

    //
    Iterator<NodeChange<Node>> it = root1.update(service, null);
    Node bar = root1.getChild(0);
    assertEquals("bar", bar.getName());
    NodeChange.Renamed<Node> renamed = (NodeChange.Renamed<Node>) it.next();
    assertEquals("bar", renamed.getName());
    assertSame(bar, renamed.getTarget());
  }

  public void testState() throws Exception {
    createNavigation(SiteType.PORTAL, "update_state");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_state"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo").add(null, "bar");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node root3 = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();

    //
    Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    root.getChild("foo").setState(new NodeState.Builder().label("foo").build());
    service.saveNode(root.getContext(), null);
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = root1.update(service, Scope.GRANDCHILDREN);
    Node foo = root1.getChild("foo");
    assertEquals("foo", foo.getState().getLabel());
    NodeChange.Added<Node> added = (NodeChange.Added<Node>) changes.next();
    assertEquals("bar", added.getTarget().getName());
    NodeChange.Updated<Node> updated = (NodeChange.Updated<Node>) changes.next();
    assertSame(foo, updated.getTarget());
    assertNotSame(0, updated.getState().getUpdatedDate());
    assertEquals(new NodeState.Builder().label("foo").target(NodeTarget.SAME_TAB.name()).updatedDate(updated.getState().getUpdatedDate()).build(), updated.getState());
    assertEquals(NodeTarget.SAME_TAB.name(), updated.getState().getTarget());
    assertFalse(changes.hasNext());

    //
    changes = root2.update(service, null);
    foo = root2.getChild("foo");
    assertEquals("foo", foo.getState().getLabel());
    updated = (NodeChange.Updated<Node>) changes.next();
    assertSame(foo, updated.getTarget());
    assertEquals(new NodeState.Builder().label("foo").target(NodeTarget.SAME_TAB.name()).updatedDate(updated.getState().getUpdatedDate()).build(), updated.getState());
    assertNotSame(0, updated.getState().getUpdatedDate());
    assertEquals(NodeTarget.SAME_TAB.name(), updated.getState().getTarget());
    assertFalse(changes.hasNext());

    //
    changes = root3.update(service, null);
    foo = root3.getChild("foo");
    assertEquals("foo", foo.getState().getLabel());
    updated = (NodeChange.Updated<Node>) changes.next();
    assertSame(foo, updated.getTarget());
    assertEquals(new NodeState.Builder().label("foo").target(NodeTarget.SAME_TAB.name()).updatedDate(updated.getState().getUpdatedDate()).build(), updated.getState());
    assertNotSame(0, updated.getState().getUpdatedDate());
    assertEquals(NodeTarget.SAME_TAB.name(), updated.getState().getTarget());
    assertFalse(changes.hasNext());
  }

  public void testUseMostActualChildren() throws Exception {
    createNavigation(SiteType.PORTAL, "update_with_most_actual_children");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_with_most_actual_children"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo").add(null, "bar");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node foo = root.getChild("foo");
    restartTransaction();

    //
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("foo").removeChild("bar");
    service.saveNode(root1.getContext(), null);
    restartTransaction();

    //
    foo.update(service, Scope.CHILDREN);
    assertNull(foo.getChild("bar"));

    // Update a second time (it actually test a previous bug)
    foo.update(service, Scope.CHILDREN);
    assertNull(foo.getChild("bar"));
  }

  public void testUpdateDeletedNode() throws Exception {
    createNavigation(SiteType.PORTAL, "update_deleted_node");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_deleted_node"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo").add(null, "bar");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node bar = root.getChild("foo").getChild("bar");
    restartTransaction();

    //
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("foo").removeChild("bar");
    service.saveNode(root1.getContext(), null);
    restartTransaction();

    //
    Iterator<NodeChange<Node>> changes = bar.update(service, Scope.CHILDREN);
    NodeChange.Removed<Node> removed = (NodeChange.Removed<Node>) changes.next();
    assertSame(bar, removed.getTarget());
    assertFalse(changes.hasNext());
  }

  public void testLoadEvents() throws Exception {
    createNavigation(SiteType.PORTAL, "update_load_events");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_load_events"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    NodeContext foo = rootContext1.add(null, "foo");
    foo.add(null, "bar1");
    foo.add(null, "bar2");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();

    //
    Node fooNode = root.getChild(0);
    assertEquals("foo", fooNode.getName());
    Node bar1 = fooNode.getChild(0);
    assertEquals("bar1", bar1.getName());
    Node bar2 = fooNode.getChild(1);
    assertEquals("bar2", bar2.getName());
  }

  public void testUpdateTwice2() throws Exception {
    createNavigation(SiteType.PORTAL, "update_twice2");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_twice2"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo").add(null, "bar");
    service.saveNode(rootContext1, null);

    restartTransaction();

    // Browser 1 : Expand the "foo" node
    Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node foo = root.getChild("foo");
    // If this line is commented, the test is passed
    service.updateNode(foo.getContext(), Scope.CHILDREN, null);
    restartTransaction();

    // Browser 2: Change the "foo" node
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("foo").removeChild("bar");
    service.saveNode(root1.getContext(), null);
    restartTransaction();

    // Browser 1: Try to expand the "foo" node 2 times ---> NPE after the 2nd
    // updateNode method
    service.updateNode(foo.getContext(), Scope.CHILDREN, null);
    service.updateNode(foo.getContext(), Scope.CHILDREN, null);
  }

  public void testMove2() throws Exception {
    createNavigation(SiteType.PORTAL, "update_move2");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_move2"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    restartTransaction();

    NodeContext<Node> root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    Node a = root.getNode("a");
    Node b = a.getChild("b");
    Node c = root.getNode("c");

    // Browser 2 : move the node "b" from "a" to "c"
    NodeContext<Node> root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    root1.getNode("c").addChild(root1.getNode("a").getChild("b"));
    service.saveNode(root1.getNode().getContext(), null);
    //
    restartTransaction();

    // Browser 1: need NodeChange event to update UI
    NodeChangeQueue<NodeContext<Node>> queue = new NodeChangeQueue<NodeContext<Node>>();
    // If update "root1" --> NodeChange.Moved --> ok
    // If update "b" --> NodeChange.Add --> ok
    // update "a" --> no NodeChange, we need an event here (NodeChange.Remove)
    // so UI can be updated
    service.updateNode(a.getContext(), Scope.CHILDREN, queue);
    Iterator<NodeChange<NodeContext<Node>>> changes = queue.iterator();
    assertTrue(changes.hasNext());
  }

  public void testScope() throws Exception {
    createNavigation(SiteType.PORTAL, "update_scope");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_scope"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    rootContext1.add(null, "c").add(null, "d");
    service.saveNode(rootContext1, null);

    //
    restartTransaction();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node a = root1.getChild("a");
    Node c = root1.getChild("c");
    assertFalse(a.getContext().isExpanded());
    assertFalse(c.getContext().isExpanded());

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("e");
    service.saveNode(root2.getContext(), null);

    //
    restartTransaction();

    //
    service.updateNode(a.getContext(), Scope.CHILDREN, null);
    assertSame(a, root1.getChild("a"));
    assertSame(c, root1.getChild("c"));
    assertNotNull(root1.getChild("e"));
    assertTrue(a.getContext().isExpanded());
    assertFalse(c.getContext().isExpanded());
    assertNotNull(a.getChild("b"));
  }

  public void _testPendingChange() throws Exception {
    createNavigation(SiteType.PORTAL, "update_pending_change");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_pending_change"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    rootContext1.add(null, "bar");
    service.saveNode(rootContext1, null);

    restartTransaction();

    Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node foo = root.getChild("foo");
    Node bar = root.getChild("bar");

    // Expand and change the "bar" node
    service.updateNode(bar.getContext(), Scope.CHILDREN, null);
    bar.addChild("juu");

    // ---> IllegalArgumentException
    // Can't expand the "foo" node, even it doesn't have any pending changes
    service.updateNode(foo.getContext(), Scope.CHILDREN, null);
  }

  public void testRemovedNavigation() throws Exception {
    createNavigation(SiteType.PORTAL, "update_removed_navigation");

    //
    restartTransaction();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("update_removed_navigation"));
    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    service.destroyNavigation(navigation);

    //
    restartTransaction();

    //
    try {
      service.updateNode(root.getContext(), null, null);
    } catch (HierarchyException e) {
      assertSame(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE, e.getError());
    }
  }

  protected NavigationService getNavigationService() {
    PortalContainer container = PortalContainer.getInstance();
    return container.getComponentInstanceOfType(NavigationService.class);
  }
}
