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
package io.meeds.portal.theme.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.portal.theme.model.ImageBytes;

/**
 * The per-node cache of branding image bytes, keyed by file id.
 */
public class ThemeFileStorageTest {

  private static final byte[] PNG         = { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4 };

  private final FileService   fileService = mock(FileService.class);

  private final ThemeFileStorage storage  = new ThemeFileStorage();

  @BeforeEach
  void setUp() throws Exception {
    ReflectionTestUtils.setField(storage, "fileService", fileService);
    when(fileService.getFile(anyLong())).thenAnswer(i -> file(i.getArgument(0), ThemeFileStorage.FILE_NAME_SPACE));
  }

  @Test
  void readingAnUploadLeavesItForTheResendOfARefusedPublication() throws Exception {
    UploadService uploadService = mock(UploadService.class);
    ReflectionTestUtils.setField(storage, "uploadService", uploadService);
    Path stored = Files.createTempFile("branding-upload", ".png");
    try {
      Files.write(stored, PNG);
      UploadResource resource = new UploadResource("up-1", "logo.png");
      resource.setStoreLocation(stored.toString());
      when(uploadService.getUploadResource("up-1")).thenReturn(resource);

      assertArrayEquals(PNG, storage.readUpload("up-1"));
      assertArrayEquals(PNG, storage.readUpload("up-1"));
      verify(uploadService, never()).removeUploadResource("up-1");

      storage.removeUpload("up-1");
      verify(uploadService).removeUploadResource("up-1");
    } finally {
      Files.deleteIfExists(stored);
    }
  }

  @Test
  void anImageIsReadOnceThenServedFromTheCacheWithItsDetectedType() throws Exception {
    ImageBytes image = storage.readImage(5L);
    assertEquals("image/png", image.type());
    assertTrue(image.updatedDate() > 0, "the file's update date travels with its cached bytes");
    storage.readImage(5L);
    verify(fileService, times(1)).getFile(5L);
  }

  @Test
  void aFileOutsideTheBrandingNamespaceIsNeverServed() throws Exception {
    when(fileService.getFile(9L)).thenReturn(file(9L, "attachments"));
    assertNull(storage.readImage(9L));
  }

  @Test
  void aDeletedFileLeavesTheCacheAtOnce() throws Exception {
    storage.readImage(5L);
    storage.deleteFile(5L);
    storage.readImage(5L);
    verify(fileService, times(2)).getFile(5L);
  }

  @Test
  void theCacheIsBoundedInBytesLeastRecentlyUsedFirst() throws Exception {
    ReflectionTestUtils.setField(storage, "imageCacheMaxBytes", (long) (2 * PNG.length));
    storage.readImage(1L);
    storage.readImage(2L);
    storage.readImage(1L);
    storage.readImage(3L);
    storage.readImage(1L);
    verify(fileService, times(1)).getFile(1L);
    storage.readImage(2L);
    verify(fileService, times(2)).getFile(2L);
  }

  @Test
  void refreshingAnExpiredEntryNeverInflatesTheCountedBytes() throws Exception {
    ReflectionTestUtils.setField(storage, "imageCacheMaxBytes", (long) (2 * PNG.length));
    long[] now = { 0 };
    storage.clock = () -> now[0];
    storage.readImage(1L);
    storage.readImage(2L);
    for (int i = 1; i <= 10; i++) {
      now[0] = i * 61_000L;
      storage.readImage(1L);
    }
    storage.readImage(2L);
    // image 2 expired too, but it was never evicted for room: one read more than its first, after the expiry
    verify(fileService, times(2)).getFile(2L);
    long bytes = (long) ReflectionTestUtils.getField(storage, "cachedBytes");
    assertEquals(2L * PNG.length, bytes);
  }

  private static FileItem file(long id, String nameSpace) throws Exception {
    return new FileItem(id, "image", "image/png", nameSpace, PNG.length, new Date(), "root", false, new ByteArrayInputStream(PNG));
  }

}
