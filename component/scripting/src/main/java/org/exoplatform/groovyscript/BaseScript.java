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
package org.exoplatform.groovyscript;

import java.io.IOException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import groovy.lang.Binding;
import groovy.lang.Script;

/**
 * The internal base script of a Groovy script as seen by the Groovy world.
 *
 */
public abstract class BaseScript extends Script {

  protected static final Log LOG = ExoLogger.getLogger(BaseScript.class);

  GroovyPrinter              printer;

  protected BaseScript() {
  }

  protected BaseScript(Binding binding) {
    super(binding);
  }

  @Override
  public Object getProperty(String property) {
    if ("out".equals(property)) {
      return printer;
    } else {
      return super.getProperty(property);
    }
  }

  @Override
  public void println(Object o) {
    printer.println(o);
  }

  @Override
  public void println() {
    printer.println();
  }

  @Override
  public void print(Object o) {
    printer.print(o);
  }

  public void flush() {
    try {
      printer.flush();
    } catch (IOException e) {
      LOG.warn("Error flushing content", e);
    }
  }

}
