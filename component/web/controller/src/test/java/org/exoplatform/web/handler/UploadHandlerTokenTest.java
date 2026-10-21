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
package org.exoplatform.web.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.upload.SvgUploadValidator;
import org.exoplatform.upload.UploadFileValidator;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Pins the token path of {@link UploadHandler} against a real
 * {@link UploadService}, and that the session-based behaviour of every other
 * request is unchanged.
 */
public class UploadHandlerTokenTest {

  private static final String               UNSAFE_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>";

  private MockedStatic<ExoContainerContext> containerContext;

  private UploadService                     uploadService;

  private UploadHandler                     handler;

  private StringWriter                      responseBody;

  private HttpServletResponse               response;

  private Map<String, String>               responseHeaders;

  private int[]                             status;

  /**
   * Builds a real upload service, served by a mocked container that also
   * provides the SVG validator, and a handler restricted to authenticated
   * users as configured by default.
   */
  @Before
  public void setUp() throws Exception {
    PortalContainerInfo containerInfo = mock(PortalContainerInfo.class);
    when(containerInfo.getContainerName()).thenReturn("portal");
    uploadService = new UploadService(containerInfo, null);
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstancesOfType(UploadFileValidator.class)).thenReturn(List.of(new SvgUploadValidator()));
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(UploadService.class)).thenReturn(uploadService);
    containerContext.when(ExoContainerContext::getCurrentContainer).thenReturn(container);

    InitParams params = new InitParams();
    ValueParam restriction = new ValueParam();
    restriction.setName("public-access-restriction");
    restriction.setValue("true");
    params.addParameter(restriction);
    handler = new UploadHandler(params);

    responseBody = new StringWriter();
    responseHeaders = new HashMap<>();
    status = new int[] { HttpServletResponse.SC_OK };
    response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(responseBody, true));
    doAnswer(invocation -> status[0] = invocation.getArgument(0, Integer.class)).when(response).setStatus(anyInt());
    doAnswer(invocation -> status[0] = invocation.getArgument(0, Integer.class)).when(response).sendError(anyInt());
    doAnswer(invocation -> responseHeaders.put(invocation.getArgument(0), invocation.getArgument(1))).when(response)
                                                                                                     .setHeader(anyString(),
                                                                                                                anyString());
  }

  /**
   * Releases the static mock.
   */
  @After
  public void tearDown() {
    containerContext.close();
  }

  /**
   * A request without session carrying a valid token uploads as the token's
   * user and answers the stored size.
   */
  @Test
  public void testTokenUploadIsAcceptedAsTokenUser() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);

    handler.execute(request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_OK, status[0]);
    assertEquals("{\"status\":\"uploaded\",\"size\":5}", responseBody.toString());
    assertEquals("no-store", responseHeaders.get("Cache-Control"));
    UploadResource upResource = uploadService.getUploadResource(uploadId, "john");
    assertNotNull(upResource);
    assertEquals("john", upResource.getOwner());
  }

  /**
   * An unknown token answers 401 with a bearer challenge, and registers
   * nothing.
   */
  @Test
  public void testInvalidTokenIsUnauthorized() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    uploadService.createUploadToken("john", uploadId, 100);

    handler.execute(request("upload", uploadId, "Bearer wrong", null, "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status[0]);
    assertEquals("Bearer error=\"invalid_token\"", responseHeaders.get("WWW-Authenticate"));
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A reused token answers 401.
   */
  @Test
  public void testReusedTokenIsUnauthorized() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    handler.execute(request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello"), response);
    uploadService.removeUploadResource(uploadId);

    handler.execute(request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status[0]);
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A token presented for another upload id answers 401.
   */
  @Test
  public void testUploadIdMismatchIsUnauthorized() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String otherId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);

    handler.execute(request("upload", otherId, "Bearer " + token, null, "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status[0]);
    assertNull(uploadService.getUploadResource(otherId));
  }

  /**
   * A file over the token's size answers 413 and is removed.
   */
  @Test
  public void testFileOverDeclaredSizeIsTooLarge() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 4);

    handler.execute(request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, status[0]);
    assertTrue(responseBody.toString().contains("5 bytes, 4 permitted"));
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A request without Content-Length answers 411.
   */
  @Test
  public void testMissingContentLengthIsLengthRequired() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    HttpServletRequest request = request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello");
    when(request.getContentLengthLong()).thenReturn(-1L);
    when(request.getContentLength()).thenReturn(-1);

    handler.execute(request, response);

    assertEquals(HttpServletResponse.SC_LENGTH_REQUIRED, status[0]);
  }

  /**
   * A token upload on an id already registered answers 409 and leaves the
   * registered resource in place.
   */
  @Test
  public void testRegisteredIdIsConflict() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    UploadResource existing = new UploadResource(uploadId, "existing.txt");
    uploadService.createUploadResource(existing);

    handler.execute(request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_CONFLICT, status[0]);
    assertEquals(existing, uploadService.getUploadResource(uploadId));
  }

  /**
   * A file a validator refuses answers 400 with the validator's message.
   */
  @Test
  public void testValidatorRefusalIsBadRequest() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 1000);

    handler.execute(request("upload", uploadId, "Bearer " + token, null, "x.svg", UNSAFE_SVG), response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, status[0]);
    assertEquals(SvgUploadValidator.INFECTED_SVG_MESSAGE, responseBody.toString());
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * Without a token and without a user, an upload is still refused with 403.
   */
  @Test
  public void testNoTokenNoUserIsForbidden() throws Exception {
    String uploadId = UUID.randomUUID().toString();

    handler.execute(request("upload", uploadId, null, null, "a.txt", "hello"), response);

    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    assertNull(uploadService.getUploadResource(uploadId));
  }

  /**
   * A header of another scheme does not reach the token path.
   */
  @Test
  public void testBasicAuthorizationIsForbidden() throws Exception {
    String uploadId = UUID.randomUUID().toString();

    handler.execute(request("upload", uploadId, "Basic am9objpwYXNz", null, "a.txt", "hello"), response);

    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  /**
   * A token does not authorize the delete, abort or progress actions: they keep
   * the session rule, and the resource stays.
   */
  @Test
  public void testTokenDoesNotAuthorizeOtherActions() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);
    handler.execute(request("upload", uploadId, "Bearer " + token, null, "a.txt", "hello"), response);
    String otherToken = uploadService.createUploadToken("john", UUID.randomUUID().toString(), 100);

    for (String action : new String[] { "delete", "abort", "progress" }) {
      HttpServletResponse actionResponse = mock(HttpServletResponse.class);
      handler.execute(request(action, uploadId, "Bearer " + otherToken, null, null, null), actionResponse);
      verify(actionResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
      verify(actionResponse, never()).getWriter();
    }
    assertNotNull(uploadService.getUploadResource(uploadId, "john"));
  }

  /**
   * A logged-in user's upload stays on the session path even with a bearer
   * header: owned by the session user, the token untouched.
   */
  @Test
  public void testLoggedInUploadIgnoresBearerHeader() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    String token = uploadService.createUploadToken("john", uploadId, 100);

    handler.execute(request("upload", uploadId, "Bearer " + token, "mary", "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_OK, status[0]);
    assertEquals("", responseBody.toString());
    assertEquals("mary", uploadService.getUploadResource(uploadId).getOwner());
  }

  /**
   * A logged-in browser upload keeps today's behaviour: empty 200 answer, and
   * the session user recorded as owner.
   */
  @Test
  public void testSessionUploadIsUnchanged() throws Exception {
    String uploadId = UUID.randomUUID().toString();

    handler.execute(request("upload", uploadId, null, "mary", "a.txt", "hello"), response);

    assertEquals(HttpServletResponse.SC_OK, status[0]);
    assertEquals("", responseBody.toString());
    assertEquals("mary", uploadService.getUploadResource(uploadId).getOwner());
    handler.execute(request("delete", uploadId, null, "paul", null, null), mock(HttpServletResponse.class));
    assertNull("the browser delete keeps answering any authenticated user", uploadService.getUploadResource(uploadId));
  }

  /**
   * Builds a mocked request to the upload handler.
   *
   * @param action action parameter
   * @param uploadId uploadId parameter
   * @param authorization Authorization header, or null
   * @param remoteUser authenticated user, or null
   * @param fileName file name of a multipart body, or null for no body
   * @param content file content
   * @return the request
   */
  private HttpServletRequest request(String action,
                                     String uploadId,
                                     String authorization,
                                     String remoteUser,
                                     String fileName,
                                     String content) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("action")).thenReturn(action);
    when(request.getParameter("uploadId")).thenReturn(uploadId);
    when(request.getParameterValues("uploadId")).thenReturn(new String[] { uploadId });
    when(request.getRemoteUser()).thenReturn(remoteUser);
    Map<String, String> headers = new HashMap<>();
    if (authorization != null) {
      headers.put("authorization", authorization);
    }
    if (fileName != null) {
      String boundary = "----TestBoundary" + UUID.randomUUID().toString().replace("-", "");
      ByteArrayOutputStream body = new ByteArrayOutputStream();
      body.write(("--" + boundary + "\r\n"
          + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
          + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
      body.write(content.getBytes(StandardCharsets.UTF_8));
      body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      byte[] bytes = body.toByteArray();
      headers.put("content-type", "multipart/form-data; boundary=" + boundary);
      headers.put("content-length", String.valueOf(bytes.length));
      when(request.getMethod()).thenReturn("POST");
      when(request.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
      when(request.getContentLength()).thenReturn(bytes.length);
      when(request.getContentLengthLong()).thenReturn((long) bytes.length);
      when(request.getCharacterEncoding()).thenReturn("UTF-8");
      ByteArrayInputStream input = new ByteArrayInputStream(bytes);
      when(request.getInputStream()).thenReturn(new ServletInputStream() {
        /** {@inheritDoc} */
        @Override
        public int read() {
          return input.read();
        }

        /** {@inheritDoc} */
        @Override
        public int read(byte[] b, int off, int len) {
          return input.read(b, off, len);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isReady() {
          return true;
        }

        /** {@inheritDoc} */
        @Override
        public void setReadListener(ReadListener readListener) {
          throw new UnsupportedOperationException();
        }
      });
    }
    when(request.getHeader(anyString())).thenAnswer(invocation -> headers.get(invocation.getArgument(0, String.class)
                                                                                       .toLowerCase()));
    HttpSession session = mock(HttpSession.class);
    Map<String, Object> attributes = new HashMap<>();
    when(session.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0, String.class)));
    doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).when(session)
                                                                                               .setAttribute(anyString(), any());
    when(session.getAttributeNames()).thenReturn(Collections.enumeration(attributes.keySet()));
    when(request.getSession(true)).thenReturn(session);
    when(request.getSession()).thenReturn(session);
    return request;
  }
}
