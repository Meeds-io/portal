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
package org.exoplatform.services.organization.externalstore;

import java.time.*;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.services.organization.externalstore.model.*;
import org.exoplatform.services.organization.idm.externalstore.IDMInMemoryQueueServiceImpl;

import junit.framework.TestCase;

public class TestIDMQueueService extends TestCase {
  IDMQueueService queueService = null;

  @Before
  public void setUp() {
    queueService = new IDMInMemoryQueueServiceImpl(null);
  }

  @Test
  public void testPush() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));
    assertEquals("Two new pushed elements must be detected", 2, queueService.countAll());
  }

  @Test
  public void testPop() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));

    IDMQueueEntry entryInError = new IDMQueueEntry(IDMEntityType.USER, "testuser3", IDMOperationType.ADD_OR_UPDATE);
    entryInError.setRetryCount(1);
    queueService.push(entryInError);

    IDMQueueEntry entryProcessed = new IDMQueueEntry(IDMEntityType.USER, "testuser4", IDMOperationType.ADD_OR_UPDATE);
    entryProcessed.setProcessed(true);
    queueService.push(entryProcessed);

    assertEquals("Only not processed and retry < maxRetries should be recognized.", 3, queueService.countAll());
    assertEquals("Only two entries are not processed and have nbRetries == 0.", 2, queueService.count(0));
    assertEquals("Only one entry is not processed and have nbRetries == 1.", 1, queueService.count(1));

    assertEquals("Only one entry is requested which has't been processed and have nbRetries == 0.",
                 1,
                 queueService.pop(1, 0, true).size());
    assertEquals("Only two entries are requested which have't been processed and have nbRetries == 0.",
                 2,
                 queueService.pop(2, 0, true).size());
    assertEquals("Only two entries are existing in queue which have't been processed and have nbRetries == 0.",
                 2,
                 queueService.pop(100, 0, true).size());

    assertEquals("Only one entry is existing in queue which has't been processed and have nbRetries == 1.",
                 1,
                 queueService.pop(100, 1, true).size());
    assertEquals("No entry is existing in queue which has't been processed and have nbRetries == 2.",
                 0,
                 queueService.pop(100, 2, true).size());

    List<IDMQueueEntry> entities = queueService.pop(1, 1, false);
    assertEquals("Only one entry is existing in queue which has't been processed and have nbRetries == 1.", 1, entities.size());
    assertEquals("Queue entry should has nbRetries == 1.", 1, entities.get(0).getRetryCount());
    assertEquals("Queue entry should has id = 'testuser3'.", "testuser3", entities.get(0).getEntityId());
    assertEquals("No entry should exists in queue which has't been processed and have nbRetries == 1.", 0, queueService.count(1));

    entities = queueService.pop(1, 0, false);
    assertEquals("Only one entry is requested which has't been processed and have nbRetries == 0.", 1, entities.size());
    assertEquals("One entry is requested which has't been processed and have nbRetries == 0. Thus nbRetries should equals to 0.",
                 0,
                 entities.get(0).getRetryCount());
    assertEquals("Last added entry should be retrieved", "testuser2", entities.get(0).getEntityId());
    assertEquals("The last request 'pop' operation should delete the requested entry."
        + " Thus, only one entry should remaining that has't been processed and have nbRetries == 0.", 1, queueService.count(0));
  }

  @Test
  public void testLastCheckedTime() throws Exception {
    assertNotNull(getLocalDateTime());
    assertNull(queueService.getLastCheckedTime(IDMEntityType.USER));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.GROUP));

    // Test add checked users time
    LocalDateTime userCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.USER, userCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.GROUP));

    // Test modify checked users time
    userCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.USER, userCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.GROUP));

    // Test add checked groups time
    LocalDateTime groupCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.GROUP, groupCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertEquals(groupCheckedTime, queueService.getLastCheckedTime(IDMEntityType.GROUP));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));

    // Test add checked roles time
    LocalDateTime roleCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.ROLE, roleCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertEquals(groupCheckedTime, queueService.getLastCheckedTime(IDMEntityType.GROUP));
    assertEquals(roleCheckedTime, queueService.getLastCheckedTime(IDMEntityType.ROLE));
  }

  @Test
  public void testProcessed() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));

    IDMQueueEntry entryInError = new IDMQueueEntry(IDMEntityType.USER, "testuser3", IDMOperationType.ADD_OR_UPDATE);
    entryInError.setRetryCount(1);
    queueService.push(entryInError);

    IDMQueueEntry entryProcessed = new IDMQueueEntry(IDMEntityType.USER, "testuser4", IDMOperationType.ADD_OR_UPDATE);
    entryProcessed.setProcessed(true);
    queueService.push(entryProcessed);

    assertEquals(3, queueService.countAll());
    assertEquals(2, queueService.count(0));
    assertEquals(1, queueService.count(1));

    List<IDMQueueEntry> entities = queueService.pop(1, 0, true);
    assertEquals(1, entities.size());
    assertFalse(entities.get(0).isProcessed());
    assertEquals(0, entities.get(0).getRetryCount());
    assertEquals("testuser2", entities.get(0).getEntityId());

    queueService.storeAsProcessed(entities);

    assertEquals(2, queueService.countAll());
    assertEquals(1, queueService.count(0));
    assertEquals(1, queueService.count(1));

    entities = queueService.pop(1, 0, true);
    assertEquals(1, entities.size());
    assertFalse(entities.get(0).isProcessed());
    assertEquals(0, entities.get(0).getRetryCount());
    assertEquals("testuser", entities.get(0).getEntityId());

    queueService.deleteProcessedEntries();

    assertEquals(2, queueService.countAll());
    assertEquals(1, queueService.count(0));
    assertEquals(1, queueService.count(1));

    entities = queueService.pop(1, 0, true);
    assertEquals(1, entities.size());
    assertFalse(entities.get(0).isProcessed());
    assertEquals(0, entities.get(0).getRetryCount());
    assertEquals("testuser", entities.get(0).getEntityId());
  }

  @Test
  public void testIncrementRetry() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));

    IDMQueueEntry entryInError = new IDMQueueEntry(IDMEntityType.USER, "testuser3", IDMOperationType.ADD_OR_UPDATE);
    entryInError.setRetryCount(1);
    queueService.push(entryInError);

    IDMQueueEntry entryProcessed = new IDMQueueEntry(IDMEntityType.USER, "testuser4", IDMOperationType.ADD_OR_UPDATE);
    entryProcessed.setProcessed(true);
    queueService.push(entryProcessed);

    List<IDMQueueEntry> entities = queueService.pop(100, 1, true);
    assertEquals(1, entities.size());
    queueService.incrementRetry(entities);
    assertEquals(0, queueService.pop(100, 1, true).size());
    assertEquals(1, queueService.pop(100, 2, true).size());

    entities = queueService.pop(100, 0, true);
    assertEquals(2, entities.size());
    queueService.incrementRetry(entities);
    assertEquals(2, queueService.pop(100, 1, true).size());
    assertEquals(1, queueService.pop(100, 2, true).size());
    assertEquals(0, queueService.pop(100, 0, true).size());
    assertEquals(0, queueService.pop(100, 3, true).size());
  }

  /**
   * @return {@link LocalDateTime} relative to current date time on server
   */
  private LocalDateTime getLocalDateTime() {
    return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
  }

}
