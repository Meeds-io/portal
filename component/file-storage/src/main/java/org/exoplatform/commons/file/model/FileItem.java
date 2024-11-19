package org.exoplatform.commons.file.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;

/**
 * Object representing a file : information + binary
 */
public class FileItem {
  protected FileInfo fileInfo;

  /**
   * Object representing a file : information + binary
   */
  protected byte[]   data;

  public FileItem(FileInfo fileInfo, InputStream inputStream) throws Exception {
    this.fileInfo = fileInfo;
    if (inputStream != null) {
      this.data = IOUtils.toByteArray(inputStream);
    }
  }

  public FileItem(Long id,
                  String name,
                  String mimetype,
                  String nameSpace,
                  long size,
                  Date updatedDate,
                  String updater,
                  boolean deleted,
                  InputStream inputStream)
      throws Exception {
    this.fileInfo = new FileInfo(id, name, mimetype, nameSpace, size, updatedDate, updater, null, deleted);
    if (inputStream != null) {
      this.data = IOUtils.toByteArray(inputStream);
    }
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  /**
   * {@inheritDoc}
   */
  public InputStream getAsStream() {
    if (data != null) {
      return new ByteArrayInputStream(data);
    } else {
      return null;
    }
  }

  public byte[] getAsByte(){
    return data;
  }

  public void setInputStream(InputStream inputStream) throws Exception {
    if (inputStream != null) {
      this.data = IOUtils.toByteArray(inputStream);
    }
  }

  public void setFileInfo(FileInfo fileInfo) {
    this.fileInfo = fileInfo;
  }

}
