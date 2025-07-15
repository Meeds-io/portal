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
package org.exoplatform.commons.persistence.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

import org.exoplatform.commons.api.persistence.ExoEntityProcessor;

/**
 * A specific hibernate scanner to allow injecting JPA Entities into
 * EntityManagerFactory
 */
public class JPADatasourceEntityScanner extends AbstractScannerImpl {

  public JPADatasourceEntityScanner() {
    this(StandardArchiveDescriptorFactory.INSTANCE);
  }

  public JPADatasourceEntityScanner(ArchiveDescriptorFactory value) {
    super(value);
  }

  @Override
  public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters params) {
    ScanEnvironment environmentWrapper = new ScanEnvironment() {
      @Override
      public URL getRootUrl() {
        return environment.getRootUrl();
      }

      @SuppressWarnings("removal")
      @Override
      public List<URL> getNonRootUrls() {
        List<URL> nonRootUrls = new ArrayList<>();
        String rootPath = environment.getRootUrl().getPath();
        addPaths(nonRootUrls, rootPath, ExoEntityProcessor.DEPRECATED_ENTITIES_IDX_PATH);
        addPaths(nonRootUrls, rootPath, ExoEntityProcessor.ENTITIES_IDX_PATH);
        return nonRootUrls;
      }

      @Override
      public List<String> getExplicitlyListedMappingFiles() {
        return environment.getExplicitlyListedMappingFiles();
      }

      @Override
      public List<String> getExplicitlyListedClassNames() {
        return environment.getExplicitlyListedClassNames();
      }
    };
    return super.scan(environmentWrapper, options, params);
  }

  private void addPaths(List<URL> nonRootUrls, String rootPath, String entitiesIdxPath) {
    try {
      Enumeration<URL> entityFiles = getClass().getClassLoader().getResources(entitiesIdxPath);
      while (entityFiles.hasMoreElements()) {
        URL url = entityFiles.nextElement();
        url = new URL(url.toExternalForm()
                         .replace("!/" + entitiesIdxPath, "")
                         .replace("jar:", "")
                         .replace(entitiesIdxPath, ""));
        if (url.getPath().startsWith(rootPath)) {
          continue;
        }
        nonRootUrls.add(url);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't access class path loader resources", e);
    }
  }

}
