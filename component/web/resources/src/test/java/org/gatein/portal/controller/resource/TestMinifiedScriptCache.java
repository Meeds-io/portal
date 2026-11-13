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
package org.gatein.portal.controller.resource;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.exoplatform.commons.utils.PropertyManager;

import junit.framework.TestCase;

public class TestMinifiedScriptCache extends TestCase {

  private static final String OPTIONS           = "options";

  private static final String DATA_DIR_PROPERTY = "gatein.data.dir";

  private Path                directory;

  private AtomicInteger       minifications;

  private UnaryOperator<String> minifier;

  @Override
  protected void setUp() throws Exception {
    directory = Files.createTempDirectory("test-js-cache");
    minifications = new AtomicInteger();
    minifier = source -> {
      minifications.incrementAndGet();
      return "min:" + source;
    };
  }

  @Override
  protected void tearDown() throws Exception {
    try (Stream<Path> paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }

  public void testRestartMinifiesOnlyChangedSources() {
    MinifiedScriptCache cache = new MinifiedScriptCache(directory.toString(), OPTIONS);
    assertFalse(cache.isTemporary());
    assertEquals(directory, cache.getDirectory());
    assertEquals("min:a;", cache.get("a;", minifier));
    assertEquals("min:b;", cache.get("b;", minifier));
    assertEquals(2, minifications.get());

    // Same directory after a restart: only the changed source is minified
    MinifiedScriptCache restarted = new MinifiedScriptCache(directory.toString(), OPTIONS);
    assertEquals("min:a;", restarted.get("a;", minifier));
    assertEquals("min:b2;", restarted.get("b2;", minifier));
    assertEquals(3, minifications.get());
  }

  public void testKeyDependsOnCompilerOptions() {
    MinifiedScriptCache cache = new MinifiedScriptCache(directory.toString(), OPTIONS);
    MinifiedScriptCache otherOptions = new MinifiedScriptCache(directory.toString(), "other options");
    assertNotEquals(cache.key("a;"), otherOptions.key("a;"));
    cache.get("a;", minifier);
    otherOptions.get("a;", minifier);
    assertEquals(2, minifications.get());
  }

  public void testFailedMinificationIsNotKept() throws IOException {
    MinifiedScriptCache cache = new MinifiedScriptCache(directory.toString(), OPTIONS);
    assertNull(cache.get("broken", source -> null));
    assertEquals(0, listFiles().size());
    assertEquals("min:broken", cache.get("broken", minifier));
    assertEquals(1, minifications.get());
  }

  public void testEmptyFileOfANonBlankSourceIsAMiss() throws IOException {
    MinifiedScriptCache cache = new MinifiedScriptCache(directory.toString(), OPTIONS);
    Path file = Files.writeString(directory.resolve(cache.key("a;") + ".js"), "");

    assertEquals("min:a;", cache.get("a;", minifier));
    assertEquals("min:a;", Files.readString(file));
    assertEquals(1, minifications.get());

    // A blank source minifies to an empty script, which stays a hit
    Files.writeString(directory.resolve(cache.key(" ") + ".js"), "");
    assertEquals("", cache.get(" ", minifier));
    assertEquals(1, minifications.get());
  }

  public void testDeletedFileIsAMiss() throws IOException {
    MinifiedScriptCache cache = new MinifiedScriptCache(directory.toString(), OPTIONS);
    cache.get("a;", minifier);
    Path file = directory.resolve(cache.key("a;") + ".js");
    assertTrue(Files.exists(file));
    Files.delete(file);

    assertEquals("min:a;", cache.get("a;", minifier));
    assertEquals(2, minifications.get());
    assertTrue(Files.exists(file));
  }

  public void testCleanupAfterTheStartupBuildDeletesOnlyUnusedFiles() throws IOException {
    MinifiedScriptCache cache = new MinifiedScriptCache(directory.toString(), OPTIONS);
    cache.get("used;", minifier);
    cache.get("unused;", minifier);
    Path used = directory.resolve(cache.key("used;") + ".js");
    Path unused = directory.resolve(cache.key("unused;") + ".js");
    FileTime old = FileTime.from(Instant.now().minus(MinifiedScriptCache.RETENTION).minus(Duration.ofDays(1)));
    Files.setLastModifiedTime(used, old);
    Files.setLastModifiedTime(unused, old);
    // A write's leftover is the cache's own, any other file is not
    Path leftover = Files.writeString(directory.resolve(cache.key("leftover;") + ".js8123456789.tmp"), "partial");
    Path foreign = Files.writeString(directory.resolve("foreign.js"), "not a cached script");
    Path foreignHexName = Files.writeString(directory.resolve(cache.key("x;").toUpperCase() + ".js"), "not a cached script");
    for (Path file : new Path[] { leftover, foreign, foreignHexName }) {
      Files.setLastModifiedTime(file, old);
    }

    // No build read them for more than the retention: a restart keeps them until its build read the ones it serves
    MinifiedScriptCache restarted = new MinifiedScriptCache(directory.toString(), OPTIONS);
    assertTrue(Files.exists(used));
    assertTrue(Files.exists(unused));
    assertEquals("min:used;", restarted.get("used;", minifier));
    assertTrue(Files.getLastModifiedTime(used).compareTo(old) > 0);

    restarted.deleteUnused();
    assertTrue(Files.exists(used));
    assertFalse(Files.exists(unused));
    assertFalse(Files.exists(leftover));
    assertTrue(Files.exists(foreign));
    assertTrue(Files.exists(foreignHexName));
    assertEquals(2, minifications.get());
  }

  public void testUnusableDirectoryFallsBackToATemporaryOne() throws IOException {
    Path regularFile = Files.createFile(directory.resolve("file"));
    MinifiedScriptCache cache = new MinifiedScriptCache(regularFile.resolve("sub").toString(), OPTIONS);
    assertTrue(cache.isTemporary());
    assertEquals("min:a;", cache.get("a;", minifier));
    assertEquals("min:a;", cache.get("a;", minifier));
    assertEquals(1, minifications.get());

    MinifiedScriptCache blank = new MinifiedScriptCache(" ", OPTIONS);
    assertTrue(blank.isTemporary());
    assertNotEquals(directory, blank.getDirectory());
  }

  public void testATemporaryDirectoryIsTheServerAccountsOnly() throws IOException {
    Path temporary = new MinifiedScriptCache("", OPTIONS).getDirectory();
    Path served = MinifiedScriptCache.ownerOnlyTemporaryDirectory("javascript_cache_");
    try {
      FileAttribute<?>[] ownerOnly = MinifiedScriptCache.OWNER_ONLY;
      if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
        assertEquals(1, ownerOnly.length);
        assertEquals("posix:permissions", ownerOnly[0].name());
        assertEquals(PosixFilePermissions.fromString("rwx------"), ownerOnly[0].value());
      } else {
        assertEquals(0, ownerOnly.length);
      }
      if (Files.getFileStore(temporary).supportsFileAttributeView("posix")) {
        assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(temporary));
        assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(served));
      }
    } finally {
      Files.deleteIfExists(served);
    }
  }

  public void testTheDirectoryDefaultsToTheDataDirectory() {
    String previousDirectory = System.getProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY);
    String previousDataDirectory = System.getProperty(DATA_DIR_PROPERTY);
    try {
      System.clearProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY);
      System.setProperty(DATA_DIR_PROPERTY, directory.toString());
      PropertyManager.refresh();
      assertEquals(directory.resolve("js-cache").toString(), MinifiedScriptCache.configuredDirectory());

      PropertyManager.setProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY, directory.resolve("other").toString());
      assertEquals(directory.resolve("other").toString(), MinifiedScriptCache.configuredDirectory());

      // Empty: a temporary directory, so every start minifies again
      PropertyManager.setProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY, "");
      assertEquals("", MinifiedScriptCache.configuredDirectory());
      assertTrue(new MinifiedScriptCache(MinifiedScriptCache.configuredDirectory(), OPTIONS).isTemporary());

      System.clearProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY);
      System.clearProperty(DATA_DIR_PROPERTY);
      PropertyManager.refresh();
      assertNull(MinifiedScriptCache.configuredDirectory());
    } finally {
      restore(MinifiedScriptCache.CACHE_DIR_PROPERTY, previousDirectory);
      restore(DATA_DIR_PROPERTY, previousDataDirectory);
      PropertyManager.refresh();
    }
  }

  private static void restore(String name, String previousValue) {
    if (previousValue == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, previousValue);
    }
  }

  private List<Path> listFiles() throws IOException {
    try (Stream<Path> files = Files.list(directory)) {
      return files.toList();
    }
  }

}
