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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

import com.google.javascript.jscomp.Compiler;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Minified scripts kept on disk, so that a restart or a reload minifies only
 * the scripts whose source changed, and again those the compiler rejected. A
 * file's name is the SHA-256 of the script's source, of Closure's release
 * version and of the compiler options, so its content is the minification of
 * that source, whichever thread or node wrote it. A build that reads a file
 * makes it recent: {@link #deleteUnused()}, once the startup build is done,
 * deletes the files no build read for {@link #RETENTION}.
 */
public class MinifiedScriptCache {

  /**
   * Where minified scripts are kept across restarts. Its files are served as
   * the platform's scripts: only the server's account may write in it.
   */
  public static final String  CACHE_DIR_PROPERTY = "exo.portal.js.cache.dir";

  /** Files no build read for that long are deleted after the startup build. */
  public static final Duration RETENTION         = Duration.ofDays(30);

  private static final Log     LOG                = ExoLogger.getLogger(MinifiedScriptCache.class);

  private static final String  CLOSURE_POM        = "/META-INF/maven/com.google.javascript/closure-compiler/pom.properties";

  /**
   * The names this cache writes: a minified script, or the temporary file a
   * write left behind. The startup cleanup deletes no other file, as the
   * directory may be shared with other content.
   */
  private static final Pattern OWN_FILE           = Pattern.compile("[0-9a-f]{64}\\.js(\\d+\\.tmp)?");

  private static final String  EXTENSION          = ".js";

  /**
   * Readable and writable by the server's account only, where the temporary
   * directory's file store applies POSIX permissions: the scripts written in a
   * temporary directory are served as the platform's. The JDK's default for a
   * temporary directory, stated here so that it does not rest on the JDK.
   */
  static final FileAttribute<?>[] OWNER_ONLY = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
      ? new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")) }
      : new FileAttribute<?>[0];

  private final Path           directory;

  private final boolean        temporary;

  private final String         keyPrefix;

  private final AtomicBoolean  writeFailureLogged = new AtomicBoolean();

  /**
   * @param configuredDirectory the directory to use; when blank, or when it
   *          cannot be created or written, a temporary directory deleted when
   *          the JVM exits
   * @param compilerOptions the compiler options, as part of each file's key
   */
  public MinifiedScriptCache(String configuredDirectory, String compilerOptions) {
    Path persistent = persistentDirectory(configuredDirectory);
    this.temporary = persistent == null;
    this.directory = temporary ? temporaryDirectory() : persistent;
    this.keyPrefix = closureVersion() + '\n' + compilerOptions + '\n';
  }

  /**
   * @return the directory {@link #CACHE_DIR_PROPERTY} names; when it is not
   *         set, {@code ${gatein.data.dir}/js-cache}; an empty value, or no
   *         data directory, leaves it empty
   */
  public static String configuredDirectory() {
    String configured = PropertyManager.getProperty(CACHE_DIR_PROPERTY);
    if (configured == null) {
      String dataDirectory = PropertyManager.getProperty("gatein.data.dir");
      return StringUtils.isBlank(dataDirectory) ? null : Paths.get(dataDirectory, "js-cache").toString();
    }
    return configured;
  }

  /**
   * @param source a script's source
   * @param minifier minifies a source, or returns null when it cannot
   * @return the minified script, read from the directory when a previous
   *         minification of the same source left it there, else the
   *         minifier's result, written to the directory when not null. An
   *         empty file for a non-blank source is a miss: a write a host crash
   *         cut short leaves one
   */
  public String get(String source, UnaryOperator<String> minifier) {
    Path file = directory.resolve(key(source) + EXTENSION);
    try {
      String minified = Files.readString(file, StandardCharsets.UTF_8);
      if (!minified.isEmpty() || source.isBlank()) {
        touch(file);
        return minified;
      }
      LOG.debug("Minified script {} is empty, minify it again", file);
    } catch (NoSuchFileException e) {
      // Not minified yet, or deleted since by the cleanup: minify again
    } catch (IOException e) {
      LOG.debug("Can't read minified script {}, minify it again", file, e);
    }
    String minified = minifier.apply(source);
    if (minified != null) {
      write(file, minified);
    }
    return minified;
  }

  public Path getDirectory() {
    return directory;
  }

  public boolean isTemporary() {
    return temporary;
  }

  /**
   * Deletes the cache's files that no build read for {@link #RETENTION}. Run
   * once the startup build has read the scripts it serves: their files are
   * then recent, however long ago the previous build ran.
   */
  public void deleteUnused() {
    if (!temporary) {
      deleteUnused(Instant.now().minus(RETENTION));
    }
  }

  String key(String source) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(keyPrefix.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is a mandatory algorithm of every JVM", e);
    }
  }

  void deleteUnused(Instant before) {
    FileTime limit = FileTime.from(before);
    try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
      for (Path file : files) {
        deleteIfOlder(file, limit);
      }
    } catch (IOException e) {
      LOG.warn("Can't list the minified scripts of {} to delete the unused ones", directory, e);
    }
  }

  private void deleteIfOlder(Path file, FileTime limit) {
    try {
      if (OWN_FILE.matcher(file.getFileName().toString()).matches() && Files.isRegularFile(file)
          && Files.getLastModifiedTime(file).compareTo(limit) < 0) {
        Files.deleteIfExists(file);
      }
    } catch (IOException e) {
      LOG.debug("Can't delete unused minified script {}", file, e);
    }
  }

  private void touch(Path file) {
    try {
      Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
    } catch (IOException e) {
      LOG.debug("Can't update the modification time of {}", file, e);
    }
  }

  private void write(Path file, String minified) {
    Path tmp = null;
    try {
      tmp = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
      Files.writeString(tmp, minified, StandardCharsets.UTF_8);
      try {
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
      if (temporary) {
        file.toFile().deleteOnExit();
      }
    } catch (IOException e) {
      if (writeFailureLogged.compareAndSet(false, true)) {
        LOG.warn("Can't keep minified scripts in {}, they will be minified again at next startup", directory, e);
      } else {
        LOG.debug("Can't keep minified script {}", file, e);
      }
      deleteQuietly(tmp);
    }
  }

  private static void deleteQuietly(Path file) {
    if (file != null) {
      try {
        Files.deleteIfExists(file);
      } catch (IOException e) {
        LOG.debug("Can't delete {}", file, e);
      }
    }
  }

  private static Path persistentDirectory(String configuredDirectory) {
    if (StringUtils.isBlank(configuredDirectory)) {
      return null;
    }
    try {
      Path path = Files.createDirectories(Paths.get(configuredDirectory));
      if (Files.isWritable(path)) {
        LOG.info("Keep minified scripts in {}", path);
        return path;
      }
      LOG.warn("Can't write in {}, minified scripts will not be kept across restarts", path);
    } catch (IOException | InvalidPathException e) {
      LOG.warn("Can't create {}, minified scripts will not be kept across restarts", configuredDirectory, e);
    }
    return null;
  }

  private static Path temporaryDirectory() {
    try {
      Path path = ownerOnlyTemporaryDirectory("js-cache");
      path.toFile().deleteOnExit();
      return path;
    } catch (IOException e) {
      throw new IllegalStateException("Can't create a temporary directory for minified scripts", e);
    }
  }

  /**
   * @param prefix the directory name's prefix
   * @return a new directory in the JVM's temporary directory, readable and
   *         writable by the server's account only where its file store
   *         applies POSIX permissions
   * @throws IOException when it cannot be created
   */
  static Path ownerOnlyTemporaryDirectory(String prefix) throws IOException {
    return Files.createTempDirectory(prefix, OWNER_ONLY);
  }

  /**
   * @return Closure's release version, read from its jar's Maven metadata, else
   *         its jar's location, else a value of this JVM only, so that a
   *         Closure upgrade never reuses a previous release's output
   */
  static String closureVersion() {
    try (InputStream pom = Compiler.class.getResourceAsStream(CLOSURE_POM)) {
      if (pom != null) {
        Properties properties = new Properties();
        properties.load(pom);
        String version = properties.getProperty("version");
        if (StringUtils.isNotBlank(version)) {
          return version;
        }
      }
    } catch (IOException e) {
      LOG.debug("Can't read Closure's release version", e);
    }
    CodeSource codeSource = Compiler.class.getProtectionDomain().getCodeSource();
    URL location = codeSource == null ? null : codeSource.getLocation();
    return location == null ? "unknown-" + System.nanoTime() : location.toString();
  }

}
