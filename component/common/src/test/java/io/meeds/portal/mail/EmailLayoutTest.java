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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmailLayoutTest {

  private static final EmailBrand BRAND = new EmailBrand("Acme Corp", "#2f5d62");

  private static EmailModel.Builder model() {
    return EmailModel.builder().lang("fr").title("Create your account").paragraph("Hello Sam,");
  }

  private static int countOf(String text, String part) {
    int count = 0;
    for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
      count++;
    }
    return count;
  }

  @Test
  public void testDocumentCarriesLanguageTitleAndHeading() {
    String html = EmailLayout.render(model().build(), BRAND);
    assertTrue(html, html.contains("<html lang=\"fr\""));
    assertTrue(html, html.contains("<title>Create your account</title>"));
    assertTrue(html, html.contains("Create your account</h1>"));
    assertEquals(html, 1, countOf(html, "<h1 "));
    assertTrue(html, html.contains("Acme Corp"));
    assertEquals("The company name is shown once", 1, countOf(html, "Acme Corp"));
  }

  @Test
  public void testBrandColorAndReadableTextOnIt() {
    String html = EmailLayout.render(model().button("Join", "https://acme.example/join").build(), BRAND);
    assertTrue(html, html.contains("background-color:#2f5d62"));
    assertTrue(html, html.contains("color:#ffffff"));
    String onYellow = EmailLayout.render(model().build(), new EmailBrand("Acme", "#f2c230"));
    assertTrue(onYellow, onYellow.contains("color:#1d2327;overflow-wrap"));
    assertFalse(onYellow, onYellow.contains("color:#ffffff;overflow-wrap"));
  }

  @Test
  public void testEveryTextIsEscaped() {
    String evil = "<script>alert('x')</script> & \"q\"";
    String html = EmailLayout.render(EmailModel.builder()
                                               .title(evil)
                                               .preheader(evil)
                                               .paragraph(evil)
                                               .fact(evil, evil)
                                               .lead(evil)
                                               .note(evil)
                                               .signoff(evil)
                                               .aside(evil)
                                               .button(evil, "https://acme.example/a?b=1&c=2")
                                               .build(),
                                     new EmailBrand(evil, "#476a9c"));
    assertFalse(html, html.contains("<script>"));
    assertTrue(html, html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;q&quot;"));
    assertTrue("a link keeps its query, escaped for html", html.contains("href=\"https://acme.example/a?b=1&amp;c=2\""));
  }

  @Test
  public void testOnlyTrustedHtmlIsInsertedAsIs() {
    String html = EmailLayout.render(model().trustedHtml("<div>Welcome <b>Jane</b></div>").build(), BRAND);
    assertTrue(html, html.contains("<div>Welcome <b>Jane</b></div>"));
    assertFalse(EmailLayout.render(model().build(), BRAND).contains("<div>Welcome"));
  }

  @Test
  public void testUnsafeLinkIsRefused() {
    for (String bad : new String[] { "javascript:alert(1)", "https://a.example/\" onclick=\"x", "data:text/html,x" }) {
      EmailModel unsafe = model().button("Join", bad).build();
      assertThrows(bad, IllegalArgumentException.class, () -> EmailLayout.render(unsafe, BRAND));
    }
    EmailLayout.render(model().button("Mail", "mailto:a@acme.example").build(), BRAND);
  }

  @Test
  public void testButtonHasAnOutlookShapeAndAPlainAddress() {
    String html = EmailLayout.render(model().button("Join", "https://acme.example/join")
                                            .fallbackLabel("Copy this address:")
                                            .build(),
                                     BRAND);
    assertTrue(html, html.contains("<!--[if mso]><v:roundrect"));
    assertTrue(html, html.contains("border-radius:8px"));
    assertTrue(html, html.contains("Copy this address:"));
    assertEquals("the link is the button twice, the plain address twice and the Gmail action twice", 6, countOf(html, "https://acme.example/join"));
  }

  @Test
  public void testNoButtonMeansNoFallbackAddress() {
    String html = EmailLayout.render(model().build(), BRAND);
    assertFalse(html, html.contains("copy this address"));
    assertFalse(html, html.contains("v:roundrect"));
  }

  @Test
  public void testFactAndSignoffParagraphs() {
    String html = EmailLayout.render(model().fact("Your login", "srivera").signoff("Thanks!\n\nSee you soon.\nTeam").build(), BRAND);
    assertTrue(html, html.contains("Your login"));
    assertTrue(html, html.contains("<strong style=\"font-size:17px;\">srivera</strong>"));
    assertTrue(html, html.contains("<p style=\"margin:0 0 14px\">Thanks!</p>"));
    assertTrue(html, html.contains("<p style=\"margin:0 0 14px\">See you soon.<br>Team</p>"));
  }

  @Test
  public void testHeaderRowSharesTheLookOfTheHeaderAndEscapesTheTitle() {
    String row = EmailLayout.headerRow(BRAND, "<b>Jordan</b> commented");
    assertTrue(row, row.startsWith("<tr>"));
    assertTrue(row, row.endsWith("</tr>"));
    assertTrue(row, row.contains("background-color:#2f5d62"));
    assertTrue(row, row.contains("Acme Corp"));
    assertTrue(row, row.contains("&lt;b&gt;Jordan&lt;/b&gt; commented</h1>"));
    assertFalse(row, row.contains("<b>"));
    assertTrue("The header of a whole email is the same row", EmailLayout.render(model().title("T").build(), BRAND)
                                                                         .contains(EmailLayout.headerRow(BRAND, "T")));
  }

  @Test
  public void testLinkStyledKeepsOneStyleAttributePerLink() {
    String out = EmailLayout.linkStyled("If not, <a target=\"_blank\" style=\"margin:30px 0;color:#2f5e92\" href=\"https://a.example/s\">settings</a>. And <a href=\"https://b.example\">b</a>");
    assertFalse("the style of the label is replaced, not doubled", out.contains("margin:30px"));
    assertEquals(2, out.split("style=", -1).length - 1);
    assertTrue(out, out.contains("target=\"_blank\"") && out.contains("href=\"https://a.example/s\">settings</a>"));
    assertTrue(out.contains("text-decoration:underline;"));
    assertEquals("", EmailLayout.linkStyled(null));
    assertEquals("no link, no change", "plain text", EmailLayout.linkStyled("plain text"));
  }

  @Test
  public void testEntityStyledNamesAreBoldAndNotUnderlined() {
    String out = EmailLayout.entityStyled("From <a href=\"https://a.example/u\" target=\"_blank\">Sam</a> now");
    assertTrue(out, out.contains("font-weight:bold") && out.contains("text-decoration:none"));
    assertTrue(out, out.contains("href=\"https://a.example/u\"") && out.endsWith("Sam</a> now"));
    assertFalse(out.contains("underline"));
    assertEquals("", EmailLayout.entityStyled(null));
  }

  @Test
  public void testAColonStaysWithItsLabel() {
    String html = EmailLayout.facts("Visioconf\u00e9rence : ", "x", "Where?", "y", "Who:", "z");
    assertTrue(html, html.contains("Visioconf\u00e9rence\u00a0:"));
    assertTrue("a question mark too", html.contains("Where?") && !html.contains("Where ?"));
    assertTrue(html.contains("Who:"));
    assertFalse("a normal space before the colon is gone", html.contains("Visioconf\u00e9rence :"));
  }

  @Test
  public void testEntityLinkEscapesTheNameAndOnlyLinksSafeAddresses() {
    String link = EmailLayout.entityLink("<b>Sprint</b> & co", "https://a.example/e?x=1&y=2");
    assertTrue(link, link.startsWith("<a href=\"https://a.example/e?x=1&amp;y=2\"") && link.contains("font-weight:bold"));
    assertTrue(link, link.contains("&lt;b&gt;Sprint&lt;/b&gt; &amp; co</a>"));
    assertFalse(link, link.contains("<b>"));
    for (String bad : new String[] { null, "", "javascript:alert(1)", "mailto:a@b.example", "/relative" }) {
      String span = EmailLayout.entityLink("Sprint", bad);
      assertTrue(bad + " -> " + span, span.startsWith("<span") && !span.contains("href"));
    }
  }

  @Test
  public void testFactsListsOnlyTheValuesThatAreThere() {
    String html = EmailLayout.facts("Where", "Room <i>1</i>", "When", "", "Who", null, "Notes", "Bring a laptop");
    assertTrue(html, html.contains(">Where</td>") && html.contains("Room <i>1</i>") && html.contains(">Notes</td>"));
    assertFalse(html, html.contains(">When</td>") || html.contains(">Who</td>"));
    assertTrue("the quote look", html.contains("border-left:4px solid") && html.contains("width:140px"));
    assertEquals("", EmailLayout.facts("A", " ", "B", null));
    assertEquals("", EmailLayout.facts());
    assertTrue("a label is escaped", EmailLayout.facts("<x>", "v").contains("&lt;x&gt;"));
  }

  @Test
  public void testQuoteWithoutSpaceAfterLeavesOutItsBottomMargin() {
    assertTrue(EmailLayout.quoteStart().contains("margin:0 0 12px 0;"));
    assertTrue(EmailLayout.quoteStart(false).contains("margin:0 0 0 0;"));
    assertEquals(EmailLayout.quoteStart(), EmailLayout.quoteStart(true));
  }

  @Test
  public void testButtonRowMarginPullsBackToTheStartOfTheColumn() {
    assertEquals("20px 0 8px -64px", EmailLayout.buttonRowMargin("fr", "64px"));
    assertEquals("20px 0 8px -64px", EmailLayout.buttonRowMargin(null, "64px"));
    assertEquals("20px -64px 8px 0", EmailLayout.buttonRowMargin("ar", "64px"));
  }

  @Test
  public void testGreetingWritesTheCommaOnce() {
    assertEquals("Hi Sam,", EmailLayout.greeting("Hi", "Sam"));
    assertEquals("Bonjour Sam,", EmailLayout.greeting("Bonjour,", "Sam"));
    assertEquals("Bonjour,", EmailLayout.greeting("Bonjour,", null));
    assertEquals("Bonjour,", EmailLayout.greeting(" Bonjour , ", " "));
    assertEquals("\u0645\u0631\u062d\u0628\u0627 Sam,", EmailLayout.greeting("\u0645\u0631\u062d\u0628\u0627\u060c", "Sam"));
    assertEquals(",", EmailLayout.greeting(null, null));
  }

  @Test
  public void testAnAddressThatTheProviderAlreadyEncodedStillMakesAButton() {
    // what HTMLEntityEncoder gives for https://x.org/p?a=1&b=2
    String encoded = "https&#x3a;&#x2f;&#x2f;x.org&#x2f;p&#x3f;a&#x3d;1&amp;b&#x3d;2";
    String button = EmailLayout.actionLink(BRAND, "Open", encoded, true);
    assertTrue(button, button.contains("href=\"https://x.org/p?a=1&amp;b=2\""));
    assertTrue("the label is not the only thing left", button.startsWith("<a "));
    String once = EmailLayout.actionLink(BRAND, "Open", "https://a.example/?x=1&amp;y=2", true);
    assertTrue("escaped once, not twice: " + once, once.contains("href=\"https://a.example/?x=1&amp;y=2\"") && !once.contains("&amp;amp;"));
    assertTrue(EmailLayout.entityLink("Sam", encoded).contains("href=\"https://x.org/p?a=1&amp;b=2\""));
    assertFalse("an encoded script address is still refused", EmailLayout.actionLink(BRAND, "Open", "javascript&#x3a;alert(1)", true).contains("href"));
    assertFalse(EmailLayout.actionLink(BRAND, "Open", "https&#x3a;&#x2f;&#x2f;x.org&#x2f;&quot; onclick=&quot;x", true).contains("onclick=\""));
  }

  @Test
  public void testTheBarOfAQuoteIsOnTheStartSideOfTheLanguage() {
    assertTrue(EmailLayout.quoteStart().contains("border-left:4px solid"));
    assertTrue(EmailLayout.quoteStart("fr").contains("border-left:4px solid"));
    assertTrue(EmailLayout.quoteStart("ar").contains("border-right:4px solid"));
    assertTrue(EmailLayout.quoteStart(false, "he").contains("border-right:4px solid") && EmailLayout.quoteStart(false, "he").contains("margin:0 0 0 0;"));
    assertTrue(EmailLayout.factsIn("fa", "A", "b").contains("border-right:4px solid"));
    assertTrue(EmailLayout.factsIn("en", "A", "b").contains("border-left:4px solid"));
  }

  @Test
  public void testLinkStyledHandlesSingleQuotedStylesAndKeepsDataAttributes() {
    String out = EmailLayout.linkStyled("<a style='margin:1px' data-style=\"keep\" href=\"https://a.example\">x</a>");
    assertEquals("one style attribute", 1, out.split(" style=", -1).length - 1 + (out.startsWith("<a style=") ? 0 : 0));
    assertTrue(out, out.contains("data-style=\"keep\"") && !out.contains("margin:1px"));
  }

  @Test
  public void testQuoteIsABarAndALightBackgroundAroundTheText() {
    String html = EmailLayout.quoteStart() + "a text" + EmailLayout.quoteEnd();
    assertTrue(html, html.startsWith("<table") && html.endsWith("</table>"));
    assertTrue(html, html.contains("border-left:4px solid") && html.contains("background-color:#f1f4f7"));
    assertTrue(html.contains("dir=\"auto\"") && html.contains("a text"));
    assertEquals(html.split("<table", -1).length, html.split("</table>", -1).length);
  }

  @Test
  public void testDarkStyleRecolorsTheInlineColorsOfTheNotificationEmails() {
    String row = EmailLayout.darkStyle() + EmailLayout.headerRow(BRAND, "Title", "en");
    assertTrue(row, row.startsWith("<style>") && row.contains("@media (prefers-color-scheme:dark)"));
    assertTrue("the inline ink and muted colors are recolored", row.contains("color:#1d2327") && row.contains("color:#5d676d"));
    assertTrue("no page margin around the header band, and a dark page behind it", row.contains("body{margin:0 !important}") && row.contains("html,body{background-color:#16191c !important}"));
    assertTrue("Outlook.com", row.contains("[data-ogsc]") && row.contains("[data-ogsb]"));
    assertFalse(":not is not understood by Gmail or Outlook", row.contains(":not("));
    assertTrue("the white background is recolored", row.contains("background-color:#ffffff") && row.contains("background-color:#16191c"));
    assertTrue("the header band keeps its brand color", row.contains("<tr><td bgcolor=\"" + BRAND.getFillColor() + "\""));
  }

  @Test
  public void testNotificationEmailsHaveASmallerTitleOnAPhone() {
    assertTrue(EmailLayout.darkStyle().contains("@media (max-width:480px){.eml-title{font-size:20px !important;padding:6px 0 18px 0 !important}}"));
    assertTrue("the header carries the class", EmailLayout.headerRow(BRAND, "T").contains("class=\"eml-title\""));
  }

  @Test
  public void testOutlinedButtonsReadOnTheDarkPage() {
    String outlined = EmailLayout.actionLink(BRAND, "View", "https://acme.example/v", false);
    assertTrue("the secondary button carries the class of the dark rule", outlined.contains(" class=\"eml-out\" "));
    assertFalse(EmailLayout.actionLink(BRAND, "Open", "https://acme.example/v", true).contains("eml-out"));
    String rule = "a.eml-out{background-color:#16191c !important;color:#8ab4f8 !important;border-color:#8ab4f8 !important}";
    assertTrue("notifications", EmailLayout.darkStyle().contains("@media (prefers-color-scheme:dark)") && EmailLayout.darkStyle().contains(rule));
    String html = EmailLayout.render(model().button("Go", "https://acme.example/go").build(), BRAND);
    assertTrue("account emails", html.contains(rule));
    assertTrue("Outlook.com", html.contains("[data-ogsc] a.eml-out{color:#8ab4f8 !important;border-color:#8ab4f8 !important}"));
    assertTrue("Outlook.com, notifications", EmailLayout.darkStyle().contains("[data-ogsc] a.eml-out{color:#8ab4f8 !important;border-color:#8ab4f8 !important}"));
    assertFalse("the rule that matched nothing is gone", EmailLayout.darkStyle().contains("padding:13px"));
    assertTrue("the tint reads on the dark page", EmailBrand.contrast("#8ab4f8", "#16191c") >= 4.5d);
  }

  @Test
  public void testDarkStyleAlsoRecolorsTheGreyTextAndTheBlueLinkOfTheActivityFragments() {
    String style = EmailLayout.darkStyle();
    assertTrue("the grey text of an activity quote is readable on the dark page", style.contains("color:#333333") && style.contains("color: #333;"));
    assertTrue("the blue mention and activity links are readable on the dark page", style.contains("a[style*=\"#2f5e92\"]") && style.contains("a[style*=\"#2F5E92\"]"));
  }

  @Test
  public void testViewActionIsOnlyWrittenForAnHttpsLinkAndEscapesTheTexts() {
    String markup = EmailLayout.viewAction("New <quest>", "Open the quest", "https&#x3a;&#x2f;&#x2f;example.test&#x2f;q&#x3f;a&#x3d;1");
    assertTrue(markup, markup.startsWith("<script type=\"application/ld+json\">") && markup.contains("\"@type\":\"ViewAction\""));
    assertTrue("the encoded address is read before it is written", markup.contains("https://example.test/q?a=1"));
    assertFalse("nothing in the markup can close the script element", markup.substring(10, markup.length() - 9).contains("<"));
    assertEquals("not an https link", "", EmailLayout.viewAction("d", "l", "http://example.test/q"));
    assertEquals("no link", "", EmailLayout.viewAction("d", "l", null));
  }

  @Test
  public void testClosingRowIsOnlySpaceWithoutAFrameOrALink() {
    String row = EmailLayout.closingRow();
    assertTrue(row, row.startsWith("<tr>") && row.endsWith("</tr>"));
    assertFalse("no frame", row.contains("border"));
    assertFalse("no link in the closing row", row.contains("<a ") || row.contains("href"));
  }

  @Test
  public void testRightToLeftLanguagesAreRecognizedWhateverTheirScriptOrRegion() {
    for (String lang : new String[] { "ar", "ar-OM", "ar_OM", "aro", "he", "he_IL", "iw", "fa", "fa-IR", "ur", "ur_PK", "ps", "ckb", "ku-Arab", "az-Arab",
        "AR" }) {
      assertTrue(lang, EmailLayout.isRtl(lang));
    }
    for (String lang : new String[] { "en", "en-US", "fr", "fr_FR", "de", "es_ES", "pt_BR", "ru", "ne", "tr", "in", "zh_CN", "ja", "ko", "th", "ku", "",
        "  ", null }) {
      assertFalse(String.valueOf(lang), EmailLayout.isRtl(lang));
    }
  }

  @Test
  public void testRightToLeftEmailIsAlignedToTheRight() {
    String html = EmailLayout.render(model().lang("he").fact("Label", "value").lead("Lead").button("Go", "https://acme.example/go").build(), BRAND);
    assertTrue(html, html.contains("<html lang=\"he\" dir=\"rtl\""));
    assertTrue(html, html.contains("<body dir=\"rtl\""));
    assertTrue("columns read from the right", html.contains("direction:rtl;"));
    assertTrue("text of the cells is aligned to the right", html.contains("<td dir=\"auto\" style=\"text-align:right;"));
    assertTrue("the bar of the fact is on the right", html.contains("border-right:4px solid #2f5d62"));
    assertFalse(html, html.contains("border-left:4px solid"));
    assertTrue("the button sits on the right", html.contains("class=\"eml-btn\" align=\"right\""));
    assertTrue("an address stays left to right", html.contains("dir=\"ltr\""));
  }

  @Test
  public void testLeftToRightEmailHasNoDirectionOverride() {
    String html = EmailLayout.render(model().lang("fr").fact("Label", "value").button("Go", "https://acme.example/go").build(), BRAND);
    assertFalse(html, html.contains("<html lang=\"fr\" dir"));
    assertFalse(html, html.contains("direction:rtl"));
    assertFalse(html, html.contains("text-align:right"));
    assertTrue(html, html.contains("border-left:4px solid #2f5d62"));
    assertFalse(html, html.contains("class=\"eml-btn\" align=\"right\""));
  }

  @Test
  public void testHeaderAndFooterRowsFollowTheLanguageToo() {
    assertTrue(EmailLayout.headerRow(BRAND, "T", "ar").contains("direction:rtl"));
    assertTrue(EmailLayout.headerRow(BRAND, "T", "ar").contains("text-align:right"));
    assertFalse(EmailLayout.headerRow(BRAND, "T", "fr").contains("direction:rtl"));
    assertFalse(EmailLayout.headerRow(BRAND, "T").contains("direction:rtl"));
  }

  @Test
  public void testGmailActionIsDeclaredForTheButton() {
    String html = EmailLayout.render(model().title("Join us").button("Join", "https://acme.example/join?a=1&b=2").build(), BRAND);
    assertTrue(html, html.contains("<script type=\"application/ld+json\">"));
    assertTrue(html, html.contains("\"@type\":\"ViewAction\""));
    assertTrue(html, html.contains("\"target\":\"https://acme.example/join?a=1\\u0026b=2\""));
    assertTrue(html, html.contains("\"name\":\"Join\""));
    assertTrue(html, html.contains("\"description\":\"Join us\""));
    assertTrue(html, html.contains("\"publisher\":{\"@type\":\"Organization\",\"name\":\"Acme Corp\"}"));
  }

  @Test
  public void testNoGmailActionWithoutAButtonOrForAnInsecureOrMailLink() {
    assertFalse(EmailLayout.render(model().build(), BRAND).contains("ld+json"));
    assertFalse("Gmail only honors https actions", EmailLayout.render(model().button("Go", "http://acme.example/go").build(), BRAND).contains("ld+json"));
    assertFalse(EmailLayout.render(model().button("Write", "mailto:a@acme.example").build(), BRAND).contains("ld+json"));
  }

  @Test
  public void testGmailActionCannotBreakOutOfItsScript() {
    String evil = "</script><img src=x onerror=alert(1)>\"";
    String html = EmailLayout.render(model().title(evil).button(evil, "https://acme.example/join").build(),
                                     new EmailBrand(evil, "#476a9c"));
    assertEquals("only the script's own closing tag", 1, countOf(html, "</script>"));
    assertFalse(html, html.contains("<img"));
    assertTrue(html, html.contains("\\u003c/script\\u003e"));
  }

  @Test
  public void testLongWordsBreakInsteadOfOverflowingTheScreen() {
    String html = EmailLayout.render(model().title("aVeryLongSpaceNameWithoutAnySpace").button("Go", "https://acme.example/go").build(), BRAND);
    assertTrue("the headline breaks long words", html.contains("overflow-wrap:anywhere;word-break:break-word;\">aVeryLong"));
    assertEquals("the columns break them too", 2, countOf(html, "max-width:600px;margin:0 auto;overflow-wrap:anywhere;word-break:break-word;"));
  }

  @Test
  public void testPrimaryActionLinkIsFilledWithTheBrandColor() {
    String link = EmailLayout.actionLink(BRAND, "Reply", "https://acme.example/r?a=1&b=2", true);
    assertTrue(link, link.startsWith("<a href=\"https://acme.example/r?a=1&amp;b=2\""));
    assertTrue(link, link.contains("background-color:#2f5d62;border:1px solid #2f5d62;color:#ffffff;"));
    assertTrue(link, link.contains("border-radius:8px"));
    assertTrue(link, link.contains(">Reply</a>"));
    assertFalse("no gradient or shadow", link.contains("gradient") || link.contains("shadow"));
    assertTrue("a light brand color gets dark text", EmailLayout.actionLink(new EmailBrand("A", "#f2c230"), "Go", "https://a.example", true)
                                                                .contains("color:#1d2327;"));
  }

  @Test
  public void testOutlinedActionLinkIsOutlinedInThePrimaryColor() {
    EmailBrand brand = new EmailBrand("Acme", "#2f5d62", null, "8px");
    String link = EmailLayout.actionLink(brand, "View", "https://acme.example/v", false);
    assertTrue(link, link.contains("background-color:#ffffff;border:1px solid " + brand.getOutlineColor() + ";color:" + brand.getLinkColor() + ";"));
    assertFalse("no gradient or shadow", link.contains("gradient") || link.contains("shadow"));
    EmailBrand light = new EmailBrand("Acme", "#f2c230", null, null);
    String lightLink = EmailLayout.actionLink(light, "View", "https://acme.example/v", false);
    assertFalse("a light color is darkened to be seen and read", lightLink.contains("border:1px solid #f2c230;"));
    assertTrue(EmailBrand.contrast("#ffffff", light.getOutlineColor()) >= 3.0d);
    assertTrue(EmailBrand.contrast("#ffffff", light.getLinkColor()) >= 4.5d);
  }

  @Test
  public void testNoStripeUnderTheHeaderAndTheKeyValueHasANeutralBackground() {
    EmailBrand brand = new EmailBrand("Acme", "#3f8487", null, "8px");
    String html = EmailLayout.render(model().fact("Label", "value").build(), brand);
    assertFalse("the third color is not used", html.contains("height:6px;line-height:6px"));
    assertTrue(html, html.contains("border-left:4px solid " + brand.getFillColor() + ";background-color:#f1f4f7"));
    assertTrue("the key value can be recolored in dark mode", html.contains("class=\"eml-tx eml-fact\""));
    assertFalse("nor in the header of the notifications", EmailLayout.headerRow(brand, "T").contains("height:6px;line-height:6px"));
  }

  @Test
  public void testHeaderIsCompact() {
    String html = EmailLayout.render(model().build(), BRAND);
    assertTrue(html, html.contains("padding:18px 0 0 0;font:bold 14px"));
    assertTrue(html, html.contains("class=\"eml-title\" style=\"padding:8px 0 20px 0;\"><h1 style=\"margin:0;font:bold 22px/1.25 "));
    assertTrue("a smaller title on a phone", html.contains(".eml-title{font-size:20px !important;padding:6px 0 18px 0 !important}"));
  }

  @Test
  public void testTextUsesTheFontOfTheBrandingWithoutLoadingIt() {
    EmailBrand inter = new EmailBrand("Acme", "#2f5d62", "Inter", null);
    String html = EmailLayout.render(model().button("Go", "https://acme.example/go").lead("Lead").note("Note").signoff("Bye").build(), inter);
    assertFalse("nothing is loaded from a third party", html.contains("<link") || html.contains("googleapis"));
    assertTrue(html, html.contains("font:15px/1.7 'Inter',Arial,Helvetica,sans-serif"));
    assertTrue("the button too", html.contains("font:bold 14px/20px 'Inter',Arial,Helvetica,sans-serif"));
    assertFalse("no fixed font is left", html.contains("Verdana") || html.contains("Trebuchet"));
    assertEquals("'Inter',Arial,Helvetica,sans-serif", EmailLayout.fontStack(inter));
    assertEquals("without a container, the default font", new EmailBrand("A", null).getFontStack(), EmailLayout.fontStack());
  }

  @Test
  public void testButtonFollowsTheBorderRadiusOfTheSite() {
    String square = EmailLayout.render(model().button("Go", "https://acme.example/go").build(), new EmailBrand("A", "#2f5d62", null, "0px"));
    assertTrue(square, square.contains("border-radius:0px;"));
    assertTrue("Outlook shape follows too", square.contains("arcsize=\"0%\""));
    String round = EmailLayout.render(model().button("Go", "https://acme.example/go").build(), new EmailBrand("A", "#2f5d62", null, "26px"));
    assertTrue(round, round.contains("border-radius:26px;"));
    assertTrue(round, round.contains("arcsize=\"50%\""));
    assertTrue(EmailLayout.actionLink(new EmailBrand("A", "#2f5d62", null, "20px"), "Go", "https://a.example", true).contains("border-radius:20px;"));
  }

  @Test
  public void testMainButtonOfANotificationCarriesTheGmailAction() {
    String main = EmailLayout.actionLink(BRAND, "Open the <post>", "https://acme.example/a?x=1&y=2", true);
    assertTrue(main, main.contains("<script type=\"application/ld+json\">"));
    assertTrue(main, main.contains("\"@type\":\"ViewAction\",\"target\":\"https://acme.example/a?x=1\\u0026y=2\""));
    assertTrue("the label is escaped in the json too", main.contains("\"name\":\"Open the \\u003cpost\\u003e\""));
    assertTrue(main, main.indexOf("</a>") < main.indexOf("<script"));
    assertFalse("a secondary button has none", EmailLayout.actionLink(BRAND, "Reply", "https://acme.example/r", false).contains("ld+json"));
    assertFalse("Gmail only honors https", EmailLayout.actionLink(BRAND, "Open", "http://acme.example/a", true).contains("ld+json"));
    assertFalse(EmailLayout.actionLink(BRAND, "Open", "mailto:a@acme.example", true).contains("ld+json"));
    String closing = EmailLayout.actionLink(BRAND, "</script><b>", "https://acme.example/a", true);
    assertEquals("the json cannot close its script", 1, closing.split("</script>", -1).length - 1);
  }

  @Test
  public void testActionLinkEscapesItsLabelAndRefusesUnsafeLinks() {
    assertTrue(EmailLayout.actionLink(BRAND, "<b>Q&amp;A</b>", "https://a.example", true).contains(">&lt;b&gt;Q&amp;A&lt;/b&gt;</a>"));
    for (String bad : new String[] { "javascript:alert(1)", "data:text/html,x", "https://a.example/\" onclick=\"x", "mailto:a@a.example", "", null }) {
      String out = EmailLayout.actionLink(BRAND, "Reply", bad, true);
      assertEquals("only the label stays for " + bad, "Reply", out);
    }
  }

  @Test
  public void testSignoffKeepsItsParagraphsWhenTheBundleKeepsTheEscapedLineBreaks() {
    String html = EmailLayout.render(model().signoff("We look forward!\\n\\nThanks and see you soon.\\nThe team").build(), BRAND);
    assertFalse(html, html.contains("\\n"));
    assertTrue(html, html.contains("<p style=\"margin:0 0 14px\">We look forward!</p><p style=\"margin:0 0 14px\">Thanks and see you soon.<br>The team</p>"));
  }

  @Test
  public void testGreetingHasTheCommaWithOrWithoutAName() {
    assertEquals("Hello Amal,", EmailLayout.greeting("Hello", "Amal"));
    assertEquals("Hello,", EmailLayout.greeting("Hello", null));
    assertEquals("Hello,", EmailLayout.greeting("Hello", "  "));
  }

  @Test
  public void testLeadInEndsWithAColonUnlessItAlreadyHasAPunctuationMark() {
    assertEquals("Click the link:", EmailLayout.leadIn(" Click the link "));
    for (String ended : new String[] { "Click.", "Click:", "Click!", "Click?", "Click\u2026", "\u0627\u0646\u0642\u0631\u061f", "\u70b9\u51fb\u3002", "Cliquez :".replace(" :", ":") }) {
      assertEquals(ended, EmailLayout.leadIn(ended));
    }
    assertEquals("", EmailLayout.leadIn("  "));
    assertEquals("", EmailLayout.leadIn(null));
  }

  @Test
  public void testJsonAndEscapeHelpersHandleSpecialCharacters() {
    assertEquals("\"a\\\"b\\\\c\\n\\r\\t\\u003c\\u003e\\u0026\\u2028\\u0001\"", EmailLayout.json("a\"b\\c\n\r\t<>&\u2028\u0001"));
    assertEquals("\"\"", EmailLayout.json(null));
    assertEquals("", EmailLayout.escape(null));
  }

  @Test
  public void testCurrentBrandFallsBackToTheDefaultWithoutAContainer() {
    EmailBrand.clearCache();
    EmailBrand brand = EmailBrand.current();
    assertEquals(EmailBrand.DEFAULT_COLOR, brand.getColor());
    assertTrue(EmailLayout.actionLink("Go", "https://a.example", true).contains(brand.getFillColor()));
  }

  @Test
  public void testEmailFollowsTheDarkModeOfTheClient() {
    String html = EmailLayout.render(model().fact("Label", "value").lead("Lead").button("Go", "https://acme.example/go").note("Note").signoff("Bye").build(),
                                     BRAND);
    assertTrue(html, html.contains("<meta name=\"color-scheme\" content=\"light dark\">"));
    assertTrue(html, html.contains("@media (prefers-color-scheme:dark){.eml-bg{background-color:#16191c !important}"));
    assertTrue("Outlook.com has its own selectors", html.contains("[data-ogsc] .eml-tx") && html.contains("[data-ogsb] .eml-bg"));
    assertTrue(html, html.contains("<body class=\"eml-bg\""));
    assertTrue("a button keeps its edge on a dark page", html.contains("td.eml-btn{box-shadow:0 0 0 1px rgba(232,235,237,.5)}"));
    assertTrue("the text can be recolored", html.contains("class=\"eml-tx\""));
    assertTrue("the muted text can be recolored", html.contains("class=\"eml-mu\""));
    assertTrue("the key value can be recolored", html.contains("class=\"eml-tx eml-fact\""));
    assertTrue("the brand color of the header stays", html.contains("background-color:#2f5d62;padding:0 24px;"));
    assertFalse("no cell is marked twice", html.contains("class=\"eml-tx\" class") || html.contains("class=\"eml-mu\" class"));
  }

  @Test
  public void testDarkModeDoesNotBreakTheRightToLeftAlignment() {
    String html = EmailLayout.render(model().lang("ar").fact("Label", "value").build(), BRAND);
    assertTrue(html, html.contains("<body dir=\"rtl\" class=\"eml-bg\""));
    assertTrue(html, html.contains("<td dir=\"auto\" style=\"text-align:right;"));
    assertTrue(html, html.contains("class=\"eml-tx eml-fact\""));
  }

  @Test
  public void testBrandRejectsAColorThatIsNotAHexColor() {
    assertEquals(EmailBrand.DEFAULT_COLOR, new EmailBrand("A", "red;background-color:url(x)").getColor());
    assertEquals(EmailBrand.DEFAULT_COLOR, new EmailBrand("A", null).getColor());
    assertEquals("#abcdef", new EmailBrand("A", " #ABCDEF ").getColor());
    assertEquals("", new EmailBrand(null, null).getCompanyName());
  }

  @Test
  public void testOutlookGetsATableOfSixHundredPixelsAroundEachColumn() {
    String html = EmailLayout.render(model().build(), BRAND);
    assertEquals(2, countOf(html, "<!--[if mso]><table role=\"presentation\" width=\"600\" align=\"center\""));
    assertEquals(2, countOf(html, "<!--[if mso]></td></tr></table><![endif]-->"));
  }

  @Test
  public void testPreviewLineIsHiddenInEveryWayAndOnlyWhenThereIsOne() {
    String html = EmailLayout.render(model().preheader("482915").build(), BRAND);
    assertTrue(html, html.contains("display:none;visibility:hidden;opacity:0;color:transparent;height:0;width:0;max-height:0;max-width:0;font-size:1px;line-height:1px;mso-hide:all;overflow:hidden;\">482915</div>"));
    assertFalse(EmailLayout.render(model().build(), BRAND).contains("mso-hide:all"));
  }

  @Test
  public void testSolidColorsUseBackgroundColorWhichOutlookUnderstands() {
    String html = EmailLayout.render(model().fact("L", "v").button("Go", "https://acme.example/go").build(), BRAND);
    assertFalse(html, html.contains("background:"));
    assertTrue(html, html.contains("background-color:#2f5d62"));
  }

  @Test
  public void testHeaderOfALightBrandColorIsNotRecoloredInDarkMode() {
    String html = EmailLayout.render(model().fact("L", "v").build(), new EmailBrand("Acme", "#f2c230"));
    int header = html.indexOf("<h1");
    String head = html.substring(0, html.indexOf("</h1>"));
    assertTrue(header > 0);
    assertFalse("the header text keeps its own color", head.substring(head.indexOf("<body")).contains("class=\"eml-tx\""));
    assertTrue("the content is recolored", html.substring(html.indexOf("</h1>")).contains("class=\"eml-tx"));
  }

  @Test
  public void testEveryButtonHasTheSameSizeTypeAndOutline() {
    String standalone = EmailLayout.render(model().button("Go", "https://acme.example/go").build(), BRAND);
    String inline = EmailLayout.actionLink(BRAND, "Go", "https://acme.example/go", true);
    for (String part : new String[] { "padding:7px 20px;", "font:bold 14px/20px ", "border:1px solid #2f5d62;", "border-radius:8px;" }) {
      assertTrue("standalone: " + part, standalone.contains(part));
      assertTrue("inline: " + part, inline.contains(part));
    }
    assertTrue("the Outlook shape has the same height", standalone.contains("height:36px;"));
  }

  @Test
  public void testActionLinksStackAndNeverOverflowTheScreen() {
    String link = EmailLayout.actionLink(BRAND, "View the full discussion", "https://acme.example/d", false);
    assertTrue(link, link.contains("display:block;width:-moz-fit-content;width:fit-content;max-width:100%;margin:0 0 12px 0;"));
    assertFalse("no side by side buttons that wrap raggedly", link.contains("inline-block"));
  }

  @Test
  public void testEachTextBlockOfARightToLeftEmailChoosesItsOwnDirection() {
    String html = EmailLayout.render(model().lang("ar").paragraph("Click on the link.").signoff("Thanks").lead("Go").build(), BRAND);
    assertTrue(html, html.contains("<p dir=\"auto\" style=\""));
    assertTrue(html, html.contains("<h1 dir=\"auto\" style=\""));
    assertFalse("a left to right email is left alone", EmailLayout.render(model().lang("fr").paragraph("Cliquez").build(), BRAND).contains("dir=\"auto\""));
  }

  @Test
  public void testDirectionHelpersForTheTemplatesThatKeepTheirOwnLayout() {
    assertEquals("rtl", EmailLayout.dir("ar"));
    assertEquals("ltr", EmailLayout.dir("fr"));
    assertEquals("right", EmailLayout.start("he"));
    assertEquals("left", EmailLayout.start("en"));
    assertEquals("left", EmailLayout.end("fa"));
    assertEquals("right", EmailLayout.end("de"));
    assertEquals("0 15px 0 0", EmailLayout.gap("en", "15px"));
    assertEquals("0 0 0 15px", EmailLayout.gap("ar", "15px"));
    assertEquals("a missing language is left to right", "ltr", EmailLayout.dir(null));
  }

  @Test
  public void testTextColorHasTheBetterContrast() {
    assertEquals("#ffffff", new EmailBrand("A", "#000000").getTextColor());
    assertEquals("#1d2327", new EmailBrand("A", "#ffffff").getTextColor());
    assertEquals("#ffffff", new EmailBrand("A", "#476a9c").getTextColor());
    assertEquals("#1d2327", new EmailBrand("A", "#f2c230").getTextColor());
    for (String c : new String[] { "#476a9c", "#2f5d62", "#f2c230", "#d6d6d6", "#e53935", "#00897b" }) {
      EmailBrand brand = new EmailBrand("A", c);
      assertTrue(c, EmailBrand.contrast(brand.getTextColor(), brand.getColor()) >= 4.5d || c.equals("#e53935") || c.equals("#00897b"));
    }
  }

}
