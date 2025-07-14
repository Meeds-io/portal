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

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.NameSpace;
import org.exoplatform.commons.file.resource.FileSystemResourceProvider;
import org.exoplatform.commons.file.resource.BinaryProvider;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.impl.FileServiceImpl;
import org.exoplatform.commons.file.storage.DataStorage;
import org.exoplatform.commons.file.storage.dao.FileInfoDAO;
import org.exoplatform.commons.file.storage.dao.NameSpaceDAO;
import org.exoplatform.commons.file.storage.entity.FileInfoEntity;
import org.exoplatform.commons.file.storage.entity.NameSpaceEntity;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.ListenerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FileServiceImplTest {

  @Rule
  public TemporaryFolder       folder = new TemporaryFolder();

  @Mock
  private FileInfoDAO          fileInfoDAO;

  @Mock
  private NameSpaceDAO         nameSpaceDAO;

  @Mock
  private DataStorage          jpaDataStorage;

  @Mock
  private NameSpaceService     nameSpaceService;

  @Mock
  private PortalContainer      portalContainer;

  @Mock
  private EntityManagerService emService;

  @Mock
  private EntityManager        em;

  @Mock
  private EntityTransaction    transaction;

  @Mock
  private ListenerService      listenerService;

  @Before
  public void setUp() throws Exception {
    // Mock service to pass the ExoTransactionalAspect
    ExoContainerContext.setCurrentContainer(portalContainer);
    when(portalContainer.getComponentInstanceOfType(EntityManagerService.class)).thenReturn(emService);
    when(emService.getEntityManager()).thenReturn(em);
    when(em.getTransaction()).thenReturn(transaction);
    when(transaction.isActive()).thenReturn(true);
  }

  @After
  public void tearDown() throws Exception {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void shouldReturnFile() throws Exception {
    BinaryProvider binaryProvider = new FileSystemResourceProvider(folder.getRoot().getAbsolutePath());
    // Given
    lenient().when(nameSpaceDAO.find(anyLong())).thenReturn(new NameSpaceEntity(1, "file", "Default NameSpace"));
    lenient().when(fileInfoDAO.find(anyLong()))
             .thenReturn(new FileInfoEntity(1,
                                            "file1",
                                            null,
                                            1,
                                            null,
                                            "",
                                            "d41d8cd98f00b204e9800998ecf8427e",
                                            false).setNameSpaceEntity(new NameSpaceEntity(1,
                                                                                          "file",
                                                                                          "Default NameSpace")));
    when(jpaDataStorage.getFileInfo(anyLong())).thenReturn(new FileInfo(1L,
                                                                        "file1",
                                                                        null,
                                                                        "file",
                                                                        1,
                                                                        null,
                                                                        "",
                                                                        "d41d8cd98f00b204e9800998ecf8427e",
                                                                        false));
    FileService fileService = new FileServiceImpl(jpaDataStorage, binaryProvider, nameSpaceService, listenerService);

    // When
    FileItem file = fileService.getFile(1);
    assertNotNull(file);
    assertEquals(1, file.getFileInfo().getId().longValue());
    assertEquals("file1", file.getFileInfo().getName());
  }

  @Test
  public void shouldWriteFile() throws Exception {
    BinaryProvider binaryProvider = new FileSystemResourceProvider(folder.getRoot().getAbsolutePath());
    // Given
    lenient().when(fileInfoDAO.create(any(FileInfoEntity.class))).thenReturn(new FileInfoEntity());
    lenient().when(jpaDataStorage.getNameSpace(any())).thenReturn(new NameSpace(null, null));
    lenient().when(jpaDataStorage.create(any(FileInfo.class), any(NameSpace.class)))
             .thenReturn(new FileInfo(1L,
                                      "file1",
                                      null,
                                      "file",
                                      1,
                                      null,
                                      "",
                                      "d41d8cd98f00b204e9800998ecf8427e",
                                      false));
    FileService fileService = new FileServiceImpl(jpaDataStorage, binaryProvider, nameSpaceService, listenerService);

    // When
    FileItem fileItem = new FileItem(null,
                                     "file1",
                                     "",
                                     null,
                                     1,
                                     new Date(),
                                     "",
                                     false,
                                     new ByteArrayInputStream("test".getBytes()));
    fileService.writeFile(fileItem);
    FileInfo fileInfo = fileItem.getFileInfo();
    fileInfo.setId(1L);
    // Then
    verify(jpaDataStorage, times(1)).create(any(FileInfo.class), any(NameSpace.class));
    verify(listenerService, times(1)).broadcast(eq("file.created"),
                                                argThat((FileInfo f) -> f.getId() != null && f.getId() > 0),
                                                eq(null));
  }

  @Test
  public void shouldUpdateFile() throws Exception {
    BinaryProvider binaryProvider = new FileSystemResourceProvider(folder.getRoot().getAbsolutePath());
    // Given
    lenient().when(fileInfoDAO.create(any(FileInfoEntity.class))).thenReturn(new FileInfoEntity());
    lenient().when(jpaDataStorage.getNameSpace(any())).thenReturn(new NameSpace(null, null));
    lenient().when(jpaDataStorage.updateFileInfo(any(FileInfo.class)))
             .thenReturn(new FileInfo(1L, "fileToUpdate", null, "file", 1, null, "", "d41d8cd98f00b204e9800998ecf8427e", false));

    lenient().when(jpaDataStorage.getFileInfo(1L))
             .thenReturn(new FileInfo(1L, "file1", null, "file", 1, null, "", "d41d8cd98f00b204e9800998ecf8427e", false));
    FileService fileService = new FileServiceImpl(jpaDataStorage, binaryProvider, nameSpaceService, listenerService);

    // When
    FileItem fileItem = new FileItem(1L,
                                     "fileToUpdate",
                                     "",
                                     null,
                                     1,
                                     new Date(),
                                     "",
                                     false,
                                     new ByteArrayInputStream("test".getBytes()));
    fileService.updateFile(fileItem);
    FileInfo fileInfo = fileItem.getFileInfo();
    fileInfo.setId(1L);
    // Then
    verify(jpaDataStorage, times(1)).updateFileInfo(any(FileInfo.class));
    verify(listenerService, times(1)).broadcast("file.updated", fileInfo, null);
  }

  @Test
  public void shouldDeleteFile() throws Exception {
    BinaryProvider binaryProvider = new FileSystemResourceProvider(folder.getRoot().getAbsolutePath());
    // Given
    FileInfo fileInfo = new FileInfo(1L, "file", null, "file", 1, null, "", "d41d8cd98f00b204e9800998ecf8427e", false);
    FileInfo updatedFileInfo = new FileInfo(1L, "file", null, "file", 1, null, "", "d41d8cd98f00b204e9800998ecf8427e", true);
    lenient().when(jpaDataStorage.updateFileInfo(any(FileInfo.class))).thenReturn(updatedFileInfo);

    lenient().when(jpaDataStorage.getFileInfo(1L))
             .thenReturn(fileInfo);
    FileService fileService = new FileServiceImpl(jpaDataStorage, binaryProvider, nameSpaceService, listenerService);

    // When
    fileService.deleteFile(1L);
    // Then
    verify(jpaDataStorage, times(1)).updateFileInfo(any(FileInfo.class));
    verify(listenerService, times(1)).broadcast("file.deleted", updatedFileInfo, null);
  }

  @Test
  public void shouldRollbackFileWriteWhenSomethingGoesWrong() throws Exception {
    BinaryProvider binaryProvider = new FileSystemResourceProvider(folder.getRoot().getAbsolutePath());
    // Given
    lenient().when(fileInfoDAO.create(any(FileInfoEntity.class))).thenThrow(RuntimeException.class);
    FileService fileService = new FileServiceImpl(jpaDataStorage, binaryProvider, nameSpaceService, listenerService);

    // When
    FileItem file = new FileItem(null,
                                 "file1",
                                 "plain/text",
                                 null,
                                 1,
                                 new Date(),
                                 "john",
                                 false,
                                 new ByteArrayInputStream("test".getBytes())) {
      @Override
      public InputStream getAsStream() {
        throw new IllegalStateException();
      }
    };
    FileItem createdFile = null;
    try {
      createdFile = fileService.writeFile(file);
    } catch (Exception e) {
      // expected exception
    }
    assertNull(createdFile);
  }
}
