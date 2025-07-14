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
package exo.portal.component.identiy.opendsconfig;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.gatein.common.xml.NoSuchElementException;
import org.gatein.common.xml.TooManyElementException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class XMLTools
{

   /** Document builder factory. */
   private static final DocumentBuilderFactory buildFactory = DocumentBuilderFactory.newInstance();

   /** Transformer factory. */
   private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

   /** Default output format which is : no xml declaration, no document type, indent. */
   private static Properties DEFAULT_FORMAT = createFormat(true, false, true, "utf-8");


   /** prevent instantiation */
   private XMLTools()
   {
   }

   /** Return the builder factory. */
   public static DocumentBuilderFactory getDocumentBuilderFactory()
   {
      return buildFactory;
   }

      private static Properties createFormat(boolean omitXMLDeclaration, boolean standalone, boolean indented, String encoding)
   {
      Properties format = new Properties();
      format.setProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration ? "yes" : "no");
      format.setProperty(OutputKeys.STANDALONE, standalone ? "yes" : "no");
      format.setProperty(OutputKeys.INDENT, indented ? "yes" : "no");
      format.setProperty(OutputKeys.ENCODING, encoding);
      return format;
   }

   /**
    *
    */
   public static String toString(Document doc, boolean omitXMLDeclaration, boolean standalone, boolean indented, String encoding) throws TransformerException
   {
      Properties format = createFormat(omitXMLDeclaration, standalone, indented, encoding);
      return toString(doc, format);
   }

   /**
    * Serialize the document with the default format : - No XML declaration - Indented - Encoding is UTF-8
    *
    * @see #toString(Document,Properties)
    */
   public static String toString(Document doc) throws TransformerException
   {
      return toString(doc, DEFAULT_FORMAT);
   }

   /** @see #toString(Document) */
   public static String toString(Element element) throws ParserConfigurationException, TransformerException
   {
      return toString(element, DEFAULT_FORMAT);
   }

   /** Converts an element to a String representation. */
   private static String toString(Element element, Properties properties) throws ParserConfigurationException, TransformerException
   {
      Document doc = buildFactory.newDocumentBuilder().newDocument();
      element = (Element)doc.importNode(element, true);
      doc.appendChild(element);
      return toString(doc, properties);
   }

   /** Converts an document to a String representation. */
   private static String toString(Document doc, Properties format) throws TransformerException
   {
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperties(format);
      StringWriter writer = new StringWriter();
      Source source = new DOMSource(doc);
      Result result = new StreamResult(writer);
      transformer.transform(source, result);
      return writer.toString();
   }

   /**
    * Perform trimming by default
    *
    * @param element
    * @return
    * @throws IllegalArgumentException
    * @see #asString(Element,boolean)
    */
   public static String asString(Element element) throws IllegalArgumentException
   {
      return asString(element, true);
   }

   /**
    * Get the element's content as a string.
    *
    * @param element the container
    * @param trim    true if text should be trimmed before returning result
    * @throws IllegalArgumentException if the element content is mixed or null
    */
   public static String asString(Element element, boolean trim) throws IllegalArgumentException
   {
      if (element == null)
      {
         throw new IllegalArgumentException("No null element allowed");
      }

      //
      StringBuffer buffer = new StringBuffer();
      NodeList children = element.getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         Node child = children.item(i);
         switch (child.getNodeType())
         {
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
               buffer.append(((Text)child).getData());
               break;
            case Node.ELEMENT_NODE:
               throw new IllegalArgumentException("Mixed content not allowed");
            default:
               break;
         }
      }
      String result = buffer.toString();
      if (trim)
      {
         result = result.trim();
      }
      return result;
   }

   /**
    * Return an optional child of an element with the specified name.
    *
    * @param element the parent element
    * @param name    the child name
    * @param strict  if the child must be present
    * @return the child element or null if it does not exist and strict is set to false
    * @throws IllegalArgumentException if an argument is null
    * @throws NoSuchElementException   if strict is true and the element is not present
    * @throws TooManyElementException  if more than one element is found
    */
   public static Element getUniqueChild(Element element, String name, boolean strict) throws IllegalArgumentException,
      NoSuchElementException, TooManyElementException
   {
      return getUniqueChild(element, null, name, strict);
   }

   /**
    * Return an optional child of an element with the specified name and the optionally specified namespace uri.
    *
    * @param element the parent element
    * @param name the child name
    * @param uri the child uri
    * @param strict if the child must be present
    * @return the child element or null if it does not exist and strict is set to false
    * @throws IllegalArgumentException if an argument is null
    * @throws NoSuchElementException   if strict is true and the element is not present
    * @throws TooManyElementException  if more than one element is found
    */
   public static Element getUniqueChild(Element element, String uri, String name, boolean strict) throws IllegalArgumentException,
      NoSuchElementException, TooManyElementException
   {
      List list = getChildren(element, uri, name);
      switch (list.size())
      {
         case 0:
            if (strict)
            {
               throw new NoSuchElementException("Missing child " + name + " of element " + element.getNodeName());
            }
            else
            {
               return null;
            }
         case 1:
            return (Element)list.get(0);
         default:
            throw new TooManyElementException("Too many children for element " + element.getNodeName());
      }
   }

   /**
    * Return an iterator for all the children of the given element having the specified name.
    *
    * @param element the parent element
    * @param name    the child names
    * @return an iterator for the designated elements
    * @throws IllegalArgumentException if the element is null or the name is null
    */
   public static Iterator<Element> getChildrenIterator(Element element, String name) throws IllegalArgumentException
   {
      return getChildren(element, name).iterator();
   }

   /**
    * Return all the children of the given element having the specified name. The collection object can be modified.
    *
    * @param element the parent element
    * @param name    the child names
    * @return a list of elements
    * @throws IllegalArgumentException if the element is null or the name is null
    */
   public static List<Element> getChildren(Element element, String name) throws IllegalArgumentException
   {
      return getChildren(element, null, name);
   }

   /**
    * Return all the children of the given element having the specified name and the optionally specified namespace URI.
    * The collection object can be modified.
    *
    * @param element the parent element
    * @param uri the children uri
    * @param name the children name
    * @return a list of elements
    * @throws IllegalArgumentException if the element is null or the name is null
    */
   public static List<Element> getChildren(Element element, String uri, String name) throws IllegalArgumentException
   {
      if (element == null)
      {
         throw new IllegalArgumentException("No element found");
      }
      if (name == null)
      {
         throw new IllegalArgumentException("No name specified");
      }
      ArrayList<Element> result = new ArrayList<Element>();
      NodeList list = element.getChildNodes();
      for (int i = 0; i < list.getLength(); i++)
      {
         Node node = list.item(i);
         if (node.getNodeType() == Node.ELEMENT_NODE)
         {
            Element childElt = (Element)node;

            //
            if (uri == null)
            {
               if (childElt.getTagName().equals(name))
               {
                  result.add(childElt);
               }
            }
            else if (uri.equals(childElt.getNamespaceURI()))
            {
               if (childElt.getLocalName().equals(name))
               {
                  result.add(childElt);
               }
            }
         }
      }
      return result;
   }
}