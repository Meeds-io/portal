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
package org.exoplatform.web.controller.router;

import org.exoplatform.web.controller.QualifiedName;

/**
 * Various predefined names for testing purpose.
 *
 */
public interface Names {

    QualifiedName Q_A = QualifiedName.parse("q:a");
    QualifiedName A = QualifiedName.parse("a");
    QualifiedName B = QualifiedName.parse("b");
    QualifiedName P = QualifiedName.parse("p");
    QualifiedName FOO = QualifiedName.parse("foo");
    QualifiedName BAR = QualifiedName.parse("bar");
    QualifiedName JUU = QualifiedName.parse("juu");
    QualifiedName GTN_HANDLER = QualifiedName.parse("gtn:handler");
    QualifiedName GTN_LANG = QualifiedName.parse("gtn:lang");
    QualifiedName GTN_SITENAME = QualifiedName.parse("gtn:sitename");
    QualifiedName GTN_SITETYPE = QualifiedName.parse("gtn:sitetype");
    QualifiedName GTN_ACCESS = QualifiedName.parse("gtn:access");
    QualifiedName GTN_PATH = QualifiedName.parse("gtn:path");
    QualifiedName GTN_MIN = QualifiedName.parse("gtn:min");
}
