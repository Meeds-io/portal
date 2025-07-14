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

public class Position {

    /** . */
    private final int col;

    /** . */
    private final int line;

    public Position(int col, int line) {
        if (col < 0) {
            throw new IllegalArgumentException();
        }
        if (line < 0) {
            throw new IllegalArgumentException();
        }

        //
        this.col = col;
        this.line = line;
    }

    public int getCol() {
        return col;
    }

    public int getLine() {
        return line;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Position) {
            Position that = (Position) obj;
            return col == that.col && line == that.line;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Position[col=" + col + ",line=" + line + "]";
    }
}
