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
package org.exoplatform.portal.resource.compressor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.google.javascript.jscomp.SourceFile;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.resource.AbstractWebResourceTest;
import org.exoplatform.portal.resource.compressor.impl.ClosureCompressorPlugin;
import org.exoplatform.portal.resource.compressor.impl.JSMinCompressorPlugin;
import org.exoplatform.portal.resource.compressor.impl.ResourceCompressorService;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;

public class TestResourceCompressorService extends AbstractWebResourceTest {
    public void testInitializing() {
        ResourceCompressorService compressor = (ResourceCompressorService) getContainer().getComponentInstanceOfType(
                ResourceCompressor.class);
        assertNotNull(compressor);
        assertTrue(compressor instanceof ResourceCompressorService);

        assertNotNull(compressor.getCompressorPlugin(ResourceType.JAVASCRIPT, "MockCompressorPlugin"));

        assertNotNull(compressor.getCompressorPlugin(ResourceType.JAVASCRIPT, "JSMinCompressorPlugin"));
    }

    public void testPriority() {
        ResourceCompressorService compressor = (ResourceCompressorService) getContainer().getComponentInstanceOfType(
                ResourceCompressor.class);
        ResourceCompressorPlugin plugin = compressor.getHighestPriorityCompressorPlugin(ResourceType.JAVASCRIPT);
        assertTrue(plugin instanceof JSMinCompressorPlugin);
    }

    public void testJSMinCompressing() throws IOException {
        File jsFile = new File("src/test/resources/javascript.js");
        File jsCompressedFile = new File("target/jsmin-compressed-file.js");

        Reader reader = new FileReader(jsFile);
        Writer writer = new FileWriter(jsCompressedFile);

        ResourceCompressorService compressor = (ResourceCompressorService) getContainer().getComponentInstanceOfType(
                ResourceCompressor.class);
        try {
            compressor.compress(reader, writer, ResourceType.JAVASCRIPT);
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
        writer.close();
        assertTrue(jsCompressedFile.length() > 0);
        assertTrue(jsFile.length() > jsCompressedFile.length());
        log.info("The original javascript (" + getFileSize(jsFile) + ") is compressed by JSMIN into "
                + jsCompressedFile.getAbsolutePath() + " (" + getFileSize(jsCompressedFile) + ")");
    }

    public void testClosureCompressing() throws Exception {
        File jsFile = new File("src/test/resources/javascript.js");
        File jsCompressedFile = new File("target/closure-compressed-file.js");
        Reader reader = new FileReader(jsFile);
        Writer writer = new FileWriter(jsCompressedFile);

        ResourceCompressorService compressor = (ResourceCompressorService) getContainer().getComponentInstanceOfType(
                ResourceCompressor.class);

        InitParams priorityParam = new InitParams();
        ValueParam param = new ValueParam();
        param.setName("plugin.priority");
        param.setValue("10");
        priorityParam.addParameter(param);
        ClosureCompressorPlugin plugin = new ClosureCompressorPlugin(priorityParam);
        compressor.registerCompressorPlugin(plugin);
        try {
            compressor.compress(reader, writer, ResourceType.JAVASCRIPT);
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        } finally {
            unregisterCompressorPlugin(compressor, plugin);
            reader.close();
            writer.close();
        }

        assertTrue(jsCompressedFile.length() > 0);
        assertTrue(jsFile.length() > jsCompressedFile.length());
        log.info("The original javascript (" + getFileSize(jsFile) + ") is compressed by CLOSURE COMPILER into "
                + jsCompressedFile.getAbsolutePath() + " (" + getFileSize(jsCompressedFile) + ")");

        String expectedJS = closureCompress(jsFile);
        assertEquals(expectedJS.length(), jsCompressedFile.length());
    }

    private void unregisterCompressorPlugin(ResourceCompressorService compressor, ResourceCompressorPlugin plugin)
            throws Exception {
        Field pluginsField = compressor.getClass().getDeclaredField("plugins");
        pluginsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ResourceType, List<ResourceCompressorPlugin>> plugins = (Map<ResourceType, List<ResourceCompressorPlugin>>) pluginsField
                .get(compressor);
        plugins.get(plugin.getResourceType()).remove(plugin);
    }

    private String closureCompress(File input) throws Exception {
        Compiler compiler = new Compiler();
        CompilerOptions options = new CompilerOptions();
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_2016);
        options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        SourceFile extern = SourceFile.fromCode("extern", "");

        SourceFile jsInput;
        try {
            Path path = Paths.get(input.getPath());
            jsInput = SourceFile.fromPath(path, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ResourceCompressorException(ex);
        }

        compiler.compile(extern, jsInput, options);
        return compiler.toSource();
    }

    public void testYUICSSCompressing() throws IOException {
        File cssFile = new File("src/test/resources/Stylesheet.css");
        File compressedCssFile = new File("target/yui-compressed-file.css");

        Reader reader = new FileReader(cssFile);
        Writer writer = new FileWriter(compressedCssFile);

        ResourceCompressorService compressor = (ResourceCompressorService) getContainer().getComponentInstanceOfType(
                ResourceCompressor.class);
        try {
            compressor.compress(reader, writer, ResourceType.STYLESHEET);
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
        writer.close();
        assertTrue(compressedCssFile.length() > 0);
        assertTrue(cssFile.length() > compressedCssFile.length());
        log.info("The original CSS (" + getFileSize(cssFile) + ") is compressed by YUI library into "
                + compressedCssFile.getAbsolutePath() + " (" + getFileSize(compressedCssFile) + ")");
    }

    private String getFileSize(File file) {
        return (file.length() / 1024) + " KB";
    }
}
