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

public class JRegexFactory extends RegexFactory {

    /** . */
    public static final RegexFactory INSTANCE = new JRegexFactory();

    private JRegexFactory() {
    }

    @Override
    public Regex compile(String pattern) {
        return new JRegex(pattern);
    }

    @Override
    public String getName() {
        return "jregex";
    }

    public static class JRegex extends Regex {

        /** . */
        private final jregex.Pattern pattern;

        public JRegex(String regex) {
            this.pattern = new jregex.Pattern(regex);
        }

        public Matcher matcher() {
            return new Matcher() {

                /** . */
                private jregex.Matcher impl;

                private jregex.Matcher get(CharSequence seq) {
                    String s = seq.toString();
                    if (impl == null) {
                        impl = pattern.matcher(s);
                    } else {
                        impl.setTarget(s);
                    }
                    return impl;
                }

                @Override
                public boolean matches(CharSequence s) {
                    return get(s).matches();
                }

                @Override
                public Match[] find(CharSequence s) {
                    jregex.Matcher matcher = get(s);
                    if (matcher.find()) {
                        Match[] matches = new Match[matcher.groupCount()];
                        for (int i = 0; i < matcher.groupCount(); i++) {
                            if (matcher.isCaptured(i)) {
                                matches[i] = new Match(matcher.start(i), matcher.end(i), matcher.group(i));
                            } else {
                                matches[i] = NULL_MATCH;
                            }
                        }
                        return matches;
                    } else {
                        return NO_MATCHES;
                    }
                }
            };
        }

        @Override
        public String getPattern() {
            return pattern.toString();
        }
    }
}
