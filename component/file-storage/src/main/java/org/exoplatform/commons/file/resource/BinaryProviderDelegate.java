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
package org.exoplatform.commons.file.resource;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This service class allow to detect the implementation of File RDBMS API to
 * use as Binary provider.
 */
public class BinaryProviderDelegate implements BinaryProvider, Startable {

  private static final Log                LOG                  = ExoLogger.getLogger(BinaryProviderDelegate.class);

  private static final String             STORAGE_TYPE         = "storageType";

  private static final String             DEFAULT_PROVIDER_CLASS ="org.exoplatform.commons.file.resource.FileSystemResourceProvider";

  /**
   * The class that implements {@link BinaryProvider}.
   */
  private Class<? extends BinaryProvider> binaryProviderClass;

  private BinaryProvider                  delegate;

  private InitParams                      initParams;

  private List<ResourceProviderPlugin>    plugins              = new ArrayList<>();

  public BinaryProviderDelegate(InitParams initParams) {
    this.initParams = initParams;
  }

  @Override
  public void put(String name, InputStream data) throws IOException {
    delegate.put(name, data);
  }

  @Override
  public void put(FileItem fileItem) throws IOException {
    delegate.put(fileItem);
  }

  @Override
  public void put(String name, byte[] data) throws IOException {
    delegate.put(name, data);
  }

  @Override
  public InputStream getStream(String name) {
    return delegate.getStream(name);
  }

  @Override
  public String getFilePath(FileInfo fileInfo) throws IOException {
    return delegate.getFilePath(fileInfo);
  }

  @Override
  public String getFilePath(String name) throws IOException {
    return delegate.getFilePath(name);
  }

  @Override
  public byte[] getBytes(String name) {
    return delegate.getBytes(name);
  }

  @Override
  public void remove(String name) throws IOException {
    delegate.remove(name);
  }

  @Override
  public boolean remove(FileInfo fileInfo) throws IOException {
    return delegate.remove(fileInfo);
  }

  @Override
  public boolean exists(String name) throws IOException {
    return delegate.exists(name);
  }

  @Override
  public long lastModified(String name) throws IOException {
    return delegate.lastModified(name);
  }

  @Override
  public URL getURL(String name) {
    return delegate.getURL(name);
  }

  @Override
  public String getLocation() {
    return delegate.getLocation();
  }

  @SuppressWarnings("unchecked")
  private void initBinaryProviderClass(String className) {
    try {
      Class<?> clazz = ClassLoading.forName(className, this);
      if (BinaryProvider.class.isAssignableFrom(clazz)) {
        binaryProviderClass = (Class<? extends BinaryProvider>) clazz;
      } else {
        LOG.error("Invalid value for binaryProviderClass, {} does not implement BinaryProvider interface.", className);
      }
    } catch (ClassNotFoundException e) {
      LOG.error("Invalid value for binaryProviderClass, class {} not found.", className);
    }
  }

  /**
   * Add a resourceProvider plugin
   *
   * @param resourceProviderPlugin resourceProvider plugin to add
   */
  public void addResourceProviderPlugin(ResourceProviderPlugin resourceProviderPlugin) {
    this.plugins.add(resourceProviderPlugin);
  }

  @Override
  public void start() {
    String provider = null;
    String storageType = null;
    ValueParam valueParam = initParams.getValueParam(STORAGE_TYPE);

    if (valueParam != null) {
      storageType = valueParam.getValue();
    }
    if (storageType != null && !storageType.isEmpty()) {
      for (ResourceProviderPlugin resource : this.plugins) {
        provider = resource.getResourceProviderData().get(storageType);
        if (provider != null && !provider.isEmpty()) {
          break;
        }
      }
    }
    if (provider == null || provider.isEmpty()) {
      provider = DEFAULT_PROVIDER_CLASS;
    }

    initBinaryProviderClass(provider);
    LOG.info("Binary provider used " + provider);
    delegate = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(binaryProviderClass);
  }

  @Override
  public void stop() {
    plugins.clear();
  }
}
