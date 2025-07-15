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
package org.exoplatform.portal.config.serialize;

import org.apache.commons.lang3.StringUtils;
import org.jibx.runtime.IAliasable;
import org.jibx.runtime.IMarshaller;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshaller;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.UnmarshallingContext;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Properties;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.config.serialize.model.Preference;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.exoplatform.portal.pom.spi.portlet.PortletBuilder;

public class AbstractApplicationHandler implements IMarshaller, IUnmarshaller, IAliasable {

  private static final String PREFERENCES = "preferences";

  private static final String PORTLET     = "portlet";

  private String              mUri;

  @SuppressWarnings("unused")
  private int                 mIndex;

  private String              mName;

  public AbstractApplicationHandler() {
  }

  public AbstractApplicationHandler(String mUri, int mIndex, String mName) {
    this.mUri = mUri;
    this.mIndex = mIndex;
    this.mName = mName;
  }

  // IMarshaller implementation

  public boolean isExtension(String s) {
    throw new UnsupportedOperationException();
  }

  public void marshal(Object o, IMarshallingContext iMarshallingContext) throws JiBXException {
    throw new UnsupportedOperationException();
  }

  // IUnmarshaller implementation

  public boolean isPresent(IUnmarshallingContext ctx) throws JiBXException {
    return ctx.isAt(mUri, mName);
  }

  public Object unmarshal(Object obj, IUnmarshallingContext ictx) throws JiBXException {
    UnmarshallingContext ctx = (UnmarshallingContext) ictx;
    if (!ctx.isAt(mUri, mName)) {
      ctx.throwStartTagNameError(mUri, mName);
    }

    //
    if (obj != null) {
      throw new AssertionError("That should not happen");
    }

    // Id
    String id = optionalAttribute(ctx, "id");
    String profiles = optionalAttribute(ctx, "profiles");

    //
    ctx.parsePastStartTag(mUri, mName);

    //
    if (!ctx.isAt(mUri, PORTLET)) {
      return null;
    }

    ctx.parsePastStartTag(mUri, PORTLET);
    String applicationName = ctx.parseElementText(mUri, "application-ref");
    String portletName = ctx.parseElementText(mUri, "portlet-ref");
    String contentId = applicationName + "/" + portletName;
    Application app = Application.createPortletApplication();

    TransientApplicationState state;
    if (ctx.isAt(mUri, PREFERENCES)) {
      PortletBuilder builder = new PortletBuilder();
      ctx.parsePastStartTag(mUri, PREFERENCES);
      while (ctx.isAt(mUri, "preference")) {
        Preference value = (Preference) ctx.unmarshalElement();
        builder.add(value.getName(), value.getValues(), value.isReadOnly());
      }
      ctx.parsePastEndTag(mUri, PREFERENCES);
      state = new TransientApplicationState(contentId, builder.build());
    } else {
      state = new TransientApplicationState(contentId, null);
    }

    ctx.parsePastEndTag(mUri, PORTLET);

    app.setState(state);

    //
    nextOptionalTag(ctx, "application-type");
    String theme = nextOptionalTag(ctx, "theme");
    String title = nextOptionalTag(ctx, "title");
    String accessPermissions = nextOptionalTag(ctx, "access-permissions");
    boolean showInfoBar = nextOptionalBooleanTag(ctx, "show-info-bar", false);
    boolean showApplicationState = nextOptionalBooleanTag(ctx, "show-application-state", false);
    boolean showApplicationMode = nextOptionalBooleanTag(ctx, "show-application-mode", false);
    String description = nextOptionalTag(ctx, "description");
    String icon = nextOptionalTag(ctx, "icon");
    String width = nextOptionalTag(ctx, "width");
    String height = nextOptionalTag(ctx, "height");
    String cssClass = nextOptionalTag(ctx, "cssClass");

    ModelStyle style = null;
    if (ctx.isAt(mUri, "css-style")) {
      style = (ModelStyle) ctx.unmarshalElement();
    }

    //
    Properties properties = null;
    if (ctx.isAt(mUri, "properties")) {
      properties = (Properties) ctx.unmarshalElement();
    }
    if (StringUtils.isNotBlank(profiles)) {
      if (properties == null) {
        properties = new Properties();
      } else {
        properties = new Properties(properties);
      }
      properties.put(MappedAttributes.PROFILES.getName(), profiles);
    }

    //
    ctx.parsePastEndTag(mUri, mName);

    //
    app.setId(id);
    app.setTheme(theme);
    app.setTitle(title);
    app.setAccessPermissions(StringUtils.isBlank(accessPermissions) ? new String[] { "Everyone" } :
                                                                    JibxArraySerialize.deserializeStringArray(accessPermissions));
    app.setShowInfoBar(showInfoBar);
    app.setShowApplicationState(showApplicationState);
    app.setShowApplicationMode(showApplicationMode);
    app.setDescription(description);
    app.setIcon(icon);
    app.setWidth(width);
    app.setHeight(height);
    app.setCssClass(cssClass);
    app.setProperties(properties);
    app.setCssStyle(style);

    //
    return app;
  }

  private String optionalAttribute(UnmarshallingContext ctx, String attrName) throws JiBXException {
    String value = null;
    if (ctx.hasAttribute(mUri, attrName)) {
      value = ctx.attributeText(mUri, attrName);
    }
    return value;
  }

  private String nextOptionalTag(UnmarshallingContext ctx, String tagName) throws JiBXException {
    String value = null;
    if (ctx.isAt(mUri, tagName)) {
      value = ctx.parseElementText(mUri, tagName);
    }
    return value;
  }

  private boolean nextOptionalBooleanTag(UnmarshallingContext ctx, String tagName, boolean defaultValue) throws JiBXException {
    Boolean value = defaultValue;
    if (ctx.isAt(mUri, tagName)) {
      value = ctx.parseElementBoolean(mUri, tagName);
    }
    return value;
  }
}
