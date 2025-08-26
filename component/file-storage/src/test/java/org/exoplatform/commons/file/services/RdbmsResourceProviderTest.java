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
package org.exoplatform.commons.file.services;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;

import org.exoplatform.commons.file.CommonsJPAIntegrationTest;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.resource.BinaryProvider;
import org.exoplatform.commons.file.resource.FileUtils;
import org.exoplatform.commons.file.resource.RdbmsResourceProvider;

/**
 * Rdbms Resource Provider test class.
 */
public class RdbmsResourceProviderTest extends CommonsJPAIntegrationTest {

  @Override
  @Before
  public void setUp() {
    super.setUp();
    fileBinaryDAO.deleteAll();
  }

  @Override
  @After
  public void tearDown() {
    fileBinaryDAO.deleteAll();
    super.tearDown();
  }

  public void testWriteBinary() throws Exception {
    // Given
    RdbmsResourceProvider rdbmsResourceProvider = new RdbmsResourceProvider(fileBinaryDAO);
    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream(new byte[] {}));
    file.getFileInfo().setChecksum("checksum");
    rdbmsResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());

    // Then
    ByteArrayInputStream createdData = (ByteArrayInputStream) rdbmsResourceProvider.getStream(file.getFileInfo().getChecksum());
    assertNotNull(createdData);
  }

  public void testWriteBinaryWhenFileAlreadyExistsAndBinaryHasChanged() throws Exception {
    // Given
    RdbmsResourceProvider rdbmsResourceProvider = new RdbmsResourceProvider(fileBinaryDAO);

    // When
    FileItem file = new FileItem(1L, "file2", "", null, 1, null, "", false, new ByteArrayInputStream("test2".getBytes()));
    file.getFileInfo().setChecksum("checksum");
    rdbmsResourceProvider.put(file);
    ByteArrayInputStream createdData = (ByteArrayInputStream) rdbmsResourceProvider.getStream(file.getFileInfo().getChecksum());
    assertNotNull(createdData);

    file.setInputStream(new ByteArrayInputStream("test-updated".getBytes()));
    file.getFileInfo().setChecksum(String.valueOf("test-updated".hashCode()));
    rdbmsResourceProvider.put(file);

    // Then
    ByteArrayInputStream updateddData = (ByteArrayInputStream) rdbmsResourceProvider.getStream(file.getFileInfo().getChecksum());
    assertNotEquals(new String(FileUtils.readBytes(updateddData)), new String(FileUtils.readBytes(createdData)));
  }

  public void testFileAlreadyExistsAndBinaryHasNotChanged() throws Exception {
    // Given
    RdbmsResourceProvider rdbmsResourceProvider = new RdbmsResourceProvider(fileBinaryDAO);

    // When
    FileItem file = new FileItem(1L, "file3", "", null, 1, null, "", false, new ByteArrayInputStream("test3".getBytes()));
    file.getFileInfo().setChecksum("checksum");
    rdbmsResourceProvider.put(file);
    String created = rdbmsResourceProvider.getFilePath(file.getFileInfo());
    try {
      rdbmsResourceProvider.put(file);
      fail();
    } catch (Throwable ex) {
      // Expected
    }

    // Then
    String updated = rdbmsResourceProvider.getFilePath(file.getFileInfo());
    assertEquals(updated, created);
  }

  public void testDeleteBinary() throws Exception {
    // Given
    RdbmsResourceProvider rdbmsResourceProvider = new RdbmsResourceProvider(fileBinaryDAO);

    // When
    FileItem file = new FileItem(1L, "file4", "", null, 1, null, "", false, new ByteArrayInputStream("test4".getBytes()));
    file.getFileInfo().setChecksum("checksum");
    rdbmsResourceProvider.put(file);
    String created = rdbmsResourceProvider.getFilePath(file.getFileInfo());
    assertNotNull(created);
    rdbmsResourceProvider.remove(file.getFileInfo());

    // Then
    String deleted = rdbmsResourceProvider.getFilePath(file.getFileInfo());
    assertNull(deleted);
  }

  public void testDeletingABinaryWhichDoesNotExist() throws Exception {
    // Given
    RdbmsResourceProvider rdbmsResourceProvider = new RdbmsResourceProvider(fileBinaryDAO);

    // When
    FileItem file = new FileItem(1L, "file5", "", null, 1, null, "", false, new ByteArrayInputStream("test5".getBytes()));
    boolean deleted = rdbmsResourceProvider.remove(file.getFileInfo());

    // Then
    assertEquals(deleted, false);
  }

  public void shouldReturnNullWhenChecksumIsNotValid() throws Exception {
    // Given
    BinaryProvider binaryProvider = new RdbmsResourceProvider(fileBinaryDAO);

    // When
    FileInfo fileInfo = new FileInfo(1L, "file6", "", null, 1, null, "", "", false);
    String path = binaryProvider.getFilePath(fileInfo);

    // Then
    assertNull(path);
  }
}
