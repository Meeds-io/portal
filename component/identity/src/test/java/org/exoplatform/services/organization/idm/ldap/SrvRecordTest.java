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
package org.exoplatform.services.organization.idm.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SrvRecordTest {

  @Test
  public void testParseValidRecord() {
    SrvRecord parsedRecord = SrvRecord.parse("10 60 389 dc1.example.com.");
    assertEquals(10, parsedRecord.getPriority());
    assertEquals(60, parsedRecord.getWeight());
    assertEquals(389, parsedRecord.getPort());
    assertEquals("dc1.example.com", parsedRecord.getTarget());
  }

  @Test
  public void testParseStripsTrailingDotOnly() {
    SrvRecord parsedRecord = SrvRecord.parse("0 0 636 dc2.example.com");
    assertEquals("dc2.example.com", parsedRecord.getTarget());
  }

  @Test
  public void testParseWithExtraWhitespace() {
    SrvRecord parsedRecord = SrvRecord.parse("  5   50   389   dc3.example.com.  ");
    assertEquals(5, parsedRecord.getPriority());
    assertEquals(50, parsedRecord.getWeight());
    assertEquals(389, parsedRecord.getPort());
    assertEquals("dc3.example.com", parsedRecord.getTarget());
  }

  @Test
  public void testParseInvalidTokenCount() {
    assertNull(SrvRecord.parse("10 60 389"));
    assertNull(SrvRecord.parse("10 60 389 dc1.example.com. extra"));
  }

  @Test
  public void testParseInvalidNumbers() {
    assertNull(SrvRecord.parse("abc 60 389 dc1.example.com."));
  }

  @Test
  public void testParseBlank() {
    assertNull(SrvRecord.parse(""));
    assertNull(SrvRecord.parse(null));
  }

  @Test
  public void testToUrl() {
    SrvRecord srvRecord = new SrvRecord(0, 100, 636, "dc1.example.com");
    assertEquals("ldaps://dc1.example.com:636", srvRecord.toUrl("ldaps"));
  }
}
