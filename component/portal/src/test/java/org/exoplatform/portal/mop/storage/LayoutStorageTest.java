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
import java.util.List;

import jakarta.persistence.EntityTransaction;

import org.json.simple.JSONArray;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.mop.dao.AbstractDAOTest;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;

public class LayoutStorageTest extends AbstractDAOTest {

  private LayoutStorage      layoutStorage;

  private EntityTransaction transaction;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.layoutStorage = getContainer().getComponentInstanceOfType(LayoutStorage.class);

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

  public void testTextBackgroundStylePersistedAndReloaded() {
    ModelStyle style = new ModelStyle();
    style.setTextTitleBackgroundColor("#FFFFFFFF");
    style.setTextTitleBackgroundImage("/portal/rest/v1/social/attachments/containerTextTitleBackground/1/2");
    style.setTextTitleBackgroundEffect("linear-gradient(#FFFFFFFF 0%, #999999FF 100%)");
    style.setTextTitleBackgroundPosition("top left");
    style.setTextTitleBackgroundSize("cover");
    style.setTextTitleBackgroundRepeat("no-repeat");
    style.setTextTitleBackgroundPadding("10px 10px 10px 10px");
    style.setTextTitleBackgroundRadius("8px 8px 8px 8px");

    style.setTextHeaderBackgroundColor("#EEEEEEFF");
    style.setTextHeaderBackgroundImage("/portal/rest/v1/social/attachments/containerTextHeaderBackground/1/2");
    style.setTextHeaderBackgroundEffect("linear-gradient(#EEEEEEFF 0%, #888888FF 100%)");
    style.setTextHeaderBackgroundPosition("top right");
    style.setTextHeaderBackgroundSize("contain");
    style.setTextHeaderBackgroundRepeat("repeat");
    style.setTextHeaderBackgroundPadding("5px 5px 5px 5px");
    style.setTextHeaderBackgroundRadius("4px 4px 4px 4px");

    style.setTextBackgroundColor("#DDDDDDFF");
    style.setTextBackgroundImage("/portal/rest/v1/social/attachments/containerTextBodyBackground/1/2");
    style.setTextBackgroundEffect("linear-gradient(#DDDDDDFF 0%, #777777FF 100%)");
    style.setTextBackgroundPosition("bottom left");
    style.setTextBackgroundSize("cover");
    style.setTextBackgroundRepeat("no-repeat");
    style.setTextBackgroundPadding("3px 3px 3px 3px");
    style.setTextBackgroundRadius("2px 2px 2px 2px");

    style.setTextSubtitleBackgroundColor("#CCCCCCFF");
    style.setTextSubtitleBackgroundImage("/portal/rest/v1/social/attachments/containerTextSubtitleBackground/1/2");
    style.setTextSubtitleBackgroundEffect("linear-gradient(#CCCCCCFF 0%, #666666FF 100%)");
    style.setTextSubtitleBackgroundPosition("bottom right");
    style.setTextSubtitleBackgroundSize("contain");
    style.setTextSubtitleBackgroundRepeat("repeat-x");
    style.setTextSubtitleBackgroundPadding("1px 1px 1px 1px");
    style.setTextSubtitleBackgroundRadius("1px 1px 1px 1px");

    ContainerData containerData = new ContainerData(null,
                                                     "testTextBackground",
                                                     "testTextBackground",
                                                     null,
                                                     "system:/groovy/portal/webui/container/UIContainer.gtmpl",
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     style,
                                                     null,
                                                     Collections.emptyList(),
                                                     Collections.emptyList());

    List<ComponentEntity> saved = layoutStorage.saveChildren(new JSONArray(),
                                                             Collections.<ComponentData> singletonList(containerData));
    assertEquals(1, saved.size());
    restartTransaction();

    ContainerEntity savedEntity = (ContainerEntity) saved.get(0);
    JSONArray body = new JSONArray();
    body.add(savedEntity.toJSON());

    List<ComponentData> loaded = layoutStorage.buildChildren(body);
    assertEquals(1, loaded.size());
    ContainerData loadedContainer = (ContainerData) loaded.get(0);
    ModelStyle loadedStyle = loadedContainer.getCssStyle();
    assertNotNull(loadedStyle);

    assertEquals("#FFFFFFFF", loadedStyle.getTextTitleBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextTitleBackground/1/2", loadedStyle.getTextTitleBackgroundImage());
    assertEquals("linear-gradient(#FFFFFFFF 0%, #999999FF 100%)", loadedStyle.getTextTitleBackgroundEffect());
    assertEquals("top left", loadedStyle.getTextTitleBackgroundPosition());
    assertEquals("cover", loadedStyle.getTextTitleBackgroundSize());
    assertEquals("no-repeat", loadedStyle.getTextTitleBackgroundRepeat());
    assertEquals("10px 10px 10px 10px", loadedStyle.getTextTitleBackgroundPadding());
    assertEquals("8px 8px 8px 8px", loadedStyle.getTextTitleBackgroundRadius());

    assertEquals("#EEEEEEFF", loadedStyle.getTextHeaderBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextHeaderBackground/1/2", loadedStyle.getTextHeaderBackgroundImage());
    assertEquals("linear-gradient(#EEEEEEFF 0%, #888888FF 100%)", loadedStyle.getTextHeaderBackgroundEffect());
    assertEquals("top right", loadedStyle.getTextHeaderBackgroundPosition());
    assertEquals("contain", loadedStyle.getTextHeaderBackgroundSize());
    assertEquals("repeat", loadedStyle.getTextHeaderBackgroundRepeat());
    assertEquals("5px 5px 5px 5px", loadedStyle.getTextHeaderBackgroundPadding());
    assertEquals("4px 4px 4px 4px", loadedStyle.getTextHeaderBackgroundRadius());

    assertEquals("#DDDDDDFF", loadedStyle.getTextBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextBodyBackground/1/2", loadedStyle.getTextBackgroundImage());
    assertEquals("linear-gradient(#DDDDDDFF 0%, #777777FF 100%)", loadedStyle.getTextBackgroundEffect());
    assertEquals("bottom left", loadedStyle.getTextBackgroundPosition());
    assertEquals("cover", loadedStyle.getTextBackgroundSize());
    assertEquals("no-repeat", loadedStyle.getTextBackgroundRepeat());
    assertEquals("3px 3px 3px 3px", loadedStyle.getTextBackgroundPadding());
    assertEquals("2px 2px 2px 2px", loadedStyle.getTextBackgroundRadius());

    assertEquals("#CCCCCCFF", loadedStyle.getTextSubtitleBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextSubtitleBackground/1/2",
                 loadedStyle.getTextSubtitleBackgroundImage());
    assertEquals("linear-gradient(#CCCCCCFF 0%, #666666FF 100%)", loadedStyle.getTextSubtitleBackgroundEffect());
    assertEquals("bottom right", loadedStyle.getTextSubtitleBackgroundPosition());
    assertEquals("contain", loadedStyle.getTextSubtitleBackgroundSize());
    assertEquals("repeat-x", loadedStyle.getTextSubtitleBackgroundRepeat());
    assertEquals("1px 1px 1px 1px", loadedStyle.getTextSubtitleBackgroundPadding());
    assertEquals("1px 1px 1px 1px", loadedStyle.getTextSubtitleBackgroundRadius());
  }

}
