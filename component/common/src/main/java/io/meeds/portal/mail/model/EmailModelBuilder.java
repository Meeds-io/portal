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

import java.util.ArrayList;
import java.util.List;

/**
 * Builds an {@link EmailModel}: what an email says, apart from how it looks
 */
public final class EmailModelBuilder {

  String             lang          = "en";

  String             title         = "";

  String             preheader;

  final List<String> paragraphs    = new ArrayList<>();

  String             trustedHtml;

  String             factLabel;

  String             factValue;

  String             lead;

  String             buttonLabel;

  String             buttonLink;

  String             note;

  String             signoff;

  String             aside;

  String             fallbackLabel = "If the button does not work, copy this address into your browser:";

  EmailModelBuilder() {
  }

  /** The language of the reader, as a BCP 47 tag; it sets the lang of the document */
  public EmailModelBuilder lang(String lang) {
    if (lang != null && !lang.isBlank()) {
      this.lang = lang;
    }
    return this;
  }

  /** The headline, shown in the header band, and the subject of the document */
  public EmailModelBuilder title(String title) {
    this.title = title == null ? "" : title;
    return this;
  }

  /** The preview a mail client shows next to the subject */
  public EmailModelBuilder preheader(String preheader) {
    this.preheader = preheader;
    return this;
  }

  /** A paragraph of the message, in plain text */
  public EmailModelBuilder paragraph(String paragraph) {
    if (paragraph != null && !paragraph.isBlank()) {
      this.paragraphs.add(paragraph);
    }
    return this;
  }

  /** HTML the caller has already sanitized; inserted as is after the paragraphs */
  public EmailModelBuilder trustedHtml(String trustedHtml) {
    this.trustedHtml = trustedHtml;
    return this;
  }

  /** A value the reader may need to keep, such as a login, set apart from the text */
  public EmailModelBuilder fact(String label, String value) {
    this.factLabel = label;
    this.factValue = value;
    return this;
  }

  /** The sentence that introduces the button */
  public EmailModelBuilder lead(String lead) {
    this.lead = lead;
    return this;
  }

  /** The action of the email; the link must be http, https or mailto */
  public EmailModelBuilder button(String label, String link) {
    this.buttonLabel = label;
    this.buttonLink = link;
    return this;
  }

  /** A short remark under the button, such as how long the link lasts */
  public EmailModelBuilder note(String note) {
    this.note = note;
    return this;
  }

  /** The closing words; a blank line starts a new paragraph */
  public EmailModelBuilder signoff(String signoff) {
    this.signoff = signoff;
    return this;
  }

  /** A last remark in small print, after the closing words */
  public EmailModelBuilder aside(String aside) {
    this.aside = aside;
    return this;
  }

  /** The sentence that introduces the plain address shown under the button */
  public EmailModelBuilder fallbackLabel(String fallbackLabel) {
    if (fallbackLabel != null && !fallbackLabel.isBlank()) {
      this.fallbackLabel = fallbackLabel;
    }
    return this;
  }

  public EmailModel build() {
    return new EmailModel(this);
  }

}
