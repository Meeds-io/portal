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
package org.exoplatform.portal.resource;

/**
 * Designed to plugged into SkipCommentReader for custom handling of comment block
 *
 */
public abstract class CommentBlockHandler {

    public abstract void handle(CharSequence commentBlock, SkipCommentReader reader);

    /**
     * A handler that push back content of comment block into the cache if content of comment block is
     *
     * orientation=lt or orientation=rt
     */
    public static class OrientationCommentBlockHandler extends CommentBlockHandler {

        private static final String LT = "orientation=lt";

        private static final String RT = "orientation=rt";

        @Override
        public void handle(CharSequence commentBlock, SkipCommentReader reader) {
            if (findInterestingContentIn(commentBlock)) {
                reader.pushback(commentBlock);
                reader.setNumberOfCommingEscapes(commentBlock.length()); /* The comment block won't be skipped */
            }
        }

        /**
         * Return true if content of comment block is either
         *
         * orientation=lt or orientation=rt
         *
         * @param commentBlock
         * @return
         */
        private boolean findInterestingContentIn(CharSequence commentBlock) {

            int indexOfFirstO = 0;

            while (indexOfFirstO < commentBlock.length()) {
                if (commentBlock.charAt(indexOfFirstO) == 'o') {
                    break;
                } else {
                    indexOfFirstO++;
                }
            }

            if (commentBlock.length() <= (indexOfFirstO + LT.length())) {
                return false;
            }
            for (int i = 0; i < LT.length(); i++) {
                if (commentBlock.charAt(indexOfFirstO + i) != LT.charAt(i) && i != (LT.length() - 2)) {
                    return false;
                }
            }
            return commentBlock.charAt(indexOfFirstO + LT.length() - 2) == 'l'
                    || commentBlock.charAt(indexOfFirstO + LT.length() - 2) == 'r';
        }

    }
}
