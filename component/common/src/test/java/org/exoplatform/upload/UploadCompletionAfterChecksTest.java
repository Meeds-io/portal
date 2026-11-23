/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.container.xml.PortalContainerInfo;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * An upload is reported complete, and its file is readable at its store
 * location, only once every {@link UploadFileValidator} has passed.
 */
public class UploadCompletionAfterChecksTest {

  private static final String      BOUNDARY = "----uploadCompletionBoundary";

  private UploadService            uploadService;

  private UploadFileValidator      validator;

  private final String             id1      = "completeId1" + UUID.randomUUID();

  private final String             id2      = "completeId2" + UUID.randomUUID();

  private final String             id3      = "completeId3" + UUID.randomUUID();

  private final String             id4      = "completeId4" + UUID.randomUUID();

  private final String             id5      = "completeId5" + UUID.randomUUID();

  private final String             id6      = "completeId6" + UUID.randomUUID();

  @BeforeEach
  public void setUp() throws Exception {
    PortalContainerInfo containerInfo = mock(PortalContainerInfo.class);
    when(containerInfo.getContainerName()).thenReturn("uploadCompletionAfterChecksTest");
    validator = mock(UploadFileValidator.class);
    when(validator.supports(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
    uploadService = new UploadService(containerInfo, null) {
      @Override
      List<UploadFileValidator> getUploadFileValidators() {
        return List.of(validator);
      }
    };
  }

  @Test
  public void resourceIsNotCompleteNorReadableWhileValidatorsRun() throws Exception {
    AtomicReference<Integer> statusDuringChecks = new AtomicReference<>();
    AtomicReference<Boolean> readableDuringChecks = new AtomicReference<>();
    AtomicReference<Boolean> fullyCountedDuringChecks = new AtomicReference<>();
    org.mockito.Mockito.doAnswer(invocation -> {
      UploadResource resource = uploadService.getUploadResource(id1);
      statusDuringChecks.set(resource.getStatus());
      readableDuringChecks.set(new File(resource.getStoreLocation()).exists());
      fullyCountedDuringChecks.set(resource.getUploadedSize() >= resource.getEstimatedSize());
      InputStream checked = invocation.getArgument(2);
      assertEquals("hello", new String(checked.readAllBytes(), StandardCharsets.UTF_8));
      return null;
    }).when(validator).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    uploadService.createUploadResource(id1, multipartRequest());

    assertEquals(UploadResource.UPLOADING_STATUS, statusDuringChecks.get());
    assertFalse(readableDuringChecks.get());
    assertFalse(fullyCountedDuringChecks.get());
    UploadResource resource = uploadService.getUploadResource(id1);
    assertEquals(UploadResource.UPLOADED_STATUS, resource.getStatus());
    assertEquals(resource.getEstimatedSize(), resource.getUploadedSize());
    assertEquals("hello", Files.readString(Path.of(resource.getStoreLocation())));
    assertEquals(List.of(id1), storedNames(resource));
    uploadService.removeUploadResource(id1);
  }

  @Test
  public void refusedUploadEndsFailedWithNoReadableFile() throws Exception {
    AtomicReference<UploadResource> seen = new AtomicReference<>();
    org.mockito.Mockito.doAnswer(invocation -> {
      seen.set(uploadService.getUploadResource(id2));
      throw new UploadRefusedException(new FileUploadException("refused"));
    }).when(validator).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource(id2, multipartRequest()));

    UploadResource resource = seen.get();
    assertNotNull(resource);
    assertEquals(UploadResource.FAILED_STATUS, resource.getStatus());
    assertNull(uploadService.getUploadResource(id2));
    assertFalse(new File(resource.getStoreLocation()).exists());
    assertTrue(storedNames(resource).isEmpty(), "no file is left in the upload location");
  }

  @Test
  public void uploadIsCompleteWithItsLongReceivedLengthWhenTheRequestLengthExceedsAnInt() throws Exception {
    // getContentLength() is -1 above 2 GiB, the Content-length header stays exact
    uploadService.createUploadResource(id3, multipartRequest(-1));

    UploadResource resource = uploadService.getUploadResource(id3);
    assertEquals(UploadResource.UPLOADED_STATUS, resource.getStatus());
    assertEquals(multipartBytes().length, resource.getUploadedSize());
    assertEquals("hello", Files.readString(Path.of(resource.getStoreLocation())));
    uploadService.removeUploadResource(id3);
  }

  @Test
  public void uploadRemovedWhileCheckedLeavesNoFile() throws Exception {
    AtomicReference<UploadResource> seen = new AtomicReference<>();
    org.mockito.Mockito.doAnswer(invocation -> {
      seen.set(uploadService.getUploadResource(id4));
      uploadService.removeUploadResource(id4);
      return null;
    }).when(validator).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    uploadService.createUploadResource(id4, multipartRequest());

    UploadResource resource = seen.get();
    assertNotNull(resource);
    assertNull(uploadService.getUploadResource(id4));
    assertFalse(new File(resource.getStoreLocation()).exists());
    assertTrue(storedNames(resource).isEmpty(), "no file is left in the upload location");
  }

  @Test
  public void uploadReplacedWhileCheckedLeavesTheReplacementUntouched() throws Exception {
    AtomicReference<UploadResource> seen = new AtomicReference<>();
    org.mockito.Mockito.doAnswer(invocation -> {
      UploadResource original = uploadService.getUploadResource(id5);
      seen.set(original);
      UploadResource replacement = new UploadResource(id5);
      replacement.setStoreLocation(original.getStoreLocation());
      Files.writeString(Path.of(replacement.getStoreLocation()), "replacement");
      uploadService.createUploadResource(replacement);
      return null;
    }).when(validator).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    uploadService.createUploadResource(id5, multipartRequest());

    UploadResource original = seen.get();
    assertNotNull(original);
    assertEquals("replacement", Files.readString(Path.of(original.getStoreLocation())));
    assertEquals(List.of(id5), storedNames(original), "only the replacement's file is left, no staged file");
    uploadService.removeUploadResource(id5);
  }

  @Test
  public void removalWaitsForThePublicationOfACheckedUpload() throws Exception {
    AtomicBoolean removalWaited = new AtomicBoolean();
    AtomicReference<Thread> removalThread = new AtomicReference<>();
    AtomicReference<UploadResource> registered = new AtomicReference<>();
    Runnable removalAfterTheRegistryCheck = () -> {
      registered.set(uploadService.getUploadResource(id6));
      Thread removal = new Thread(() -> uploadService.removeUploadResource(id6));
      removalThread.set(removal);
      removal.start();
      try {
        removal.join(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      removalWaited.set(removal.isAlive());
    };
    java.lang.reflect.Field field = UploadService.class.getDeclaredField("uploadResources");
    field.setAccessible(true);
    field.set(uploadService, new RegistryRunningAfterTheGetOf(id6, removalAfterTheRegistryCheck));

    try {
      uploadService.createUploadResource(id6, multipartRequest());
    } finally {
      removalThread.get().join(10000);
    }
    UploadResource resource = registered.get();

    // a removal landing right after the registry check waits for the publication
    assertTrue(removalWaited.get(), "the removal must not land between the registry check and the move");
    assertNotNull(resource);
    assertEquals(UploadResource.UPLOADED_STATUS, resource.getStatus());
    assertNull(uploadService.getUploadResource(id6));
    assertFalse(new File(resource.getStoreLocation()).exists());
  }

  /**
   * A registry with the monitor semantics of
   * {@link java.util.Collections#synchronizedMap(Map)}, that runs an action
   * right after the first {@code get} of one id has returned.
   */
  private static class RegistryRunningAfterTheGetOf extends LinkedHashMap<String, UploadResource> {
    private static final long serialVersionUID = 1L;

    private final String      id;

    private final transient Runnable action;

    private final AtomicBoolean ran = new AtomicBoolean();

    RegistryRunningAfterTheGetOf(String id, Runnable action) {
      this.id = id;
      this.action = action;
    }

    @Override
    public UploadResource get(Object key) {
      UploadResource value;
      synchronized (this) {
        value = super.get(key);
      }
      if (id.equals(key) && value != null && ran.compareAndSet(false, true)) {
        action.run();
      }
      return value;
    }

    @Override
    public synchronized UploadResource put(String key, UploadResource value) {
      return super.put(key, value);
    }

    @Override
    public synchronized UploadResource putIfAbsent(String key, UploadResource value) {
      return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized boolean containsKey(Object key) {
      return super.containsKey(key);
    }

    @Override
    public synchronized UploadResource remove(Object key) {
      return super.remove(key);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
      return super.remove(key, value);
    }

    @Override
    public synchronized UploadResource compute(String key,
                                               java.util.function.BiFunction<? super String, ? super UploadResource, ? extends UploadResource> fn) {
      return super.compute(key, fn);
    }

    @Override
    public synchronized UploadResource computeIfPresent(String key,
                                                        java.util.function.BiFunction<? super String, ? super UploadResource, ? extends UploadResource> fn) {
      return super.computeIfPresent(key, fn);
    }
  }

  private static List<String> storedNames(UploadResource resource) throws IOException {
    try (var files = Files.list(Path.of(resource.getStoreLocation()).getParent())) {
      return files.map(p -> p.getFileName().toString()).filter(n -> n.startsWith(resource.getUploadId())).toList();
    }
  }

  private static HttpServletRequest multipartRequest() throws IOException {
    return multipartRequest(multipartBytes().length);
  }

  private static byte[] multipartBytes() {
    String body = "--" + BOUNDARY + "\r\n"
                  + "Content-Disposition: form-data; name=\"file\"; filename=\"file.txt\"\r\n"
                  + "Content-Type: text/plain\r\n\r\n"
                  + "hello\r\n"
                  + "--" + BOUNDARY + "--\r\n";
    return body.getBytes(StandardCharsets.UTF_8);
  }

  private static HttpServletRequest multipartRequest(int reportedContentLength) throws IOException {
    byte[] bytes = multipartBytes();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getRemoteUser()).thenReturn("john");
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(request.getMethod()).thenReturn("POST");
    when(request.getContentType()).thenReturn("multipart/form-data; boundary=" + BOUNDARY);
    when(request.getContentLength()).thenReturn(reportedContentLength);
    when(request.getContentLengthLong()).thenReturn((long) bytes.length);
    // the parser reads the length as a long from the header, getContentLength() is -1 above 2 GiB
    when(request.getHeader("Content-length")).thenReturn(String.valueOf(bytes.length));
    when(request.getInputStream()).thenReturn(new ServletInputStream() {
      private final ByteArrayInputStream delegate = new ByteArrayInputStream(bytes);

      @Override
      public int read() {
        return delegate.read();
      }

      @Override
      public boolean isFinished() {
        return delegate.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        // read synchronously
      }
    });
    return request;
  }

}
