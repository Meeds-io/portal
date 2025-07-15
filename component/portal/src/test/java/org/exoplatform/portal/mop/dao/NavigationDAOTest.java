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
package org.exoplatform.portal.mop.dao;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.portal.jdbc.entity.NavigationEntity;
import org.exoplatform.portal.jdbc.entity.NodeEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.NodeTarget;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;

public class NavigationDAOTest extends AbstractDAOTest {

  private SiteDAO           siteDAO;

  private NavigationDAO     navigationDAO;

  private NodeDAO           nodeDAO;

  private EntityTransaction transaction;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.navigationDAO = getContainer().getComponentInstanceOfType(NavigationDAO.class);
    this.nodeDAO = getContainer().getComponentInstanceOfType(NodeDAO.class);
    this.siteDAO = getContainer().getComponentInstanceOfType(SiteDAO.class);

    EntityManagerService managerService = getContainer().getComponentInstanceOfType(EntityManagerService.class);
    transaction = managerService.getEntityManager().getTransaction();
    transaction.begin();
  }

  @Override
  protected void tearDown() throws Exception {
    if (transaction.isActive()) {
      transaction.rollback();
    }
    super.tearDown();
    end();
  }

  public void testCreateNav() {
    NavigationEntity nav = createNav("classic");
    navigationDAO.create(nav);

    NavigationEntity expected = navigationDAO.find(nav.getId());
    assertNotNull(expected);
    assertNav(expected, nav);
  }

  public void testCreateNode() {
    NodeEntity node1 = createNode("node1");

    NodeEntity node2 = createNode("node2");
    node2.setParent(node1);

    NodeEntity node3 = createNode("node3");
    node3.setParent(node1);

    node1.setChildren(Arrays.asList(node3, node2));
    nodeDAO.create(node1);

    NodeEntity expected = nodeDAO.find(node1.getId());
    assertNotNull(expected);
    assertNode(expected, node1);

    List<NodeEntity> children = expected.getChildren();
    assertEquals(2, children.size());
    assertNode(children.get(0), node3);
    assertNode(children.get(1), node2);
  }

  public void testUpdateNav() {
    NodeEntity node1 = createNode("node1");
    nodeDAO.create(node1);

    NavigationEntity nav = createNav("classic");
    nav.setRootNode(node1);
    navigationDAO.create(nav);

    NavigationEntity expected = navigationDAO.find(nav.getId());
    assertNotNull(expected);
    assertNav(expected, nav);

    nav.setPriority(2);
    navigationDAO.update(nav);

    expected = navigationDAO.find(nav.getId());
    assertNav(expected, nav);
  }

  /**
   * @return
   */
  private NavigationEntity createNav(String ownerId) {
    NavigationEntity nav = new NavigationEntity();
    nav.setOwner(getOrCreateSite("classic"));
    nav.setPriority(1);
//    nav.setId(UUID.randomUUID().toString());
    return nav;
  }

  private SiteEntity getOrCreateSite(String name) {
    SiteEntity siteEntity = siteDAO.findByKey(SiteType.PORTAL.key(name));
    if (siteEntity == null) {
      siteEntity = new SiteEntity();
      siteEntity.setSiteType(SiteType.PORTAL);
      siteEntity.setName(name);
      siteDAO.create(siteEntity);
    }
    return siteEntity;
  }

  private void assertNode(NodeEntity expected, NodeEntity node) {
    assertEquals(expected.getEndTime(), node.getEndTime());
    assertEquals(expected.getIcon(), node.getIcon());
    assertEquals(expected.getIndex(), node.getIndex());
    assertEquals(expected.getLabel(), node.getLabel());
    assertEquals(expected.getName(), node.getName());
    assertEquals(expected.getStartTime(), node.getStartTime());
    assertEquals(expected.getId(), node.getId());
    assertEquals(expected.getVisibility(), node.getVisibility());
    assertEquals(expected.getTarget(), node.getTarget());
  }

  private NodeEntity createNode(String name) {
    NodeEntity node = new NodeEntity();
    //node.setId(UUID.randomUUID().toString());
    node.setName(name);
    node.setEndTime(1);
    node.setIcon("icon");
    node.setIndex(1);
    node.setLabel("label");
    node.setStartTime(2);
    node.setVisibility(Visibility.SYSTEM);
    node.setTarget(NodeTarget.SAME_TAB);
    return node;
  }

  private void assertNav(NavigationEntity expected, NavigationEntity nav) {
    assertEquals(expected.getOwnerId(), nav.getOwnerId());
    assertEquals(expected.getPriority(), nav.getPriority());
    assertEquals(expected.getId(), nav.getId());
    assertEquals(expected.getOwnerType(), nav.getOwnerType());
  }
}
