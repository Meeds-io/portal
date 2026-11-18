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
package org.exoplatform.portal.resource;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import jakarta.servlet.ServletContext;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.component.test.web.WebAppImpl;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.resource.compressor.ResourceCompressor;
import org.exoplatform.test.mocks.servlet.MockServletContext;
import org.exoplatform.test.mocks.servlet.MockServletRequest;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.javascript.JavascriptConfigParser;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.io.IOTools;
import org.gatein.portal.controller.resource.MinifiedScriptCache;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.ScriptContent;
import org.gatein.portal.controller.resource.script.FetchMode;
import org.gatein.portal.controller.resource.script.ScriptResource;
import org.json.JSONObject;

public class TestJavascriptConfigService extends AbstractWebResourceTest {
    private static final ControllerContext CONTROLLER_CONTEXT = new MockControllerContext();

    private JavascriptConfigService jsService;

    private static ServletContext mockServletContext;

    private static boolean isFirstStartup = true;

    /** The mock webapp's files, which a test may change. */
    private static Map<String, String> mockResources;

    /** Runs when the mock webapp's file is read. */
    private static final Map<String, Runnable> READ_HOOKS = new ConcurrentHashMap<>();

    @Override
    protected void setUp() throws Exception {
        final PortalContainer portalContainer = getContainer();
        jsService = (JavascriptConfigService) portalContainer.getComponentInstanceOfType(JavascriptConfigService.class);

        if (isFirstStartup) {
            Map<String, String> resources = new ConcurrentHashMap<>();
            mockResources = resources;
            resources.put("/js/script1.js", "aaa;");
            resources.put("/js/script2.js", "bbb;");
            resources.put("/js/module1.js", "ccc;");
            resources.put("/js/module2.js", "ddd;");
            resources.put("/js/common.js", "kkk;");
            resources.put("/js/pluginTest.js", "iii;");
            resources.put("/js/exclude.js", "eee;");
            //trim all space character: tab, space, non-breaking space...
            resources.put("/js/normalize_test.js", " \n \t /* // */  //  /* \n  /* /*  */  \n  ggg; // /* */ \n");
            mockServletContext = new MockJSServletContext("mockwebapp", resources);
            jsService.registerContext(new WebAppImpl(mockServletContext, Thread.currentThread().getContextClassLoader()));

            URL url = portalContainer.getPortalClassLoader().getResource("mockwebapp/gatein-resources.xml");
            JavascriptConfigParser.processConfigResource(url.openStream(), jsService, mockServletContext);

            isFirstStartup = false;
        }
    }

    public void testGetScript() throws Exception {
        // no wrapper for SCRIPTS
        String script1 = "aaa;"
                + "\nif (typeof define === 'function' && define.amd && !require.specified('SHARED/script1')) {define('SHARED/script1');}";
        String script2 = "bbb;"
                + "\nif (typeof define === 'function' && define.amd && !require.specified('SHARED/script2')) {define('SHARED/script2');}";
        assertReader(script1, jsService.getScript(new ResourceId(ResourceScope.SHARED, "script1"), null));
        assertReader(script2, jsService.getScript(new ResourceId(ResourceScope.SHARED, "script2"), null));

        // wrapper for MODULE
        // test for Alias : module1 is used with 'm1' alias
        String module1 = "\ndefine('SHARED/module1', [], function() {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[];\neXo.define.deps=[];"
                + "\nreturn ccc;\n});";
        assertReader(module1, jsService.getScript(new ResourceId(ResourceScope.SHARED, "module1"), null));

        // module2 depends on module1
        // test for Alias : module1 is used with 'mod1' alias, module2 use default name for alias
        String module2 = "\ndefine('SHARED/module2', [\"SHARED/module1\"], function(mod1) {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[\"mod1\"];\neXo.define.deps=[mod1];"
                + "\nreturn ddd;\n});";
        assertReader(module2, jsService.getScript(new ResourceId(ResourceScope.SHARED, "module2"), null));
    }

    public void testCommonJS() throws Exception {
        String commonjs = "\ndefine('SHARED/commonjs', [\"require\",\"exports\",\"module\"], function(require,exports,module) {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[\"require\",\"exports\",\"module\"];"
                + "\neXo.define.deps=[eXo.require,exports,module];" + "\nreturn kkk;\n});";
        assertReader(commonjs, jsService.getScript(new ResourceId(ResourceScope.SHARED, "commonjs"), null));
    }

    public void testExcludeJS() throws Exception {
        String excludejs = "\ndefine('SHARED/excludejs', [], function() {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[];"
                + "\neXo.define.deps=[];" + "\nreturn eee;\n});";
        assertReader(excludejs, jsService.getScript(new ResourceId(ResourceScope.SHARED, "excludejs"), null));
    }

    public void testRequireJSPlugin() throws Exception {
        String pluginTest = "\ndefine('SHARED/pluginTest', [\"SHARED/text!/path/to/file.js\",\"SHARED/text!/path/to/file2.js\"],"
                + " function(text,text_1) {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\n"
                + "eXo.define.names=[\"text!/path/to/file.js\",\"text!/path/to/file2.js\"];"
                + "\neXo.define.deps=[text,text_1];" + "\nreturn iii;\n});";
        assertReader(pluginTest, jsService.getScript(new ResourceId(ResourceScope.SHARED, "pluginTest"), null));
    }

    public void testGroupingScript() throws Exception {
        String module1 = "\n//Begin SHARED/module1\n\ndefine('SHARED/module1', [], function() {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[];\neXo.define.deps=[];"
                + "\nreturn ccc;\n});\n//End SHARED/module1";
        String module2 = "\n//Begin SHARED/module2\n\ndefine('SHARED/module2', [\"SHARED/module1\"], function(mod1) {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[\"mod1\"];\neXo.define.deps=[mod1];"
                + "\nreturn ddd;\n});\n//End SHARED/module2";

        StringWriter buffer = new StringWriter();
        IOTools.copy(jsService.getScript(new ResourceId(ResourceScope.GROUP, "fooGroup"), null), buffer);

        // the order of module1, and module2 in group is not known
        String result = buffer.toString();
        result = result.replace(module1, "");
        result = result.replace(module2, "");
        assertTrue(result.trim().isEmpty());
    }

    public void testGetJSConfig() throws Exception {
        JSONObject config = jsService.getJSConfig();

        // All SCRIPTS and remote resource have to had dependencies declared in shim configuration
        JSONObject shim = config.getJSONObject("shim");
        assertNotNull(shim);

        JSONObject remoteDep = shim.getJSONObject("remote2");
        assertNotNull(remoteDep);
        assertEquals("{\"deps\":[\"remote1\"]}", remoteDep.toString());

        JSONObject scriptDep = shim.getJSONObject("SHARED/script2");
        assertNotNull(scriptDep);
        assertEquals("{\"deps\":[\"SHARED/script1\"]}", scriptDep.toString());

        // requireJS need a map of all resourceIDs and urls
        // requireJS add ".js" automatically so we truncate it from url
        JSONObject paths = config.getJSONObject("paths");
        assertNotNull(paths);
        // Return remote module/script url as it's declared in gatein-resources.xml
        assertEquals("http://js/remote1.js", paths.getString("remote1"));
        assertEquals("http://js/remote2.js", paths.getString("remote2"));

        // module1 and module2 are grouped
        assertTrue(paths.getString("SHARED/module1").contains("/mockwebapp/js/fooGroup.js"));
        assertTrue(paths.getString("SHARED/module2").contains("/mockwebapp/js/fooGroup.js"));

        // navController url for scripts
        assertTrue(paths.getString("SHARED/script1").contains("/mockwebapp/js/script1.js"));
        assertTrue(paths.getString("SHARED/script2").contains("/mockwebapp/js/script2.js"));
    }

    public void testGenerateURL() throws Exception {
        ResourceId remote1 = new ResourceId(ResourceScope.SHARED, "remote1");
        String remoteURL = jsService.generateUrl(remote1);
        // Return remote module/script url as it's declared in gatein-resources.xml
        assertEquals("http://js/remote1.js", remoteURL);

        ResourceId module1 = new ResourceId(ResourceScope.SHARED, "module1");
        remoteURL = jsService.generateUrl(module1);
        assertEquals("/mockwebapp/js/module1.js?hash=-55414319&scope=SHARED&minify=true", remoteURL);
    }

    public void testGenerateURLWithLocale() throws Exception {
      ScriptResource resource = jsService.getResource(new ResourceId(ResourceScope.SHARED, "text"));
      resource.addSupportedLocale(Locale.ENGLISH);
      resource.addSupportedLocale(Locale.FRENCH);

      JSONObject config = jsService.getJSConfig();
      assertNotNull(config);
      assertNotNull(config.getJSONObject("paths"));
      assertNotNull(config.getJSONObject("paths").getString("SHARED/text"));
      assertEquals("/mockwebapp/js/text.js?hash=1278397679&scope=SHARED&minify=true", config.getJSONObject("paths").getString("SHARED/text"));

      config = jsService.getJSConfig();
      assertNotNull(config);
      assertNotNull(config.getJSONObject("paths"));
      assertNotNull(config.getJSONObject("paths").getString("SHARED/text"));
      assertTrue(config.getJSONObject("paths").getString("SHARED/text").contains("/mockwebapp/js/text.js"));
    }

    public void testNormalize() throws Exception {
        String normalizedJS = "\ndefine('SHARED/normalize_test', [], function() {"
                + "\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;\neXo.define.names=[];\neXo.define.deps=[];"
                + "\nreturn ggg; // /* */ \n\n});";
        assertReader(normalizedJS, jsService.getScript(new ResourceId(ResourceScope.SHARED, "normalize_test"), null));
    }

    public void testReloadServesTheChangedScriptOnly() throws Exception {
        JSONObject paths = jsService.getJSConfig().getJSONObject("paths");
        String script1Url = paths.getString("SHARED/script1");
        String script2Url = paths.getString("SHARED/script2");
        try {
            mockResources.put("/js/script1.js", "aaa2;");
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);

            JSONObject reloaded = jsService.getJSConfig().getJSONObject("paths");
            assertNotEquals(script1Url, reloaded.getString("SHARED/script1"));
            assertEquals(script2Url, reloaded.getString("SHARED/script2"));
            assertTrue(content("script1").startsWith("aaa2;"));
            assertTrue(reloaded.getString("SHARED/script1").contains("hash=" + scriptHash("script1")));
        } finally {
            restoreAndReload();
        }
        // Same content, same URL
        assertEquals(script1Url, jsService.getJSConfig().getJSONObject("paths").getString("SHARED/script1"));
    }

    public void testReloadKeepsTheCurrentScriptsUntilTheSwap() throws Exception {
        JSONObject config = jsService.getJSConfig();
        String script1 = content("script1");
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        READ_HOOKS.put("/js/script1.js", () -> {
            reading.countDown();
            await(gate);
        });
        try {
            mockResources.put("/js/script1.js", "aaa3;");
            Future<?> reload = jsService.scheduleReload();
            assertTrue(reading.await(30, TimeUnit.SECONDS));

            // Pages neither wait for the rebuild nor see part of it
            assertSame(config, CompletableFuture.supplyAsync(jsService::getJSConfig).get(5, TimeUnit.SECONDS));
            assertEquals(script1, CompletableFuture.supplyAsync(() -> content("script1")).get(5, TimeUnit.SECONDS));

            gate.countDown();
            reload.get(30, TimeUnit.SECONDS);
            assertNotSame(config, jsService.getJSConfig());
            assertTrue(content("script1").startsWith("aaa3;"));
        } finally {
            gate.countDown();
            READ_HOOKS.clear();
            restoreAndReload();
        }
    }

    public void testReloadKeepsTheCurrentScriptsWhenAModuleFails() throws Exception {
        JSONObject config = jsService.getJSConfig();
        String script1 = content("script1");
        READ_HOOKS.put("/js/script2.js", () -> {
            throw new IllegalStateException("unreadable");
        });
        try {
            mockResources.put("/js/script1.js", "aaa4;");
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);
            assertSame(config, jsService.getJSConfig());
            assertEquals(script1, content("script1"));

            // Once the module is fixed, the reload can run again
            READ_HOOKS.clear();
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);
            assertNotSame(config, jsService.getJSConfig());
            assertTrue(content("script1").startsWith("aaa4;"));
        } finally {
            READ_HOOKS.clear();
            restoreAndReload();
        }
    }

    public void testReloadJavascriptChecksTheModule() throws Exception {
        assertReloadRefused("script1", "SCOPE/id");
        assertReloadRefused("SHARED/unknown", "No javascript module");
        try {
            jsService.reloadJavascript("UNKNOWN/script1");
            fail("An unknown scope is refused");
        } catch (IllegalArgumentException e) {
            // expected
        }
        jsService.reloadJavascript("SHARED/script1");

        // A portlet module's name holds its context path
        String descriptor = "<gatein-resources xmlns=\"http://www.exoplatform.org/xml/ns/gatein_resources_1_5\">"
                + "<portlet><name>FooPortlet</name><module><script><path>/js/script1.js</path></script></module></portlet>"
                + "</gatein-resources>";
        JavascriptConfigParser.processConfigResource(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)),
                                                     jsService,
                                                     mockServletContext);
        ResourceId portletModule = new ResourceId(ResourceScope.PORTLET, "mockwebapp/FooPortlet");
        assertNotNull(jsService.getResource(portletModule));
        try {
            jsService.reloadJavascript("PORTLET/mockwebapp/FooPortlet");
        } finally {
            jsService.getScriptGraph().removeResource(portletModule);
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);
        }
    }

    public void testASecondReloadWaitsForTheFirst() throws Exception {
        // Every minification is in the cache: a rebuild reads the files within milliseconds
        jsService.scheduleReload().get(30, TimeUnit.SECONDS);
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger script1Reads = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();
        // The first rebuild's read of script1 waits for the gate
        for (String path : mockResources.keySet()) {
            READ_HOOKS.put(path, () -> {
                reads.incrementAndGet();
                if ("/js/script1.js".equals(path) && script1Reads.incrementAndGet() == 1) {
                    reading.countDown();
                    await(gate);
                }
            });
        }
        try {
            Future<?> first = jsService.scheduleReload();
            assertTrue(reading.await(30, TimeUnit.SECONDS));
            int firstRebuildReads = awaitStable(reads);
            Future<?> second = jsService.scheduleReload();

            Thread.sleep(2000);
            assertEquals("The second rebuild started during the first one", firstRebuildReads, reads.get());
            assertFalse(second.isDone());

            gate.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
            assertEquals(2, script1Reads.get());
        } finally {
            gate.countDown();
            READ_HOOKS.clear();
        }
    }

    public void testManagedReloadsServeTheChangedScript() throws Exception {
        // The current scripts serve script1 before it changes
        assertTrue(content("script1").startsWith("aaa;"));
        String url = jsService.getJSConfig().getJSONObject("paths").getString("SHARED/script1");
        try {
            mockResources.put("/js/script1.js", "aaa5;");
            jsService.reloadJavascripts();
            awaitServed("script1", "aaa5;");
            assertNotEquals(url, jsService.getJSConfig().getJSONObject("paths").getString("SHARED/script1"));

            mockResources.put("/js/script1.js", "aaa6;");
            jsService.reloadJavascript("SHARED/script1");
            awaitServed("script1", "aaa6;");
        } finally {
            restoreAndReload();
        }
    }

    public void testAModuleTheCompilerRejectsIsServedAsIs() throws Exception {
        JSONObject config = jsService.getJSConfig();
        try {
            mockResources.put("/js/script1.js", "var = ;");
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);

            // The reload applies, and the module is served unminified
            assertNotSame(config, jsService.getJSConfig());
            assertTrue(content("script1"), content("script1").contains("var = ;"));
            assertTrue(jsService.getJSConfig()
                                .getJSONObject("paths")
                                .getString("SHARED/script1")
                                .contains("hash=" + scriptHash("script1")));
        } finally {
            restoreAndReload();
        }
    }

    public void testAGroupedModuleTheCompilerRejectsIsServedAsIs() throws Exception {
        JSONObject config = jsService.getJSConfig();
        try {
            mockResources.put("/js/module1.js", "var = ;");
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);

            // The reload applies, and the group serves the module unminified
            assertNotSame(config, jsService.getJSConfig());
            ScriptContent group = jsService.getScriptContent(ResourceScope.GROUP, "fooGroup", true);
            String content = new String(group.getContentAsBytes(), StandardCharsets.UTF_8);
            assertTrue(content, content.contains("var = ;"));
            ScriptResource module1 = jsService.getResource(new ResourceId(ResourceScope.SHARED, "module1"));
            assertTrue(jsService.generateUrl(module1).contains("hash=" + group.getHash()));
        } finally {
            mockResources.put("/js/module1.js", "ccc;");
            jsService.scheduleReload().get(30, TimeUnit.SECONDS);
        }
    }

    public void testTheStartupBuildKeepsTheMinifiedScriptsItReads() throws Exception {
        Path directory = Files.createTempDirectory("test-js-cache-startup");
        String previousDirectory = PropertyManager.getProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY);
        PropertyManager.setProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY, directory.toString());
        try {
            // A server's startup build writes the minified scripts
            startedService().stop();

            // No build read them for more than the retention: each is marked, to tell a cache hit from a new minification
            FileTime old = FileTime.from(Instant.now().minus(MinifiedScriptCache.RETENTION).minus(Duration.ofDays(1)));
            Map<Path, String> minified = new HashMap<>();
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.toList()) {
                    String content = Files.readString(file) + "/*from the cache*/";
                    Files.writeString(file, content);
                    Files.setLastModifiedTime(file, old);
                    minified.put(file, content);
                }
            }
            assertFalse(minified.isEmpty());
            Path unused = Files.writeString(directory.resolve("0".repeat(64) + ".js"), "unused;");
            Files.setLastModifiedTime(unused, old);

            // The next startup build reads them from the cache before the cleanup deletes the unused one
            startedService().stop();
            assertFalse(Files.exists(unused));
            for (Map.Entry<Path, String> file : minified.entrySet()) {
                assertEquals(file.getKey().toString(), file.getValue(), Files.readString(file.getKey()));
            }
        } finally {
            if (previousDirectory == null) {
                System.clearProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY);
                PropertyManager.refresh();
            } else {
                PropertyManager.setProperty(MinifiedScriptCache.CACHE_DIR_PROPERTY, previousDirectory);
            }
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(directory);
        }
    }

    /**
     * @return a new service over the mock webapp, once its startup build ran
     */
    private JavascriptConfigService startedService() throws Exception {
        PortalContainer container = getContainer();
        JavascriptConfigService service = new JavascriptConfigService(container.getContext(),
                                                                      container.getComponentInstanceOfType(ResourceCompressor.class));
        service.registerContext(new WebAppImpl(mockServletContext, Thread.currentThread().getContextClassLoader()));
        URL url = container.getPortalClassLoader().getResource("mockwebapp/gatein-resources.xml");
        JavascriptConfigParser.processConfigResource(url.openStream(), service, mockServletContext);
        service.initData();
        // The reload thread runs one task at a time: once this one ran, the startup build had
        service.scheduleReload().get(30, TimeUnit.SECONDS);
        return service;
    }

    /**
     * @return the counter's value once it stayed the same for half a second
     */
    private static int awaitStable(AtomicInteger counter) throws InterruptedException {
        int value;
        do {
            value = counter.get();
            Thread.sleep(500);
        } while (value != counter.get());
        return value;
    }

    /**
     * Waits for a reload to serve the script's new content, under a URL
     * naming its hash.
     */
    private void awaitServed(String script, String content) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!content(script).startsWith(content)
                || !jsService.getJSConfig()
                             .getJSONObject("paths")
                             .getString("SHARED/" + script)
                             .contains("hash=" + scriptHash(script))) {
            assertTrue(script + " is not served with " + content, System.currentTimeMillis() < deadline);
            Thread.sleep(50);
        }
    }

    private void assertReloadRefused(String jsModule, String message) {
        try {
            jsService.reloadJavascript(jsModule);
            fail(jsModule + " is refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(message));
        }
    }

    private String content(String script) {
        return new String(jsService.getScriptContent(ResourceScope.SHARED, script, true).getContentAsBytes(),
                          StandardCharsets.UTF_8);
    }

    private long scriptHash(String script) {
        return jsService.getScriptContent(ResourceScope.SHARED, script, true).getHash();
    }

    private void restoreAndReload() throws Exception {
        mockResources.put("/js/script1.js", "aaa;");
        jsService.scheduleReload().get(30, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void testResolveIDs() {
        Map<ResourceId, FetchMode> tmp = new HashMap<ResourceId, FetchMode>();
        tmp.put(new ResourceId(ResourceScope.SHARED, "script2"), null);

        Map<ScriptResource, FetchMode> ids = jsService.resolveIds(tmp);
        // Only resolve dependencies for SCRIPTS
        assertEquals(2, ids.size());
        List<ScriptResource> result = new ArrayList<ScriptResource>(ids.keySet());
        assertEquals("script1", result.get(0).getId().getName());
        assertEquals("script2", result.get(1).getId().getName());

        tmp.clear();
        // module2 depends on module1 but we don't resolve dependencies for it
        tmp.put(new ResourceId(ResourceScope.SHARED, "module2"), null);
        ids = jsService.resolveIds(tmp);
        assertEquals(1, ids.size());
        assertEquals("module2", ids.keySet().iterator().next().getId().getName());
    }

    /*
     * public void testCommonJScripts() { assertEquals(5, jsService.getCommonJScripts().size());
     * assertTrue(jsService.isModuleLoaded("js.test1")); assertTrue(jsService.isModuleLoaded("js.test2"));
     * assertTrue(jsService.isModuleLoaded("js.test3")); assertTrue(jsService.isModuleLoaded("js.test4"));
     * assertTrue(jsService.isModuleLoaded("js.test7"));
     *
     * assertFalse(jsService.isModuleLoaded("js.test5"));
     *
     * // InputStream script = jsService.getScript(new ResourceId(ResourceScope.SHARED, "common"), "js.test1");
     * assertNotNull(script); }
     */

    private void assertReader(String expected, Reader actual) throws Exception {
        StringWriter buffer = new StringWriter();
        IOTools.copy(actual, buffer, 1);
        assertEquals(expected, buffer.toString());
    }

    /*
     * public void testPortalJScript() throws IOException { Collection<Javascript> site = jsService.getPortalJScripts("site1");
     * assertEquals(1, site.size()); Iterator<Javascript> iterator = site.iterator();
     * assertEquals(mockServletContext.getContextPath() + "/js/test5.js", iterator.next().getPath());
     *
     * // InputStream script = jsService.getScript(new ResourceId(ResourceScope.PORTAL, "site1")); assertNotNull(script);
     * assertEquals("", read(script));
     *
     * site = jsService.getPortalJScripts("site2"); assertEquals(2, site.size()); iterator = site.iterator();
     * assertEquals(mockServletContext.getContextPath() + "/js/test6.js", iterator.next().getPath());
     * assertEquals(mockServletContext.getContextPath() + "/js/test5.js", iterator.next().getPath());
     *
     * // script = jsService.getScript(new ResourceId(ResourceScope.PORTAL, "site2")); assertNotNull(script); assertEquals("",
     * read(script));
     *
     * // assertNull(jsService.getPortalJScripts("classic")); assertNull(jsService.getScript(new
     * ResourceId(ResourceScope.PORTAL, "classic")));
     *
     * // jsService.removePortalJScripts("site1"); assertNull(jsService.getPortalJScripts("site1"));
     * assertNull(jsService.getScript(new ResourceId(ResourceScope.PORTAL, "site1")));
     *
     * // Javascript portalJScript = Javascript.create(new ResourceId(ResourceScope.PORTAL, "/portal"), "portal1",
     * "/mockwebapp", "/portal", Integer.MAX_VALUE); jsService.addPortalJScript(portalJScript); String jScript =
     * jsService.getJScript(portalJScript.getPath()); assertNull(jScript); assertEquals("", read(script));
     * resResolver.addResource(portalJScript.getPath(), "bar1"); jScript = jsService.getJScript(portalJScript.getPath());
     * assertEquals("bar1", jScript); }
     */

    private static class MockControllerContext extends ControllerContext {
        public MockControllerContext() {
            super(null, null, new MockServletRequest(null, null), null, null);
        }

        @Override
        public void renderURL(Map<QualifiedName, String> parameters, URIWriter uriWriter) throws IOException {
            String lang = parameters.get(QualifiedName.create("gtn", "lang"));
            lang = StringUtils.isBlank(lang) ? "" : "-" + lang;
            uriWriter.appendSegment("mock_url_of_" + parameters.get(QualifiedName.create("gtn", "resource")) + lang + ".js");
        }
    }

    private static class MockJSServletContext extends MockServletContext {
        private Map<String, String> resources;

        public MockJSServletContext(String contextName, Map<String, String> resources) {
            super(contextName);
            this.resources = resources;
        }

        public String getContextPath() {
            return "/" + getServletContextName();
        }

        @Override
        public InputStream getResourceAsStream(String s) {
            Runnable hook = READ_HOOKS.get(s);
            if (hook != null) {
                hook.run();
            }
            String input = resources.get(s);
            if (input != null) {
                return new ByteArrayInputStream(input.getBytes());
            } else {
                return null;
            }
        }
    }
}
