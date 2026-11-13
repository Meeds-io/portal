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
package org.gatein.portal.controller.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.UnaryOperator;

import org.gatein.common.io.IOTools;

import com.google.javascript.jscomp.AbstractCommandLineRunner;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.LoggerErrorManager;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.CharsetTextEncoder;
import org.exoplatform.commons.utils.CompositeReader;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.TextEncoder;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

/**
 * Loads a script's served content: its composite source, minified unless
 * developing, kept in a file named after the content's SHA-256, so that the
 * same content is written once whatever the number of loaders and reloads.
 */
public class ScriptLoader implements Loader<ScriptKey, ScriptContent, Object> {

  private static final CompilationLevel             COMPILATION_LEVEL              = CompilationLevel.SIMPLE_OPTIMIZATIONS;

  private static final CheckLevel                   NON_STANDARD_JSDOC             = CheckLevel.OFF;

  private static final boolean                      STRICT_MODE_INPUT              = false;

  private static final CompilerOptions.LanguageMode LANGUAGE_IN                    = CompilerOptions.LanguageMode.ECMASCRIPT_2021;

  private static final CompilerOptions.LanguageMode LANGUAGE_OUT                   = CompilerOptions.LanguageMode.STABLE;

  private static final CompilerOptions.Environment  EXTERNS                        = CompilerOptions.Environment.BROWSER;

  /**
   * Every option {@link #compile(String, String)} sets, part of each minified
   * script's key: a script minified with other options is minified again.
   */
  private static final String                       COMPILER_OPTIONS               = COMPILATION_LEVEL + ";NON_STANDARD_JSDOC="
      + NON_STANDARD_JSDOC + ";strictModeInput=" + STRICT_MODE_INPUT + ";in=" + LANGUAGE_IN + ";out=" + LANGUAGE_OUT + ";externs="
      + EXTERNS;

  private static final boolean  DEVELOPPING                    = PropertyManager.isDevelopping();

  private static final String   ERROR_MINIFYING_MODULE_MESSAGE = "Error while minifying module {}, retrieve it as is: {}";

  protected static final Log    LOG                            = ExoLogger.getLogger(ScriptLoader.class);

  private static Path           servedDirectory;

  private final JavascriptConfigService service;

  private final MinifiedScriptCache     minifiedScripts;

  /**
   * A loader that reads scripts from the container's
   * {@link JavascriptConfigService} and minifies each one at each load.
   */
  public ScriptLoader() {
    this(null, null);
  }

  /**
   * @param service the service that builds the scripts' sources; when null,
   *          the container's one
   * @param minifiedScripts where minified scripts are kept; when null, each
   *          load minifies again
   */
  public ScriptLoader(JavascriptConfigService service, MinifiedScriptCache minifiedScripts) {
    this.service = service;
    this.minifiedScripts = minifiedScripts;
  }

  @Override
  public ScriptContent retrieve(Object context, ScriptKey key) throws Exception { // NOSONAR
    byte[] fileContentBytes = getFileContent(key);
    if (DEVELOPPING) {
      return new ScriptContent(fileContentBytes, 0);
    } else if (fileContentBytes.length == 0) {
      return null;
    } else {
      // The file and the hash are built together from the same bytes
      return new ScriptContent(servedFile(fileContentBytes), Arrays.hashCode(fileContentBytes));
    }
  }

  /**
   * @return every option {@link #compile(String, String)} sets, part of each
   *         minified script's key
   */
  public static String compilerOptions() {
    return COMPILER_OPTIONS;
  }

  private static File servedFile(byte[] bytes) throws IOException {
    Path file = servedDirectory().resolve(sha256(bytes) + ".js");
    if (!Files.exists(file)) {
      Path tmp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
      Files.write(tmp, bytes);
      try {
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
      // Ensure to clean cached file on JVM exit
      file.toFile().deleteOnExit();
    }
    return file.toFile();
  }

  private static synchronized Path servedDirectory() throws IOException {
    if (servedDirectory == null || !Files.isDirectory(servedDirectory)) {
      servedDirectory = MinifiedScriptCache.ownerOnlyTemporaryDirectory("javascript_cache_");
      servedDirectory.toFile().deleteOnExit();
    }
    return servedDirectory;
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is a mandatory algorithm of every JVM", e);
    }
  }

  private byte[] getFileContent(ScriptKey key) throws Exception { // NOSONAR
    JavascriptConfigService scriptService = service == null ? ExoContainerContext.getService(JavascriptConfigService.class)
                                                            : service;
    CompositeReader script = scriptService.getCompositeScript(key.id, key.locale);
    String sourceName = key.id.getScope() + "/" + key.id.getName() + ".js";
    if (script == null) {
      return new byte[0];
    }
    Reader result = null;
    if (key.minified) {
      List<Reader> minifiedReaders = new ArrayList<>();
      if (CompositeReader.isMinify(script)) {
        String errorMessage = minify(sourceName, script, minifiedReaders);
        if (errorMessage != null) {
          LOG.warn(ERROR_MINIFYING_MODULE_MESSAGE, sourceName, errorMessage);
          result = unminified(scriptService, key, script);
        }
      } else {
        List<Reader> readers = script.getCompounds();
        for (Reader reader : readers) {
          if (CompositeReader.isMinify(reader)) {
            String errorMessage = minify(sourceName, reader, minifiedReaders);
            if (errorMessage != null) {
              LOG.warn(ERROR_MINIFYING_MODULE_MESSAGE, sourceName, errorMessage);
              result = unminified(scriptService, key, script);
              break;
            }
          } else {
            minifiedReaders.add(reader);
          }
        }
      }
      if (result == null) {
        result = new CompositeReader(minifiedReaders);
      }
    } else {
      result = script;
    }

    // Encode data
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      TextEncoder encoder = CharsetTextEncoder.getUTF8();
      char[] buffer = new char[256];
      for (int l = result.read(buffer); l != -1; l = result.read(buffer)) {
        encoder.encode(buffer, 0, l, out);
      }
      return out.toByteArray();
    } finally {
      result.close();
    }
  }

  /**
   * @return the module as is, read again: the minification consumed the
   *         readers of the script it was given
   */
  private static Reader unminified(JavascriptConfigService scriptService, ScriptKey key, CompositeReader consumed) throws Exception { // NOSONAR
    consumed.close();
    CompositeReader script = scriptService.getCompositeScript(key.id, key.locale);
    return script == null ? new StringReader("") : script;
  }

  private String minify(String sourceName, Reader reader, List<Reader> minifiedReaders) throws IOException {
    StringWriter code = new StringWriter();
    IOTools.copy(reader, code);
    String source = code.toString();
    String[] errorMessage = new String[1];
    UnaryOperator<String> minifier = s -> {
      try {
        Minification minification = compile(sourceName, s);
        errorMessage[0] = minification.errorMessage();
        return minification.code();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
    String minified;
    try {
      minified = minifiedScripts == null ? minifier.apply(source) : minifiedScripts.get(source, minifier);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    if (minified == null) {
      return errorMessage[0];
    } else {
      minifiedReaders.add(new StringReader(minified));
      return null;
    }
  }

  private static Minification compile(String sourceName, String source) throws IOException {
    CompilerOptions options = new CompilerOptions();
    options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, NON_STANDARD_JSDOC);
    options.setStrictModeInput(STRICT_MODE_INPUT);
    options.setLanguageIn(LANGUAGE_IN);
    options.setLanguageOut(LANGUAGE_OUT);
    COMPILATION_LEVEL.setOptionsForCompilationLevel(options);

    List<SourceFile> inputs = List.of(SourceFile.fromCode(sourceName, source));

    com.google.javascript.jscomp.Compiler compiler = new Compiler();
    compiler.setErrorManager(new LoggerErrorManager(java.util.logging.Logger.getLogger(ScriptLoader.class.getName())));
    Result res = compiler.compile(AbstractCommandLineRunner.getBuiltinExterns(EXTERNS),
                                  inputs,
                                  options);
    if (res.success) {
      return new Minification(compiler.toSource(), null);
    } else {
      StringBuilder msg = new StringBuilder("Handle me gracefully JS errors\n");
      for (JSError error : res.errors) {
        msg.append(error.getSourceName())
           .append(":")
           .append(error.getLineNumber())
           .append(" ")
           .append(error.getDescription())
           .append("\n");
      }
      return new Minification(null, msg.toString());
    }
  }

}
