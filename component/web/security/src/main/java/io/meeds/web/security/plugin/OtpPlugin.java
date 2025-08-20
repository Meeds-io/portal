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
package io.meeds.web.security.plugin;

public interface OtpPlugin {

  /**
   * @return Plugin name
   */
  String getName();

  /**
   * @param userName user login
   * @return true if user can use this otp plugin, else false
   */
  boolean canUse(String userName);

  /**
   * Generates a new OTP code for a designated username
   * 
   * @param userName
   */
  void generateOtpCode(String userName);

  /**
   * Validates and clear OTP code for a designated user
   * 
   * @param userName
   * @param otpCode
   * @return true if valid, else false
   */
  boolean validateOtp(String userName, String otpCode);

}
