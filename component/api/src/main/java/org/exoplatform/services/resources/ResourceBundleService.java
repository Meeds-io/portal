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
package org.exoplatform.services.resources;

import java.util.Locale;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.PageList;

/**
 * May 7, 2004
 *
 */
public interface ResourceBundleService {

  /**
   * Default Crowdin language different from default platform language
   */
  static final Locale DEFAULT_CROWDIN_LOCALE   = Locale.ENGLISH;

  static final String DEFAULT_CROWDIN_LANGUAGE = DEFAULT_CROWDIN_LOCALE.toLanguageTag();

  /**
   * This method should search for a template in the database that the service
   * manage or the resource in the classpath of the current thread class loader
   *
   * @param name The name of the resource bunlde
   * @param locale the locale
   * @return A ResourceBunlde or null if no ResourceBundle is found
   */
  ResourceBundle getResourceBundle(String name, Locale locale);

  /**
   * This method should search for a template in the database that the service
   * manage or the resource in the classpath of the specified class loader
   *
   * @param name the name of the resource
   * @param locale the locale
   * @param cl The classloader
   * @return
   */
  ResourceBundle getResourceBundle(String name, Locale locale, ClassLoader cl);

  /**
   * This method will call the method public ResourceBundle
   * getResourceBundle(String[] name, Locale locale, ClassLoader cl) and using
   * the the classloader of the current thread
   */
  ResourceBundle getResourceBundle(String[] name, Locale locale);

  /**
   * This method will look for all the resources with the given names and merge
   * into one resource bundle, the properties in the later resource bundle name
   * will have the higher priority than the previous one.
   */
  ResourceBundle getResourceBundle(String[] name, Locale locale, ClassLoader cl);

  /**
   * This method should look for a resource bundle in the database that match
   * the given id. The ResourceBundleData store the data as text instead of a
   * properties map.
   *
   * @param id The id of the resource bundle data
   * @return A ResourceBundleData instance or no record is found.
   */
  ResourceBundleData getResourceBundleData(String id);

  /**
   * This method remove the data record in the databas and return the data
   * instance after it has been removed
   *
   * @param id The id of the data record
   * @return A ResourceBundleData instance
   */
  ResourceBundleData removeResourceBundleData(String id);

  /**
   * This method shoudl create or update a ResourceBundleData instance
   *
   * @param data the ResourceBundleData instance to update or create
   */
  void saveResourceBundle(ResourceBundleData data);

  /**
   * This method search and return a page description iterator
   *
   * @param q The search criteria
   * @return A PageDescription Iterator
   */
  PageList<ResourceBundleData> findResourceDescriptions(Query q);

  /**
   * This method is acted as a factory of
   *
   * @return
   */
  ResourceBundleData createResourceBundleDataInstance();

  /**
   * The developer can store the common properties in certain resource bundles
   * so later he can merge a resource bundle with the shared resource bunldes
   *
   * @return the name of the shared resource bundle
   */
  String[] getSharedResourceBundleNames();

  /**
   * Return resource bundle content
   * 
   * @param resourceBundleName Resource bundle FQN
   * @param locale {@link Locale}
   * @return Json Content in string representation
   */
  default String getResourceBundleContent(String resourceBundleName, Locale locale) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param key Label Key
   * @param locale {@link Locale}
   * @return the associated string key value in designated locale else will
   *         retrieve it using DEFAULT_CROWDIN_LOCALE else if not found, return
   *         null
   */
  default String getSharedString(String key, Locale locale) {
    throw new UnsupportedOperationException();
  }
}
