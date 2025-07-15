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
package org.exoplatform.portal.resource.compressor.impl;

import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.WarningLevel;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.portal.resource.compressor.BaseResourceCompressorPlugin;
import org.exoplatform.portal.resource.compressor.ResourceCompressorException;
import org.exoplatform.portal.resource.compressor.ResourceType;

/**
 * A compressor based on the Google <a href="http://code.google.com/closure/">Closure Tools</a> javascript compiler. This plugin
 * compresses javascript resources and provides three levels of compression, configured via the <code>level</code> init param:
 * <ul>
 * <li><code>basic</code> encodes with the <code>WHITESPACE_ONLY</code> Closure level</li>
 * <li><code>simple</code> encodes with the <code>SIMPLE_OPTIMIZATIONS</code> Closure level</li>
 * <li><code>advanced</code> encodes with the <code>ADVANCED_OPTIMIZATIONS</code> Closure level</li>
 * </ul>
 * The precise meaning of the Closure levels are described on
 * <a href="http://code.google.com/closure/compiler/docs/compilation_levels.html">this page</a>.
 */
public class ClosureCompressorPlugin extends BaseResourceCompressorPlugin {

    /** . */
    private volatile CompilationLevel compilationLevel;

    public ClosureCompressorPlugin(InitParams params) {
        super(params);

        //
        ValueParam vp = params.getValueParam("level");
        CompilationLevel level = null;
        if (vp != null) {
            String value = vp.getValue().trim().toLowerCase();
            log.debug("found compressor level configuration " + value);
            setLevel(value);
        }

        //
        if (level == null) {
            log.debug("no compressor level found, will use simple level instead");
            level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
        }

        //
        this.compilationLevel = level;
    }

    @Managed
    @ManagedDescription("The compression level value among {basic,simple,advanced}")
    public String getLevel() {
        if (compilationLevel == CompilationLevel.WHITESPACE_ONLY) {
            return "basic";
        } else if (compilationLevel == CompilationLevel.ADVANCED_OPTIMIZATIONS) {
            return "advanced";
        } else {
            return "simple";
        }
    }

    @Managed
    public void setLevel(String level) {
        if (level != null) {
            level = level.trim().toLowerCase();
            if ("basic".equals(level)) {
                log.debug("configuring to basic level configuration");
                compilationLevel = CompilationLevel.WHITESPACE_ONLY;
            } else if ("simple".equals(level)) {
                log.debug("configuring to simple level configuration");
                compilationLevel = CompilationLevel.SIMPLE_OPTIMIZATIONS;
            } else if ("advanced".equals(level)) {
                log.debug("configuring to advanced level configuration");
                compilationLevel = CompilationLevel.ADVANCED_OPTIMIZATIONS;
            }
        }
    }

    public ResourceType getResourceType() {
        return ResourceType.JAVASCRIPT;
    }

    public void compress(Reader input, Writer output) throws ResourceCompressorException {
        CompilationLevel level = compilationLevel;
        if (level == null) {
            level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
        }

        //
        Compiler compiler = new Compiler();
        CompilerOptions options = new CompilerOptions();
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_2021);
        options.setStrictModeInput(false);
        options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);
        options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);
        level.setOptionsForCompilationLevel(options);
        WarningLevel.QUIET.setOptionsForWarningLevel(options);
        SourceFile extern = SourceFile.fromCode("extern", "");

        //
        SourceFile jsInput;
        try {
            String code = IOUtils.toString(input);
            code = SourceFile.fromCode("code", code).getCode();
            jsInput = SourceFile.fromCode("jsInput", code);
            compiler.compile(extern, jsInput, options);
            output.write(compiler.toSource());
        } catch (Exception ex) {
            throw new ResourceCompressorException(ex);
        }
    }
}
