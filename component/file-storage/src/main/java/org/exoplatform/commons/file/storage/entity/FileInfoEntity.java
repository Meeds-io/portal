package org.exoplatform.commons.file.storage.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity(name = "FileInfoEntity")
@Table(name = "FILES_FILES")
@NamedQuery(name = "fileEntity.findByChecksum", query = "SELECT t FROM FileInfoEntity t WHERE t.checksum = :checksum AND t.deleted = FALSE")
@NamedQuery(name = "fileEntity.countByChecksum", query = "SELECT COUNT(t) FROM FileInfoEntity t WHERE t.checksum = :checksum AND t.deleted = FALSE")
public class FileInfoEntity {

  @Id
  @Column(name = "FILE_ID")
  @SequenceGenerator(name = "SEQ_FILES_FILES_FILE_ID", sequenceName = "SEQ_FILES_FILES_FILE_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FILES_FILES_FILE_ID")
  private Long            id;

  @Column(name = "NAME")
  private String          name;

  @Column(name = "MIMETYPE")
  private String          mimetype;

  @Column(name = "FILE_SIZE")
  private long            size;

  @Column(name = "UPDATED_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date            updatedDate;

  @Column(name = "UPDATER")
  private String          updater;

  @Column(name = "CHECKSUM")
  private String          checksum;

  @Column(name = "DELETED")
  private boolean         deleted;

  @ManyToOne
  @JoinColumn(name = "NAMESPACE_ID")
  private NameSpaceEntity nameSpaceEntity;

  public FileInfoEntity() {
  }

  public FileInfoEntity(String name,
                        String mimetype,
                        long size,
                        Date updatedDate,
                        String updater,
                        String checksum,
                        boolean deleted) {
    this.name = name;
    this.mimetype = mimetype;
    this.size = size;
    this.updatedDate = updatedDate;
    this.updater = updater;
    this.checksum = checksum;
    this.deleted = deleted;
  }

  public FileInfoEntity(long id,
                        String name,
                        String mimetype,
                        long size,
                        Date updatedDate,
                        String updater,
                        String checksum,
                        boolean deleted) {
    this(name, mimetype, size, updatedDate, updater, checksum, deleted);
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMimetype() {
    return mimetype;
  }

  public void setMimetype(String mimetype) {
    this.mimetype = mimetype;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getUpdater() {
    return updater;
  }

  public void setUpdater(String updater) {
    this.updater = updater;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public NameSpaceEntity getNameSpaceEntity() {
    return nameSpaceEntity;
  }

  public FileInfoEntity setNameSpaceEntity(NameSpaceEntity nameSpaceEntity) {
    this.nameSpaceEntity = nameSpaceEntity;
    return this;
  }
}
