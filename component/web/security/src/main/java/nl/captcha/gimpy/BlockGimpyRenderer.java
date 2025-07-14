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
package nl.captcha.gimpy;

import java.awt.image.BufferedImage;
import com.jhlabs.image.BlockFilter;

import static nl.captcha.util.ImageUtil.applyFilter;

public class BlockGimpyRenderer implements GimpyRenderer {
	
	private static final int DEF_BLOCK_SIZE = 3;
	private final int _blockSize;
	
	public BlockGimpyRenderer() {
		this(DEF_BLOCK_SIZE);
	}
	
	public BlockGimpyRenderer(int blockSize) {
		_blockSize = blockSize;
	}
	
	public void gimp(BufferedImage image) {
		BlockFilter filter = new BlockFilter();
		filter.setBlockSize(_blockSize);

        BufferedImage buffer = filter.filter(image, null);
        applyFilter(buffer, null);
    }
}
