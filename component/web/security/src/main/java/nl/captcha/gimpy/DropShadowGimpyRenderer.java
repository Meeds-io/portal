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
import com.jhlabs.image.ShadowFilter;

import static nl.captcha.util.ImageUtil.applyFilter;

/**
 * Twists text and adds a dark drop-shadow.
 * 
 */
public class DropShadowGimpyRenderer implements GimpyRenderer {
	private static final int DEFAULT_RADIUS = 3;
	private static final int DEFAULT_OPACITY = 75;
	
	private final int _radius;
	private final int _opacity;
	
	public DropShadowGimpyRenderer() {
		this(DEFAULT_RADIUS, DEFAULT_OPACITY);
	}
	
	public DropShadowGimpyRenderer(int radius, int opacity) {
		_radius = radius;
		_opacity = opacity;
	}

    public void gimp(BufferedImage image) {
        ShadowFilter sFilter = new ShadowFilter();
        sFilter.setRadius(_radius);
        sFilter.setOpacity(_opacity);
        
        BufferedImage buffer = sFilter.filter(image, null);
        applyFilter(buffer, null);
    }
}