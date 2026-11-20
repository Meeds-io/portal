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
package io.meeds.portal.mail;

import java.util.ArrayList;
import java.util.List;

/**
 * What an email says, apart from how it looks. Every text is plain text and
 * is escaped by {@link EmailLayout}; the only HTML accepted is
 * {@link Builder#trustedHtml(String)}, for markup its caller has already
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

  private EmailModel(Builder b) {
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

  public static Builder builder() {
    return new Builder();
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

  public static final class Builder {

    private String             lang          = "en";

    private String             title         = "";

    private String             preheader;

    private final List<String> paragraphs    = new ArrayList<>();

    private String             trustedHtml;

    private String             factLabel;

    private String             factValue;

    private String             lead;

    private String             buttonLabel;

    private String             buttonLink;

    private String             note;

    private String             signoff;

    private String             aside;

    private String             fallbackLabel = "If the button does not work, copy this address into your browser:";

    private Builder() {
    }

    /** The language of the reader, as a BCP 47 tag; it sets the lang of the document */
    public Builder lang(String lang) {
      if (lang != null && !lang.isBlank()) {
        this.lang = lang;
      }
      return this;
    }

    /** The headline, shown in the header band, and the subject of the document */
    public Builder title(String title) {
      this.title = title == null ? "" : title;
      return this;
    }

    /** The preview a mail client shows next to the subject */
    public Builder preheader(String preheader) {
      this.preheader = preheader;
      return this;
    }

    /** A paragraph of the message, in plain text */
    public Builder paragraph(String paragraph) {
      if (paragraph != null && !paragraph.isBlank()) {
        this.paragraphs.add(paragraph);
      }
      return this;
    }

    /** HTML the caller has already sanitized; inserted as is after the paragraphs */
    public Builder trustedHtml(String trustedHtml) {
      this.trustedHtml = trustedHtml;
      return this;
    }

    /** A value the reader may need to keep, such as a login, set apart from the text */
    public Builder fact(String label, String value) {
      this.factLabel = label;
      this.factValue = value;
      return this;
    }

    /** The sentence that introduces the button */
    public Builder lead(String lead) {
      this.lead = lead;
      return this;
    }

    /** The action of the email; the link must be http, https or mailto */
    public Builder button(String label, String link) {
      this.buttonLabel = label;
      this.buttonLink = link;
      return this;
    }

    /** A short remark under the button, such as how long the link lasts */
    public Builder note(String note) {
      this.note = note;
      return this;
    }

    /** The closing words; a blank line starts a new paragraph */
    public Builder signoff(String signoff) {
      this.signoff = signoff;
      return this;
    }

    /** A last remark in small print, after the closing words */
    public Builder aside(String aside) {
      this.aside = aside;
      return this;
    }

    /** The sentence that introduces the plain address shown under the button */
    public Builder fallbackLabel(String fallbackLabel) {
      if (fallbackLabel != null && !fallbackLabel.isBlank()) {
        this.fallbackLabel = fallbackLabel;
      }
      return this;
    }

    public EmailModel build() {
      return new EmailModel(this);
    }

  }

}
