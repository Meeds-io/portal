/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.importer;

import java.util.HashMap;
import java.util.List;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class PageImporter {
    private final SiteKey siteKey;
    /** . */
    private final List<Page> list;

    /** . */
    private final LayoutService layoutService;

    /** . */
    private final ImportMode mode;

    public PageImporter(ImportMode importMode, SiteKey siteKey, List<Page> list, LayoutService layoutService) {
        this.siteKey = siteKey;
        this.mode = importMode;
        this.list = list;
        this.layoutService = layoutService;
    }

    public void perform() {
        HashMap<String, Page> hashPageList = new HashMap<>();
        for (Page page : list) {
            hashPageList.put(page.getPageId(), page);
        }

        //We temporary don't support to delete user site's pages, it's risk because user site page, navigation is controlled by 
        //UserSiteLifecycle which is difference with portal site and group site
        if (mode == ImportMode.OVERWRITE && !siteKey.getType().equals(SiteType.USER)) {
          layoutService.removePages(siteKey);
        }
        
        for (Page src : list) {
            PageContext existingPage = layoutService.getPageContext(src.getPageKey());
            Page dst;
            
            //
            switch (mode) {
                case CONSERVE:
                    dst = null;
                    break;
                case INSERT:
                    if (existingPage == null) {
                        dst = src;
                    } else {
                        dst = null;
                    }
                    break;
                case MERGE:
                case OVERWRITE:
                    dst = src;
                    break;
                default:
                    throw new AssertionError();
            }
            
            if (dst != null) {
                PageState dstState = Utils.toPageState(dst);
                layoutService.save(new PageContext(src.getPageKey(), dstState), dst);
            }            
        }
    }
}
