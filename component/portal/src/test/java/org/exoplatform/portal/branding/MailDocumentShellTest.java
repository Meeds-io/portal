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
package org.exoplatform.portal.branding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;


/**
 * The document every HTML mail is wrapped in.
 */
public class MailDocumentShellTest {

  private static final Map<String, String> DARK = Map.of(MailDocumentShell.BACKGROUND, "#14181B",
                                                         MailDocumentShell.SURFACE, "#1E2328",
                                                         MailDocumentShell.TEXT, "#E6E9EB",
                                                         MailDocumentShell.LINK, "#5FA9AC");

  @Test
  void anAutomaticRecipientGetsTheDarkBlockOverTheRoleClasses() {
    String mail = MailDocumentShell.wrap("<table class=\"mail-surface\"><tr><td>Hi</td></tr></table>", "auto", DARK);
    assertTrue(mail.startsWith("<!DOCTYPE html><html><head>"), mail);
    assertTrue(mail.contains("<meta name=\"color-scheme\" content=\"light dark\">"), mail);
    assertTrue(mail.contains("<meta name=\"supported-color-schemes\" content=\"light dark\">"), mail);
    assertTrue(mail.contains("@media (prefers-color-scheme: dark) {"), mail);
    assertTrue(mail.contains(".mail-surface{background-color:#1E2328 !important;}"), mail);
    assertTrue(mail.contains(".mail-text{color:#E6E9EB !important;}"), mail);
    assertTrue(mail.endsWith("<table class=\"mail-surface\"><tr><td>Hi</td></tr></table></body></html>"), mail);
  }

  @Test
  void anExplicitModeGetsItsSchemeAndNoBlock() {
    String light = MailDocumentShell.wrap("<div>Hi</div>", "light", DARK);
    assertTrue(light.contains("content=\"light\""), light);
    assertFalse(light.contains("<style>"), light);
    assertTrue(MailDocumentShell.wrap("<div>Hi</div>", "dark", null).contains("content=\"dark\""));
  }

  @Test
  void aDocumentIsLeftAsItIsAndANonColourIsNeverWritten() {
    String document = "  <!DOCTYPE html><html><body>Hi</body></html>";
    assertEquals(document, MailDocumentShell.wrap(document, "auto", DARK));
    assertEquals("<HTML><body/></HTML>", MailDocumentShell.wrap("<HTML><body/></HTML>", "auto", DARK));
    Map<String, String> forged = new HashMap<>(DARK);
    forged.put(MailDocumentShell.TEXT, "red}</style><script>alert(1)</script>");
    String mail = MailDocumentShell.wrap("<div/>", "auto", forged);
    assertFalse(mail.contains("<script>"), mail);
    assertFalse(mail.contains(".mail-text"), mail);
  }

}
