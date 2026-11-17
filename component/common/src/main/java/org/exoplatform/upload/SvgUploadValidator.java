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
package org.exoplatform.upload;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Rejects unsafe SVG uploads without modifying the uploaded file.
 * <p>
 * This validator is intentionally SVG-only. Non-SVG files are not opened or
 * parsed by this class.
 */
public class SvgUploadValidator implements UploadFileValidator {

  public static final String INFECTED_SVG_MESSAGE = "Infected file detected. Uploading is forbidden";

  /**
   * The deepest element nesting an SVG may have, 0 for no limit (the default, as
   * before JDK 25, whose {@code jaxp.properties} sets a limit of 100).
   */
  public static final String MAX_ELEMENT_DEPTH_PROPERTY = "exo.upload.svg.maxElementDepth";

  private static final Log  LOG = ExoLogger.getLogger(SvgUploadValidator.class);

  private static final long DEFAULT_MAX_SVG_VALIDATION_SIZE = 10L * 1024L * 1024L;

  private static final int  DEFAULT_MAX_ELEMENT_DEPTH = 0;

  private final long        maxSvgValidationSize;

  private final int         maxElementDepth;

  public SvgUploadValidator() {
    this(DEFAULT_MAX_SVG_VALIDATION_SIZE);
  }

  public SvgUploadValidator(long maxSvgValidationSize) {
    this.maxSvgValidationSize = maxSvgValidationSize;
    this.maxElementDepth = readMaxElementDepth();
  }

  @Override
  public boolean supports(String fileName, String mimeType) {
    return isSvgMimeType(mimeType) || isSvgFileName(fileName) || isSvgzFileName(fileName);
  }

  @Override
  public void validate(String fileName, String mimeType, InputStream inputStream) throws FileUploadException {
    if (inputStream == null) {
      throw new FileUploadException("Unable to validate SVG file");
    }

    try {
      validateSvg(openSvgValidationInputStream(fileName, inputStream));
    } catch (UnsafeSvgException e) {
      throw new FileUploadException(INFECTED_SVG_MESSAGE, e);
    } catch (SvgTooLargeException e) {
      throw new FileUploadException("SVG file is too large to be safely validated", e);
    } catch (SAXException e) {
      throw new FileUploadException("Invalid SVG file", e);
    } catch (ParserConfigurationException | IOException e) {
      throw new FileUploadException("Unable to validate SVG file", e);
    }
  }

  private InputStream openSvgValidationInputStream(String fileName, InputStream inputStream) throws IOException {
    InputStream nonClosingInputStream = new NonClosingInputStream(inputStream);
    InputStream bufferedInputStream = new BufferedInputStream(nonClosingInputStream);
    InputStream svgInputStream = isSvgzFileName(fileName) ? new GZIPInputStream(bufferedInputStream) : bufferedInputStream;
    return new MaxBytesInputStream(svgInputStream, maxSvgValidationSize);
  }

  private void validateSvg(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    configureSecureFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
    configureSecureFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
    configureSecureFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
    configureSecureFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

    SAXParser saxParser = factory.newSAXParser();
    // The parse streams, so a deep drawing exhausts no stack
    saxParser.setProperty("jdk.xml.maxElementDepth", String.valueOf(maxElementDepth));
    saxParser.parse(new InputSource(inputStream), new SvgSafetyHandler());
  }

  private static int readMaxElementDepth() {
    String value = PropertyManager.getProperty(MAX_ELEMENT_DEPTH_PROPERTY);
    if (StringUtils.isBlank(value)) {
      return DEFAULT_MAX_ELEMENT_DEPTH;
    }
    try {
      int depth = Integer.parseInt(value.trim());
      if (depth >= 0) {
        return depth;
      }
    } catch (NumberFormatException e) {
      // Reported below
    }
    LOG.warn("{} is '{}', not a number of levels: no limit is used", MAX_ELEMENT_DEPTH_PROPERTY, value);
    return DEFAULT_MAX_ELEMENT_DEPTH;
  }

  private void configureSecureFeature(SAXParserFactory factory, String feature, boolean value)
      throws ParserConfigurationException, SAXException {
    factory.setFeature(feature, value);
  }

  private boolean isSvgMimeType(String mimeType) {
    return mimeType != null && "image/svg+xml".equalsIgnoreCase(trimMimeTypeParameters(mimeType));
  }

  private String trimMimeTypeParameters(String mimeType) {
    int separatorIndex = mimeType.indexOf(';');
    if (separatorIndex >= 0) {
      return mimeType.substring(0, separatorIndex).trim();
    }
    return mimeType.trim();
  }

  private boolean isSvgFileName(String fileName) {
    return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".svg");
  }

  private boolean isSvgzFileName(String fileName) {
    return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".svgz");
  }

  /**
   * Prevents validators or XML parsers from closing the caller-owned upload
   * stream.
   */
  private static class NonClosingInputStream extends FilterInputStream { // NOSONAR

    NonClosingInputStream(InputStream inputStream) {
      super(inputStream);
    }

    @Override
    public void close() throws IOException {
      // The caller owns the original upload stream.
    }
  }

  /**
   * Bounds SVG validation work without requiring the whole file to be loaded in
   * memory first.
   */
  private static class MaxBytesInputStream extends FilterInputStream {

    private final long maxBytes;

    private long       bytesRead;

    MaxBytesInputStream(InputStream inputStream, long maxBytes) {
      super(inputStream);
      this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
      int read = super.read();
      if (read >= 0) {
        incrementBytesRead(1);
      }
      return read;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      int read = super.read(bytes, offset, length);
      if (read > 0) {
        incrementBytesRead(read);
      }
      return read;
    }

    private void incrementBytesRead(long increment) throws IOException {
      bytesRead += increment;
      if (maxBytes > 0 && bytesRead > maxBytes) {
        throw new SvgTooLargeException();
      }
    }
  }

  private static class SvgSafetyHandler extends DefaultHandler {

    private int           styleDepth;

    private StringBuilder styleText = new StringBuilder();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      String tagName = normalizeXmlName(localName, qName);
      if (isDangerousSvgTag(tagName)) {
        throw new UnsafeSvgException("Unsafe SVG tag detected: " + tagName);
      }
      if ("style".equals(tagName)) {
        styleDepth++;
      }

      boolean animatesHref = false;
      for (int i = 0; i < attributes.getLength(); i++) {
        String attributeName = normalizeXmlName(attributes.getLocalName(i), attributes.getQName(i));
        String attributeValue = attributes.getValue(i);
        if (isDangerousSvgAttribute(attributeName, attributeValue)) {
          throw new UnsafeSvgException("Unsafe SVG attribute detected: " + attributeName);
        }
        animatesHref |= "attributename".equals(attributeName) && attributeValue != null
                        && "href".equals(normalizeXmlName(null, removeAsciiWhitespaces(attributeValue)));
      }
      if (animatesHref && isAnimationTag(tagName)) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String attributeName = normalizeXmlName(attributes.getLocalName(i), attributes.getQName(i));
          if (isAnimatedValueAttribute(attributeName) && hasExternalAnimatedReference(attributes.getValue(i), "values".equals(attributeName))) {
            throw new UnsafeSvgException("External reference animated on an href: " + attributeName);
          }
        }
      }
    }

    @Override
    public void characters(char[] chars, int start, int length) {
      if (styleDepth > 0) {
        styleText.append(chars, start, length);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (styleDepth > 0 && "style".equals(normalizeXmlName(localName, qName))) {
        styleDepth--;
        if (styleDepth == 0) {
          String css = styleText.toString();
          styleText = new StringBuilder();
          if (hasExternalCssReference(css)) {
            throw new UnsafeSvgException("External reference in a style element");
          }
        }
      }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
      if (target != null && "xml-stylesheet".equalsIgnoreCase(target)) {
        throw new UnsafeSvgException("External stylesheet processing instruction detected");
      }
    }

    private static String normalizeXmlName(String localName, String qName) {
      String name = localName == null || localName.isEmpty() ? qName : localName;
      if (name == null) {
        return "";
      }
      int namespaceSeparatorIndex = name.indexOf(':');
      if (namespaceSeparatorIndex >= 0 && namespaceSeparatorIndex + 1 < name.length()) {
        name = name.substring(namespaceSeparatorIndex + 1);
      }
      return name.toLowerCase(Locale.ROOT);
    }

    private static boolean isDangerousSvgTag(String tagName) {
      return "script".equals(tagName)
          || "foreignobject".equals(tagName)
          || "iframe".equals(tagName)
          || "object".equals(tagName)
          || "embed".equals(tagName)
          || "applet".equals(tagName);
    }

    private static boolean isDangerousSvgAttribute(String attributeName, String attributeValue) {
      if (attributeName == null || attributeName.isEmpty()) {
        return false;
      }
      if (attributeName.startsWith("on")) {
        return true;
      }
      if (attributeValue == null) {
        return false;
      }

      if (hasExternalCssReference(attributeValue)) {
        // a style declaration or a presentation attribute (fill, filter, mask...) reaching a resource through CSS
        return true;
      }
      String normalizedValue = removeAsciiWhitespaces(attributeValue).toLowerCase(Locale.ROOT);
      if ("href".equals(attributeName) && !normalizedValue.startsWith("#") && !normalizedValue.startsWith("data:image/")) {
        // an external reference (href, xlink:href): a fragment of the same document or an inline image only
        return true;
      }
      return normalizedValue.startsWith("javascript:")
          || normalizedValue.contains("javascript:")
          || normalizedValue.startsWith("data:text/html")
          || normalizedValue.contains("data:text/html");
    }

    private static boolean isAnimationTag(String tagName) {
      return "set".equals(tagName) || tagName.startsWith("animate");
    }

    private static boolean isAnimatedValueAttribute(String attributeName) {
      return "to".equals(attributeName) || "from".equals(attributeName) || "by".equals(attributeName)
          || "values".equals(attributeName);
    }

    /**
     * @param animatedValues the value, or the ';' separated values, an
     *          animation gives an href
     * @param list true for a values list, whose items are separated by ';'
     * @return true when one is not a fragment of the document nor an inline
     *         image
     */
    private static boolean hasExternalAnimatedReference(String animatedValues, boolean list) {
      if (animatedValues == null) {
        return false;
      }
      for (String value : list ? animatedValues.split(";") : new String[] { animatedValues }) {
        String normalized = removeAsciiWhitespaces(value).toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty() && !normalized.startsWith("#") && !normalized.startsWith("data:image/")) {
          return true;
        }
      }
      return false;
    }

    /**
     * @param css a style declaration list, a style element's text or any
     *          attribute value
     * @return true when it imports a stylesheet, or holds a CSS function
     *         (url, src, image, image-set) whose target is not a fragment of
     *         the document nor an inline image. Comments and escapes are
     *         resolved first, so that they cannot hide the function.
     */
    private static boolean hasExternalCssReference(String css) {
      if (css.indexOf('\\') < 0 && css.indexOf('(') < 0 && css.indexOf('@') < 0) {
        return false;
      }
      String normalized = resolveCssEscapes(stripCssComments(css)).toLowerCase(Locale.ROOT);
      if (normalized.contains("@import")) {
        return true;
      }
      for (String function : new String[] { "url(", "src(", "image(", "image-set(" }) {
        int from = 0;
        while ((from = normalized.indexOf(function, from)) >= 0) {
          int argumentsStart = from + function.length();
          from = argumentsStart;
          if (hasExternalCssArgument(normalized, argumentsStart, "image-set(".equals(function))) {
            return true;
          }
        }
      }
      return false;
    }

    private static boolean hasExternalCssArgument(String css, int start, boolean stringsOnly) {
      int position = start;
      while (position < css.length() && css.charAt(position) != ')') {
        char c = css.charAt(position);
        if (Character.isWhitespace(c) || c == ',') {
          position++;
          continue;
        }
        int end;
        String target;
        if (c == '"' || c == '\'') {
          end = css.indexOf(c, position + 1);
          end = end < 0 ? css.length() : end;
          target = css.substring(position + 1, end);
          position = end + 1;
        } else if (stringsOnly) {
          // a nested function or a resolution descriptor of an image set: not a target
          position = skipCssToken(css, position);
          continue;
        } else {
          end = css.indexOf(')', position);
          end = end < 0 ? css.length() : end;
          target = css.substring(position, end).trim();
          position = end;
        }
        if (!target.startsWith("#") && !target.startsWith("data:image/")) {
          return true;
        }
        if (!stringsOnly) {
          return false;
        }
      }
      return false;
    }

    private static int skipCssToken(String css, int start) {
      int position = start;
      while (position < css.length() && !Character.isWhitespace(css.charAt(position)) && css.charAt(position) != ','
             && css.charAt(position) != ')') {
        position++;
      }
      return Math.max(position, start + 1);
    }

    private static String stripCssComments(String css) {
      StringBuilder result = new StringBuilder(css.length());
      int position = 0;
      while (position < css.length()) {
        int comment = css.indexOf("/*", position);
        if (comment < 0) {
          result.append(css, position, css.length());
          break;
        }
        result.append(css, position, comment);
        int end = css.indexOf("*/", comment + 2);
        position = end < 0 ? css.length() : end + 2;
      }
      return result.toString();
    }

    private static String resolveCssEscapes(String css) {
      if (css.indexOf('\\') < 0) {
        return css;
      }
      StringBuilder result = new StringBuilder(css.length());
      int position = 0;
      while (position < css.length()) {
        char c = css.charAt(position++);
        if (c != '\\' || position >= css.length()) {
          result.append(c);
          continue;
        }
        int digits = 0;
        int codePoint = 0;
        while (digits < 6 && position < css.length() && Character.digit(css.charAt(position), 16) >= 0) {
          codePoint = codePoint * 16 + Character.digit(css.charAt(position), 16);
          position++;
          digits++;
        }
        if (digits > 0) {
          result.appendCodePoint(Character.isValidCodePoint(codePoint) && codePoint > 0 ? codePoint : 0xFFFD);
          if (position < css.length() && Character.isWhitespace(css.charAt(position))) {
            position++;
          }
        } else {
          char escaped = css.charAt(position++);
          if (escaped != '\n') {
            result.append(escaped);
          }
        }
      }
      return result.toString();
    }

    private static String removeAsciiWhitespaces(String value) {
      StringBuilder result = new StringBuilder(value.length());
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f') {
          result.append(c);
        }
      }
      return result.toString().trim();
    }
  }

  private static class UnsafeSvgException extends SAXException {

    private static final long serialVersionUID = 1L;

    UnsafeSvgException(String message) {
      super(message);
    }
  }

  private static class SvgTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;
  }
}
