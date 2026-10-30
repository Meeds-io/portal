/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.container.xml.PortalContainerInfo;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * The registry rule of the request door: an id registered by an authenticated
 * request belongs to that user, an owner-less registered id is refused to
 * every request, and the registry decides and writes atomically.
 */
public class UploadServiceTest {

  private static final String BOUNDARY = "----uploadServiceTestBoundary";

  private UploadService       uploadService;

  @BeforeEach
  public void setUp() throws Exception {
    PortalContainerInfo containerInfo = mock(PortalContainerInfo.class);
    when(containerInfo.getContainerName()).thenReturn("uploadServiceTest");
    uploadService = new UploadService(containerInfo, null) {
      /**
       * The validators are resolved through the Kernel container, which a
       * unit test does not boot
       */
      @Override
      List<UploadFileValidator> getUploadFileValidators() {
        return Collections.emptyList();
      }
    };
  }

  @Test
  public void anAuthenticatedRequestRegistersTheIdUnderItsUser() throws Exception {
    uploadService.createUploadResource("id1", multipartRequest("john"));

    UploadResource resource = uploadService.getUploadResource("id1");
    assertNotNull(resource);
    assertEquals("john", resource.getOwner());
    assertEquals(UploadResource.UPLOADED_STATUS, resource.getStatus());
    assertEquals("file.txt", resource.getFileName());
    assertTrue(new File(resource.getStoreLocation()).exists());
  }

  @Test
  public void anotherUserIsRefusedAnIdHeldByAUser() throws Exception {
    uploadService.createUploadResource("id1", multipartRequest("john"));

    HttpServletRequest marysRequest = multipartRequest("mary");
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource("id1", marysRequest));
    assertEquals("john", uploadService.getUploadResource("id1").getOwner());
  }

  @Test
  public void theSameUserRegistersTheIdAgainFromAnotherSession() throws Exception {
    uploadService.createUploadResource("id1", multipartRequest("john"));
    UploadResource first = uploadService.getUploadResource("id1");

    uploadService.createUploadResource("id1", multipartRequest("john"));

    UploadResource second = uploadService.getUploadResource("id1");
    assertNotNull(second);
    assertNotSame(first, second);
    assertEquals("john", second.getOwner());
  }

  @Test
  public void anAnonymousRequestRegistersAnOwnerLessIdAndCannotRegisterItAgain() throws Exception {
    uploadService.createUploadResource("id1", multipartRequest(null));
    assertNull(uploadService.getUploadResource("id1").getOwner());

    HttpServletRequest again = multipartRequest(null);
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource("id1", again));
    HttpServletRequest johnsRequest = multipartRequest("john");
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource("id1", johnsRequest));
  }

  @Test
  public void aServerSideIdIsRefusedToEveryRequest() throws Exception {
    UploadResource staged = new UploadResource("staged");
    staged.setStatus(UploadResource.UPLOADED_STATUS);
    uploadService.createUploadResource(staged);

    HttpServletRequest johnsRequest = multipartRequest("john");
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource("staged", johnsRequest));
    HttpServletRequest anonymousRequest = multipartRequest(null);
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource("staged", anonymousRequest));
    assertSame(staged, uploadService.getUploadResource("staged"));
  }

  @Test
  public void aMalformedIdIsRefusedBeforeAnyRegistration() throws Exception {
    HttpServletRequest johnsRequest = multipartRequest("john");
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource("../id", johnsRequest));
    assertThrows(FileUploadException.class, () -> uploadService.createUploadResource(null, johnsRequest));
    assertNull(uploadService.getUploadResource("../id"));
  }

  @Test
  public void removalAsTheRequestUserRefusesAnotherUsersIdAndRemovesTheOthers() throws Exception {
    uploadService.createUploadResource("johns", multipartRequest("john"));
    uploadService.createUploadResource("anonymous", multipartRequest(null));
    File johnsFile = new File(uploadService.getUploadResource("johns").getStoreLocation());

    HttpServletRequest marysRequest = multipartRequest("mary");
    assertThrows(FileUploadException.class, () -> uploadService.removeUploadResource("johns", marysRequest));
    assertNotNull(uploadService.getUploadResource("johns"));
    assertTrue(johnsFile.exists());

    uploadService.removeUploadResource("anonymous", marysRequest);
    assertNull(uploadService.getUploadResource("anonymous"));

    uploadService.removeUploadResource("johns", multipartRequest("john"));
    assertNull(uploadService.getUploadResource("johns"));
    assertTrue(!johnsFile.exists());

    uploadService.removeUploadResource("missing", marysRequest);
    uploadService.removeUploadResource(null, marysRequest);
  }

  @Test
  public void theServerSideRemovalKeepsRemovingAnyId() throws Exception {
    uploadService.createUploadResource("johns", multipartRequest("john"));
    uploadService.removeUploadResource("johns");
    assertNull(uploadService.getUploadResource("johns"));
    uploadService.removeUploadResource(null);
  }

  @Test
  public void aNullIdIsNeverAKey() {
    assertNull(uploadService.getUploadResource(null));
    uploadService.addUploadLimit(null, 5);
    uploadService.addUploadLimit(null, 5, UploadService.UploadUnit.KB);
    uploadService.removeUploadLimit(null);
    assertTrue(uploadService.getUploadLimits().isEmpty());
  }

  /**
   * Fails on a plain map, whose concurrent puts lose registrations
   */
  @Test
  public void concurrentRegistrationsOfDistinctIdsAllSurvive() throws Exception {
    int threads = 32;
    int perThread = 200;
    for (int round = 0; round < 5; round++) {
      String prefix = "r" + round + "-";
      CyclicBarrier barrier = new CyclicBarrier(threads);
      CountDownLatch done = new CountDownLatch(threads);
      List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
      for (int t = 0; t < threads; t++) {
        int thread = t;
        new Thread(() -> {
          try {
            barrier.await();
            for (int i = 0; i < perThread; i++) {
              UploadResource resource = new UploadResource(prefix + thread + "-" + i);
              uploadService.createUploadResource(resource);
            }
          } catch (Throwable e) {
            failures.add(e);
          } finally {
            done.countDown();
          }
        }).start();
      }
      done.await();
      assertTrue(failures.isEmpty(), failures.toString());
      for (int t = 0; t < threads; t++) {
        for (int i = 0; i < perThread; i++) {
          assertNotNull(uploadService.getUploadResource(prefix + t + "-" + i), prefix + t + "-" + i);
        }
      }
    }
  }

  /**
   * Fails on a registry that checks then puts in two steps
   */
  @Test
  public void concurrentRegistrationsOfOneIdByTwoUsersLeaveExactlyOne() throws Exception {
    for (int round = 0; round < 200; round++) {
      String id = "shared" + round;
      CyclicBarrier barrier = new CyclicBarrier(2);
      CountDownLatch done = new CountDownLatch(2);
      AtomicInteger accepted = new AtomicInteger();
      AtomicInteger refused = new AtomicInteger();
      for (String user : new String[] { "john", "mary" }) {
        new Thread(() -> {
          try {
            barrier.await();
            uploadService.createUploadResource(id, multipartRequest(user));
            accepted.incrementAndGet();
          } catch (FileUploadException e) {
            refused.incrementAndGet();
          } catch (Exception e) {
            throw new IllegalStateException(e);
          } finally {
            done.countDown();
          }
        }).start();
      }
      done.await();
      assertEquals(1, accepted.get(), id);
      assertEquals(1, refused.get(), id);
      assertNotNull(uploadService.getUploadResource(id));
    }
  }

  private static HttpServletRequest multipartRequest(String username) throws IOException {
    String body = "--" + BOUNDARY + "\r\n"
                  + "Content-Disposition: form-data; name=\"file\"; filename=\"file.txt\"\r\n"
                  + "Content-Type: text/plain\r\n\r\n"
                  + "hello\r\n"
                  + "--" + BOUNDARY + "--\r\n";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getRemoteUser()).thenReturn(username);
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(request.getMethod()).thenReturn("POST");
    when(request.getContentType()).thenReturn("multipart/form-data; boundary=" + BOUNDARY);
    when(request.getContentLength()).thenReturn(bytes.length);
    when(request.getContentLengthLong()).thenReturn((long) bytes.length);
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
        // the test reads the stream synchronously
      }
    });
    return request;
  }

}
