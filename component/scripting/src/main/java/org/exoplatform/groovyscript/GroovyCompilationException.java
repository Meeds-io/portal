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

public class GroovyCompilationException extends TemplateCompilationException {

    /** . */
    private final String groovyText;

    public GroovyCompilationException(Throwable cause, String templateText, String groovyText) {
        super(cause, templateText);

        //
        this.groovyText = groovyText;
    }

    public String getGroovyText() {
        return groovyText;
    }

    @Override
    public String getMessage() {
        return "Groovy compilation exception\n" + "template: " + getTemplateText() + "\n" + "compiled to Groovy: "
                + getGroovyText() + "\n";
    }
}
