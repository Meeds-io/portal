/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.web.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public final class RedirectUrlValidator {

  private RedirectUrlValidator() {
  }

  public static String sanitizeInitialURI(HttpServletRequest request, String initialURI) {
    String fallback = request.getContextPath();

    if (StringUtils.isBlank(initialURI)) {
      return fallback;
    }

    String value = initialURI.trim();

    if (containsControlCharacter(value) || containsEncodedControlCharacter(value)) {
      return fallback;
    }

    // Browsers may normalize backslashes as slashes while processing redirects.
    if (value.indexOf('\\') >= 0) {
      return fallback;
    }

    // Reject scheme-relative URLs such as //evil.example.
    if (value.startsWith("//")) {
      return fallback;
    }

    // Only local absolute paths are accepted.
    if (!value.startsWith("/")) {
      return fallback;
    }

    // Avoid URL parser differentials based on encoded slashes or backslashes.
    String lowerValue = value.toLowerCase(Locale.ROOT);
    if (lowerValue.contains("%2f") || lowerValue.contains("%5c")) {
      return fallback;
    }

    try {
      URI uri = new URI(value);

      // Reject absolute, opaque or host-bearing URIs: http://x, http:x, http:@x, etc.
      if (uri.isAbsolute() || uri.isOpaque() || uri.getScheme() != null || uri.getHost() != null) {
        return fallback;
      }

      String normalizedValue = uri.normalize().toString();
      String contextPath = request.getContextPath();

      if (StringUtils.isNotBlank(contextPath)
          && !normalizedValue.equals(contextPath)
          && !normalizedValue.startsWith(contextPath + "/")) {
        return fallback;
      }

      return normalizedValue;
    } catch (URISyntaxException e) {
      return fallback;
    }
  }

  private static boolean containsControlCharacter(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isISOControl(value.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsEncodedControlCharacter(String value) {
    String lowerValue = value.toLowerCase(Locale.ROOT);
    return lowerValue.contains("%00")
        || lowerValue.contains("%0a")
        || lowerValue.contains("%0d");
  }
}
