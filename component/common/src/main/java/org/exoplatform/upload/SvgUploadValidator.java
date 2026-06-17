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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Rejects unsafe SVG uploads without modifying the uploaded file.
 * <p>
 * This validator is intentionally SVG-only. Non-SVG files are not opened or
 * parsed by this class.
 */
public class SvgUploadValidator implements UploadFileValidator {

  public static final String INFECTED_SVG_MESSAGE = "Infected file detected. Uploading is forbidden";

  private static final long DEFAULT_MAX_SVG_VALIDATION_SIZE = 10L * 1024L * 1024L;

  private final long        maxSvgValidationSize;

  public SvgUploadValidator() {
    this(DEFAULT_MAX_SVG_VALIDATION_SIZE);
  }

  public SvgUploadValidator(long maxSvgValidationSize) {
    this.maxSvgValidationSize = maxSvgValidationSize;
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
    saxParser.parse(new InputSource(inputStream), new SvgSafetyHandler());
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

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      String tagName = normalizeXmlName(localName, qName);
      if (isDangerousSvgTag(tagName)) {
        throw new UnsafeSvgException("Unsafe SVG tag detected: " + tagName);
      }

      for (int i = 0; i < attributes.getLength(); i++) {
        String attributeName = normalizeXmlName(attributes.getLocalName(i), attributes.getQName(i));
        String attributeValue = attributes.getValue(i);
        if (isDangerousSvgAttribute(attributeName, attributeValue)) {
          throw new UnsafeSvgException("Unsafe SVG attribute detected: " + attributeName);
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

      String normalizedValue = removeAsciiWhitespaces(attributeValue).toLowerCase(Locale.ROOT);
      return normalizedValue.startsWith("javascript:")
          || normalizedValue.contains("javascript:")
          || normalizedValue.startsWith("data:text/html")
          || normalizedValue.contains("data:text/html");
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
