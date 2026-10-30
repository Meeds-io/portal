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
package org.exoplatform.web.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.upload.UploadService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The delete and abort actions remove an upload as the request's user, and
 * answer a refusal with 400 as the upload action does
 */
public class UploadHandlerTest {

  private MockedStatic<ExoContainerContext> containerContext;

  private UploadService                     uploadService;

  private HttpServletRequest                request;

  private HttpServletResponse               response;

  private StringWriter                      body;

  @BeforeEach
  public void setUp() throws Exception {
    uploadService = mock(UploadService.class);
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(UploadService.class)).thenReturn(uploadService);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    body = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(body));
    when(request.getParameterValues("uploadId")).thenReturn(new String[] { "id1" });
  }

  @AfterEach
  public void tearDown() {
    containerContext.close();
  }

  @Test
  public void deleteRemovesTheUploadAsTheRequestUser() throws Exception {
    when(request.getParameter("action")).thenReturn("delete");

    new UploadHandler(null).execute(request, response);

    verify(uploadService).removeUploadResource("id1", request);
    verify(uploadService, never()).removeUploadResource(anyString());
    verify(response, never()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void abortRemovesTheUploadAsTheRequestUser() throws Exception {
    when(request.getParameter("action")).thenReturn("abort");

    new UploadHandler(null).execute(request, response);

    verify(uploadService).removeUploadResource("id1", request);
    verify(uploadService, never()).removeUploadResource(anyString());
  }

  @Test
  public void aRefusedRemovalAnswers400WithTheReason() throws Exception {
    when(request.getParameter("action")).thenReturn("delete");
    doThrow(new FileUploadException("Upload id id1 is held by another user")).when(uploadService)
                                                                              .removeUploadResource("id1", request);

    new UploadHandler(null).execute(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    body.flush();
    assertEquals("Upload id id1 is held by another user", body.toString());
  }

}
