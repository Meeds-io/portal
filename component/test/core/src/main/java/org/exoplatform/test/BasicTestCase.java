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
package org.exoplatform.test;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class BasicTestCase extends TestCase {
  private static int testNumber_ = 1;

  protected int      counter_;

  public BasicTestCase() {
  }

  public BasicTestCase(String name) {
    super(name);
  }

  protected int getTestNumber() {
    return testNumber_;
  }

  protected void setTestNumber(int num) {
    testNumber_ = num;
  }

  protected int getCounter() {
    return counter_ + 1;
  }

  protected void runTest() throws Throwable {
    long t = System.currentTimeMillis();
    long firstRun = 0;
    int testNum = getTestNumber();
    for (counter_ = 0; counter_ < testNum; counter_++) {
      super.runTest();
      if (counter_ == 0) {
        firstRun = System.currentTimeMillis() - t;
      }
    }
    t = System.currentTimeMillis() - t;
  }

  protected String getDescription() {
    return "Run test " + getClass().getName();
  }

  protected static void hasObjectInCollection(Object obj, Collection c, Comparator comparator) throws Exception {
    Iterator iter = c.iterator();
    while (iter.hasNext()) {
      Object o = iter.next();
      if (comparator.compare(obj, o) == 0)
        return;
    }
    throw new Exception("Object " + obj + " hasn't in collection " + c);
  }

  protected static void assertCollection(Collection c1, Collection c2, Comparator comparator) throws Exception {
    if (c1.size() != c2.size()) {
      throw new Exception("Size of collection_1:" + c1.size() + " is not equals to collection_2:" + c2.size());
    }

    for (Object o : c1)
      hasObjectInCollection(o, c2, comparator);
  }

  protected void assertObject(Object o1, Object o2, Comparator comparator) throws Exception {
    if (comparator.compare(o1, o2) != 0) {
      throw new Exception("Object " + o1 + "not equals to" + o2);
    }
  }
}
