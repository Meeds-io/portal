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
package io.meeds.portal.mop;

import java.io.InputStream;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.UnmarshalledObject;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import junit.framework.TestCase;
import lombok.SneakyThrows;

public class PageParsingTest extends TestCase {

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public void testParsePage() {
    try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("page.xml");) {
      UnmarshalledObject<Page> obj = ModelUnmarshaller.unmarshall(Page.class, inputStream);
      Page page = obj.getObject();
      assertNotNull(page);
      assertEquals(1, page.getChildren().size());
      Container sectionsParent = (Container) page.getChildren().get(0);
      assertEquals("system:/groovy/portal/webui/container/UIPageLayout.gtmpl", sectionsParent.getTemplate());

      assertEquals(2, sectionsParent.getChildren().size());

      Container columnsSection = (Container) sectionsParent.getChildren().get(0);
      assertEquals(2, columnsSection.getChildren().size());
      assertEquals("FlexContainer", columnsSection.getTemplate());
      assertNotNull(columnsSection.getCssClass());
      String cssClasses = columnsSection.getCssClass();
      assertTrue("'grid-cols-md-12' not found in:" + cssClasses, cssClasses.contains("grid-cols-md-12"));
      assertTrue("'grid-cols-lg-12' not found in:" + cssClasses, cssClasses.contains("grid-cols-lg-12"));
      assertTrue("'grid-cols-xl-12' not found in:" + cssClasses, cssClasses.contains("grid-cols-xl-12"));
      assertTrue("'layout-mobile-columns' not found in:" + cssClasses, cssClasses.contains("layout-mobile-columns"));
      assertTrue("'layout-sticky-application' not found in:" + cssClasses, cssClasses.contains("layout-sticky-application"));
      assertFalse("'grid-rows' should not be present in:" + cssClasses, cssClasses.contains("grid-rows"));
      assertTrue("'TEST-columns-section-class' not found in:" + cssClasses, cssClasses.contains("TEST-columns-section-class"));

      Container column = (Container) columnsSection.getChildren().get(0);
      assertEquals(2, column.getChildren().size());
      assertEquals("CellContainer", column.getTemplate());
      assertNotNull(column.getCssClass());
      assertTrue("'flex-cell' not found", column.getCssClass().contains("flex-cell"));
      assertTrue("'grid-cell-colspan-md-9' not found", column.getCssClass().contains("grid-cell-colspan-md-9"));
      assertTrue("'grid-cell-colspan-lg-9' not found", column.getCssClass().contains("grid-cell-colspan-lg-9"));
      assertTrue("'grid-cell-colspan-xl-9' not found", column.getCssClass().contains("grid-cell-colspan-xl-9"));
      assertTrue("'grid-cell-rowspan-md-1' not found", column.getCssClass().contains("grid-cell-rowspan-md-1"));
      assertTrue("'grid-cell-rowspan-lg-1' not found", column.getCssClass().contains("grid-cell-rowspan-lg-1"));
      assertTrue("'grid-cell-rowspan-xl-1' not found", column.getCssClass().contains("grid-cell-rowspan-xl-1"));
      assertTrue("'TEST-column-class' custom class not found", column.getCssClass().contains("TEST-column-class"));

      Application columnApplication = (Application) column.getChildren().get(0);
      assertNotNull(columnApplication.getCssClass());
      assertTrue("'mt-0' not found in: " + columnApplication.getCssClass(), columnApplication.getCssClass().contains("mt-0"));
      assertTrue("'mb-n3' not found in: " + columnApplication.getCssClass(), columnApplication.getCssClass().contains("mb-n3"));
      assertTrue("'me-n4' not found", columnApplication.getCssClass().contains("me-n4"));
      assertTrue("'ms-n5' not found", columnApplication.getCssClass().contains("ms-n5"));
      assertTrue("'brtr-4' not found", columnApplication.getCssClass().contains("brtr-4"));
      assertTrue("'brtl-2' not found", columnApplication.getCssClass().contains("brtl-2"));
      assertTrue("'brbr-1' not found", columnApplication.getCssClass().contains("brbr-1"));
      assertTrue("'brbl-0' not found", columnApplication.getCssClass().contains("brbl-0"));
      assertTrue("'hidden-sm-and-down' not found", columnApplication.getCssClass().contains("hidden-sm-and-down"));

      assertTrue(column.getChildren().get(1) instanceof Container);

      Container gridSection = (Container) sectionsParent.getChildren().get(1);
      assertEquals(3, gridSection.getChildren().size());
      assertEquals("GridContainer", gridSection.getTemplate());
      assertNotNull(gridSection.getCssClass());
      assertTrue("'grid-cols-md-4' not found", gridSection.getCssClass().contains("grid-cols-md-4"));
      assertTrue("'grid-cols-lg-4' not found", gridSection.getCssClass().contains("grid-cols-lg-4"));
      assertTrue("'grid-cols-xl-4' not found", gridSection.getCssClass().contains("grid-cols-xl-4"));
      assertTrue("'grid-rows-md-2' not found", gridSection.getCssClass().contains("grid-rows-md-2"));
      assertTrue("'grid-rows-lg-2' not found", gridSection.getCssClass().contains("grid-rows-lg-2"));
      assertTrue("'grid-rows-xl-2' not found", gridSection.getCssClass().contains("grid-rows-xl-2"));
      assertTrue("'TEST-grid-section-class' not found", gridSection.getCssClass().contains("TEST-grid-section-class"));

      Container cell = (Container) gridSection.getChildren().get(0);
      assertEquals(1, cell.getChildren().size());
      assertEquals("CellContainer", cell.getTemplate());
      assertNotNull(cell.getCssClass());
      assertTrue("'grid-cell' not found", cell.getCssClass().contains("grid-cell"));
      assertTrue("'grid-cell-colspan-md-2' not found", cell.getCssClass().contains("grid-cell-colspan-md-2"));
      assertTrue("'grid-cell-colspan-lg-2' not found", cell.getCssClass().contains("grid-cell-colspan-lg-2"));
      assertTrue("'grid-cell-colspan-xl-2' not found", cell.getCssClass().contains("grid-cell-colspan-xl-2"));
      assertTrue("'grid-cell-rowspan-md-3' not found", cell.getCssClass().contains("grid-cell-rowspan-md-3"));
      assertTrue("'grid-cell-rowspan-lg-3' not found", cell.getCssClass().contains("grid-cell-rowspan-lg-3"));
      assertTrue("'grid-cell-rowspan-xl-3' not found", cell.getCssClass().contains("grid-cell-rowspan-xl-3"));
      assertTrue("'TEST-grid-cell-class' custom class not found", cell.getCssClass().contains("TEST-grid-cell-class"));
      assertTrue("'mt-n5' not found", cell.getCssClass().contains("mt-n5"));
      assertTrue("'mb-n4' not found", cell.getCssClass().contains("mb-n4"));
      assertTrue("'me-n3' not found", cell.getCssClass().contains("me-n3"));
      assertTrue("'ms-n1' not found", cell.getCssClass().contains("ms-n1"));
      assertTrue("'brtr-0' not found", cell.getCssClass().contains("brtr-0"));
      assertTrue("'brtl-1' not found", cell.getCssClass().contains("brtl-1"));
      assertTrue("'brbr-2' not found", cell.getCssClass().contains("brbr-2"));
      assertTrue("'brbl-4' not found", cell.getCssClass().contains("brbl-4"));
      assertTrue("'hidden-sm-and-down' not found", cell.getCssClass().contains("hidden-sm-and-down"));
    }
  }

}
