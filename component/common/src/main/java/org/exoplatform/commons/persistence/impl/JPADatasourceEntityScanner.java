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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import jakarta.persistence.Converter;

/**
 * Hibernate 7 scanner for Meeds JPA entities. This scanner does not delegate to
 * Hibernate's old default scanner. It returns Hibernate ScanResult descriptors
 * directly from Meeds entity indexes.
 */
public class JPADatasourceEntityScanner implements Scanner {

  /** Path to the generated entities.idx file **/
  public static final String ENTITIES_IDX_PATH = "jpa-entities.idx";

  @Override
  public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters params) {
    ClassLoader classLoader = getClassLoader();

    Set<ClassDescriptor> classes = new LinkedHashSet<>();

    addIndexedClasses(classes, classLoader, ENTITIES_IDX_PATH);
    addExplicitClasses(classes, classLoader, environment.getExplicitlyListedClassNames());

    Set<MappingFileDescriptor> mappingFiles = new LinkedHashSet<>();
    addExplicitMappingFiles(mappingFiles, classLoader, environment.getExplicitlyListedMappingFiles());

    return new ExoScanResult(classes, mappingFiles);
  }

  private void addIndexedClasses(Set<ClassDescriptor> classes, ClassLoader classLoader, String indexPath) {
    try {
      Enumeration<URL> resources = classLoader.getResources(indexPath);

      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
          reader.lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(line -> !line.startsWith("#"))
                .forEach(className -> classes.add(new ExoClassDescriptor(classLoader, className)));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't access JPA entity index resource: " + indexPath, e);
    }
  }

  private void addExplicitClasses(Set<ClassDescriptor> classes, ClassLoader classLoader, List<String> classNames) {
    if (classNames == null) {
      return;
    }

    for (String className : classNames) {
      if (className != null && !className.isBlank()) {
        classes.add(new ExoClassDescriptor(classLoader, className.trim()));
      }
    }
  }

  private void addExplicitMappingFiles(Set<MappingFileDescriptor> mappingFiles,
                                       ClassLoader classLoader,
                                       List<String> mappingFileNames) {
    if (mappingFileNames == null) {
      return;
    }

    for (String mappingFileName : mappingFileNames) {
      if (mappingFileName != null && !mappingFileName.isBlank()) {
        mappingFiles.add(new ExoMappingFileDescriptor(classLoader, mappingFileName.trim()));
      }
    }
  }

  private ClassLoader getClassLoader() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    return contextClassLoader == null ? JPADatasourceEntityScanner.class.getClassLoader() : contextClassLoader;
  }

  private static final class ExoScanResult implements ScanResult {

    private final Set<ClassDescriptor>       classes;

    private final Set<MappingFileDescriptor> mappingFiles;

    private ExoScanResult(Set<ClassDescriptor> classes, Set<MappingFileDescriptor> mappingFiles) {
      this.classes = Collections.unmodifiableSet(classes);
      this.mappingFiles = Collections.unmodifiableSet(mappingFiles);
    }

    @Override
    public Set<PackageDescriptor> getLocatedPackages() {
      return Collections.emptySet();
    }

    @Override
    public Set<ClassDescriptor> getLocatedClasses() {
      return classes;
    }

    @Override
    public Set<MappingFileDescriptor> getLocatedMappingFiles() {
      return mappingFiles;
    }
  }

  private static final class ExoClassDescriptor implements ClassDescriptor {

    private final ClassLoader classLoader;

    private final String      className;

    private ExoClassDescriptor(ClassLoader classLoader, String className) {
      this.classLoader = classLoader;
      this.className = className;
    }

    @Override
    public String getName() {
      return className;
    }

    @Override
    public Categorization getCategorization() {
      if (isConverterClass()) {
        return Categorization.CONVERTER;
      }
      return Categorization.MODEL;
    }

    @Override
    public InputStreamAccess getStreamAccess() {
      String resourceName = className.replace('.', '/') + ".class";
      return new ClasspathInputStreamAccess(classLoader, resourceName);
    }

    private boolean isConverterClass() {
      try {
        Class<?> clazz = Class.forName(className, false, classLoader);
        return clazz.isAnnotationPresent(Converter.class);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Indexed JPA class not found: " + className, e);
      }
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ExoClassDescriptor that && className.equals(that.className);
    }

    @Override
    public int hashCode() {
      return className.hashCode();
    }
  }

  private static final class ExoMappingFileDescriptor implements MappingFileDescriptor {

    private final ClassLoader classLoader;

    private final String      mappingFileName;

    private ExoMappingFileDescriptor(ClassLoader classLoader, String mappingFileName) {
      this.classLoader = classLoader;
      this.mappingFileName = mappingFileName;
    }

    @Override
    public String getName() {
      return mappingFileName;
    }

    @Override
    public InputStreamAccess getStreamAccess() {
      return new ClasspathInputStreamAccess(classLoader, mappingFileName);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ExoMappingFileDescriptor that && mappingFileName.equals(that.mappingFileName);
    }

    @Override
    public int hashCode() {
      return mappingFileName.hashCode();
    }
  }

  private static final class ClasspathInputStreamAccess implements InputStreamAccess {

    private final ClassLoader classLoader;

    private final String      resourceName;

    private ClasspathInputStreamAccess(ClassLoader classLoader, String resourceName) {
      this.classLoader = classLoader;
      this.resourceName = resourceName;
    }

    @Override
    public String getStreamName() {
      return resourceName;
    }

    @Override
    public InputStream accessInputStream() {
      InputStream stream = classLoader.getResourceAsStream(resourceName);

      if (stream == null) {
        throw new IllegalStateException("Classpath resource not found: " + resourceName);
      }

      return stream;
    }
  }
}
