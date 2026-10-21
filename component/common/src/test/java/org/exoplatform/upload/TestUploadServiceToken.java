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
package org.exoplatform.upload;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.container.xml.ValueParam;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Pins the one-time upload token and the owner-checked accessors of
 * {@link UploadService}, through the real multipart parsing and the configured
 * {@link SvgUploadValidator}.
 */
@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/services/upload-service.xml")
})
public class TestUploadServiceToken extends AbstractKernelTest {

  private static final String UNSAFE_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>";

  private UploadService       uploadService;

  /**
   * Gets the container's upload service.
   */
  @Override
  protected void beforeRunBare() {
    super.beforeRunBare();
    uploadService = getContainer().getComponentInstanceOfType(UploadService.class);
  }

  /**
   * A token upload registers the file as the token's user, ties it to the
   * request's session for cleanup, and stores it on disk.
   */
  public void testTokenUploadRegistersResourceOwnedByTokenUser() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    MultipartRequest request = multipart("hello.txt", "text/plain", "hello world");

    UploadResource upResource = uploadService.createUploadResourceWithToken(token, uploadId, request.request);

    assertNotNull(upResource);
    assertEquals("john", upResource.getOwner());
    assertEquals(UploadResource.UPLOADED_STATUS, upResource.getStatus());
    assertEquals("hello.txt", upResource.getFileName());
    assertEquals(11, new File(upResource.getStoreLocation()).length());
    assertSame(upResource, uploadService.getUploadResource(uploadId, "john"));
    @SuppressWarnings("unchecked")
    Set<String> sessionIds = (Set<String>) request.sessionAttributes.get(UploadService.UPLOAD_RESOURCES_STACK);
    assertTrue(sessionIds.contains(uploadId));
    assertFalse("the per-upload limit is released after the upload", uploadService.getUploadLimits().containsKey(uploadId));
    uploadService.removeUploadResource(uploadId);
  }

  /**
   * A token is consumed by its first presentation: a second upload with it is
   * refused.
   */
  public void testTokenIsSingleUse() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    uploadService.createUploadResourceWithToken(token, uploadId, multipart("a.txt", "text/plain", "a").request);
    uploadService.removeUploadResource(uploadId);

    assertRefusedToken(token, uploadId);
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * An unknown or blank token is refused and registers nothing.
   */
  public void testUnknownTokenIsRefused() throws Exception {
    String uploadId = newUploadId();
    uploadService.createUploadToken("john", uploadId, 100);
    assertRefusedToken("not-a-token", uploadId);
    assertRefusedToken(" ", uploadId);
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A token presented with another upload id is refused, and burnt: it no
   * longer works for its own id either.
   */
  public void testTokenForAnotherUploadIdIsRefusedAndConsumed() throws Exception {
    String uploadId = newUploadId();
    String otherId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 100);

    assertRefusedToken(token, otherId);
    assertNull(uploadService.getUploadResource(otherId));
    assertRefusedToken(token, uploadId);
    assertRefusedToken(uploadService.createUploadToken("john", uploadId, 100), null);
  }

  /**
   * An expired token is refused.
   */
  public void testExpiredTokenIsRefused() throws Exception {
    UploadService expiringService = new UploadService(containerInfo(), validityParams(0));
    String uploadId = newUploadId();
    String token = expiringService.createUploadToken("john", uploadId, 100);
    try {
      expiringService.createUploadResourceWithToken(token, uploadId, multipart("a.txt", "text/plain", "a").request);
      fail("An expired token must be refused");
    } catch (IllegalAccessException e) {
      assertEquals(UploadService.UPLOAD_TOKEN_INVALID, e.getMessage());
    }
    assertNull(expiringService.getUploadResource(uploadId));
  }

  /**
   * A token within its validity is accepted by a service configured with a
   * validity: the expiry check compares against the configured duration.
   */
  public void testTokenWithinConfiguredValidityIsAccepted() throws Exception {
    UploadService configuredService = new UploadService(containerInfo(), validityParams(60));
    String uploadId = newUploadId();
    String token = configuredService.createUploadToken("john", uploadId, 100);
    UploadResource upResource = configuredService.createUploadResourceWithToken(token,
                                                                                 uploadId,
                                                                                 multipart("a.txt", "text/plain", "a").request);
    assertEquals("john", upResource.getOwner());
    configuredService.removeUploadResource(uploadId);
  }

  /**
   * A request whose Content-Length exceeds the declared size plus the multipart
   * allowance is refused before anything is registered.
   */
  public void testContentLengthOverDeclaredSizeIsRefused() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 10);
    MultipartRequest request = multipart("a.txt", "text/plain", "a");
    long tooLong = 10 + UploadService.UPLOAD_TOKEN_MULTIPART_ALLOWANCE + 1;
    when(request.request.getContentLengthLong()).thenReturn(tooLong);
    when(request.request.getContentLength()).thenReturn((int) tooLong);
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, request.request);
      fail("A request over the declared size must be refused");
    } catch (FileUploadSizeException e) {
      assertEquals(tooLong, e.getActualSize());
    }
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A request whose Content-Length is exactly the declared size plus the
   * allowance is not refused by the Content-Length check: the bound is
   * inclusive. The header is stubbed at the bound over a shorter body, so the
   * test only asserts that the refusal, if any, is not that check's.
   */
  public void testContentLengthAtBoundIsNotRefusedBySizeCheck() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 10);
    MultipartRequest request = multipart("a.txt", "text/plain", "a");
    long atBound = 10 + UploadService.UPLOAD_TOKEN_MULTIPART_ALLOWANCE;
    when(request.request.getContentLengthLong()).thenReturn(atBound);
    when(request.request.getContentLength()).thenReturn((int) atBound);
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, request.request);
    } catch (FileUploadSizeException e) {
      assertFalse("the Content-Length check must accept a request at its bound", e.getActualSize() == atBound);
    } catch (FileUploadException e) {
      // the shorter body may fail to parse: not the check under test
    } finally {
      uploadService.removeUploadResource(uploadId);
    }
  }

  /**
   * A file larger than the declared size is refused even when the request fits
   * in the multipart allowance, and its resource is removed.
   */
  public void testFileOverDeclaredSizeIsRefusedAndRemoved() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 10);
    MultipartRequest request = multipart("a.txt", "text/plain", "x".repeat(11));
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, request.request);
      fail("A file over the declared size must be refused");
    } catch (FileUploadSizeException e) {
      assertEquals(11, e.getActualSize());
      assertEquals(10, e.getPermitted());
    }
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A file of exactly the declared size is accepted.
   */
  public void testFileAtDeclaredSizeIsAccepted() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 10);
    UploadResource upResource = uploadService.createUploadResourceWithToken(token,
                                                                             uploadId,
                                                                             multipart("a.txt", "text/plain", "x".repeat(10)).request);
    assertEquals(10, new File(upResource.getStoreLocation()).length());
    uploadService.removeUploadResource(uploadId);
  }

  /**
   * A request without Content-Length is refused with an actual size of -1.
   */
  public void testMissingContentLengthIsRefused() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 10);
    MultipartRequest request = multipart("a.txt", "text/plain", "a");
    when(request.request.getContentLengthLong()).thenReturn(-1L);
    when(request.request.getContentLength()).thenReturn(-1);
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, request.request);
      fail("A request without Content-Length must be refused");
    } catch (FileUploadSizeException e) {
      assertEquals(-1, e.getActualSize());
    }
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A token upload never replaces a resource already registered under its id.
   */
  public void testTokenUploadOnRegisteredIdIsRefused() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    UploadResource existing = new UploadResource(uploadId, "existing.txt");
    existing.setOwner("mary");
    uploadService.createUploadResource(existing);
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, multipart("a.txt", "text/plain", "a").request);
      fail("A registered id must not be replaced");
    } catch (ObjectAlreadyExistsException e) {
      assertEquals(UploadService.UPLOAD_ID_ALREADY_IN_USE, e.getMessage());
    }
    assertSame(existing, uploadService.getUploadResource(uploadId));
    uploadService.removeUploadResource(uploadId);
  }

  /**
   * The upload validators run on the token path: a scripted SVG is refused
   * and its resource removed.
   */
  public void testValidatorsRunOnTokenUpload() throws Exception {
    String uploadId = newUploadId();
    String token = uploadService.createUploadToken("john", uploadId, 1000);
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, multipart("x.svg", "image/svg+xml", UNSAFE_SVG).request);
      fail("A scripted SVG must be refused");
    } catch (FileUploadException e) {
      assertEquals(SvgUploadValidator.INFECTED_SVG_MESSAGE, e.getMessage());
    }
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * Token issuance refuses a blank user, an invalid id, a non-positive size and
   * an id already registered.
   */
  public void testCreateUploadTokenValidatesItsArguments() {
    assertIllegalArgument(() -> uploadService.createUploadToken(null, newUploadId(), 1));
    assertIllegalArgument(() -> uploadService.createUploadToken(" ", newUploadId(), 1));
    assertIllegalArgument(() -> uploadService.createUploadToken("john", null, 1));
    assertIllegalArgument(() -> uploadService.createUploadToken("john", "", 1));
    assertIllegalArgument(() -> uploadService.createUploadToken("john", "../etc", 1));
    assertIllegalArgument(() -> uploadService.createUploadToken("john", newUploadId(), 0));
    String uploadId = newUploadId();
    uploadService.createUploadResource(new UploadResource(uploadId));
    assertIllegalArgument(() -> uploadService.createUploadToken("john", uploadId, 1));
    uploadService.removeUploadResource(uploadId);
    assertNotNull(uploadService.createUploadToken("john", newUploadId(), 1));
  }

  /**
   * Two tokens are never equal and never contain the upload id or the user.
   */
  public void testTokensAreRandomAndOpaque() {
    String uploadId = newUploadId();
    String first = uploadService.createUploadToken("john", uploadId, 1);
    String second = uploadService.createUploadToken("john", uploadId, 1);
    assertFalse(first.equals(second));
    assertFalse(first.contains(uploadId));
    assertFalse(first.contains("john"));
    assertTrue(first.length() >= 43);
  }

  /**
   * A browser upload records the request's authenticated user as owner, and
   * the owner-checked accessors answer that user only.
   */
  public void testOwnerCheckedAccessors() throws Exception {
    String uploadId = newUploadId();
    MultipartRequest request = multipart("a.txt", "text/plain", "a");
    when(request.request.getRemoteUser()).thenReturn("john");
    uploadService.createUploadResource(uploadId, request.request);

    UploadResource upResource = uploadService.getUploadResource(uploadId);
    assertEquals("john", upResource.getOwner());
    assertSame(upResource, uploadService.getUploadResource(uploadId, "john"));
    assertNull(uploadService.getUploadResource(uploadId, "mary"));
    assertNull(uploadService.getUploadResource(uploadId, null));
    assertNull(uploadService.getUploadResource(null, "john"));

    assertFalse(uploadService.removeUploadResource(uploadId, "mary"));
    assertSame(upResource, uploadService.getUploadResource(uploadId));
    assertTrue(new File(upResource.getStoreLocation()).exists());

    assertTrue(uploadService.removeUploadResource(uploadId, "john"));
    assertNull(uploadService.getUploadResource(uploadId));
    assertFalse(new File(upResource.getStoreLocation()).exists());
    assertFalse(uploadService.removeUploadResource(uploadId, "john"));
  }

  /**
   * A resource without owner (anonymous upload, server-side registration) is
   * answered by the legacy accessors only.
   */
  public void testResourceWithoutOwnerIsNotReturnedByOwnerCheckedAccessors() throws Exception {
    String uploadId = newUploadId();
    uploadService.createUploadResource(uploadId, multipart("a.txt", "text/plain", "a").request);
    assertNull(uploadService.getUploadResource(uploadId).getOwner());
    assertNull(uploadService.getUploadResource(uploadId, "john"));
    assertFalse(uploadService.removeUploadResource(uploadId, "john"));
    assertNotNull(uploadService.getUploadResource(uploadId));
    uploadService.removeUploadResource(uploadId);
  }

  /**
   * Concurrent registrations of distinct ids, each with its own limit, all
   * survive in both registries: request threads write them at the same time.
   */
  public void testConcurrentRegistrationsOfDistinctIdsAllSurvive() throws Exception {
    UploadService service = new UploadService(containerInfo(), null);
    int threads = 8;
    int idsPerThread = 2000;
    List<List<String>> ids = new ArrayList<>();
    for (int t = 0; t < threads; t++) {
      List<String> threadIds = new ArrayList<>();
      for (int i = 0; i < idsPerThread; i++) {
        threadIds.add(newUploadId());
      }
      ids.add(threadIds);
    }
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (List<String> threadIds : ids) {
        futures.add(executor.submit(() -> {
          ExoContainerContext.setCurrentContainer(getContainer());
          start.await();
          for (String id : threadIds) {
            service.addUploadLimit(id, 5, UploadService.UploadUnit.KB);
            service.createUploadResource(new UploadResource(id));
          }
          return null;
        }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }
    int lostResources = 0;
    int lostLimits = 0;
    for (List<String> threadIds : ids) {
      for (String id : threadIds) {
        if (service.getUploadResource(id) == null) {
          lostResources++;
        }
        if (service.getUploadLimits().get(id) == null) {
          lostLimits++;
        }
      }
    }
    assertEquals("lost resource registrations", 0, lostResources);
    assertEquals("lost upload limits", 0, lostLimits);
  }

  /**
   * Parallel token uploads above the service's default limit all succeed: none
   * of them loses the per-upload limit its token sets, while other uploads add
   * their own limits at the same time.
   */
  public void testParallelTokenUploadsNeverLoseTheirLimit() throws Exception {
    for (int round = 0; round < 4; round++) {
      runParallelTokenUploads();
    }
  }

  /**
   * One round of {@link #testParallelTokenUploadsNeverLoseTheirLimit()}, on a
   * fresh service whose default limit is 1 MB: 8 threads upload 1.1 MB files
   * with 2 MB tokens while 4 threads add other uploads' limits.
   */
  private void runParallelTokenUploads() throws Exception {
    InitParams params = new InitParams();
    ValueParam defaultLimit = new ValueParam();
    defaultLimit.setName("upload.limit.size");
    defaultLimit.setValue("1");
    params.addParameter(defaultLimit);
    UploadService service = new UploadService(containerInfo(), params);
    String content = "x".repeat(1100 * 1024);
    int uploaders = 8;
    int uploadsPerThread = 15;
    AtomicInteger failures = new AtomicInteger();
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch uploadsDone = new CountDownLatch(uploaders);
    ExecutorService executor = Executors.newFixedThreadPool(uploaders + 4);
    List<String> uploadedIds = java.util.Collections.synchronizedList(new ArrayList<>());
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int t = 0; t < uploaders; t++) {
        futures.add(executor.submit(() -> {
          try {
            ExoContainerContext.setCurrentContainer(getContainer());
            ExoContainerContext.setCurrentContainer(getContainer());
          start.await();
            for (int i = 0; i < uploadsPerThread; i++) {
              String uploadId = newUploadId();
              String token = service.createUploadToken("john", uploadId, 2L * 1024 * 1024);
              try {
                service.createUploadResourceWithToken(token, uploadId, multipart("big.txt", "text/plain", content).request);
                uploadedIds.add(uploadId);
              } catch (FileUploadException e) {
                failures.incrementAndGet();
              }
            }
          } finally {
            uploadsDone.countDown();
          }
          return null;
        }));
      }
      for (int t = 0; t < 4; t++) {
        futures.add(executor.submit(() -> {
          ExoContainerContext.setCurrentContainer(getContainer());
          start.await();
          int added = 0;
          while (uploadsDone.getCount() > 0 && added < 200_000) {
            // distinct ids kept in the map: every resize races the token uploads' limits
            service.addUploadLimit(newUploadId(), 1, UploadService.UploadUnit.KB);
            added++;
          }
          return null;
        }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(120, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
      for (String uploadId : uploadedIds) {
        service.removeUploadResource(uploadId);
      }
    }
    assertEquals("token uploads refused for a lost limit or registration", 0, failures.get());
    assertEquals(uploaders * uploadsPerThread, uploadedIds.size());
  }

  /**
   * Two concurrent token uploads of one id register it exactly once: one
   * succeeds, the other gets an ObjectAlreadyExistsException, and the refused
   * one does not remove the winner's resource. Repeated to give the race a
   * chance. Each worker thread binds the test's container, as a request thread
   * has its portal container, so the validators come from it.
   */
  public void testConcurrentTokenUploadsOfOneIdRegisterOnce() throws Exception {
    UploadService service = new UploadService(containerInfo(), null);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      for (int trial = 0; trial < 300; trial++) {
        String uploadId = newUploadId();
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Future<String>> futures = new ArrayList<>();
        for (String user : new String[] { "john", "mary" }) {
          String token = service.createUploadToken(user, uploadId, 100);
          MultipartRequest request = multipart("a.txt", "text/plain", user);
          futures.add(executor.submit(() -> {
            ExoContainerContext.setCurrentContainer(getContainer());
            barrier.await(10, TimeUnit.SECONDS);
            try {
              service.createUploadResourceWithToken(token, uploadId, request.request);
              return user;
            } catch (ObjectAlreadyExistsException e) {
              return "conflict";
            }
          }));
        }
        String first = futures.get(0).get(30, TimeUnit.SECONDS);
        String second = futures.get(1).get(30, TimeUnit.SECONDS);
        String winner = "conflict".equals(first) ? second : first;
        assertTrue("trial " + trial + ": exactly one success and one conflict, got " + first + " and " + second,
                   "conflict".equals(first) != "conflict".equals(second));
        UploadResource upResource = service.getUploadResource(uploadId);
        assertNotNull("trial " + trial + ": the winner's resource is kept", upResource);
        assertEquals(winner, upResource.getOwner());
        assertTrue(new File(upResource.getStoreLocation()).exists());
        service.removeUploadResource(uploadId);
      }
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * Asserts the token is refused for the upload id.
   *
   * @param token token presented
   * @param uploadId upload id presented
   */
  private void assertRefusedToken(String token, String uploadId) throws Exception {
    try {
      uploadService.createUploadResourceWithToken(token, uploadId, multipart("a.txt", "text/plain", "a").request);
      fail("The token must be refused");
    } catch (IllegalAccessException e) {
      assertEquals(UploadService.UPLOAD_TOKEN_INVALID, e.getMessage());
    }
  }

  /**
   * Asserts the call throws an {@link IllegalArgumentException}.
   *
   * @param call the call
   */
  private void assertIllegalArgument(Runnable call) {
    try {
      call.run();
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  /**
   * Generates an upload id the way a token issuer does.
   *
   * @return a random upload id
   */
  private String newUploadId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Builds a container info for a standalone service instance.
   *
   * @return a container info named "portal"
   */
  private PortalContainerInfo containerInfo() {
    PortalContainerInfo containerInfo = mock(PortalContainerInfo.class);
    when(containerInfo.getContainerName()).thenReturn("portal");
    return containerInfo;
  }

  /**
   * Builds service parameters with the given token validity.
   *
   * @param seconds token validity in seconds
   * @return the init params
   */
  private InitParams validityParams(long seconds) {
    InitParams params = new InitParams();
    ValueParam validity = new ValueParam();
    validity.setName("upload.token.validity.seconds");
    validity.setValue(String.valueOf(seconds));
    params.addParameter(validity);
    return params;
  }

  /**
   * Builds a mocked multipart request carrying one file, with a session that
   * keeps its attributes.
   *
   * @param fileName file name
   * @param contentType file content type
   * @param content file content
   * @return the request and its session attributes
   */
  private MultipartRequest multipart(String fileName, String contentType, String content) throws IOException {
    String boundary = "----TestBoundary" + UUID.randomUUID().toString().replace("-", "");
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.write(("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
        + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    body.write(content.getBytes(StandardCharsets.UTF_8));
    body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    byte[] bytes = body.toByteArray();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("POST");
    when(request.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
    when(request.getContentLength()).thenReturn(bytes.length);
    when(request.getContentLengthLong()).thenReturn((long) bytes.length);
    when(request.getCharacterEncoding()).thenReturn("UTF-8");
    when(request.getHeader(anyString())).thenAnswer(invocation -> {
      String name = invocation.getArgument(0, String.class);
      if ("content-type".equalsIgnoreCase(name)) {
        return "multipart/form-data; boundary=" + boundary;
      } else if ("content-length".equalsIgnoreCase(name)) {
        return String.valueOf(bytes.length);
      }
      return null;
    });
    when(request.getInputStream()).thenReturn(new ByteArrayServletInputStream(bytes));

    Map<String, Object> sessionAttributes = new HashMap<>();
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(anyString())).thenAnswer(invocation -> sessionAttributes.get(invocation.getArgument(0, String.class)));
    org.mockito.Mockito.doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                       .when(session)
                       .setAttribute(anyString(), org.mockito.ArgumentMatchers.any());
    when(request.getSession(true)).thenReturn(session);
    when(request.getSession()).thenReturn(session);
    return new MultipartRequest(request, sessionAttributes, bytes.length);
  }

  /** A mocked multipart request, its session's attributes and its length. */
  private record MultipartRequest(HttpServletRequest request, Map<String, Object> sessionAttributes, long length) {
  }

  /** A servlet input stream over a byte array. */
  private static class ByteArrayServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream input;

    /**
     * Wraps the bytes.
     *
     * @param bytes the request body
     */
    ByteArrayServletInputStream(byte[] bytes) {
      this.input = new ByteArrayInputStream(bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() {
      return input.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) {
      return input.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinished() {
      return input.available() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException();
    }
  }
}
