/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.gatein.portal.controller.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
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

import lombok.Getter;
import lombok.SneakyThrows;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ScriptLoader implements Loader<ScriptKey, ScriptContent, Object> {

  private static final boolean  DEVELOPPING                    = PropertyManager.isDevelopping();

  private static final String   ERROR_MINIFYING_MODULE_MESSAGE = "Error while minifying module, retrieve it as is";

  protected static final Log    LOG                            = ExoLogger.getLogger(ScriptLoader.class);

  @Getter
  private Map<Integer, File>    files                          = new ConcurrentHashMap<>();

  private Map<Integer, Integer> hashes                         = new ConcurrentHashMap<>();

  @Override
  public ScriptContent retrieve(Object context, ScriptKey key) throws Exception { // NOSONAR
    if (DEVELOPPING) {
      byte[] fileContentBytes = getFileContent(key);
      return new ScriptContent(fileContentBytes, 0);
    } else {
      File jsFile = files.computeIfAbsent(key.hashCode(),
                                          k -> cacheFileContent(key));
      if (jsFile == null) {
        files.remove(key.hashCode());
        return null;
      } else {
        return new ScriptContent(jsFile, hashes.get(key.hashCode()));
      }
    }
  }

  @SneakyThrows
  private File cacheFileContent(ScriptKey key) {
    byte[] fileContentBytes = getFileContent(key);
    if (fileContentBytes.length == 0) {
      return null;
    } else {
      hashes.put(key.hashCode(), Arrays.hashCode(fileContentBytes));
      // Cache result into a temporary file
      String name = key.getId().getName();
      if (name.indexOf("/") >= 0) {
        name = name.substring(name.lastIndexOf("/") + 1);
      }
      File file = File.createTempFile("javascript_cache_", name + ".js");
      FileUtils.writeByteArrayToFile(file, fileContentBytes);
      // Ensure to clean cached file on JVM
      // exit
      file.deleteOnExit();
      return file;
    }
  }

  private byte[] getFileContent(ScriptKey key) throws Exception { // NOSONAR
    CompositeReader script = ExoContainerContext.getService(JavascriptConfigService.class)
                                                .getCompositeScript(key.id, key.locale);
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
          result = script;
        }
      } else {
        List<Reader> readers = script.getCompounds();
        for (Reader reader : readers) {
          if (CompositeReader.isMinify(reader)) {
            String errorMessage = minify(sourceName, reader, minifiedReaders);
            if (errorMessage != null) {
              LOG.warn(ERROR_MINIFYING_MODULE_MESSAGE, sourceName, errorMessage);
              result = script;
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

  private String minify(String sourceName, Reader reader, List<Reader> minifiedReaders) throws IOException {
    CompilationLevel level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
    CompilerOptions options = new CompilerOptions();
    options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);
    options.setStrictModeInput(false);
    options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_2021);
    options.setLanguageOut(CompilerOptions.LanguageMode.STABLE);
    level.setOptionsForCompilationLevel(options);

    StringWriter code = new StringWriter();
    IOTools.copy(reader, code);
    SourceFile[] inputs = new SourceFile[] { SourceFile.fromCode(sourceName, code.toString()) };

    com.google.javascript.jscomp.Compiler compiler = new Compiler();
    compiler.setErrorManager(new LoggerErrorManager(java.util.logging.Logger.getLogger(ScriptLoader.class.getName())));
    Result res = compiler.compile(AbstractCommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.BROWSER),
                                  Arrays.asList(inputs),
                                  options);
    if (res.success) {
      minifiedReaders.add(new StringReader(compiler.toSource()));
      return null;
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
      return msg.toString();
    }
  }

}
