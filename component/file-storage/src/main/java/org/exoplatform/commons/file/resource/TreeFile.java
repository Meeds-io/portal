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

import java.io.File;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/*
* TreeFile implementation : Override delete File operation.
*/
public class TreeFile extends File {
  private static Log        fLog             = ExoLogger.getLogger("org.exoplatform.commons.file.resource.TreeFile");

  private static final long serialVersionUID = 5125295927077006487L;

  private final FileCleaner cleaner;

  private final File        rootDir;

  TreeFile(String fileName, FileCleaner cleaner, File rootDir) {
    super(fileName);
    this.cleaner = cleaner;
    this.rootDir = rootDir;
  }

  @Override
  public boolean delete() {
    boolean res = super.delete();
    if (res)
      deleteParent(new File(getParent()));

    return res;
  }

  protected boolean deleteParent(File fp) {
    boolean res = false;
    String fpPath = fp.getAbsolutePath();
    String rootPath = rootDir.getAbsolutePath();
    if (fpPath.startsWith(rootPath) && fpPath.length() > rootPath.length()) {
      if (fp.isDirectory()) {
        String[] ls = fp.list();
        if (ls != null && ls.length <= 0) {
          if (res = fp.delete()) {
            res = deleteParent(new File(fp.getParent()));
          } else {
            fLog.warn("Parent directory can not be deleted now. " + fp.getAbsolutePath());
            cleaner.addFile(new TreeFile(fp.getAbsolutePath(), cleaner, rootDir));
          }
        }
      } else {
        fLog.warn("Parent can not be a file but found " + fp.getAbsolutePath());
      }
    }
    return res;
  }
}
