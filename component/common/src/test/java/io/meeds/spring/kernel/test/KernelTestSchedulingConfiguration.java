/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.spring.kernel.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.meeds.spring.kernel.KernelSchedulingConfiguration;

/**
 * Gives a test context that scans the kernel test module the scheduler that
 * runs a scheduled method naming
 * {@link io.meeds.spring.kernel.ClusterTaskScheduler#NAME}, as the kernel
 * module gives it to an application: a test context that scans a package
 * holding such a method otherwise fails to start.
 */
@Configuration
@Import(KernelSchedulingConfiguration.class)
public class KernelTestSchedulingConfiguration {
}
