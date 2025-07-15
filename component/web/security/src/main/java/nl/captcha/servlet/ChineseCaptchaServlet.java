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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import nl.captcha.Captcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.text.producer.ChineseTextProducer;

/**
 * Generate a CAPTCHA image/answer pair using Chinese characters.
 * 
 */
public class ChineseCaptchaServlet extends SimpleCaptchaServlet {

	private static final long serialVersionUID = -66324012009340831L;

	@Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        Captcha captcha;
        if (session.getAttribute(NAME) == null) {
	        captcha = new Captcha.Builder(_width, _height)
	            	.addText(new ChineseTextProducer())
	            	.gimp()
	            	.addBorder()
	                .addNoise()
	                .addBackground(new GradiatedBackgroundProducer())
	                .build();

	        session.setAttribute(NAME, captcha);
	        CaptchaServletUtil.writeImage(resp, captcha.getImage());
	        
	        return;
        }

        captcha = (Captcha) session.getAttribute(NAME);
        CaptchaServletUtil.writeImage(resp, captcha.getImage());
    }
}