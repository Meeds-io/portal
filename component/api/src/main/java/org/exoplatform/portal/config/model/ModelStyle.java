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
package org.exoplatform.portal.config.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModelStyle implements Serializable {

  private static final long serialVersionUID = 3688402796115766370L;

  private Integer           marginTop;

  private Integer           marginBottom;

  private Integer           marginRight;

  private Integer           marginLeft;

  private Integer           radiusTopRight;

  private Integer           radiusTopLeft;

  private Integer           radiusBottomRight;

  private Integer           radiusBottomLeft;

  private Boolean           mobileHidden;

  private String            borderColor;

  private String            borderSize;

  private String            boxShadow;

  private String            backgroundColor;

  private String            backgroundImage;

  private String            backgroundEffect;

  private String            backgroundPosition;

  private String            backgroundSize;

  private String            backgroundRepeat;

  private String            textTitleColor;

  private String            textTitleFontSize;

  private String            textTitleFontWeight;

  private String            textTitleFontStyle;

  private String            textHeaderColor;

  private String            textHeaderFontSize;

  private String            textHeaderFontWeight;

  private String            textHeaderFontStyle;

  private String            textColor;

  private String            textFontSize;

  private String            textFontWeight;

  private String            textFontStyle;

  private String            textSubtitleColor;

  private String            textSubtitleFontSize;

  private String            textSubtitleFontWeight;

  private String            textSubtitleFontStyle;

  public String getCssClass() { // NOSONAR
    return getCssClass(null);
  }

  public String getCssClass(String existingCssClass) { // NOSONAR
    return getCssClass(existingCssClass, false, 20);
  }

  public String getCssClass(String existingCssClass, boolean sectionStyle, int diff) { // NOSONAR
    StringBuilder cssClass = new StringBuilder();
    if (!sectionStyle) {
      if (marginTop != null && marginTop >= 0 && !StringUtils.contains(existingCssClass, "mt-")) {
        cssClass.append(" mt-");
        if (marginTop < diff) {
          cssClass.append("n");
        }
        cssClass.append(Math.abs((marginTop - diff) / 4));
      }
      if (marginBottom != null && marginBottom >= 0 && !StringUtils.contains(existingCssClass, "mb-")) {
        cssClass.append(" mb-");
        if (marginBottom < diff) {
          cssClass.append("n");
        }
        cssClass.append(Math.abs((marginBottom - diff) / 4));
      }
      if (marginRight != null && marginRight >= 0 && !StringUtils.contains(existingCssClass, "me-")) {
        cssClass.append(" me-");
        if (marginRight < diff) {
          cssClass.append("n");
        }
        cssClass.append(Math.abs((marginRight - diff) / 4));
      }
      if (marginLeft != null && marginLeft >= 0 && !StringUtils.contains(existingCssClass, "ms-")) {
        cssClass.append(" ms-");
        if (marginLeft < diff) {
          cssClass.append("n");
        }
        cssClass.append(Math.abs((marginLeft - diff) / 4));
      }
      if (radiusTopRight != null && !StringUtils.contains(existingCssClass, "brtr-")) {
        cssClass.append(" brtr-");
        cssClass.append(radiusTopRight / 4);
      }
      if (radiusTopLeft != null && !StringUtils.contains(existingCssClass, "brtl-")) {
        cssClass.append(" brtl-");
        cssClass.append(radiusTopLeft / 4);
      }
      if (radiusBottomRight != null && !StringUtils.contains(existingCssClass, "brbr-")) {
        cssClass.append(" brbr-");
        cssClass.append(radiusBottomRight / 4);
      }
      if (radiusBottomLeft != null && !StringUtils.contains(existingCssClass, "brbl-")) {
        cssClass.append(" brbl-");
        cssClass.append(radiusBottomLeft / 4);
      }
    }
    if (mobileHidden != null && mobileHidden.booleanValue() && !StringUtils.contains(existingCssClass, "hidden-sm-and-down")) {
      cssClass.append(" hidden-sm-and-down");
    }
    return cssClass.toString();
  }

}
