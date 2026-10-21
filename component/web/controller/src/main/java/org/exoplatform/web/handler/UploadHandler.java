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
package org.exoplatform.web.handler;

import java.io.File;
import java.io.Writer;
import java.net.URLEncoder;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.commons.lang3.StringUtils;
import org.gatein.common.text.EntityEncoder;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.upload.UploadService.UploadLimit;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SARL Author : Nhu Dinh Thuan nhudinhthuan@exoplatform.com Dec 9, 2006
 */
public class UploadHandler extends WebRequestHandler {

    private static final String RESTRICT_PUBLIC_PARAM = "public-access-restriction";

    private static final String BEARER_PREFIX         = "Bearer ";

    private boolean             isRestrictToPublic    = false;

    public enum UploadServiceAction {
        PROGRESS, UPLOAD, DELETE, ABORT
    }

    public UploadHandler(InitParams params) {
        if (params != null) {
            ValueParam value = params.getValueParam(RESTRICT_PUBLIC_PARAM);
            if (value != null) {
                isRestrictToPublic = Boolean.parseBoolean(value.getValue().trim());
            }
        }
    }

    public String getHandlerName() {
        return "upload";
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        execute(context.getRequest(), context.getResponse());
        return true;
    }

    /**
     * Serves the upload actions. An {@code upload} action from a request without
     * an authenticated user and carrying an {@code Authorization: Bearer} header
     * is authorized by that one-time upload token alone
     * ({@link #executeTokenUpload}); every other request keeps the session-based
     * behaviour, so a token never authorizes the {@code progress},
     * {@code delete} or {@code abort} actions, and a logged-in user's request is
     * never handled by the token path.
     *
     * @param req the request
     * @param res the response
     * @throws Exception when writing the response fails
     */
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String bearerToken = req.getRemoteUser() == null ? getBearerToken(req) : null;
        if (bearerToken != null && UploadServiceAction.UPLOAD.name().equalsIgnoreCase(req.getParameter("action"))) {
            executeTokenUpload(req, res, bearerToken);
            return;
        }

        if (isRestrictToPublic && req.getRemoteUser() == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = req.getParameter("action");
        String[] uploadIds = req.getParameterValues("uploadId");

        res.setHeader("Cache-Control", "no-cache");

        UploadService service = ExoContainerContext.getService(UploadService.class);
        if (action == null || action.length() < 1)
            return;

        UploadServiceAction uploadActionService = UploadServiceAction.valueOf(StringUtils.upperCase(action));
        if (uploadActionService == UploadServiceAction.PROGRESS) {
            Writer writer = res.getWriter();
            if (uploadIds == null)
                return;
            StringBuilder value = new StringBuilder();
            value.append("{\n  upload : {");
            for (int i = 0; i < uploadIds.length; i++) {
                UploadResource upResource = service.getUploadResource(uploadIds[i]);
                if (upResource == null)
                    continue;
                if (upResource.getStatus() == UploadResource.FAILED_STATUS) {
                    UploadLimit limit = service.getUploadLimits().get(uploadIds[i]);
                    value.append("\n    \"").append(uploadIds[i]).append("\": {");
                    value.append("\n      \"status\":").append('\"').append("failed").append("\",");
                    value.append("\n      \"size\":").append('\"').append(limit.getLimit()).append("\",");
                    value.append("\n      \"unit\":").append('\"').append(limit.getUnit()).append("\"");
                    value.append("\n    }");
                    continue;
                }
                double percent = 100;
                if (upResource.getStatus() == UploadResource.UPLOADING_STATUS) {
                    percent = (upResource.getUploadedSize() * 100) / upResource.getEstimatedSize();
                }
                value.append("\n    \"").append(uploadIds[i]).append("\": {");
                value.append("\n      \"percent\":").append('\"').append((int) percent).append("\",");
                String fileName = EntityEncoder.FULL.encode(upResource.getFileName());
                value.append("\n      \"fileName\":").append('\"').append(encodeName(fileName)).append("\"");
                value.append("\n    }");
                if (i < uploadIds.length - 1)
                    value.append(',');
            }
            value.append("\n  }\n}");
            writer.append(value);
        } else if (uploadActionService == UploadServiceAction.UPLOAD) {
          try {
            service.createUploadResource(req);
          } catch (FileUploadException e) {
            if (!res.isCommitted()) {
              res.resetBuffer();
              res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
              res.setContentType("text/plain");
              res.setCharacterEncoding("UTF-8");

              String message = StringUtils.defaultIfBlank(e.getMessage(), "Upload failed");
              res.getWriter().write(message);
            }
          }
        } else if (uploadActionService == UploadServiceAction.DELETE) {
            service.removeUploadResource(uploadIds[0]);
        } else if (uploadActionService == UploadServiceAction.ABORT) {
            service.removeUploadResource(uploadIds[0]);
        }
    }

    /**
     * Registers the request's file under its {@code uploadId} parameter, as the
     * user the one-time upload token was issued for. Answers 200 with
     * {@code {"status":"uploaded","size":<bytes>}}, 401 for an unknown,
     * expired, already used or mismatched token, 411 without a
     * {@code Content-Length}, 413 above the token's size, 409 for an upload id
     * already registered, and 400 when the request is not one multipart file or
     * a validator refuses it.
     *
     * @param req the request
     * @param res the response
     * @param token the token taken from the {@code Authorization} header
     * @throws Exception when writing the response fails
     */
    private void executeTokenUpload(HttpServletRequest req, HttpServletResponse res, String token) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        UploadService service = ExoContainerContext.getService(UploadService.class);
        try {
            UploadResource upResource = service.createUploadResourceWithToken(token, req.getParameter("uploadId"), req);
            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"status\":\"uploaded\",\"size\":" + new File(upResource.getStoreLocation()).length() + "}");
        } catch (IllegalAccessException e) {
            res.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "Invalid, expired or already used upload token");
        } catch (ObjectAlreadyExistsException e) {
            writeError(res, HttpServletResponse.SC_CONFLICT, "Upload id already in use");
        } catch (FileUploadSizeException e) {
            if (e.getActualSize() < 0) {
                writeError(res, HttpServletResponse.SC_LENGTH_REQUIRED, e.getMessage());
            } else {
                writeError(res, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, e.getMessage() + ": " + e.getActualSize()
                    + " bytes, " + e.getPermitted() + " permitted");
            }
        } catch (FileUploadException e) {
            writeError(res, HttpServletResponse.SC_BAD_REQUEST, StringUtils.defaultIfBlank(e.getMessage(), "Upload failed"));
        }
    }

    /**
     * Writes a plain-text error response, unless the response is already
     * committed.
     *
     * @param res the response
     * @param status the HTTP status
     * @param message the message, for the client to diagnose the refusal
     * @throws Exception when writing the response fails
     */
    private void writeError(HttpServletResponse res, int status, String message) throws Exception {
        if (!res.isCommitted()) {
            res.resetBuffer();
            res.setStatus(status);
            res.setContentType("text/plain");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(message);
        }
    }

    /**
     * Reads a bearer token from the {@code Authorization} header.
     *
     * @param req the request
     * @return the token, or null when the header is absent, of another scheme,
     *         or empty
     */
    private String getBearerToken(HttpServletRequest req) {
        String authorization = req.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return false;
    }

    public String encodeName(String name) throws Exception {
        String[] arr = name.split(" ");
        String str = "";
        for (int i = 0; i < arr.length; i++) {
            str += " " + URLEncoder.encode(arr[i], "UTF-8");
        }
        return str;
    }

}
