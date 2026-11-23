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
package io.meeds.portal.mail.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.meeds.portal.mail.model.EmailBrand;
import io.meeds.portal.mail.model.EmailModel;

/**
 * The one look of the emails of the platform: a header band in the brand
 * color that carries the company name and the title, then a single column of
 * text that follows the direction of the language (left to right, or right to
 * left for Arabic, Hebrew, Persian...) with the action as a button. It is made of tables
 * and inline styles, the only markup every mail client renders alike, and
 * every text it is given is escaped.
 */
public final class EmailLayout {

  private static final String   INK          = "#1d2327";

  private static final String   MUTED        = "#5d676d";

  private static final String   FACT_BG      = "#f1f4f7";

  private static final String   COLOR_DECLARATION = ";color:";

  private static final Pattern  SAFE_LINK    = Pattern.compile("(?i)^(https?://|mailto:)[^\\s\"'<>]+$");

  private static final Pattern  PARAGRAPHS   = Pattern.compile("\n{2,}");

  /** Languages written from right to left (aro is the Arabic variant of the platform, ckb is Central Kurdish) */
  private static final Set<String> RTL_LANGUAGES = Set.of("ar", "aro", "arc", "ckb", "dv", "fa", "he", "iw", "ks", "ps", "sd", "ug", "ur",
                                                          "yi");

  /** Scripts written from right to left, which make a locale right to left whatever its language */
  private static final Set<String> RTL_SCRIPTS   = Set.of("arab", "hebr", "thaa", "syrc", "nkoo", "adlm", "rohg");

  /** An outlined button on the dark page: its border and its text in a tint that reads (about 9:1) on #16191c */
  private static final String OUTLINED_DARK = "a.eml-out{background-color:#16191c !important;color:#8ab4f8 !important;border-color:#8ab4f8 !important}";

  /** The title is smaller on a phone */
  private static final String TITLE_PHONE = "@media (max-width:480px){.eml-title{font-size:20px !important;padding:6px 0 18px 0 !important}";

  /**
   * What the email looks like for a reader whose client is in dark mode and
   * follows the dark styles of the email (Apple Mail, Outlook on Apple
   * devices, Thunderbird...); Outlook.com has its own selectors. Clients
   * that recolor emails by themselves, as the Gmail apps do, ignore it. The
   * brand color of the header and of the buttons stays as it is.
   */
  private static final String DARK_STYLE =
      ":root{color-scheme:light dark;supported-color-schemes:light dark}"
          + "@media (prefers-color-scheme:dark){.eml-bg{background-color:#16191c !important}.eml-tx{color:#e8ebed !important}"
          + ".eml-mu,.eml-mu a{color:#a3adb3 !important}.eml-fact{background-color:#20262a !important}.eml-tx a{color:#8ab4f8 !important}td.eml-btn{box-shadow:0 0 0 1px rgba(232,235,237,.5)}" + OUTLINED_DARK + "}"
          + "[data-ogsc] .eml-tx{color:#e8ebed !important}[data-ogsc] .eml-mu,[data-ogsc] .eml-mu a{color:#a3adb3 !important}"
          + "[data-ogsb] .eml-bg{background-color:#16191c !important}[data-ogsb] .eml-fact{background-color:#20262a !important}"
          + "[data-ogsc] a.eml-out{color:#8ab4f8 !important;border-color:#8ab4f8 !important}";

  /**
   * The dark styles of the notification emails, whose templates carry their
   * colors inline and have no head to hold a style: the rules match those
   * inline colors, so no template needs a class. The brand color of the
   * header and of the buttons stays as it is.
   */
  private static final String NOTIFICATION_DARK_STYLE =
      "<style>" + TITLE_PHONE + "}:root{color-scheme:light dark;supported-color-schemes:light dark}body{margin:0 !important}@media (prefers-color-scheme:dark){html,body{background-color:#16191c !important}"
          + "[bgcolor=\"#ffffff\"],[style*=\"background-color: #ffffff\"],[style*=\"background-color:#ffffff\"],[style*=\"background: #ffffff\"]{background-color:#16191c !important}"
          + "[style*=\"background-color:#f1f4f7\"]{background-color:#20262a !important}"
          + "[style*=\"color:#1d2327\"],[style*=\"color: #1d2327\"]{color:#e8ebed !important}"
          + "[style*=\"color:#5d676d\"],[style*=\"color: #5d676d\"]{color:#a3adb3 !important}"
          + "[style*=\"color:#333333\"],[style*=\"color: #333333\"],[style*=\"color: #333;\"],[style*=\"color:#333;\"]{color:#e8ebed !important}"
          + "a[style*=\"#2f5e92\"],a[style*=\"#2F5E92\"],a[style*=\"underline\"],a[style*=\"font-weight\"],span[style*=\"font-weight\"]{color:#8ab4f8 !important}"
          + OUTLINED_DARK + "}"
          + "[data-ogsb] [bgcolor=\"#ffffff\"],[data-ogsb] [style*=\"background-color:#ffffff\"],[data-ogsb] [style*=\"background-color: #ffffff\"]{background-color:#16191c !important}"
          + "[data-ogsc] [style*=\"color:#1d2327\"],[data-ogsc] [style*=\"color: #1d2327\"]{color:#e8ebed !important}"
          + "[data-ogsc] [style*=\"color:#5d676d\"],[data-ogsc] [style*=\"color: #5d676d\"]{color:#a3adb3 !important}"
          + "[data-ogsc] a.eml-out{color:#8ab4f8 !important;border-color:#8ab4f8 !important}</style>";

  /** One size, one type and one outline for every button of every email */
  private static final String BUTTON_PAD    = "7px 20px";

  /** The height of a button: 20 of line, 14 of padding, 2 of border */
  private static final int    BUTTON_HEIGHT = 36;

  private static final String END_COLUMN = "<!--[if mso]></td></tr></table><![endif]-->";

  private static final Pattern STYLE_ATTRIBUTE = Pattern.compile("(?<![\\w-])style\\s*=\\s*(?:\"[^\"]*\"|'[^']*')");

  /** A label that may already carry the basic html entities, as plain text */
  private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(?:[xX]([0-9a-fA-F]{1,6})|(\\d{1,7}));");

  private EmailLayout() {
  }

  /**
   * @return the greeting followed by the name of the reader, or by nothing
   *         but the comma when the name is not known
   */
  public static String greeting(String hello, String name) {
    // some translations already end the greeting with a comma: it is written once
    String word = hello == null ? "" : stripTrailingCommas(hello.trim());
    return name == null || name.isBlank() ? word + "," : word + " " + name + ",";
  }

  /**
   * @return the sentence that introduces a link, ended by a colon unless its
   *         translation already ends with a punctuation mark
   */
  public static String leadIn(String sentence) {
    String trimmed = sentence == null ? "" : sentence.trim();
    return trimmed.isEmpty() || ".:!?,;\u2026\u060c\u061b\u061f\u3002\uff1a\uff01\uff1f".indexOf(trimmed.charAt(trimmed.length() - 1)) >= 0 ? trimmed
                                                                                                                                              : trimmed + ":";
  }

  /**
   * Tells whether a language is written from right to left, as Arabic,
   * Hebrew, Persian or Urdu are. The platform's own locale configuration
   * only declares Arabic as such, so the direction of an email is taken from
   * the language itself.
   *
   * @param lang a BCP 47 language tag or a Java locale name, such as ar,
   *          he-IL, fa_IR or ku-Arab
   * @return true when the language, or its script, is written from right to
   *         left
   */
  public static boolean isRtl(String lang) {
    if (lang == null || lang.isBlank()) {
      return false;
    }
    String[] parts = lang.trim().toLowerCase(Locale.ROOT).split("[-_]");
    if (RTL_LANGUAGES.contains(parts[0])) {
      return true;
    }
    for (int i = 1; i < parts.length; i++) {
      if (RTL_SCRIPTS.contains(parts[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param model what the email says
   * @param brand how it is dressed
   * @return the complete html document
   * @throws IllegalArgumentException when the link of the button is not an
   *           http, https or mailto address
   */
  public static String render(EmailModel model, EmailBrand brand) {
    String link = null;
    if (model.getButtonLabel() != null && model.getButtonLink() != null) {
      link = model.getButtonLink().trim();
      if (!SAFE_LINK.matcher(link).matches()) {
        throw new IllegalArgumentException("The link of an email button must be an http, https or mailto address");
      }
    }
    String font = brand.getFontStack();
    String title = escape(model.getTitle());
    StringBuilder html = new StringBuilder(4096);
    boolean rtl = isRtl(model.getLang());
    html.append("<!doctype html><html lang=\"").append(escape(model.getLang())).append("\"").append(rtl ? " dir=\"rtl\"" : "")
        .append(" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">")
        .append("<head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
        .append("<meta name=\"color-scheme\" content=\"light dark\"><meta name=\"supported-color-schemes\" content=\"light dark\">")
        .append("<title>").append(title).append("</title>").append(actionMarkup(model, brand, link))
        .append("<style>").append(TITLE_PHONE)
        .append(".eml-btn,.eml-btn a{display:block !important;text-align:center !important}.eml-btn{width:100% !important}}")
        .append(DARK_STYLE).append("</style>")
        .append("</head><body").append(rtl ? " dir=\"rtl\"" : "").append(" class=\"eml-bg\" style=\"margin:0;padding:0;background-color:#ffffff;\">");
    if (model.getPreheader() != null && !model.getPreheader().isBlank()) {
      html.append("<div style=\"display:none;visibility:hidden;opacity:0;color:transparent;height:0;width:0;max-height:0;max-width:0;font-size:1px;line-height:1px;mso-hide:all;overflow:hidden;\">").append(escape(model.getPreheader())).append("</div>");
    }
    html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
    header(html, brand, title, rtl);
    // only what follows the header is recolored in dark mode: the header keeps the brand color and its own text color
    int contentStart = html.length();
    // content
    html.append("<tr><td style=\"padding:0 24px;\">").append(column(rtl));
    row(html, font, "30px 0 0 0", model.getParagraphs().stream().map(p -> paragraph(escape(p))).reduce("", String::concat));
    if (model.getTrustedHtml() != null && !model.getTrustedHtml().isBlank()) {
      row(html, font, "0", model.getTrustedHtml());
    }
    if (model.getFactValue() != null && !model.getFactValue().isBlank()) {
      html.append("<tr><td style=\"padding:6px 0 18px 0;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>")
          .append("<td style=\"border-").append(rtl ? "right" : "left").append(":4px solid ").append(brand.getFillColor()).append(";background-color:").append(FACT_BG)
          .append(";padding:12px 16px;font:15px/1.5 ").append(font).append(COLOR_DECLARATION).append(INK).append(";\">")
          .append("<span style=\"font-size:12px;color:").append(MUTED).append(";\">").append(escape(model.getFactLabel()))
          .append("</span><br><strong style=\"font-size:17px;\">").append(escape(model.getFactValue()))
          .append("</strong></td></tr></table></td></tr>");
    }
    if (model.getLead() != null && !model.getLead().isBlank()) {
      html.append("<tr><td style=\"font:15px/1.7 ").append(font).append(COLOR_DECLARATION).append(INK).append(";\">")
          .append(escape(model.getLead())).append("</td></tr>");
    }
    if (link != null) {
      button(html, escape(model.getButtonLabel()), escape(link), brand, model.getButtonLabel().length(), rtl);
    }
    if (model.getNote() != null && !model.getNote().isBlank()) {
      html.append("<tr><td style=\"padding:12px 0 0 0;font:13px/1.6 ").append(font).append(COLOR_DECLARATION).append(MUTED).append(";\">")
          .append(escape(model.getNote())).append("</td></tr>");
    }
    if (model.getSignoff() != null && !model.getSignoff().isBlank()) {
      html.append("<tr><td style=\"padding:32px 0 0 0;font:15px/1.7 ").append(font).append(COLOR_DECLARATION).append(INK).append(";\">")
          .append(signoff(model.getSignoff())).append("</td></tr>");
    }
    if (model.getAside() != null && !model.getAside().isBlank()) {
      html.append("<tr><td style=\"padding:24px 0 0 0;font:13px/1.6 ").append(font).append(COLOR_DECLARATION).append(MUTED).append(";\">")
          .append(escape(model.getAside())).append("</td></tr>");
    }
    if (link != null) {
      html.append("<tr><td style=\"padding:32px 0 0 0;font:12px/1.7 ").append(font).append(COLOR_DECLARATION).append(MUTED).append(";\">")
          .append(escape(model.getFallbackLabel())).append("<br><a href=\"").append(escape(link)).append("\" style=\"color:")
          .append(MUTED).append(";overflow-wrap:anywhere;word-break:break-word;\" dir=\"ltr\">").append(escape(link)).append("</a></td></tr>");
    }
    html.append("<tr><td style=\"height:48px;line-height:48px;font-size:0;\">&nbsp;</td></tr>")
        .append("</table>").append(END_COLUMN).append("</td></tr></table></body></html>");
    return directed(html.substring(0, contentStart) + classed(html.substring(contentStart)), rtl);
  }

  /**
   * The dark styles of the notification emails, to put once before the header
   * row: the colors of those templates are inline, the rules match them.
   *
   * @return a {@code <style>} element
   */
  public static String darkStyle() {
    return NOTIFICATION_DARK_STYLE;
  }

  /**
   * The header of an email as a table row, for the emails that are laid out
   * by their own template (the notifications) and only share the look of
   * the header: the company name and the title in the brand color
   *
   * @param brand how the email is dressed
   * @param title the headline, in plain text
   * @return a {@code <tr>} to include in a table
   */
  public static String headerRow(EmailBrand brand, String title) {
    return headerRow(brand, title, null);
  }

  /**
   * The same header for a reader whose language is written from right to
   * left, when {@code lang} says so
   */
  public static String headerRow(EmailBrand brand, String title, String lang) {
    boolean rtl = isRtl(lang);
    StringBuilder html = new StringBuilder(1024);
    header(html, brand, escape(title), rtl);
    return directed(html.toString(), rtl);
  }

  /**
   * Opens the block that quotes what somebody wrote (a message, a comment, a
   * description) in an email laid out by its own template: a bar in the brand
   * color and a light background, the same as the key value of the other
   * emails. The text follows, then {@link #quoteEnd()}.
   *
   * @return the opening of a table that holds the quote
   */
  public static String quoteStart() {
    return quoteStart(true, null);
  }

  /** Same as {@link #quoteStart()}, the bar on the side where the text of the language starts */
  public static String quoteStart(String lang) {
    return quoteStart(true, lang);
  }

  /**
   * Same as {@link #quoteStart()}; {@code spaceAfter} false leaves out the
   * space under the box, for a quote that is the last block of its cell and
   * is followed by a button, which brings its own space
   */
  public static String quoteStart(boolean spaceAfter) {
    return quoteStart(spaceAfter, null);
  }

  /** The general form: space under the box or not, bar on the start side of the language */
  public static String quoteStart(boolean spaceAfter, String lang) {
    EmailBrand brand = EmailBrand.current();
    return "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"margin:0 0 " + (spaceAfter ? "12px" : "0") + " 0;\"><tr>"
        + "<td dir=\"auto\" style=\"padding:12px 16px;border-" + (isRtl(lang) ? "right" : "left") + ":4px solid " + brand.getFillColor() + ";background-color:" + FACT_BG
        + ";font:15px/1.7 " + brand.getFontStack() + COLOR_DECLARATION + INK + ";overflow-wrap:anywhere;word-break:break-word;\">";
  }

  /** @return the end of the block opened by {@link #quoteStart()} */
  public static String quoteEnd() {
    return "</td></tr></table>"; // NOSONAR kept as a method: the templates call it
  }

  /**
   * The name of an entity (event, calendar, person, space) inside a sentence:
   * escaped, bold and in the link color, a link when the address is a safe
   * http or https one, plain bold text otherwise.
   *
   * @param label the name, plain text
   * @param url the address it points to, or null
   * @return the html of the name
   */
  public static String entityLink(String label, String url) {
    String text = escape(plain(label));
    String link = plain(url).trim();
    String style = "color:" + linkColor() + ";text-decoration:none;font-weight:bold;font-family:" + fontStack() + ";";
    if (SAFE_LINK.matcher(link).matches() && !link.regionMatches(true, 0, "mailto:", 0, 7)) {
      return "<a href=\"" + escape(link) + "\" target=\"_blank\" style=\"" + style + "\">" + text + "</a>";
    }
    return "<span style=\"" + style + "\">" + text + "</span>";
  }

  /**
   * A box of labelled values (where, when, who) in an email laid out by its
   * own template: the quote look with a fixed label column. A pair whose value
   * is blank is left out.
   *
   * @param labelsAndValues label, value, label, value...: the label is plain
   *          text, the value is html that the caller has already escaped
   * @return the html of the box, or an empty text when no value is left
   */
  public static String facts(String... labelsAndValues) {
    return factsIn(null, labelsAndValues);
  }

  /** Same as {@link #facts(String...)}, with the bar on the side where the text of {@code lang} starts */
  public static String factsIn(String lang, String... labelsAndValues) {
    StringBuilder rows = new StringBuilder();
    String font = fontStack();
    for (int i = 0; labelsAndValues != null && i + 1 < labelsAndValues.length; i += 2) {
      String value = labelsAndValues[i + 1];
      if (value == null || value.isBlank()) {
        continue;
      }
      rows.append("<tr><td valign=\"top\" style=\"width:140px;padding:3px 12px 3px 0;font:bold 13px/1.6 ").append(font).append(COLOR_DECLARATION)
          .append(MUTED).append(";\">").append(label(labelsAndValues[i])).append("</td><td valign=\"top\" dir=\"auto\" style=\"padding:3px 0;font:15px/1.6 ")
          .append(font).append(COLOR_DECLARATION).append(INK).append(";overflow-wrap:anywhere;word-break:break-word;\">").append(value).append("</td></tr>");
    }
    return rows.isEmpty() ? "" : quoteStart(true, lang) + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" + rows + "</table>" + quoteEnd();
  }

  /**
   * The margin of a row of buttons that sits in the text column of an email
   * with an avatar: it pulls the row back to the start of the content column,
   * where the buttons of every other email are, on the left for a left to
   * right language and on the right for a right to left one.
   *
   * @param lang the language of the email
   * @param pull the width of the avatar column, such as {@code 64px}
   * @return the value of a {@code margin} declaration
   */
  public static String buttonRowMargin(String lang, String pull) {
    return isRtl(lang) ? "20px -" + pull + " 8px 0" : "20px 0 8px -" + pull;
  }

  /**
   * Gives the links of a label (the footer sentence that holds the link to the
   * notification settings) the link style of the emails, as their only
   * {@code style} attribute: the label may already carry one, and a tag with
   * two of them is not valid
   *
   * @param html a label that holds {@code <a>} tags, or null
   * @return the label whose links are underlined in the link color
   */
  public static String linkStyled(String html) {
    return styleLinks(html, "text-decoration:underline;");
  }

  /**
   * Same as {@link #linkStyled(String)} for the links that name a person, a
   * space or any entity (built in Java, so without style): bold, in the link
   * color, not underlined
   *
   * @param html a label or a message that holds {@code <a>} tags, or null
   * @return the same text whose links are styled as the names of the emails
   */
  public static String entityStyled(String html) {
    return styleLinks(html, "text-decoration:none;font-weight:bold;");
  }

  /**
   * The closing row of an email laid out by its own template: the space under
   * the last block, as the account emails have, with no frame and no link (the
   * button of the email and the plain address under it are the ways to the
   * site)
   *
   * @return a {@code <tr>} to include in a table
   */
  public static String closingRow() {
    return "<tr><td style=\"padding:0 0 40px 0;font-size:0;line-height:0;\">&nbsp;</td></tr>"; // NOSONAR kept as a method: the templates call it
  }

  /**
   * A button for an email that is laid out by its own template, such as a
   * notification: a flat button with rounded corners. Buttons that follow each
   * other sit on one line, as wide as their label and never wider than the
   * screen, and wrap onto the next line when the screen is too narrow, so that
   * they read the same on a computer and on a phone without a media query. The
   * primary one is filled with the brand color; the secondary ones are quiet,
   * outlined in the primary color, so that buttons side by side do not
   * compete.
   *
   * @param brand how the email is dressed
   * @param label the text of the button, in plain text
   * @param href an http or https address; for any other address only the
   *          label is written
   * @param primary true for the main action of the email
   * @return the html of the button
   */
  public static String actionLink(EmailBrand brand, String label, String href, boolean primary) {
    String text = escape(plain(label));
    String link = plain(href).trim();
    if (!SAFE_LINK.matcher(link).matches() || link.regionMatches(true, 0, "mailto:", 0, 7)) {
      return text;
    }
    String fill = primary ? brand.getFillColor() : "#ffffff";
    String ink = primary ? brand.getTextColor() : brand.getLinkColor();
    String border = primary ? brand.getFillColor() : brand.getOutlineColor();
    return "<a href=\"" + escape(link) + "\" target=\"_blank\"" + (primary ? "" : " class=\"eml-out\"") + " style=\"display:inline-block;vertical-align:top;max-width:100%;margin:0 4px 12px 0;padding:" + BUTTON_PAD + ";border-radius:" + brand.getRadius() + ";"
        + "background-color:" + fill + ";border:1px solid " + border + COLOR_DECLARATION + ink + ";font:bold 14px/20px " + brand.getFontStack()
        + ";text-decoration:none;\">" + text + "</a>" + (primary ? actionMarkup(brand, plain(label), link) : "");
  }

  /**
   * The color of the links in the text of an email that keeps its own
   * template, so that they follow the primary color of the site
   */
  public static String linkColor() {
    return linkColor(EmailBrand.current());
  }

  /**
   * The fonts of the text of an email that keeps its own template: the font
   * of the platform branding first, then the fonts that every mail client
   * has
   */
  public static String fontStack() {
    return fontStack(EmailBrand.current());
  }

  public static String fontStack(EmailBrand brand) {
    return brand.getFontStack();
  }

  public static String linkColor(EmailBrand brand) {
    return brand.getLinkColor();
  }

  /**
   * The action markup alone, for an email whose main link is in its text (a
   * link in a quote) and has no button
   *
   * @param description what the email is about, in plain text
   * @param label the name of the action, in plain text
   * @param href the address, which only counts when it is https
   * @return the markup, or nothing for any other address
   */
  public static String viewAction(String description, String label, String href) {
    String link = plain(href).trim();
    return SAFE_LINK.matcher(link).matches() ? actionMarkup(EmailBrand.current(), plain(description), plain(label), link) : "";
  }

  /** The same button with the brand of the site */
  public static String actionLink(String label, String href, boolean primary) {
    return actionLink(EmailBrand.current(), label, href, primary);
  }

  /**
   * @return rtl or ltr, for the dir attribute of a template that has to follow
   *         the direction of the reader's language
   */
  public static String dir(String lang) {
    return isRtl(lang) ? "rtl" : "ltr";
  }

  /**
   * @return the side where text starts: left, or right in a right to left
   *         language
   */
  public static String start(String lang) {
    return isRtl(lang) ? "right" : "left";
  }

  /**
   * @return the side where text ends
   */
  public static String end(String lang) {
    return isRtl(lang) ? "left" : "right";
  }

  /**
   * @param size such as 15px
   * @return a padding that leaves a gap after the content, on the side where
   *         it ends: the gap between an avatar and the text beside it
   */
  public static String gap(String lang, String size) {
    return isRtl(lang) ? "0 0 0 " + size : "0 " + size + " 0 0";
  }

  /**
   * A json string that can sit inside a script element: the characters that
   * could close it or be read as html are written as escapes
   */
  static String json(String text) {
    StringBuilder out = new StringBuilder("\"");
    for (int i = 0; text != null && i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
      case '"' -> out.append("\\\"");
      case '\\' -> out.append("\\\\");
      case '\n' -> out.append("\\n");
      case '\r' -> out.append("\\r");
      case '\t' -> out.append("\\t");
      default -> {
        if (c < 0x20 || c == '<' || c == '>' || c == '&' || c == '\u2028' || c == '\u2029') {
          out.append(String.format("\\u%04x", (int) c));
        } else {
          out.append(c);
        }
      }
      }
    }
    return out.append('"').toString();
  }

  /**
   * Escapes a text for an html element or a double quoted attribute
   */
  static String escape(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(text.length() + 16);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
      case '&' -> out.append("&amp;");
      case '<' -> out.append("&lt;");
      case '>' -> out.append("&gt;");
      case '"' -> out.append("&quot;");
      case '\'' -> out.append("&#39;");
      default -> out.append(c);
      }
    }
    return out.toString();
  }

  private static String stripTrailingCommas(String text) {
    int end = text.length();
    while (end > 0 && (text.charAt(end - 1) == ',' || text.charAt(end - 1) == '\u060c' || Character.isWhitespace(text.charAt(end - 1)))) {
      end--;
    }
    return text.substring(0, end);
  }

  /** A label, escaped; the space that some languages put before a colon (or ? or !) cannot break the line there */
  private static String label(String text) {
    String value = plain(text).stripTrailing();
    int last = value.length() - 1;
    if (last > 0 && ":;!?".indexOf(value.charAt(last)) >= 0 && Character.isWhitespace(value.charAt(last - 1))) {
      value = value.substring(0, last).stripTrailing() + "\u00a0" + value.charAt(last);
    }
    return escape(value);
  }

  private static String styleLinks(String html, String decoration) {
    if (html == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(html.length() + 64);
    String color = linkColor();
    String font = fontStack();
    int pos = 0;
    while (true) {
      int start = html.indexOf("<a ", pos);
      int end = start < 0 ? -1 : html.indexOf('>', start);
      if (end < 0) {
        break;
      }
      out.append(html, pos, start);
      String attributes = withoutStyleAttribute(html.substring(start + 3, end));
      out.append("<a style=\"color:").append(color).append(';').append(decoration).append("font-family:")
         .append(font).append(";\" ").append(attributes.trim()).append('>');
      pos = end + 1;
    }
    return out.append(html, pos, html.length()).toString();
  }

  private static String withoutStyleAttribute(String attributes) {
    return STYLE_ATTRIBUTE.matcher(attributes).replaceAll("");
  }

  private static void header(StringBuilder html, EmailBrand brand, String escapedTitle, boolean rtl) {
    String color = brand.getFillColor();
    String onColor = brand.getTextColor();
    String font = brand.getFontStack();
    html.append("<tr><td bgcolor=\"").append(color).append("\" style=\"background-color:").append(color).append(";padding:0 24px;\">")
        .append(column(rtl))
        .append("<tr><td style=\"padding:18px 0 0 0;font:bold 14px ").append(font).append(COLOR_DECLARATION).append(onColor).append(";\">")
        .append(escape(brand.getCompanyName())).append("</td></tr>")
        .append("<tr><td class=\"eml-title\" style=\"padding:8px 0 20px 0;\"><h1 style=\"margin:0;font:bold 22px/1.25 ")
        .append(font).append(COLOR_DECLARATION).append(onColor).append(";overflow-wrap:anywhere;word-break:break-word;\">").append(escapedTitle).append("</h1></td></tr>")
        .append("</table>").append(END_COLUMN).append("</td></tr>");
  }

  /**
   * The action of the email as schema.org markup, which Gmail turns into a
   * button next to the subject in the inbox and on hover. Gmail only does so
   * for senders registered with Google, and only for https links; everywhere
   * else the markup is ignored, so it is only written for an https link.
   */
  private static String actionMarkup(EmailModel model, EmailBrand brand, String link) {
    return actionMarkup(brand, model.getTitle(), model.getButtonLabel(), link);
  }

  /**
   * The same markup for the main button of an email that keeps its own
   * template (a notification): the description is the label of the button
   */
  private static String actionMarkup(EmailBrand brand, String label, String link) {
    return actionMarkup(brand, label, label, link);
  }

  private static String actionMarkup(EmailBrand brand, String description, String label, String link) {
    if (link == null || !link.regionMatches(true, 0, "https://", 0, 8)) {
      return "";
    }
    String url = json(link);
    return "<script type=\"application/ld+json\">{\"@context\":\"http://schema.org\",\"@type\":\"EmailMessage\",\"description\":"
        + json(description) + ",\"publisher\":{\"@type\":\"Organization\",\"name\":" + json(brand.getCompanyName())
        + "},\"potentialAction\":{\"@type\":\"ViewAction\",\"target\":" + url + ",\"url\":" + url + ",\"name\":"
        + json(label) + "}}</script>";
  }

  /**
   * A text that may already carry html entities (the providers of the
   * notifications encode what they bind, down to the slashes of an address) as
   * the plain text it stands for, so that it is checked and escaped once
   */
  private static String plain(String text) {
    if (text == null) {
      return "";
    }
    String decoded = NUMERIC_ENTITY.matcher(text).replaceAll(m -> {
      int codePoint = m.group(1) != null ? Integer.parseInt(m.group(1), 16) : Integer.parseInt(m.group(2));
      return codePoint > 0 && Character.isValidCodePoint(codePoint) ? Matcher.quoteReplacement(new String(Character.toChars(codePoint))) : "";
    });
    return decoded.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
  }

  /** Marks the text, the muted text and the key value, so that the dark styles can recolor them */
  private static String classed(String html) {
    String out = addClass(html, "<td style=\"", "background-color:" + FACT_BG, "eml-tx eml-fact");
    out = addClass(out, "<td style=\"", "color:" + INK + ";", "eml-tx");
    out = addClass(out, "<td style=\"", "color:" + MUTED + ";", "eml-mu");
    return addClass(out, "<span style=\"", "color:" + MUTED + ";", "eml-mu");
  }

  /** Adds a class to the tags of one attribute, a style that holds the marker, without a regular expression to backtrack */
  private static String addClass(String html, String open, String marker, String cssClass) {
    StringBuilder out = new StringBuilder(html.length() + 64);
    int pos = 0;
    while (true) {
      int start = html.indexOf(open, pos);
      int end = start < 0 ? -1 : html.indexOf('"', start + open.length());
      if (end < 0) {
        break;
      }
      out.append(html, pos, end + 1);
      boolean alone = end + 1 < html.length() && html.charAt(end + 1) == '>';
      if (alone && html.substring(start + open.length(), end).contains(marker)) {
        out.append(" class=\"").append(cssClass).append('"');
      }
      pos = end + 1;
    }
    return out.append(html, pos, html.length()).toString();
  }

  private static String column(boolean rtl) {
    // Outlook for Windows ignores max-width: it gets a table of 600 pixels around the column
    return "<!--[if mso]><table role=\"presentation\" width=\"600\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\"><tr><td><![endif]-->"
        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"" + (rtl ? " dir=\"rtl\"" : "")
        + " style=\"max-width:600px;margin:0 auto;overflow-wrap:anywhere;word-break:break-word;" + (rtl ? "direction:rtl;" : "") + "\">";
  }

  /** Cells of a right to left email align their text to the right: the direction alone is not honored by every client */
  private static String directed(String html, boolean rtl) {
    return rtl ? html.replace("<td style=\"", "<td dir=\"auto\" style=\"text-align:right;")
                     .replace("<p style=\"", "<p dir=\"auto\" style=\"")
                     .replace("<h1 style=\"", "<h1 dir=\"auto\" style=\"")
               : html;
  }

  private static void row(StringBuilder html, String font, String padding, String content) {
    if (content.isEmpty()) {
      return;
    }
    html.append("<tr><td style=\"padding:").append(padding).append(";font:15px/1.7 ").append(font).append(COLOR_DECLARATION).append(INK)
        .append(";\">").append(content).append("</td></tr>");
  }

  private static String paragraph(String escapedText) {
    return "<p style=\"margin:0 0 14px\">" + escapedText + "</p>";
  }

  private static String signoff(String text) {
    StringBuilder out = new StringBuilder();
    // some bundles keep the escape of a line break as the two characters \n
    for (String block : PARAGRAPHS.split(text.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n").trim())) {
      out.append("<p style=\"margin:0 0 14px\">").append(escape(block.trim()).replace("\n", "<br>")).append("</p>");
    }
    return out.toString();
  }

  private static void button(StringBuilder html, String label, String href, EmailBrand brand, int labelLength, boolean rtl) {
    String color = brand.getFillColor();
    String onColor = brand.getTextColor();
    String radius = brand.getRadius();
    int arc = Math.min(50, Math.round(Integer.parseInt(radius.replace("px", "")) * 100f / BUTTON_HEIGHT));
    int width = Math.max(120, labelLength * 9 + 50);
    html.append("<tr><td style=\"padding:20px 0 0 0;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" class=\"eml-btn\"" + (rtl ? " align=\"right\"" : "") + "><tr>")
        .append("<td class=\"eml-btn\" align=\"center\" bgcolor=\"").append(color).append("\" style=\"background-color:").append(color)
        .append(";border-radius:").append(radius).append(";\">")
        // Outlook ignores the radius and padding of a link: it gets a shape of its own
        .append("<!--[if mso]><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" href=\"").append(href)
        .append("\" style=\"height:" + BUTTON_HEIGHT + "px;v-text-anchor:middle;width:").append(width)
        .append("px;\" arcsize=\"" + arc + "%\" stroke=\"f\" fillcolor=\"").append(color).append("\"><w:anchorlock/><center style=\"color:")
        .append(onColor).append(";font-family:Arial,sans-serif;font-size:15px;font-weight:bold;\">").append(label)
        .append("</center></v:roundrect><![endif]-->")
        .append("<!--[if !mso]><!--><a href=\"").append(href).append("\" style=\"display:inline-block;padding:" + BUTTON_PAD + ";font:bold 14px/20px " + brand.getFontStack())
        .append(COLOR_DECLARATION).append(onColor).append(";text-decoration:none;border:1px solid ").append(color).append(";border-radius:").append(radius).append(";\">")
        .append(label).append("</a><!--<![endif]--></td></tr></table></td></tr>");
  }

}
