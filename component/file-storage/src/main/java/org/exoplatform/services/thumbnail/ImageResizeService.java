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
package org.exoplatform.services.thumbnail;

@FunctionalInterface
public interface ImageResizeService {


    /**
     * Create a scaled image by resizing an initial given image
     *
     * @param image Target image content to be resized
     * @param width Target resized image width
     * @param height Target resized image height
     * @param fitExact Fit resized image to the exact given with and height (true) or automatic fit (false)
     * @param ultraQuality return an ultra quality resized image (may take more execution time)
     * @return byte array of image content
     * @throws Exception
     */
    byte[] scaleImage(byte[] image, int width, int height, boolean fitExact, boolean ultraQuality) throws Exception;

}
