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
package org.exoplatform.groovyscript.text;

import org.exoplatform.resolver.ResourceResolver;
import org.gatein.common.concurrent.AtomicPositiveLong;
import org.gatein.common.concurrent.LongSampler;



public class TemplateStatistic {

    private final LongSampler times = new LongSampler(1000);

    private String name;

    private final AtomicPositiveLong maxTime = new AtomicPositiveLong();

    private final AtomicPositiveLong minTime = new AtomicPositiveLong();

    // count variable, store number of request
    private volatile long countRequest = 0;

    // resolver for name
    private ResourceResolver resolver;

    public TemplateStatistic(String name) {
        this.name = name;
    }

    public void setTime(long timeMillis) {

        //
        times.add(timeMillis);

        // if time > max time then put a new max time value
        maxTime.setIfGreater(timeMillis);

        // generate first value for min time
        minTime.setIfLower(timeMillis);

        //
        countRequest++;
    }

    public double getMaxTime() {
        long maxTime = this.maxTime.get();
        if (maxTime == -1) {
            return -1;
        }
        return maxTime;
    }

    public double getMinTime() {
        long minTime = this.minTime.get();
        if (minTime == -1) {
            return -1;
        }
        return minTime;
    }

    public double getAverageTime() {
        return times.average();
    }

    public long executionCount() {
        return countRequest;
    }

    public void setResolver(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }
}
