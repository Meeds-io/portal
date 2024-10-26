/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.navigation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.mop.SiteKey;

/**
 * An immutable node data class.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class NodeData implements Serializable {

  private static final long serialVersionUID = 5801881702083669782L;

  /** . */
  final String              parentId;

  /** . */
  final String              id;

  /** . */
  final SiteKey             siteKey;

  /** . */
  final String              name;

  /** . */
  final NodeState           state;

  /** . */
  final String[]            children;

  /** . */
  final String              target;

  /** . */
  final long                updatedDate;

  public NodeData(String parentId,
                  String id,
                  SiteKey siteKey,
                  String name,
                  NodeState state,
                  String[] children,
                  String target,
                  long updatedDate) {
    this.parentId = parentId;
    this.id = id;
    this.siteKey = siteKey;
    this.name = name;
    this.state = state;
    this.children = children;
    this.target = target;
    this.updatedDate = updatedDate;
  }

  NodeData(NodeContext<?> context) {
    int size = 0;
    for (NodeContext<?> current = context.getFirst(); current != null; current = current.getNext()) {
      size++;
    }
    String[] children = new String[size];
    for (NodeContext<?> current = context.getFirst(); current != null; current = current.getNext()) {
      children[children.length - size--] = current.handle;
    }
    String parentId = context.getParent() != null ? context.getParent().handle : null;
    String id = context.handle;
    String name = context.getName();
    NodeState state = context.getState();

    //
    this.parentId = parentId;
    this.id = id;
    this.name = name;
    this.state = state;
    this.children = children;
    this.siteKey = context.getState().getSiteKey();
    this.target = state.getTarget();
    this.updatedDate = state.getUpdatedDate();
  }

  public Iterator<String> iterator(boolean reverse) {
    if (reverse) {
      return new Iterator<String>() {
        int index = children.length;

        public boolean hasNext() {
          return index > 0;
        }

        public String next() {
          if (index > 0) {
            return children[--index];
          } else {
            throw new NoSuchElementException();
          }
        }
      };
    } else {
      return new Iterator<String>() {
        int index = 0;

        public boolean hasNext() {
          return index < children.length;
        }

        public String next() {
          if (index < children.length) {
            return children[index++];
          } else {
            throw new NoSuchElementException();
          }
        }
      };
    }
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public NodeState getState() {
    return state;
  }

  public SiteKey getSiteKey() {
    return siteKey;
  }

  public String getParentId() {
    return this.parentId;
  }

  public String getTarget() {
    return target;
  }

  public long getUpdatedDate() {
    return updatedDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof NodeData))
      return false;

    NodeData nodeData = (NodeData) o;

    return StringUtils.equals(parentId, nodeData.parentId) && StringUtils.equals(id, nodeData.id)
           && StringUtils.equals(name, nodeData.name);
  }

  @Override
  public int hashCode() {
    int result = parentId != null ? parentId.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "NodeData[id=" + id + ",name=" + name + ",state=" + state + ",children=" + Arrays.asList(children) + "]";
  }
}
