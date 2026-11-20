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
package org.exoplatform.portal.branding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;

import io.meeds.portal.mail.EmailBrand;
import io.meeds.portal.mail.EmailPreview;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.BrandingFile;
import org.exoplatform.portal.branding.model.ThemeStylesheet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/platform/branding")
@Tag(name = "/v1/platform/branding", description = "Managing branding information")
public class BrandingRestResourcesV1 implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(BrandingRestResourcesV1.class);

  private static final String IMAGE_MIME_TYPE        = "image/png";

  // 1 year
  private static final int    CACHE_IN_SECONDS       = 365 * 24 * 3600;

  private static final long   CACHE_IN_MILLI_SECONDS = CACHE_IN_SECONDS * 1000L;

  private static final Pattern REST_PATH          = Pattern.compile("/rest(?:/|$)");

  private static final Pattern LANGUAGE_TAG          = Pattern.compile("[A-Za-z]{2,3}([-_][A-Za-z0-9]{2,8}){0,3}");




  private BrandingService     brandingService;

  public BrandingRestResourcesV1(BrandingService brandingService) {
    this.brandingService = brandingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get Branding information", description = "Get Branding information", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response getBrandingInformation() {
    Branding brandingInformation = brandingService.getBrandingInformation(false);
    return Response.ok(brandingInformation).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/default")
  @RolesAllowed("administrators")
  @Operation(summary = "Get default theme style", description = "Get default theme style", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response getDefaultBrandingThemeStyle() {
    Map<String, String> defaultThemeStyle = brandingService.getDefaultThemeStyle();
    return Response.ok(defaultThemeStyle).build();
  }

  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update Branding information", description = "Update Branding information", method = "PUT")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "204", description = "Branding information updated"),
                          @ApiResponse(responseCode = "400", description = "Invalid value, the body is the message code"),
  })
  public Response updateBrandingInformation(Branding branding) {
    try {
      brandingService.updateBrandingInformation(branding);
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      // expected refusal of an invalid theme or CSS value: the message code lets the Branding UI explain it
      LOG.debug("Branding update refused: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }
  }

  @POST
  @Path("/email-preview")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_HTML)
  @Operation(summary = "Preview an email with a branding",
             description = "Renders a sample of an email of the platform with the saved branding, "
                 + "for the preview of the branding page",
             method = "POST")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "The html of the sample email"),
                          @ApiResponse(responseCode = "400", description = "Unknown email template, the body is the message code"),
  })
  public Response previewEmail(@Context
  UriInfo uriInfo, EmailPreviewRequest request) {
    EmailPreviewRequest preview = request == null ? new EmailPreviewRequest() : request;
    if (!EmailPreview.isTemplate(preview.getTemplate())) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("unknownEmailTemplate")
                     .type(MediaType.TEXT_PLAIN)
                     .header("Cache-Control", "no-store")
                     .build();
    }
    // the saved branding only: nothing of the request is written in the preview
    EmailBrand brand = EmailBrand.from(brandingService);
    String lang = getLanguage();
    EmailPreview.Reader reader = getReader();
    String html = EmailPreview.render(preview.getTemplate(),
                                      brand,
                                      getBundle(lang),
                                      lang.replace('_', '-'),
                                      getSiteUrl(uriInfo),
                                      reader);
    return Response.ok(html, MediaType.TEXT_HTML_TYPE)
                   // a preview is data for an iframe: it runs nothing, loads nothing but images, and is never cached
                   .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; img-src https: http: data:")
                   .header("X-Content-Type-Options", "nosniff")
                   .header("Cache-Control", "no-store")
                   .build();
  }

  /**
   * The language of the preview, which is the default language of the
   * platform as the emails are sent in it unless the person has chosen
   * another one
   */
  protected String getLanguage() {
    try {
      LocaleConfigService localeConfigService = ExoContainerContext.getService(LocaleConfigService.class);
      LocaleConfig config = localeConfigService == null ? null : localeConfigService.getDefaultLocaleConfig();
      String lang = config == null ? null : config.getLocaleName();
      if (lang != null && LANGUAGE_TAG.matcher(lang.trim()).matches()) {
        return lang.trim();
      }
    } catch (Exception e) {
      LOG.debug("Cannot get the default language of the platform, English is used", e);
    }
    return "en";
  }

  /**
   * The labels of the emails in a language, in English when the platform
   * does not have them
   */
  protected ResourceBundle getBundle(String lang) {
    ResourceBundleService bundleService = ExoContainerContext.getService(ResourceBundleService.class);
    if (bundleService == null) {
      return null;
    }
    String[] names = bundleService.getSharedResourceBundleNames();
    ResourceBundle bundle = bundleService.getResourceBundle(names, Locale.forLanguageTag(lang.replace('_', '-')));
    return bundle == null ? bundleService.getResourceBundle(names, Locale.ENGLISH) : bundle;
  }

  /**
   * The administrator who reads the preview, so that the greeting of the
   * sample is the one they would get
   *
   * @return the reader, a value is null when it is not known
   */
  protected EmailPreview.Reader getReader() {
    try {
      ConversationState state = ConversationState.getCurrent();
      String userName = state == null || state.getIdentity() == null ? null : state.getIdentity().getUserId();
      OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);
      User user = userName == null || organizationService == null ? null : organizationService.getUserHandler().findUserByName(userName);
      if (user != null) {
        String full = (StringUtils.defaultString(user.getFirstName()) + " " + StringUtils.defaultString(user.getLastName())).trim();
        return new EmailPreview.Reader(full, user.getFirstName(), user.getUserName());
      }
    } catch (Exception e) {
      LOG.debug("Cannot get the user who previews the email, a sample name is used", e);
    }
    return new EmailPreview.Reader(null, null, null);
  }

  /**
   * The address of the site the administrator is using: the domain of the
   * request, followed by the context of the portal. The base address of the
   * REST layer may or may not end with a slash, so the request address is
   * the second source.
   */
  private static String getSiteUrl(UriInfo uriInfo) {
    if (uriInfo == null) {
      return null;
    }
    String site = siteOf(uriInfo.getBaseUri());
    return site != null ? site : siteOf(uriInfo.getRequestUri());
  }

  private static String siteOf(URI uri) {
    if (uri == null || uri.getHost() == null) {
      return null;
    }
    String address = withoutDefaultPort(uri);
    Matcher rest = REST_PATH.matcher(address);
    return rest.find() && rest.start() > 0 ? address.substring(0, rest.start()) : null;
  }

  /**
   * Behind a proxy the platform sees the port of the scheme (443 for https,
   * 80 for http) as an explicit one: a link written with it is the same
   * address, but nobody writes it
   */
  private static String withoutDefaultPort(URI uri) {
    String address = uri.toString();
    int port = uri.getPort();
    boolean isDefault = port == 443 && "https".equalsIgnoreCase(uri.getScheme()) || port == 80 && "http".equalsIgnoreCase(uri.getScheme());
    return isDefault ? address.replaceFirst("^(https?://[^/:]+):" + port + "(?=/|$)", "$1") : address;
  }

  @GET
  @Path("/logo")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Branding logo", description = "Get Branding logo", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Branding logo retrieved"),
                          @ApiResponse(responseCode = "404", description = "Branding logo not found"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving branding logo")
  })
  public Response getBrandingLogo(
                                  @Context
                                  Request request,
                                  @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                  @QueryParam("v")
                                  String version) {
    return getBrandingFileResponse(brandingService.getLogo(), request, version);
  }

  @GET
  @Path("/pageBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default Page Background image", description = "Get Default Page Background image", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
                          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getPageBackground(
                                    @Context
                                    Request request,
                                    @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                    @QueryParam("v")
                                    String version) {
    return getBrandingFileResponse(brandingService.getPageBackground(), request, version);
  }

  @GET
  @Path("/topBarBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default top bar Background image", description = "Get Default top bar Background image", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getTopBarBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getTopBarBackground(), request, version);
  }

  @GET
  @Path("/sideBarBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default sidebar Background image", description = "Get Default sidebar Background image", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getSideBarBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getSideBarBackground(), request, version);
  }

  @GET
  @Path("/drawerBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default drawer Background image", description = "Get Default drawer Background image", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getDrawerBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getDrawerBackground(), request, version);
  }

  @GET
  @Path("/appBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default application Background image", description = "Get the platform-wide default application background image (Branding and Theme > Pages & Apps style)", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getAppBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getAppBackground(), request, version);
  }

  @GET
  @Path("/appTextTitleBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default application title text Background image", description = "Get the platform-wide default application title text background image (Branding and Theme > Pages & Apps style)", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getAppTextTitleBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getAppTextTitleBackground(), request, version);
  }

  @GET
  @Path("/appTextHeaderBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default application header text Background image", description = "Get the platform-wide default application header text background image (Branding and Theme > Pages & Apps style)", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getAppTextHeaderBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getAppTextHeaderBackground(), request, version);
  }

  @GET
  @Path("/favicon")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Branding favicon", description = "Get Branding favicon", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Branding favicon retrieved"),
                          @ApiResponse(responseCode = "404", description = "Branding favicon not found"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving branding favicon")
  })
  public Response getBrandingFavicon(
                                     @Context
                                     Request request,
                                     @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                     @QueryParam("v")
                                     String version) {
    return getBrandingFileResponse(brandingService.getFavicon(), request, version);
  }

  @GET
  @Path("/loginBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get authentication pages left panel background",
             description = "Get authentication pages left panel background", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
                          @ApiResponse(responseCode = "404", description = "Resource not found"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving resource")
  })
  public Response getLoginBackground(
                                     @Context
                                     Request request,
                                     @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                     @QueryParam("v")
                                     String version) {
    return getBrandingFileResponse(brandingService.getLoginBackground(), request, version);
  }

  @GET
  @Path("/css")
  @Operation(summary = "Get Branding CSS content", description = "Get Branding CSS content", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Branding css retrieved"),
                          @ApiResponse(responseCode = "304", description = "Branding css not modified"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving branding css")
  })
  public Response getBrandingCSS(
                                 @Context
                                 Request request,
                                 @Parameter(description = "The value of lastModified parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                 @QueryParam("v")
                                 String lastModified) {
    ThemeStylesheet stylesheet = brandingService.getThemeStylesheet();
    long lastUpdated = stylesheet.stamp();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null || StringUtils.isBlank(lastModified)) {
      builder = Response.ok(stylesheet.css(), "text/css");
      builder.tag(eTag);
      CacheControl cc = new CacheControl();
      cc.setMaxAge(86400);
      builder.type("text/css");
      // the exposed time also carries the shipped template's fingerprint (v= parameter, ETag): as a date it may lie
      // in the future, which RFC 9110 forbids for Last-Modified, so the header is capped to now
      builder.lastModified(new Date(Math.min(lastUpdated, System.currentTimeMillis())));
      if (StringUtils.isNotBlank(lastModified)) {
        builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
      }
      // If the query has a lastModified parameter, it means that the client
      // will change the lastModified entry when it really changes
      // Which means that we can cache the image in browser side
      // for a long time
      builder.cacheControl(cc);
    }
    return builder.build();
  }

  private Response getBrandingFileResponse(BrandingFile iconFile,
                                           Request request,
                                           String version) {
    if (iconFile == null || iconFile.getData() == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    long lastUpdated = iconFile.getUpdatedDate();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null || StringUtils.isBlank(version)) {
      InputStream stream = new ByteArrayInputStream(iconFile.getData());
      builder = Response.ok(stream, IMAGE_MIME_TYPE);
      // If the query has a lastModified parameter, it means that the client
      // will change the lastModified entry when it really changes
      // Which means that we can cache the image in browser side
      // for a long time
      if (StringUtils.isNotBlank(version)) {
        builder.tag(eTag);
        builder.lastModified(new Date(lastUpdated));
        builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
        CacheControl cc = new CacheControl();
        cc.setMaxAge(CACHE_IN_SECONDS);
        builder.cacheControl(cc);
      }
    }
    return builder.build();
  }

}
