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
package io.meeds.portal.theme.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ImageBytesTest {

  @Test
  public void twoImagesWithTheSameBytesAreEqual() {
    ImageBytes image = new ImageBytes(new byte[] { 1, 2, 3 }, "image/png", 7L);
    ImageBytes same = new ImageBytes(new byte[] { 1, 2, 3 }, "image/png", 7L);

    assertEquals(image, same);
    assertEquals(image.hashCode(), same.hashCode());
    assertEquals(new ImageBytes(null, "image/png", 7L), new ImageBytes(null, "image/png", 7L));
    assertEquals(new ImageBytes(null, "image/png", 7L).hashCode(), new ImageBytes(null, "image/png", 7L).hashCode());
  }

  @Test
  public void differentBytesTypeOrLoadTimeAreNotEqual() {
    ImageBytes image = new ImageBytes(new byte[] { 1, 2, 3 }, "image/png", 7L);

    assertNotEquals(image, new ImageBytes(new byte[] { 1, 2, 4 }, "image/png", 7L));
    assertNotEquals(image, new ImageBytes(new byte[] { 1, 2, 3 }, "image/jpeg", 7L));
    assertNotEquals(image, new ImageBytes(new byte[] { 1, 2, 3 }, "image/png", 8L));
  }

  @Test
  public void toStringNamesTheLengthNotTheBytes() {
    String text = new ImageBytes(new byte[] { 65, 66, 67 }, "image/png", 7L).toString();

    assertTrue(text.contains("length=3"), text);
    assertFalse(text.contains("[B@"), text);
    assertEquals("ImageBytes[type=null, length=0, loadedAt=0]", new ImageBytes(null, null, 0L).toString());
  }

}
