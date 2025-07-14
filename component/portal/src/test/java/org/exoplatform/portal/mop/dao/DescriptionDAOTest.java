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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;
import org.exoplatform.portal.mop.dao.DescriptionDAO;

public class DescriptionDAOTest extends AbstractDAOTest {
  private DescriptionDAO descDAO;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.descDAO = getContainer().getComponentInstanceOfType(DescriptionDAO.class);
  }

  @Override
  protected void tearDown() throws Exception {
    descDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testCreate() {
    DescriptionEntity desc = createDescription("testCreate");
    descDAO.create(desc);
    restartTransaction();

    DescriptionEntity result = descDAO.find(desc.getId());
    assertNotNull(result);
    assertDescription(desc, result);
  }

  public void testSave() {
    DescriptionEntity desc = createDescription("testSave");
    descDAO.create(desc);
    restartTransaction();

    descDAO.saveDescription("testSave", new DescriptionState("testName2", "testDesc2"));
    restartTransaction();

    DescriptionEntity result = descDAO.find(desc.getId());
    assertEquals("testName2", result.getState().getName());
    assertEquals("testDesc2", result.getState().getDescription());
  }

  public void testSave2() {
    DescriptionEntity desc = createDescription("testSave2");
    descDAO.create(desc);
    restartTransaction();

    Map<String, DescriptionState> localized = new HashMap<String, DescriptionState>();
    localized.put(I18N.toTagIdentifier(Locale.FRENCH), new DescriptionState("name1", "desc1"));
    localized.put(I18N.toTagIdentifier(Locale.ITALY), new DescriptionState("name2", "desc2"));
    descDAO.saveDescriptions("testSave2", localized);
    restartTransaction();

    DescriptionEntity result = descDAO.find(desc.getId());
    assertNotNull(result.getLocalized());
    Map<String, DescriptionState> resultLocalized = result.getLocalized();
    assertEquals(2, resultLocalized.size());
    assertEquals(new DescriptionState("name1", "desc1"), resultLocalized.get(I18N.toTagIdentifier(Locale.FRENCH)));
    assertEquals(new DescriptionState("name2", "desc2"), resultLocalized.get(I18N.toTagIdentifier(Locale.ITALY)));
  }
  
  public void testDelete() {
    DescriptionEntity desc = createDescription("testDelete");
    descDAO.create(desc);
    restartTransaction();
    
    descDAO.deleteByRefId("testDelete");
    restartTransaction();
    assertNull(descDAO.find(desc.getId()));
    assertNull(descDAO.getByRefId("testDelete"));
  }

  private void assertDescription(DescriptionEntity expected, DescriptionEntity result) {
    assertEquals(expected.getReferenceId(), result.getReferenceId());
    assertEquals(expected.getId(), result.getId());
    assertEquals(expected.getState(), result.getState());
    if (expected.getLocalized() == null) {
      assertNull(result.getLocalized());
    } else {
      assertEquals(expected.getLocalized().size(), result.getLocalized().size());
      for (String locale : expected.getLocalized().keySet()) {
        assertEquals(expected.getLocalized().get(locale), result.getLocalized().get(locale));
      }
    }
  }

  private DescriptionEntity createDescription(String refId) {
    DescriptionEntity desc = new DescriptionEntity();
    desc.setReferenceId(refId);
    DescriptionState state = new DescriptionState("testName", "testDesc");
    desc.setState(state);
    Map<String, DescriptionState> localized = new HashMap<String, DescriptionState>();
    localized.put(I18N.toTagIdentifier(Locale.FRENCH), new DescriptionState("testName1", "testDesc1"));
    localized.put(I18N.toTagIdentifier(Locale.ENGLISH), new DescriptionState("testName2", "testDesc2"));
    desc.setLocalized(localized);
    return desc;
  }

}
