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
package org.exoplatform.services.organization.idm.externalstore.jpa;

import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class IDMQueueDAO extends GenericDAOJPAImpl<IDMQueueEntity, Long> {

  public int countAllNotProcessedAndMaxNbRetries(int maxRetries) {
    return getEntityManager().createNamedQuery("IDMQueueEntity.countAllNotProcessedAndMaxNbRetries", Long.class)
                             .setParameter("nbRetries", maxRetries)
                             .getSingleResult()
                             .intValue();
  }

  public int countAllNotProcessedAndNbRetries(int nbRetries) {
    return getEntityManager().createNamedQuery("IDMQueueEntity.countAllNotProcessedAndNbRetries", Long.class)
                             .setParameter("nbRetries", nbRetries)
                             .getSingleResult()
                             .intValue();
  }

  public List<IDMQueueEntity> getEntriesNotProcessedWithNBRetries(int nbRetries, int limit) {
    return getEntityManager().createNamedQuery("IDMQueueEntity.getEntriesNotProcessedWithNBRetries", IDMQueueEntity.class)
                             .setParameter("nbRetries", nbRetries)
                             .setMaxResults(limit)
                             .getResultList();
  }

  @ExoTransactional
  public void setProcessed(List<Long> ids) {
    getEntityManager().createNamedQuery("IDMQueueEntity.setEntriesAsProcessed").setParameter("ids", ids).executeUpdate();
  }

  @ExoTransactional
  public void incrementRetry(List<Long> ids) {
    getEntityManager().createNamedQuery("IDMQueueEntity.incrementEntriesRetry").setParameter("ids", ids).executeUpdate();
  }

  @ExoTransactional
  public void deleteProcessedEntries() {
    getEntityManager().createNamedQuery("IDMQueueEntity.deleteProcessedEntries").executeUpdate();
  }

  @ExoTransactional
  public void deleteExceededRetriesEntries(int maxRetries) {
    getEntityManager().createNamedQuery("IDMQueueEntity.deleteExceededRetriesEntries")
                      .setParameter("maxRetries", maxRetries)
                      .executeUpdate();
  }

}
