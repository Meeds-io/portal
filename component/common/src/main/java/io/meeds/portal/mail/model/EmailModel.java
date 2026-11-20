/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.mail.model;

import java.util.List;

/**
 * What an email says, apart from how it looks. Every text is plain text and
 * is escaped by {@link io.meeds.portal.mail.util.EmailLayout}; the only HTML accepted is
 * {@link EmailModelBuilder#trustedHtml(String)}, for markup its caller has already
 * sanitized.
 */
public final class EmailModel {

  private final String       lang;

  private final String       title;

  private final String       preheader;

  private final List<String> paragraphs;

  private final String       trustedHtml;

  private final String       factLabel;

  private final String       factValue;

  private final String       lead;

  private final String       buttonLabel;

  private final String       buttonLink;

  private final String       note;

  private final String       signoff;

  private final String       aside;

  private final String       fallbackLabel;

  EmailModel(EmailModelBuilder b) {
    this.lang = b.lang;
    this.title = b.title;
    this.preheader = b.preheader;
    this.paragraphs = List.copyOf(b.paragraphs);
    this.trustedHtml = b.trustedHtml;
    this.factLabel = b.factLabel;
    this.factValue = b.factValue;
    this.lead = b.lead;
    this.buttonLabel = b.buttonLabel;
    this.buttonLink = b.buttonLink;
    this.note = b.note;
    this.signoff = b.signoff;
    this.aside = b.aside;
    this.fallbackLabel = b.fallbackLabel;
  }

  public static EmailModelBuilder builder() {
    return new EmailModelBuilder();
  }

  public String getLang() {
    return lang;
  }

  public String getTitle() {
    return title;
  }

  public String getPreheader() {
    return preheader;
  }

  public List<String> getParagraphs() {
    return paragraphs;
  }

  public String getTrustedHtml() {
    return trustedHtml;
  }

  public String getFactLabel() {
    return factLabel;
  }

  public String getFactValue() {
    return factValue;
  }

  public String getLead() {
    return lead;
  }

  public String getButtonLabel() {
    return buttonLabel;
  }

  public String getButtonLink() {
    return buttonLink;
  }

  public String getNote() {
    return note;
  }

  public String getSignoff() {
    return signoff;
  }

  public String getAside() {
    return aside;
  }

  public String getFallbackLabel() {
    return fallbackLabel;
  }

}
