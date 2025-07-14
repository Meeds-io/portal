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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.gatein.common.util.Tools;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.AbstractConfigTest;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.dao.DescriptionDAO;

public class TestDescriptionService extends AbstractConfigTest {

  private DescriptionStorage service;
  
  private DescriptionDAO descDAO;
  
  private String id = "testId";
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    //
    PortalContainer container = PortalContainer.getInstance();
    service = container.getComponentInstanceOfType(DescriptionStorage.class);
    descDAO = getContainer().getComponentInstanceOfType(DescriptionDAO.class);

    //
    begin();
  }

  @Override
  protected void tearDown() throws Exception {
    descDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testResolveNoDescription() throws Exception {
    assertEquals(null, service.resolveDescription("notExists", null, Locale.ENGLISH));
  }

  public void testResolveLocalizedDescription() throws Exception {
    Map<Locale, org.exoplatform.portal.mop.State> descriptions = new HashMap<Locale, org.exoplatform.portal.mop.State>();
    descriptions.put(Locale.ENGLISH, new State("name_en", null));
    descriptions.put(Locale.UK, new State("name_en_GB", null));
    service.setDescriptions(id, descriptions);

    //
    assertEquals(null, service.resolveDescription(id, null, Locale.GERMAN));
    assertEquals(null, service.resolveDescription(id, null, new Locale("", "GB")));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, Locale.ENGLISH, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en_GB", null), service.resolveDescription(id, Locale.UK, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, Locale.US, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, null, Locale.ENGLISH));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, null, Locale.US));
    assertEquals(new org.exoplatform.portal.mop.State("name_en_GB", null), service.resolveDescription(id, null, Locale.UK));
  }

  public void testResolveDescription() throws Exception {
    Map<Locale, org.exoplatform.portal.mop.State> descriptions = new HashMap<Locale, org.exoplatform.portal.mop.State>();
    descriptions.put(Locale.ENGLISH, new State("name_en", null));
    descriptions.put(Locale.UK, new State("name_en_GB", null));

    service.setDescription(id, new State("name", null));
    service.setDescriptions(id, descriptions);

    //
    assertEquals(null, service.resolveDescription(id, null, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, Locale.ENGLISH, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en_GB", null), service.resolveDescription(id, Locale.UK, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, Locale.US, Locale.GERMAN));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, null, Locale.ENGLISH));
    assertEquals(new org.exoplatform.portal.mop.State("name_en", null), service.resolveDescription(id, null, Locale.US));
    assertEquals(new org.exoplatform.portal.mop.State("name_en_GB", null), service.resolveDescription(id, null, Locale.UK));
  }

  public void testGetDefaultDescription() throws Exception {
    service.setDescription(id, new State("foo_name", null));

    //
    assertEquals(new org.exoplatform.portal.mop.State("foo_name", null), service.getDescription(id));
  }

  public void testRemoveDefaultDescription() throws Exception {
    service.setDescription(id, new State("foo_name", null));
    assertEquals(new org.exoplatform.portal.mop.State("foo_name", null), service.getDescription(id));
    
    service.setDescription(id, null);
    assertNull(service.getDescription(id));
  }

  public void testSetLocalizedDescription() throws Exception {    
    service.setDescription(id, Locale.ENGLISH, new org.exoplatform.portal.mop.State("foo_english", null));

    //
    assertEquals(new State("foo_english", null), service.getDescription(id, Locale.ENGLISH));
  }

  public void testSetInvalidLocaleDescription() throws Exception {
    try {
      service.setDescription(id, new Locale("", "GB"), new org.exoplatform.portal.mop.State("foo_invalid", null));
      fail();
    } catch (IllegalArgumentException e) {
    }

    //
    try {
      service.setDescription(id, new Locale("en", "GB", "variant"), new org.exoplatform.portal.mop.State("foo_invalid", null));
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void testAddLocalizedDescription() throws Exception {
    service.setDescription(id, Locale.ENGLISH, new org.exoplatform.portal.mop.State("add_english", null));
    service.setDescription(id, Locale.FRENCH, new org.exoplatform.portal.mop.State("add_french", null));

    //
    State desc = service.getDescription(id, Locale.ENGLISH);
    assertNotNull(desc);
    assertEquals("add_english", desc.getName());
    desc = service.getDescription(id, Locale.FRENCH);
    assertNotNull(desc);
    assertEquals("add_french", desc.getName());
  }

  public void testGetDescriptions() throws Exception {
    assertNull(service.getDescriptions(id));
    
    Map<Locale, org.exoplatform.portal.mop.State> descriptions = new HashMap<Locale, org.exoplatform.portal.mop.State>();
    descriptions.put(Locale.ENGLISH, new State("foo_english", null));
    descriptions.put(Locale.FRENCH, new State("foo_french", null));
    service.setDescriptions(id, descriptions);

    //
    Map<Locale, org.exoplatform.portal.mop.State> description = service.getDescriptions(id);
    assertEquals(Tools.toSet(Locale.ENGLISH, Locale.FRENCH), description.keySet());
    assertEquals(new org.exoplatform.portal.mop.State("foo_english", null), description.get(Locale.ENGLISH));
    assertEquals(new org.exoplatform.portal.mop.State("foo_french", null), description.get(Locale.FRENCH));
  }

  public void testSetDescriptions() throws Exception {
    assertNull(service.getDescriptions(id));

    //
    Map<Locale, org.exoplatform.portal.mop.State> description = new HashMap<Locale, org.exoplatform.portal.mop.State>();
    description.put(Locale.ENGLISH, new org.exoplatform.portal.mop.State("bar_english", null));
    description.put(Locale.FRENCH, new org.exoplatform.portal.mop.State("bar_french", null));
    service.setDescriptions(id, description);

    //
    description = service.getDescriptions(id);
    assertEquals(Tools.toSet(Locale.ENGLISH, Locale.FRENCH), description.keySet());
    assertEquals(new org.exoplatform.portal.mop.State("bar_english", null), description.get(Locale.ENGLISH));
    assertEquals(new org.exoplatform.portal.mop.State("bar_french", null), description.get(Locale.FRENCH));

    //
    description = new HashMap<Locale, org.exoplatform.portal.mop.State>();
    description.put(Locale.ENGLISH, new org.exoplatform.portal.mop.State("bar_english_2", null));
    service.setDescriptions(id, description);

    //
    description = service.getDescriptions(id);
    assertEquals(Tools.toSet(Locale.ENGLISH), description.keySet());
    assertEquals(new org.exoplatform.portal.mop.State("bar_english_2", null), description.get(Locale.ENGLISH));
  }

  public void testSetInvalidLocaleDescriptions() throws Exception {
    try {
      service.setDescriptions(id,
                          Collections.singletonMap(new Locale("", "GB"), new org.exoplatform.portal.mop.State("bar_invalid", null)));
      fail();
    } catch (IllegalArgumentException e) {
    }
    try {
      service.setDescriptions(id,
                          Collections.singletonMap(new Locale("en", "GB", "variant"), new org.exoplatform.portal.mop.State("bar_invalid", null)));
      fail();
    } catch (IllegalArgumentException e) {
    }
  }
}
