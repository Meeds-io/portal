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
package io.meeds.portal.theme.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Every branding image is checked by its bytes: type, size, the favicon's
 * shape, and an SVG's content.
 */
public class BrandingImageValidatorTest {

  @Test
  void theTypeIsReadFromTheBytesNotTheName() throws Exception {
    assertEquals(BrandingImageValidator.PNG, BrandingImageValidator.validateLogo(png(40, 20)));
    assertEquals(BrandingImageValidator.SVG,
                 BrandingImageValidator.validateLogo(svg("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 40 20\"/>")));
    byte[] gif = "GIF89a....".getBytes(StandardCharsets.US_ASCII);
    assertEquals(BrandingImageValidator.TYPE_REFUSED,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateLogo(gif)).getMessage());
    assertEquals(BrandingImageValidator.GIF, BrandingImageValidator.validateImage(gif));
    byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
    assertEquals(BrandingImageValidator.TYPE_REFUSED,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateImage(html)).getMessage());
  }

  @Test
  void eachImageHasItsSizeCap() throws Exception {
    byte[] bigLogo = new byte[BrandingImageValidator.LOGO_MAX_BYTES + 1];
    System.arraycopy(png(40, 20), 0, bigLogo, 0, 8);
    assertEquals(BrandingImageValidator.TOO_LARGE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateLogo(bigLogo)).getMessage());
    byte[] bigImage = new byte[BrandingImageValidator.IMAGE_MAX_BYTES + 1];
    System.arraycopy(png(40, 20), 0, bigImage, 0, 8);
    assertEquals(BrandingImageValidator.TOO_LARGE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateImage(bigImage)).getMessage());
    byte[] bigFavicon = pngHeader(32, 32, BrandingImageValidator.FAVICON_MAX_BYTES + 1);
    assertEquals(BrandingImageValidator.TOO_LARGE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateFavicon(bigFavicon)).getMessage());
  }

  @Test
  void aFileExactlyAtItsCapIsAccepted() throws Exception {
    byte[] logo = new byte[BrandingImageValidator.LOGO_MAX_BYTES];
    System.arraycopy(png(40, 20), 0, logo, 0, 8);
    assertEquals(BrandingImageValidator.PNG, BrandingImageValidator.validateLogo(logo));
    byte[] image = new byte[BrandingImageValidator.IMAGE_MAX_BYTES];
    System.arraycopy(png(40, 20), 0, image, 0, 8);
    assertEquals(BrandingImageValidator.PNG, BrandingImageValidator.validateImage(image));
    assertEquals(BrandingImageValidator.PNG,
                 BrandingImageValidator.validateFavicon(pngHeader(32, 32, BrandingImageValidator.FAVICON_MAX_BYTES)));
  }

  @Test
  void aConfiguredFileIsAnyKnownImageTypeWithoutTheUploadCapsAndAnSvgStillChecked() throws Exception {
    // the built-in favicon is a WebP, a type an uploaded favicon may not be
    byte[] webp = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' };
    assertEquals(BrandingImageValidator.WEBP, BrandingImageValidator.validateConfigured(webp));
    assertEquals(BrandingImageValidator.ICO, BrandingImageValidator.validateConfigured(ico(16, 16)));
    byte[] bigLogo = new byte[BrandingImageValidator.LOGO_MAX_BYTES + 1];
    System.arraycopy(png(40, 20), 0, bigLogo, 0, 8);
    assertEquals(BrandingImageValidator.PNG, BrandingImageValidator.validateConfigured(bigLogo));
    assertEquals(BrandingImageValidator.TYPE_REFUSED,
                 assertThrows(IllegalArgumentException.class,
                              () -> BrandingImageValidator.validateConfigured(new byte[] { 1, 2, 3, 4 })).getMessage());
    byte[] unsafe = svg("<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\" viewBox=\"0 0 40 40\"/>");
    assertEquals(BrandingImageValidator.SVG_UNSAFE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateConfigured(unsafe)).getMessage());
  }

  @Test
  void aFaviconIsSquareAndAtLeast32Pixels() throws Exception {
    assertEquals(BrandingImageValidator.PNG, BrandingImageValidator.validateFavicon(png(32, 32)));
    assertEquals(BrandingImageValidator.FAVICON_NOT_SQUARE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateFavicon(png(16, 16))).getMessage());
    assertEquals(BrandingImageValidator.FAVICON_NOT_SQUARE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateFavicon(png(32, 16))).getMessage());
    assertEquals(BrandingImageValidator.ICO, BrandingImageValidator.validateFavicon(ico(16, 0)), "0 means 256");
    assertEquals(BrandingImageValidator.FAVICON_NOT_SQUARE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateFavicon(ico(16, 16))).getMessage());
    assertEquals(BrandingImageValidator.SVG,
                 BrandingImageValidator.validateFavicon(svg("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\"/>")));
    assertEquals(BrandingImageValidator.FAVICON_NOT_SQUARE,
                 assertThrows(IllegalArgumentException.class,
                              () -> BrandingImageValidator.validateFavicon(svg("<svg xmlns=\"http://www.w3.org/2000/svg\"/>"))).getMessage());
  }

  @Test
  void aPngSizeIsReadFromItsHeaderNeverByDecoding() {
    assertEquals(BrandingImageValidator.FAVICON_NOT_SQUARE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateFavicon(pngHeader(64, 32))).getMessage());
    // a few hundred bytes declaring a huge image: checked without allocating it
    assertEquals(BrandingImageValidator.PNG, BrandingImageValidator.validateFavicon(pngHeader(20000, 20000)));
  }

  @Test
  void anUnsafeSvgIsRefusedWhateverItsName() {
    byte[] unsafe = svg("<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\" viewBox=\"0 0 40 40\"/>");
    assertEquals(BrandingImageValidator.SVG_UNSAFE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateLogo(unsafe)).getMessage());
    byte[] external = svg("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 40 40\"><use href=\"https://x.example/a.svg#b\"/></svg>");
    assertEquals(BrandingImageValidator.SVG_UNSAFE,
                 assertThrows(IllegalArgumentException.class, () -> BrandingImageValidator.validateImage(external)).getMessage());
  }

  private static byte[] png(int width, int height) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", output);
    return output.toByteArray();
  }

  /** An ICO directory with two entries: a 16x16 one and a second of the given size, 0 meaning 256. */
  private static byte[] ico(int first, int second) {
    byte[] data = new byte[6 + 32];
    data[2] = 1;
    data[4] = 2;
    data[6] = (byte) first;
    data[7] = (byte) first;
    data[22] = (byte) second;
    data[23] = (byte) second;
    return data;
  }

  private static byte[] pngHeader(int width, int height) {
    return pngHeader(width, height, 274);
  }

  private static byte[] pngHeader(int width, int height, int length) {
    byte[] data = new byte[length];
    byte[] signature = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R' };
    System.arraycopy(signature, 0, data, 0, signature.length);
    for (int i = 0; i < 4; i++) {
      data[16 + i] = (byte) (width >>> (24 - 8 * i));
      data[20 + i] = (byte) (height >>> (24 - 8 * i));
    }
    return data;
  }

  private static byte[] svg(String content) {
    return content.getBytes(StandardCharsets.UTF_8);
  }

}
