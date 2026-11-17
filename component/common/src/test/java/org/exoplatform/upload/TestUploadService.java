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
package org.exoplatform.upload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/services/upload-service.xml")
})
public class TestUploadService extends AbstractKernelTest {
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    UploadService uploadService;

    @Override
    protected void beforeRunBare() {
        super.beforeRunBare();
        uploadService = getContainer().getComponentInstanceOfType(UploadService.class);
    }

    @Test
    public void testCorrectFileName() {
        String fileName;

        fileName = "test_abcdef0123456789.ext";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_abcdef0123456789.ext", fileName);
        assertFileCreation(fileName);

        fileName = "test~`!@#$%^&*()-=+_.ext";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test~`!@#$%^&_()-=+_.ext", fileName);
        assertFileCreation(fileName);

        fileName = "test[]{}\\|.ext";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test[]{}__.ext", fileName);
        assertFileCreation(fileName);

        fileName = "test;:\"'.ext";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test;__'.ext", fileName);
        assertFileCreation(fileName);

        fileName = "test_tên tiếng việt.mở rộng";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_tên tiếng việt.mở rộng", fileName);
        assertFileCreation(fileName);

        fileName = "test_越南人的名字。扩展";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_越南人的名字。扩展", fileName);
        assertFileCreation(fileName);

        fileName = "test_В'єтнамські імена. розширення";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_В'єтнамські імена. розширення", fileName);
        assertFileCreation(fileName);

        fileName = "test_베트남어 이름. 확장";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_베트남어 이름. 확장", fileName);
        assertFileCreation(fileName);

        fileName = "test_वियतनामी नामों. विस्तार";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_वियतनामी नामों. विस्तार", fileName);
        assertFileCreation(fileName);

        fileName = "test_ベトナム語名。拡大";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_ベトナム語名。拡大", fileName);
        assertFileCreation(fileName);

        fileName = "test_ਵੀਅਤਨਾਮੀ ਨਾਮ. ਵਿਸਤਾਰ";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_ਵੀਅਤਨਾਮੀ ਨਾਮ. ਵਿਸਤਾਰ", fileName);
        assertFileCreation(fileName);

        fileName = "test_Вьетнамские имена. расширение";
        fileName = uploadService.correctFileName(fileName);
        assertEquals("test_Вьетнамские имена. расширение", fileName);
        assertFileCreation(fileName);
    }

    public void testShouldNotCreateUploadResourceWhenUploadIdContainsDots() {
        String notAllowedUploadId = ".webapps.ROOT.123456";
        try {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            uploadService.createUploadResource(notAllowedUploadId, request);
            fail("The upload resource creation must fail with the id " + notAllowedUploadId);
        } catch(FileUploadException e) {
            assertEquals("Upload id " + notAllowedUploadId
                    + " is not valid, it cannot be null or contain '.' , '/' or '\\'", e.getMessage());
        }
    }

    public void testShouldNotCreateUploadResourceWhenUploadIdContainsSlashs() {
        String notAllowedUploadId = "/webapps/ROOT/123456";
        try {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            uploadService.createUploadResource(notAllowedUploadId, request);
            fail("The upload resource creation must fail with the id " + notAllowedUploadId);
        } catch(FileUploadException e) {
            assertEquals("Upload id " + notAllowedUploadId
                    + " is not valid, it cannot be null or contain '.' , '/' or '\\'", e.getMessage());
        }
    }

    public void testShouldNotCreateUploadResourceWhenUploadIdContainsAntiSlashs() {
        String notAllowedUploadId = "\\webapps\\ROOT\\123456";
        try {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            uploadService.createUploadResource(notAllowedUploadId, request);
            fail("The upload resource creation must fail with the id " + notAllowedUploadId);
        } catch(FileUploadException e) {
            assertEquals("Upload id " + notAllowedUploadId
                    + " is not valid, it cannot be null or contain '.' , '/' or '\\'", e.getMessage());
        }
    }

    @Test
    public void testShouldRejectUnsafeSvgUpload() throws Exception {
        SvgUploadValidator validator = new SvgUploadValidator();
        String unsafeSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"><rect width=\"10\" height=\"10\"/></svg>";

        try (InputStream inputStream = new ByteArrayInputStream(unsafeSvg.getBytes(StandardCharsets.UTF_8))) {
            validator.validate("infected.svg", "image/svg+xml", inputStream);
            fail("Unsafe SVG upload must be rejected");
        } catch (FileUploadException e) {
            assertEquals(SvgUploadValidator.INFECTED_SVG_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void testShouldAcceptDeeplyNestedSvgUpload() throws Exception {
        validateDeepSvg(new SvgUploadValidator(), 5000);
    }

    @Test
    public void testShouldApplyTheConfiguredSvgDepthLimit() throws Exception {
        try {
            PropertyManager.setProperty(SvgUploadValidator.MAX_ELEMENT_DEPTH_PROPERTY, "100");
            SvgUploadValidator validator = new SvgUploadValidator();
            validateDeepSvg(validator, 99);
            try {
                validateDeepSvg(validator, 100);
                fail("An SVG nested deeper than the configured limit must be rejected");
            } catch (FileUploadException e) {
                assertEquals("Invalid SVG file", e.getMessage());
            }

            PropertyManager.setProperty(SvgUploadValidator.MAX_ELEMENT_DEPTH_PROPERTY, "not a number");
            validateDeepSvg(new SvgUploadValidator(), 5000);
        } finally {
            System.clearProperty(SvgUploadValidator.MAX_ELEMENT_DEPTH_PROPERTY);
            PropertyManager.refresh();
        }
    }

    @Test
    public void testShouldRejectAnSvgReferencingAnExternalResource() throws Exception {
        SvgUploadValidator validator = new SvgUploadValidator();
        String[] external = {
            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"><image xlink:href=\"https://tracker.example/p.png\"/></svg>",
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><use href=\"other.svg#icon\"/></svg>",
        };
        for (String svg : external) {
            try (InputStream inputStream = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
                validator.validate("logo.svg", "image/svg+xml", inputStream);
                fail("An external reference must be refused: " + svg);
            } catch (FileUploadException e) {
                assertEquals(SvgUploadValidator.INFECTED_SVG_MESSAGE, e.getMessage());
            }
        }
        String[] internal = {
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><defs><g id=\"a\"/></defs><use href=\"#a\"/></svg>",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"><image xlink:href=\"data:image/png;base64,iVBORw0KGgo=\"/></svg>",
        };
        for (String svg : internal) {
            try (InputStream inputStream = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
                validator.validate("logo.svg", "image/svg+xml", inputStream);
            }
        }
    }

    @Test
    public void testShouldRejectAnSvgReachingAnExternalResourceThroughCss() throws Exception {
        String[] external = {
            svg("<style>@import url(https://attacker.example/t.css);</style>"),
            svg("<style>@import \"https://attacker.example/t.css\";</style>"),
            svg("<style><![CDATA[ .a { fill: url(https://attacker.example/p#g); } ]]></style>"),
            svg("<style>.a { background: url( 'https://attacker.example/p.png' ) }</style>"),
            svg("<style>.a { background: image-set(\"https://attacker.example/p.png\" 1x) }</style>"),
            svg("<style>.a { background: src(\"https://attacker.example/p.png\") }</style>"),
            svg("<rect style=\"fill:url(https://attacker.example/p#g)\"/>"),
            svg("<rect style=\"fill:url(&quot;//attacker.example/p#g&quot;)\"/>"),
            svg("<rect fill=\"url(https://attacker.example/p#g)\"/>"),
            svg("<rect filter=\"url(other.svg#blur)\"/>"),
            svg("<rect mask=\"url(https://attacker.example/m#m)\"/>"),
            svg("<rect style=\"fill:u\\72l(https://attacker.example/p#g)\"/>"),
            svg("<rect style=\"fill:url(/**/https://attacker.example/p#g)\"/>"),
            svg("<style>@\\69mport url(https://attacker.example/t.css);</style>"),
        };
        for (String svg : external) {
            assertRefused(svg);
        }
    }

    @Test
    public void testShouldAcceptAnSvgReferencingOnlyItselfThroughCss() throws Exception {
        String[] internal = {
            svg("<defs><linearGradient id=\"g\"/></defs><rect fill=\"url(#g)\"/>"),
            svg("<defs><linearGradient id=\"g\"/></defs><rect style=\"fill: url( '#g' ); stroke: url(&quot;#g&quot;)\"/>"),
            svg("<defs><filter id=\"f\"/></defs><style>.a { filter: url(#f); fill: red; } /* url(https://x) */</style><rect class=\"a\"/>"),
            svg("<style>.a { background: url(data:image/png;base64,iVBORw0KGgo=) }</style><rect class=\"a\"/>"),
            svg("<rect style=\"background-image: url('data:image/svg+xml;utf8,&lt;svg/&gt;')\"/>"),
            svg("<style>.a { fill: #333; stroke: rgb(1, 2, 3) }</style><rect class=\"a\" style=\"opacity: .5\"/>"),
        };
        for (String svg : internal) {
            try (InputStream inputStream = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
                new SvgUploadValidator().validate("logo.svg", "image/svg+xml", inputStream);
            }
        }
    }

    @Test
    public void testShouldRejectAnAnimationGivingAnHrefAnExternalTarget() throws Exception {
        String[] external = {
            svg("<image><set attributeName=\"href\" to=\"https://attacker.example/p.png\"/></image>"),
            svg("<image><set attributeName=\"xlink:href\" to=\"https://attacker.example/p.png\"/></image>"),
            svg("<image><animate attributeName=\"href\" values=\"#a; https://attacker.example/p.png\" dur=\"1s\"/></image>"),
            svg("<image><animate attributeName=\" href \" from=\"//attacker.example/p.png\" to=\"#a\" dur=\"1s\"/></image>"),
            svg("<use><animate attributeName=\"href\" by=\"other.svg#a\" dur=\"1s\"/></use>"),
        };
        for (String svg : external) {
            assertRefused(svg);
        }
    }

    @Test
    public void testShouldAcceptAnAnimationOfOtherAttributesAndOfHrefToItself() throws Exception {
        String[] allowed = {
            svg("<rect><set attributeName=\"fill\" to=\"red\"/></rect>"),
            svg("<rect><animate attributeName=\"width\" values=\"1;5;9\" dur=\"1s\"/></rect>"),
            svg("<defs><g id=\"a\"/><g id=\"b\"/></defs><use href=\"#a\"><set attributeName=\"href\" to=\"#b\"/></use>"),
            svg("<defs><g id=\"a\"/><g id=\"b\"/></defs><use href=\"#a\"><animate attributeName=\"href\" values=\"#a;#b\" dur=\"1s\"/></use>"),
            svg("<image><set attributeName=\"href\" to=\"data:image/png;base64,iVBORw0KGgo=\"/></image>"),
        };
        for (String svg : allowed) {
            try (InputStream inputStream = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
                new SvgUploadValidator().validate("logo.svg", "image/svg+xml", inputStream);
            }
        }
    }

    private static String svg(String content) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">" + content + "</svg>";
    }

    private static void assertRefused(String svg) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
            new SvgUploadValidator().validate("logo.svg", "image/svg+xml", inputStream);
            fail("An external reference must be refused: " + svg);
        } catch (FileUploadException e) {
            assertEquals(svg, SvgUploadValidator.INFECTED_SVG_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void testSvgValidatorShouldNotSupportNonSvgFiles() {
        SvgUploadValidator validator = new SvgUploadValidator();

        assertFalse(validator.supports("document.xml", "application/xml"));
        assertFalse(validator.supports("page.html", "text/html"));
        assertFalse(validator.supports("presentation.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertFalse(validator.supports("spreadsheet.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private void validateDeepSvg(SvgUploadValidator validator, int groups) throws Exception {
        String deepSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\">" + "<g>".repeat(groups) + "</g>".repeat(groups) + "</svg>";
        try (InputStream inputStream = new ByteArrayInputStream(deepSvg.getBytes(StandardCharsets.UTF_8))) {
            validator.validate("deep.svg", "image/svg+xml", inputStream);
        }
    }

    private void assertFileCreation(String fileName) {
        String file = tmpDir + File.separator + fileName;
        File f = new File(file);
        f.deleteOnExit();
        try {
            f.createNewFile();
        } catch (IOException ex) {
            fail("Can not create file with name: " + fileName);
        }
    }
}
