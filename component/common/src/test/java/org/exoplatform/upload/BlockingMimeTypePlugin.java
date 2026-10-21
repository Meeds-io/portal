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
package org.exoplatform.upload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

/**
 * A mime type plugin that holds an upload in the window between the parser's
 * last progress event, which marks the resource uploaded, and the write and
 * validation of its file.
 */
public class BlockingMimeTypePlugin extends MimeTypeUploadPlugin {

  private final CountDownLatch inWindow;

  private final CountDownLatch release;

  /**
   * Builds the plugin over an empty mime type table.
   *
   * @param inWindow counted down when an upload enters the window
   * @param release awaited before the upload goes on
   * @throws Exception when the plugin cannot be built
   */
  public BlockingMimeTypePlugin(CountDownLatch inWindow, CountDownLatch release) throws Exception {
    super(params(), configurationManager());
    this.inWindow = inWindow;
    this.release = release;
  }

  /**
   * Signals the window, then waits for the release.
   *
   * @param fileName uploaded file name
   * @return null, so the parser's mime type is kept
   */
  @Override
  public String getMimeType(String fileName) {
    inWindow.countDown();
    try {
      if (!release.await(30, TimeUnit.SECONDS)) {
        throw new IllegalStateException("The test never released the upload");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
    return null;
  }

  /**
   * Builds the plugin parameters naming an empty mime type table.
   *
   * @return the init params
   */
  private static InitParams params() {
    InitParams params = new InitParams();
    ValueParam path = new ValueParam();
    path.setName("mimetype-path");
    path.setValue("mimetypes.properties");
    params.addParameter(path);
    return params;
  }

  /**
   * Builds a configuration manager resolving the table to an empty file.
   *
   * @return the configuration manager
   * @throws Exception when the file cannot be created
   */
  private static ConfigurationManager configurationManager() throws Exception {
    File table = createEmptyTable();
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    when(configurationManager.getURL("mimetypes.properties")).thenReturn(table.toURI().toURL());
    return configurationManager;
  }

  /**
   * Creates an empty mime type table, deleted at exit.
   *
   * @return the file
   * @throws IOException when it cannot be created
   */
  private static File createEmptyTable() throws IOException {
    File table = Files.createTempFile("mimetypes", ".properties").toFile();
    table.deleteOnExit();
    return table;
  }
}
