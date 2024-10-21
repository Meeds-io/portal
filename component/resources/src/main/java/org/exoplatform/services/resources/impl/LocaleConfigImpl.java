/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.resources.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.ArrayUtils;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.resources.ResourceBundleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;

/**
 * @author Benjamin Mestrallet benjamin.mestrallet@exoplatform.com
 */
public class LocaleConfigImpl implements LocaleConfig {

  private static Map<String, Locale>   predefinedLocaleMap_ = null;

  @Setter
  private static ResourceBundleService resourceBundleService;

  static {
    predefinedLocaleMap_ = new HashMap<>(10);
    predefinedLocaleMap_.put("us", Locale.US);
    predefinedLocaleMap_.put("en", Locale.ENGLISH);
    predefinedLocaleMap_.put("fr", Locale.FRENCH);
    predefinedLocaleMap_.put("zh", Locale.SIMPLIFIED_CHINESE);
  }

  private Locale         locale_;

  private String         outputEncoding_;

  private String         inputEncoding_;

  private String         description_;

  private String         localeName_;

  private String         tagIdentifier_;

  private Orientation    orientation;

  private ResourceBundle        mergedGlobalNavigationBundle = null;

  public String getDescription() {
    return description_;
  }

  public void setDescription(String desc) {
    description_ = desc;
  }

  public String getOutputEncoding() {
    return outputEncoding_;
  }

  public void setOutputEncoding(String enc) {
    outputEncoding_ = enc;
  }

  public String getInputEncoding() {
    return inputEncoding_;
  }

  public void setInputEncoding(String enc) {
    inputEncoding_ = enc;
  }

  public Locale getLocale() {
    return locale_;
  }

  public void setLocale(Locale locale) {
    this.locale_ = locale;
    if (localeName_ == null) {
      this.localeName_ = locale.getLanguage();
    }
  }

  public void setLocale(String localeName) {
    this.localeName_ = localeName;
    this.locale_ = predefinedLocaleMap_.get(localeName);
    if (locale_ == null) {
      String[] localeParams = localeName.split("[_-]");
      if (localeParams.length > 1) {
        this.locale_ = new Locale.Builder().setLanguage(localeParams[0]).setRegion(localeParams[1]).build();
      } else {
        this.locale_ = new Locale.Builder().setLanguage(localeName).build();
      }
      tagIdentifier_ = I18N.toTagIdentifier(locale_);
    }
  }

  public String getTagIdentifier() {
    return tagIdentifier_;
  }

  public String getLanguage() {
    return locale_.getLanguage();
  }

  public String getLocaleName() {
    return localeName_;
  }

  public void setLocaleName(String localeName) {
    localeName_ = localeName;
  }

  public ResourceBundle getResourceBundle(String name) {
    return getResourceBundleService().getResourceBundle(name, locale_);
  }

  public ResourceBundle getMergeResourceBundle(String[] names) {
    return getResourceBundleService().getResourceBundle(names, locale_);
  }

  public ResourceBundle getNavigationResourceBundle(String ownerType, String ownerId) {
    ResourceBundle resourceBundle = getResourceBundle("locale.navigation." + ownerType + "." + ownerId.replaceAll("/", "."));
    if (resourceBundle == null) {
      return getMergedNavigationBundle();
    } else {
      return resourceBundle;
    }
  }

  public void setInput(HttpServletRequest req) throws java.io.UnsupportedEncodingException {
    req.setCharacterEncoding(inputEncoding_);
  }

  public void setOutput(HttpServletResponse res) {
    res.setContentType("text/html; charset=" + outputEncoding_);
    res.setLocale(locale_);
  }

  public Orientation getOrientation() {
    return orientation;
  }

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  @Override
  public String toString() {
    return "LocaleConfig[" + "localeName=" + localeName_ + ",locale=" + locale_ + ",description=" + description_ +
        ",inputEncoding=" + inputEncoding_ + ",outputEncoding=" + outputEncoding_ + "]";
  }

  private static ResourceBundleService getResourceBundleService() {
    if (resourceBundleService == null) {
      resourceBundleService =  ExoContainerContext.getService(ResourceBundleService.class);
    }
    return resourceBundleService;
  }

  private ResourceBundle getMergedNavigationBundle() {
    if (mergedGlobalNavigationBundle == null) {
      mergedGlobalNavigationBundle = getMergeResourceBundle(ArrayUtils.add(getResourceBundleService().getSharedResourceBundleNames(), "locale.navigation.portal.global"));
    }
    return mergedGlobalNavigationBundle;
  }

}
