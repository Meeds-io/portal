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
package nl.captcha.text.producer;

/**
 * TextProducer implementation that will return Chinese characters.
 * 
 */
public class ChineseTextProducer implements TextProducer {
    
	static final int DEFAULT_LENGTH = 5;    
    // Here's hoping none of the characters in this range are offensive.
    static final int CODE_POINT_START = 0x4E00;
    static final int CODE_POINT_END = 0x4F6F;
    private static final int NUM_CHARS = CODE_POINT_END - CODE_POINT_START;

    private final TextProducer _txtProd;	// Decorator
    
    /**
     * The default constructor will generate five Chinese characters.
     * 
     */
    public ChineseTextProducer() {
    	this(DEFAULT_LENGTH);
    }
    
    /**
     * Generate <code>length</code> number of Chinese characters.
     * 
     * @param length
     */
    public ChineseTextProducer(int length) {
    	char[] chars = new char[NUM_CHARS];
    	for (char c = CODE_POINT_START, i = 0; c < CODE_POINT_END; c++, i++) {
    		chars[i] = Character.valueOf(c);
    	}
    	_txtProd = new DefaultTextProducer(length, chars);
    }

    public String getText() {
    	return _txtProd.getText();
    }
}
