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
package nl.captcha.backgrounds;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Creates a gradiated background with the given <i>from</i> and <i>to</i>
 * Color values. If none are specified they default to light gray and white
 * respectively.
 * 
 */
public class GradiatedBackgroundProducer implements BackgroundProducer {

    private Color _fromColor = Color.DARK_GRAY;
    private Color _toColor = Color.WHITE;
    
    public BufferedImage getBackground(int width, int height) {
        // create an opaque image
        BufferedImage img = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        RenderingHints hints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g.setRenderingHints(hints);

        // create the gradient color
        GradientPaint ytow = new GradientPaint(0, 0, _fromColor, width, height,
                _toColor);

        g.setPaint(ytow);
        // draw gradient color
        g.fill(new Rectangle2D.Double(0, 0, width, height));

        // draw the transparent image over the background
        g.drawImage(img, 0, 0, null);
        g.dispose();

        return img;
    }

    public BufferedImage addBackground(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        return getBackground(width, height);
    }

    public void setFromColor(Color fromColor) {
        _fromColor = fromColor;
    }

    public void setToColor(Color toColor) {
        _toColor = toColor;
    }
}
