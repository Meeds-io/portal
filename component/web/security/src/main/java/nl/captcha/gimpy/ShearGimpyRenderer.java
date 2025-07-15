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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Random;

public class ShearGimpyRenderer implements GimpyRenderer {

    private final Random _gen = new SecureRandom();
    private final Color _color;

    public ShearGimpyRenderer() {
        this(Color.GRAY);
    }

    public ShearGimpyRenderer(Color color) {
        _color = color;
    }

    public void gimp(BufferedImage bi) {
        Graphics2D g = bi.createGraphics();
        shearX(g, bi.getWidth(), bi.getHeight());
        shearY(g, bi.getWidth(), bi.getHeight());
        g.dispose();
    }

    private void shearX(Graphics2D g, int w1, int h1) {
        int period = _gen.nextInt(10) + 5;

        boolean borderGap = true;
        int frames = 15;
        int phase = _gen.nextInt(5) + 2;

        for (int i = 0; i < h1; i++) {
            double d = (period >> 1)
                    * Math.sin((double) i / (double) period
                            + (6.2831853071795862D * phase) / frames);
            g.copyArea(0, i, w1, 1, (int) d, 0);
            if (borderGap) {
                g.setColor(_color);
                g.drawLine((int) d, i, 0, i);
                g.drawLine((int) d + w1, i, w1, i);
            }
        }
    }

    private void shearY(Graphics2D g, int w1, int h1) {
        int period = _gen.nextInt(30) + 10; // 50;

        boolean borderGap = true;
        int frames = 15;
        int phase = 7;
        for (int i = 0; i < w1; i++) {
            double d = (period >> 1)
                    * Math.sin((float) i / period
                            + (6.2831853071795862D * phase) / frames);
            g.copyArea(i, 0, 1, h1, 0, (int) d);
            if (borderGap) {
                g.setColor(_color);
                g.drawLine(i, (int) d, i, 0);
                g.drawLine(i, (int) d + h1, i, h1);
            }
        }
    }
}
