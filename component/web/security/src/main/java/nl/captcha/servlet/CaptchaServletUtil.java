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
package nl.captcha.servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.http.HttpServletResponse;

public final class CaptchaServletUtil {

  protected static final Log LOG = ExoLogger.getLogger(CaptchaServletUtil.class);

  private CaptchaServletUtil() {
    // Utils Class, no constructor
  }

  public static void writeImage(HttpServletResponse response, BufferedImage bi) {
    response.setHeader("Cache-Control", "private,no-cache,no-store");
    response.setContentType("image/png"); // PNGs allow for transparency. JPGs
                                          // do not.
    try {
      writeImage(response.getOutputStream(), bi);
    } catch (IOException e) {
      LOG.error("Error writing generated captcha image in HTTP response", e);
    }
  }

  public static void writeImage(OutputStream os, BufferedImage bi) {
    try {
      ImageIO.write(bi, "png", os);
      os.close();
    } catch (IOException e) {
      LOG.error("Error writing generated captcha image", e);
    }
  }
}
