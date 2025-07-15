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
package org.gatein.common.xml.stax.writer;

import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.staxnav.StaxNavException;

public abstract class WritableValueTypes {
    /**
     * Returns a value type for an enum class.
     *
     * @param <E> the enum parameter type
     * @return the corresponding value type
     */
    public static <E extends Enum<E>> WritableValueType<E> getEnumType() {
        return new EnumType<E>();
    }

    public static final WritableValueType<String> STRING = new WritableValueType<String>() {
        @Override
        public String format(String value) throws StaxNavException {
            return value;
        }
    };

    public static final WritableValueType<String> TRIMMED_STRING = new WritableValueType<String>() {
        @Override
        public String format(String value) throws StaxNavException {
            return value.trim();
        }
    };

    public static final WritableValueType<Boolean> BOOLEAN = new WritableValueType<Boolean>() {
        @Override
        public String format(Boolean value) throws StaxNavException {
            return value.toString();
        }
    };

    public static final WritableValueType<Integer> INTEGER = new WritableValueType<Integer>() {
        @Override
        public String format(Integer value) throws StaxNavException {
            return value.toString();
        }
    };

    public static final WritableValueType<Date> DATE = new WritableValueType<Date>() {
        @Override
        public String format(Date value) throws StaxNavException {
            Calendar cal = Calendar.getInstance();
            cal.setTime(value);
            return DatatypeConverter.printDate(cal);
        }
    };

    public static final WritableValueType<Date> DATE_TIME = new WritableValueType<Date>() {

        @Override
        public String format(Date value) throws StaxNavException {
            Calendar cal = Calendar.getInstance();
            cal.setTime(value);
            return DatatypeConverter.printDateTime(cal);
        }
    };

    protected static class EnumType<E extends Enum<E>> implements WritableValueType<E> {
        @Override
        public String format(E value) throws StaxNavException {
            return value.name();
        }
    }
}
