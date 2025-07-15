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

import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : Nguyen Thanh Hai haint@exoplatform.com Jun 29, 2011
 */
public class MimeTypeUploadPlugin extends BaseComponentPlugin {
  private static final Log    log              = ExoLogger.getLogger(MimeTypeUploadPlugin.class);
    private static final String MIMETYPE_PATH = "mimetype-path";
    private static final String DEFAULT_MIMETYPE = "mimetype-default";

    private Properties mimeTypes = new Properties();
    private String mimetypeDefault;

    public MimeTypeUploadPlugin(InitParams initParams, ConfigurationManager configurationService) throws Exception {
        ValueParam param = initParams.getValueParam(MIMETYPE_PATH);
        URL filePath = configurationService.getURL(param.getValue());
        URLConnection connection = filePath.openConnection();
        mimeTypes.load(connection.getInputStream());

        param = initParams.getValueParam(DEFAULT_MIMETYPE);
        if (param != null)
            mimetypeDefault = param.getValue();
    }

    public String getMimeType(String fileName) {
        if (fileName.indexOf('.') == -1)
            return mimetypeDefault;
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        String mimeType = mimeTypes.getProperty(ext.toLowerCase(), mimetypeDefault);
        if (mimeType == null || mimeType.length() == 0)
            return null;
        return mimeType;
    }
}
