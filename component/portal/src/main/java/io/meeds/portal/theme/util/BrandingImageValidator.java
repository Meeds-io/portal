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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exoplatform.upload.SvgUploadValidator;

/**
 * The server-side check of every branding image: its type read from its
 * bytes, never from its name or the client's label, its size, the favicon's
 * shape, and every SVG through the platform's SVG validator. A refusal is an
 * {@link IllegalArgumentException} carrying a message code; nothing is
 * sanitised.
 */
public final class BrandingImageValidator {

  public static final String     PNG                  = "image/png";

  public static final String     JPEG                 = "image/jpeg";

  public static final String     GIF                  = "image/gif";

  public static final String     WEBP                 = "image/webp";

  public static final String     SVG                  = "image/svg+xml";

  public static final String     ICO                  = "image/x-icon";

  public static final String     TYPE_REFUSED         = "branding.upload.typeRefused";

  public static final String     TOO_LARGE            = "branding.upload.tooLarge";

  public static final String     FAVICON_NOT_SQUARE   = "branding.upload.favicon.notSquare";

  public static final String     SVG_UNSAFE           = "branding.upload.svg.unsafe";

  public static final int        LOGO_MAX_BYTES       = 1024 * 1024;

  public static final int        FAVICON_MAX_BYTES    = 10 * 1024;

  public static final int        IMAGE_MAX_BYTES      = 2 * 1024 * 1024;

  public static final int        FAVICON_MIN_SIZE     = 32;

  private static final Set<String> LOGO_TYPES         = Set.of(PNG, JPEG, SVG);

  private static final Set<String> FAVICON_TYPES      = Set.of(PNG, SVG, ICO);

  private static final Set<String> IMAGE_TYPES        = Set.of(PNG, JPEG, GIF, WEBP, SVG);

  private static final Pattern   VIEW_BOX             = Pattern.compile("viewBox\\s*=\\s*[\"']\\s*[-\\d.]+[\\s,]+[-\\d.]+[\\s,]+([\\d.]+)[\\s,]+([\\d.]+)\\s*[\"']");

  private BrandingImageValidator() {
    // pure functions
  }

  /**
   * @param data a logo's bytes
   * @return its detected type
   */
  public static String validateLogo(byte[] data) {
    return validate(data, LOGO_TYPES, LOGO_MAX_BYTES);
  }

  /**
   * @param data a favicon's bytes
   * @return its detected type
   */
  public static String validateFavicon(byte[] data) {
    String type = validate(data, FAVICON_TYPES, FAVICON_MAX_BYTES);
    int[] size = dimensions(data, type);
    if (size == null || size[0] != size[1] || size[0] < FAVICON_MIN_SIZE) {
      throw new IllegalArgumentException(FAVICON_NOT_SQUARE);
    }
    return type;
  }

  /**
   * @param data a theme, page or login background image's bytes
   * @return its detected type
   */
  public static String validateImage(byte[] data) {
    return validate(data, IMAGE_TYPES, IMAGE_MAX_BYTES);
  }

  /**
   * A logo or favicon the deployment configures: any image type this check
   * knows (the built-in favicon is a WebP), an SVG checked as an upload is.
   * The upload caps, size and shape, bound what an administrator uploads,
   * not the deployment's own files.
   *
   * @param data a configured file's bytes
   * @return its detected type
   */
  public static String validateConfigured(byte[] data) {
    String type = detectType(data);
    if (type == null) {
      throw new IllegalArgumentException(TYPE_REFUSED);
    }
    return SVG.equals(type) ? validate(data, Set.of(SVG), IMAGE_MAX_BYTES) : type;
  }

  /**
   * @return the type the bytes are, read from their first bytes; null when
   *         they are no image type this check knows
   */
  public static String detectType(byte[] data) {
    if (data == null || data.length < 4) {
      return null;
    }
    if ((data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
      return PNG;
    } else if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
      return JPEG;
    } else if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8') {
      return GIF;
    } else if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' && data[8] == 'W'
               && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
      return WEBP;
    } else if (data[0] == 0 && data[1] == 0 && data[2] == 1 && data[3] == 0) {
      return ICO;
    } else if (isSvg(data)) {
      return SVG;
    }
    return null;
  }

  private static String validate(byte[] data, Set<String> accepted, int maxBytes) {
    String type = detectType(data);
    if (type == null || !accepted.contains(type)) {
      throw new IllegalArgumentException(TYPE_REFUSED);
    }
    if (data.length > maxBytes) {
      throw new IllegalArgumentException(TOO_LARGE);
    }
    if (SVG.equals(type)) {
      try {
        new SvgUploadValidator(maxBytes).validate("branding.svg", SVG, new ByteArrayInputStream(data));
      } catch (Exception e) {
        throw new IllegalArgumentException(SVG_UNSAFE, e);
      }
    }
    return type;
  }

  private static boolean isSvg(byte[] data) {
    String head = new String(data, 0, Math.min(data.length, 1024), StandardCharsets.UTF_8).stripLeading();
    if (head.startsWith("\uFEFF")) {
      head = head.substring(1).stripLeading();
    }
    String lower = head.toLowerCase(Locale.ROOT);
    return (lower.startsWith("<?xml") || lower.startsWith("<svg") || lower.startsWith("<!--") || lower.startsWith("<!doctype svg"))
           && lower.contains("<svg");
  }

  /**
   * @return width and height in pixels: PNG through ImageIO, ICO through its
   *         directory (its largest square entry), SVG through its viewBox;
   *         null when they cannot be read
   */
  static int[] dimensions(byte[] data, String type) {
    try {
      if (PNG.equals(type)) {
        // read from the IHDR chunk, never by decoding: a few hundred bytes may declare a huge image
        if (data.length < 24 || data[12] != 'I' || data[13] != 'H' || data[14] != 'D' || data[15] != 'R') {
          return null;
        }
        return new int[] { readInt(data, 16), readInt(data, 20) };
      } else if (ICO.equals(type)) {
        int count = (data[4] & 0xFF) | ((data[5] & 0xFF) << 8);
        int best = 0;
        for (int i = 0; i < count && 6 + i * 16 + 1 < data.length; i++) {
          int width = data[6 + i * 16] & 0xFF;
          int height = data[6 + i * 16 + 1] & 0xFF;
          width = width == 0 ? 256 : width;
          height = height == 0 ? 256 : height;
          if (width == height) {
            best = Math.max(best, width);
          }
        }
        return best == 0 ? null : new int[] { best, best };
      } else if (SVG.equals(type)) {
        Matcher viewBox = VIEW_BOX.matcher(new String(data, StandardCharsets.UTF_8));
        if (!viewBox.find()) {
          return null;
        }
        return new int[] { (int) Math.round(Double.parseDouble(viewBox.group(1))),
            (int) Math.round(Double.parseDouble(viewBox.group(2))) };
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private static int readInt(byte[] data, int offset) {
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
  }

}
