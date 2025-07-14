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
package nl.captcha.servlet;

import static nl.captcha.Captcha.NAME;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nl.captcha.Captcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;


/**
 * Generates, displays, and stores in session a 200x50 CAPTCHA image with sheared black text, 
 * a solid dark grey background, and a straight, slanted red line through the text.
 * 
 */
public class SimpleCaptchaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String PARAM_HEIGHT = "height";
    private static final String PARAM_WIDTH = "width";

    protected int _width = 200;
    protected int _height = 50;

    @Override
    public void init() throws ServletException {
    	if (getInitParameter(PARAM_HEIGHT) != null) {
    		_height = Integer.valueOf(getInitParameter(PARAM_HEIGHT));
    	}
    	
    	if (getInitParameter(PARAM_WIDTH) != null) {
    		_width = Integer.valueOf(getInitParameter(PARAM_WIDTH));
    	}
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Captcha captcha = new Captcha.Builder(_width, _height)
        	.addText()
        	.addBackground(new GradiatedBackgroundProducer())
            .gimp()
            .addNoise()
            .addBorder()
            .build();

        CaptchaServletUtil.writeImage(resp, captcha.getImage());
        req.getSession().setAttribute(NAME, captcha);
    }
}
