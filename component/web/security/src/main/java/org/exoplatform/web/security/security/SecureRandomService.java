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
package org.exoplatform.web.security.security;

import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;

import org.exoplatform.container.xml.InitParams;
import org.picketlink.idm.api.SecureRandomProvider;
import org.picocontainer.Startable;

/**
 * A central service for getting a {@link SecureRandom} that is granted to be reseeded regularly. It starts fast thanks to
 * seeding the underlying {@link SecureRandom} in a dedicated initialisation thread.
 *
 */
public class SecureRandomService implements Startable, SecureRandomProvider {

    protected static final String RANDOM_ALGORITHM = "random.algorithm";
    protected static final String RANDOM_ALGORITHM_PROVIDER = "random.algorithm.provider";
    protected static final String RANDOM_RESEEDING_PERIOD_MILLISECONDS = "random.algorithm.reseeding.period.milliseconds";
    protected static final String RANDOM_SEED_LENGTH_BYTES = "random.algorithm.seed.length.bytes";

    private volatile AutoReseedRandom random;
    private final CountDownLatch initializationLatch = new CountDownLatch(1);

    private final String algorithm;
    private final String algorithmProvider;
    private final int seedLength;
    private final long reseedingPeriod;

    public SecureRandomService() {
        this.algorithm = AutoReseedRandom.DEFAULT_RANDOM_ALGORITHM;
        this.algorithmProvider = AutoReseedRandom.DEFAULT_RANDOM_ALGORITHM_PROVIDER;
        this.seedLength = AutoReseedRandom.DEFAULT_SEED_LENGTH;
        this.reseedingPeriod = AutoReseedRandom.DEFAULT_RESEEDING_PERIOD;
    }

        public SecureRandomService(InitParams initParams) {
        super();
        this.algorithm = initParams != null && initParams.containsKey(RANDOM_ALGORITHM) ? initParams.getValueParam(
                RANDOM_ALGORITHM).getValue() : AutoReseedRandom.DEFAULT_RANDOM_ALGORITHM;
        this.algorithmProvider = initParams != null && initParams.containsKey(RANDOM_ALGORITHM_PROVIDER) ? initParams
                .getValueParam(RANDOM_ALGORITHM_PROVIDER).getValue() : AutoReseedRandom.DEFAULT_RANDOM_ALGORITHM_PROVIDER;
        this.seedLength = initParams != null && initParams.containsKey(RANDOM_SEED_LENGTH_BYTES) ? Integer.parseInt(initParams
                .getValueParam(RANDOM_SEED_LENGTH_BYTES).getValue()) : AutoReseedRandom.DEFAULT_SEED_LENGTH;
        this.reseedingPeriod = initParams != null && initParams.containsKey(RANDOM_RESEEDING_PERIOD_MILLISECONDS) ? Long
                .parseLong(initParams.getValueParam(RANDOM_RESEEDING_PERIOD_MILLISECONDS).getValue())
                : AutoReseedRandom.DEFAULT_RESEEDING_PERIOD;
    }

    public SecureRandom getSecureRandom() {
        /* wait until the random instance was initialized */
        try {
            initializationLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return random;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.picocontainer.Startable#start()
     */
    @Override
    public void start() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SecureRandomService.this.random = new AutoReseedRandom(SecureRandomService.this.algorithm,
                        SecureRandomService.this.algorithmProvider, SecureRandomService.this.seedLength,
                        SecureRandomService.this.reseedingPeriod);
                initializationLatch.countDown();
            }
        }, SecureRandomService.class.getSimpleName() + " initialization");
        /* Make a daemon out of t so that the VM can be shut down before t completes */
        t.setDaemon(true);
        t.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.picocontainer.Startable#stop()
     */
    @Override
    public void stop() {
        /* nothing to do */
    }
}
