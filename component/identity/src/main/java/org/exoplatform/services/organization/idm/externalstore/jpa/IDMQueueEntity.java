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

import java.io.Serializable;
import java.util.Calendar;

import org.exoplatform.services.organization.externalstore.model.IDMOperationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import io.meeds.common.persistence.PortableSequence;
import jakarta.persistence.Table;

@Entity(name = "IDMQueueEntity")
@Table(name = "IDM_QUEUE")
@NamedQuery(name = "IDMQueueEntity.countAllNotProcessedAndMaxNbRetries", query = "SELECT count(q) FROM IDMQueueEntity q "
    + " WHERE q.processed = FALSE AND nbRetries < :nbRetries ")
@NamedQuery(name = "IDMQueueEntity.countAllNotProcessedAndNbRetries", query = "SELECT count(q) FROM IDMQueueEntity q "
    + " WHERE q.processed = FALSE AND nbRetries = :nbRetries ")
@NamedQuery(name = "IDMQueueEntity.getEntriesNotProcessedWithNBRetries", query = "SELECT q FROM IDMQueueEntity q "
    + " WHERE q.processed = FALSE AND nbRetries = :nbRetries ORDER BY q.creationDate DESC, q.id ASC")
@NamedQuery(name = "IDMQueueEntity.setEntriesAsProcessed", query = "UPDATE IDMQueueEntity q "
    + " SET q.processed = TRUE WHERE q.id IN (:ids)")
@NamedQuery(name = "IDMQueueEntity.incrementEntriesRetry", query = "UPDATE IDMQueueEntity q "
    + " SET q.nbRetries = (q.nbRetries + 1) WHERE q.id IN (:ids)")
@NamedQuery(name = "IDMQueueEntity.deleteProcessedEntries", query = "DELETE FROM IDMQueueEntity q "
    + " WHERE q.processed = TRUE")
@NamedQuery(name = "IDMQueueEntity.deleteExceededRetriesEntries", query = "DELETE FROM IDMQueueEntity q "
    + " WHERE q.nbRetries >= :maxRetries")
public class IDMQueueEntity implements Serializable {

  private static final long serialVersionUID = 7102348817269095013L;

  @Id
  @Column(name = "IDM_QUEUE_ID")
  @PortableSequence(name = "SEQ_IDM_QUEUE_ID")
  private long              id;

  @Column(name = "CREATE_DATE")
  private Calendar          creationDate;

  @Column(name = "OPERATION_TYPE")
  private IDMOperationType  type;

  @Column(name = "ENTITY_ID")
  private String            entityId;

  @Column(name = "ENTITY_TYPE")
  private int               entityType;

  @Column(name = "NB_RETRIES")
  private int               nbRetries;

  @Column(name = "PROCESSED")
  private boolean           processed;

  public long getId() {
    return id;
  }

  public Calendar getCreationDate() {
    return creationDate;
  }

  public IDMQueueEntity setCreationDate(Calendar creationDate) {
    this.creationDate = creationDate;
    return this;
  }

  public IDMOperationType getType() {
    return type;
  }

  public IDMQueueEntity setType(IDMOperationType type) {
    this.type = type;
    return this;
  }

  public String getEntityId() {
    return entityId;
  }

  public IDMQueueEntity setEntityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public int getEntityType() {
    return entityType;
  }

  public IDMQueueEntity setEntityType(int entityType) {
    this.entityType = entityType;
    return this;
  }

  public int getNbRetries() {
    return nbRetries;
  }

  public IDMQueueEntity setNbRetries(int nbRetries) {
    this.nbRetries = nbRetries;
    return this;
  }

  public IDMQueueEntity setId(long id) {
    this.id = id;
    return this;
  }

  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

  public boolean isProcessed() {
    return processed;
  }
}
