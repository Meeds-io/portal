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
package org.exoplatform.groovyscript;

import java.util.Map;

import org.exoplatform.component.test.AbstractGateInTest;

/**
 * A template whose first statement is an expression section: the generated script must hold no ';' before its
 * first statement, which the parser of Groovy 3 and later refuses.
 */
public class TestLeadingSemicolon extends AbstractGateInTest {

  private static final Map<String, Object> BINDING = Map.of("x", "v");

  public void testExpressionFirst() throws Exception {
    assertRendersWithoutLeadingSemicolon("${x}a", "va");
  }

  public void testExpressionTagFirst() throws Exception {
    assertRendersWithoutLeadingSemicolon("<%=x%>a", "va");
  }

  public void testEmptyScriptletFirst() throws Exception {
    assertRendersWithoutLeadingSemicolon("<%%>${x}", "v");
  }

  public void testBlockCommentScriptletFirst() throws Exception {
    assertRendersWithoutLeadingSemicolon("<%/* licence */%>${x}", "v");
  }

  public void testLineCommentScriptletFirst() throws Exception {
    assertRendersWithoutLeadingSemicolon("<%// licence\n%>${x}", "v");
  }

  public void testStatementFirst() throws Exception {
    GroovyTemplate template = new GroovyTemplate("<%def y = 'w'%>${x}${y}");
    assertEquals("vw", template.render(BINDING));
  }

  public void testExpressionAfterTextKeepsItsSemicolon() throws Exception {
    GroovyTemplate template = new GroovyTemplate("a${x}");
    assertEquals("av", template.render(BINDING));
    assertTrue(template.getGroovy(), template.getGroovy().contains(";out.print(\"${"));
  }

  private void assertRendersWithoutLeadingSemicolon(String text, String expected) throws Exception {
    GroovyTemplate template = new GroovyTemplate(text);
    assertEquals(expected, template.render(BINDING));
    String code = template.getGroovy().replaceAll("(?s)/\\*.*?\\*/|//[^\\n]*", "").stripLeading();
    assertFalse(template.getGroovy(), code.startsWith(";"));
  }
}
