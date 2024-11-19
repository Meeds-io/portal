package org.exoplatform.commons.file.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.resource.BinaryProvider;
import org.exoplatform.commons.file.resource.FileSystemResourceProvider;

public class FileSystemBinaryProviderTest {

  @Rule
  public TemporaryFolder         folder    = new TemporaryFolder();

  @Test
  public void shouldWriteBinary() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());
    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream(new byte[] {}));
    file.getFileInfo().setChecksum("checksum");
    fileResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());

    // Then
    java.io.File createdFile = fileResourceProvider.getFile(file.getFileInfo().getChecksum());
    assertTrue(createdFile.exists());
  }

  @Test
  public void shouldWriteBinaryWhenFileAlreadyExistsAndBinaryHasChanged() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    file.getFileInfo().setChecksum("checksum");
    fileResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());
    java.io.File createdFile = fileResourceProvider.getFile(file.getFileInfo().getChecksum());
    assertTrue(createdFile.exists());
    file.getFileInfo().setChecksum(String.valueOf("test-updated".hashCode()));
    fileResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());

    // Then
    java.io.File updatedFile = fileResourceProvider.getFile(file.getFileInfo().getChecksum());
    assertNotEquals(updatedFile.getAbsolutePath(), createdFile.getAbsolutePath());
  }

  @Test
  public void shouldNotWriteBinaryWhenFileAlreadyExistsAndBinaryHasNotChanged() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    file.getFileInfo().setChecksum("checksum");
    fileResourceProvider.put(file);
    java.io.File createdFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertTrue(createdFile.exists());
    fileResourceProvider.put(file);

    // Then
    java.io.File updatedFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertEquals(updatedFile.getAbsolutePath(), createdFile.getAbsolutePath());
    // TODO need to verify also that it does not effectively write
    // verify(fileInfoDataStorage, times(1)).update(any(FileInfoEntity.class));
  }

  @Test
  public void shouldDeleteBinary() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    file.getFileInfo().setChecksum("checksum");
    fileResourceProvider.put(file);
    java.io.File createdFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertTrue(createdFile.exists());
    fileResourceProvider.remove(file.getFileInfo());

    // Then
    java.io.File deletedFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertFalse(deletedFile.exists());
  }

  @Test
  public void shouldReturnPathWhenChecksumIsValid() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileInfo fileInfo = new FileInfo(1L, "file1", "", null, 1, null, "", "d41d8cd98f00b204e9800998ecf8427e", false);
    String path = fileResourceProvider.getFilePath(fileInfo);

    // Then
    assertEquals(folder.getRoot().getPath() + "/d/4/1/d/8/c/d/9/d41d8cd98f00b204e9800998ecf8427e", path);
  }

  @Test
  public void shouldReturnNullWhenChecksumIsNotValid() throws Exception {
    // Given
    BinaryProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileInfo fileInfo = new FileInfo(1L, "file1", "", null, 1, null, "", "", false);
    String path = fileResourceProvider.getFilePath(fileInfo);

    // Then
    assertNull(path);
  }
}
