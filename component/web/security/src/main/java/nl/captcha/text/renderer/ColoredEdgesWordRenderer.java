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
package nl.captcha.text.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates an outlined version of the answer using the given colors and fonts.
 * 
 */
public class ColoredEdgesWordRenderer implements WordRenderer {

	private static final List<Color> DEFAULT_COLORS = new ArrayList<Color>();
	private static final List<Font> DEFAULT_FONTS = new ArrayList<Font>();
	private static final float DEFAULT_STROKE_WIDTH = 0.5f;
	// The text will be rendered 25%/5% of the image height/width from the X and Y axes
	private static final double YOFFSET = 0.25;
	private static final double XOFFSET = 0.05;
	
	private final List<Font> _fonts;
	private final List<Color> _colors;
	private final float _strokeWidth;
	
	static {
		DEFAULT_FONTS.add(new Font("Arial", Font.BOLD, 40));
		DEFAULT_COLORS.add(Color.BLUE);
	}

	public ColoredEdgesWordRenderer() {
		this(DEFAULT_COLORS, DEFAULT_FONTS, DEFAULT_STROKE_WIDTH);
	}
	
	public ColoredEdgesWordRenderer(List<Color> colors, List<Font> fonts) {
		this(colors, fonts, DEFAULT_STROKE_WIDTH);
	}
	
	public ColoredEdgesWordRenderer(List<Color> colors, List<Font> fonts, float strokeWidth) {
		_colors = colors != null ? colors : DEFAULT_COLORS;
		_fonts = fonts != null ? fonts : DEFAULT_FONTS;
		_strokeWidth = strokeWidth < 0 ? DEFAULT_STROKE_WIDTH : strokeWidth;
	}

	public void render(final String word, BufferedImage image) {
		Graphics2D g = image.createGraphics();
		
        RenderingHints hints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY));
        g.setRenderingHints(hints);

		AttributedString as = new AttributedString(word);
		as.addAttribute(TextAttribute.FONT, getRandomFont());

		FontRenderContext frc = g.getFontRenderContext();
		AttributedCharacterIterator aci = as.getIterator();
		
		TextLayout tl = new TextLayout(aci, frc);
        int xBaseline = (int) Math.round(image.getWidth() * XOFFSET);
        int yBaseline =  image.getHeight() - (int) Math.round(image.getHeight() * YOFFSET);
		Shape shape = tl.getOutline(AffineTransform.getTranslateInstance(xBaseline, yBaseline));

		g.setColor(getRandomColor());
		g.setStroke(new BasicStroke(_strokeWidth));
		
		g.draw(shape);
	}
	
	private Color getRandomColor() {
		return (Color) getRandomObject(_colors);
	}
	
	private Font getRandomFont() {
		return (Font) getRandomObject(_fonts);
	}
	
	private Object getRandomObject(List<? extends Object> objs) {
		if (objs.size() == 1) {
			return objs.get(0);
		}
		
		Random gen = new SecureRandom();
		int i = gen.nextInt(objs.size());
		return objs.get(i);
	}
}
