/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.theme.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.model.BrandingPublication;
import io.meeds.portal.theme.rest.util.ThemeRestUtils;
import io.meeds.portal.theme.service.ThemeService;

/**
 * The publication body as the page sends it: a JSON null in a theme map is a
 * removal and must reach the service.
 */
public class ThemeRestUtilsTest {

  @Test
  void aNullInAThemeMapSurvivesTheReading() {
    BrandingPublication publication = ThemeRestUtils.readPublication("""
        {"version": 7,
         "themes": [{"clientId": "new-1", "name": "Ocean", "startFrom": {"id": 3},
                     "light": {"secondaryColor": null, "primaryColor": "#123456"}}],
         "themeInUse": {"clientId": "new-1"},
         "modesOffered": "DARK",
         "customCss": ".a { color: red; }",
         "unknownField": true}
        """);
    assertEquals(7L, publication.getVersion());
    assertTrue(publication.getThemes().get(0).getLight().containsKey("secondaryColor"));
    assertNull(publication.getThemes().get(0).getLight().get("secondaryColor"));
    assertEquals(3L, publication.getThemes().get(0).getStartFrom().getId());
    assertEquals("new-1", publication.getThemeInUse().getClientId());
    assertEquals(ModesOffered.DARK, publication.getModesOffered());
  }

  @Test
  void aBodyThatIsNotAPublicationIsABadRequest() {
    for (String body : new String[] { null, "", "[1,2]", "{\"modesOffered\": \"SEPIA\"}", "{" }) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ThemeRestUtils.readPublication(body));
      assertEquals(ThemeRestUtils.INVALID_BODY_MESSAGE, e.getMessage());
      assertEquals(400, ThemeRestUtils.statusOf(e));
    }
  }

  @Test
  void theCustomCssBodyIsReadAndABlankOrNullRemovesIt() {
    assertEquals(".a { color: red; }", ThemeRestUtils.readCustomCss("{\"customCss\": \".a { color: red; }\"}"));
    assertEquals("", ThemeRestUtils.readCustomCss("{\"customCss\": null}"));
    for (String body : new String[] { null, "", "{}", "{\"customCss\": 3}", "{" }) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ThemeRestUtils.readCustomCss(body));
      assertEquals(ThemeRestUtils.INVALID_CUSTOM_CSS_MESSAGE, e.getMessage());
    }
  }

  @Test
  void aStaleVersionIsAConflictAndAnEngineFailureAServerError() {
    assertEquals(409, ThemeRestUtils.statusOf(new io.meeds.portal.theme.exception.ThemePublishConflictException()));
    assertEquals(500,
                 ThemeRestUtils.statusOf(new IllegalStateException(ThemeService.PUBLISH_CONFLICT_MESSAGE)),
                 "the conflict is told by its type, not by a message another failure could carry");
    assertEquals(500, ThemeRestUtils.statusOf(new IllegalStateException(ThemeService.PUBLISH_FAILED_MESSAGE)));
    assertEquals(400, ThemeRestUtils.statusOf(new IllegalArgumentException(ThemeService.NAME_ALREADY_EXISTS_MESSAGE)));
  }

}
