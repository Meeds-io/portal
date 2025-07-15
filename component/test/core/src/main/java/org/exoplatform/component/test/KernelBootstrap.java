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
package org.exoplatform.component.test;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class KernelBootstrap {

    protected final Log                          log     = ExoLogger.getLogger(getClass());

    /** The system property for gatein tmp dir. */
    private static final String TMP_DIR = "gatein.test.tmp.dir";

    /** . */
    private File tmpDir;

    /** . */
    private File targetDir;

    /** . */
    private EnumMap<ContainerScope, Set<String>> configs;

    /** . */
    private ClassLoader realClassLoader;

    /** The portal container available once the kernel is booted. */
    private PortalContainer container;

    public KernelBootstrap() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public KernelBootstrap(ClassLoader realClassLoader) {

        this.configs = new EnumMap<>(ContainerScope.class);
        configs.put(ContainerScope.ROOT, new LinkedHashSet<>());
        configs.put(ContainerScope.PORTAL, new LinkedHashSet<>());

        this.targetDir = new File(System.getProperty("gatein.test.output.path", "target/temp"));
        if (!targetDir.exists()) {
          targetDir.mkdir();
        }
        if (!targetDir.isDirectory()) {
            throw new AssertionFailedError("Target dir is not a directory");
        }
        if (!targetDir.canWrite()) {
            throw new AssertionFailedError("Target dir is not writable");
        }

        this.tmpDir = findTmpDir(this.targetDir);
        this.realClassLoader = realClassLoader;
    }

    private static File findTmpDir(File dir) {
        Set<String> fileNames = new HashSet<String>();
        for (File child : dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("gateintest-");
            }
        })) {
            fileNames.add(child.getName());
        }

        //
        String fileName;
        int count = 0;
        while (true) {
            fileName = "gateintest-" + count;
            if (!fileNames.contains(fileName)) {
                break;
            }
            count++;
        }

        //
        return new File(dir, fileName);
    }

    public File getTargetDir() {
        return targetDir;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    /**
     * Set the tmp dir of the test to a new file location. The new tmp dir must be a strict descendant of the {@link #targetDir}
     * file.
     *
     * @param tmpDir the new tmp dir
     * @throws IllegalArgumentException if the tmp dir is not a descendant of the target dir
     */
    public void setTmpDir(File tmpDir) throws IllegalArgumentException {
        for (File parent = tmpDir.getParentFile(); !targetDir.equals(parent); parent = parent.getParentFile()) {
            if (parent == null) {
                throw new IllegalArgumentException("Wrong tmp dir " + tmpDir);
            }
        }

        //
        this.tmpDir = tmpDir;
    }

    public PortalContainer getContainer() {
        return container;
    }

    public void addConfiguration(ContainerScope scope, String path) {
      if (ContainerScope.STANDALONE == scope) {
        // consider STANDALONE scope as PortalContainer Scope
        scope = ContainerScope.PORTAL;
      } else {
        // If one of defined scopes is different than STANDALONE, then add
        // default configuration files
        configs.get(ContainerScope.ROOT).add("conf/root-configuration.xml");
        configs.get(ContainerScope.ROOT).add("conf/configuration.xml");
        configs.get(ContainerScope.PORTAL).add("conf/portal-configuration.xml");
        configs.get(ContainerScope.PORTAL).add("conf/portal/configuration.xml");
        configs.get(ContainerScope.PORTAL).add("conf/extended-portal-configuration.xml");
      }
      configs.get(scope).add(path);
    }

    private void addConfiguration(ConfigurationUnit unit) {
        addConfiguration(unit.scope(), unit.path());
    }

    public void addConfiguration(ConfiguredBy configuredBy) {
        for (ConfigurationUnit unit : configuredBy.value()) {
            addConfiguration(unit);
        }
    }

    public void addConfiguration(Class<?> clazz) {
        ConfiguredBy cfBy = clazz.getAnnotation(ConfiguredBy.class);
        if (cfBy != null) {
            addConfiguration(cfBy);
        }
    }

    /**
     * Boot the kernel.
     *
     * @throws IllegalStateException if the kernel is already booted
     */
    public void boot() throws IllegalStateException {
        if (container != null) {
            throw new IllegalStateException("Already booted");
        }
        try {
            // Must clear the top container first otherwise it's not going to work well
            // it's a bit ugly but I don't want to change anything in the ExoContainerContext class for now
            // and this is for unit testing
            Field topContainerField = ExoContainerContext.class.getDeclaredField("topContainer");
            topContainerField.setAccessible(true);
            topContainerField.set(null, null);

            // Same remark than above
            Field singletonField = RootContainer.class.getDeclaredField("singleton_");
            singletonField.setAccessible(true);
            singletonField.set(null, null);

            if (!tmpDir.exists()) {
                if (!tmpDir.mkdirs()) {
                    throw new AssertionFailedError("Could not create directory " + tmpDir.getAbsolutePath());
                }
            }

            // Set property globally available for configuration XML
            System.setProperty(TMP_DIR, tmpDir.getCanonicalPath());

            if (configs.get(ContainerScope.ROOT).isEmpty()) {
              configs.get(ContainerScope.ROOT).add("conf/root-configuration.xml");
            }
            if (configs.get(ContainerScope.PORTAL).isEmpty()) {
              configs.get(ContainerScope.PORTAL).add("conf/portal-configuration.xml");
            }
            //
            ClassLoader testClassLoader = new GateInTestClassLoader(realClassLoader, configs.get(ContainerScope.ROOT),
                    configs.get(ContainerScope.PORTAL));
            Thread.currentThread().setContextClassLoader(testClassLoader);

            // Boot the container, this will associate the current thread with the portal container
            this.container = PortalContainer.getInstance();
        } catch (Exception e) {
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        } finally {
            Thread.currentThread().setContextClassLoader(realClassLoader);
        }
    }

    public void dispose() {
        if (container != null) {
            RootContainer.getInstance().stop();
            container = null;
            ExoContainerContext.setCurrentContainer(null);
        }
    }
}
