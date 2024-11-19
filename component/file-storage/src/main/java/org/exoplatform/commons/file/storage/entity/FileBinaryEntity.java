package org.exoplatform.commons.file.storage.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity(name = "FileBinaryEntity")
@Table(name = "FILES_BINARY")
@NamedQuery(name = "FileBinaryEntity.findByName", query = "SELECT t FROM FileBinaryEntity t WHERE t.name = :name")
public class FileBinaryEntity {
    @Id
    @Column(name = "BLOB_ID")
    @SequenceGenerator(name = "SEQ_FILES_BINARY_BLOB_ID", sequenceName = "SEQ_FILES_BINARY_BLOB_ID", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FILES_BINARY_BLOB_ID")
    private Long            id;

    @Column(name = "NAME")
    private String          name;

    @Column(name = "DATA", columnDefinition="BLOB")
    private byte[] data;

    @Column(name = "UPDATED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
}
