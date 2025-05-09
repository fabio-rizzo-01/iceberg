/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg;

import static org.apache.iceberg.types.Types.NestedField.required;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.iceberg.types.Types;

class V1Metadata {
  private V1Metadata() {}

  static final Schema MANIFEST_LIST_SCHEMA =
      new Schema(
          ManifestFile.PATH,
          ManifestFile.LENGTH,
          ManifestFile.SPEC_ID,
          ManifestFile.SNAPSHOT_ID,
          ManifestFile.ADDED_FILES_COUNT,
          ManifestFile.EXISTING_FILES_COUNT,
          ManifestFile.DELETED_FILES_COUNT,
          ManifestFile.PARTITION_SUMMARIES,
          ManifestFile.ADDED_ROWS_COUNT,
          ManifestFile.EXISTING_ROWS_COUNT,
          ManifestFile.DELETED_ROWS_COUNT,
          ManifestFile.KEY_METADATA);

  /**
   * A wrapper class to write any ManifestFile implementation to Avro using the v1 schema.
   *
   * <p>This is used to maintain compatibility with v1 by writing manifest list files with the old
   * schema, instead of writing a sequence number into metadata files in v1 tables.
   */
  static class ManifestFileWrapper implements ManifestFile, StructLike {
    private ManifestFile wrapped = null;

    public ManifestFile wrap(ManifestFile file) {
      this.wrapped = file;
      return this;
    }

    @Override
    public int size() {
      return MANIFEST_LIST_SCHEMA.columns().size();
    }

    @Override
    public <T> void set(int pos, T value) {
      throw new UnsupportedOperationException("Cannot modify ManifestFileWrapper wrapper via set");
    }

    @Override
    public <T> T get(int pos, Class<T> javaClass) {
      return javaClass.cast(get(pos));
    }

    private Object get(int pos) {
      switch (pos) {
        case 0:
          return path();
        case 1:
          return length();
        case 2:
          return partitionSpecId();
        case 3:
          return snapshotId();
        case 4:
          return addedFilesCount();
        case 5:
          return existingFilesCount();
        case 6:
          return deletedFilesCount();
        case 7:
          return partitions();
        case 8:
          return addedRowsCount();
        case 9:
          return existingRowsCount();
        case 10:
          return deletedRowsCount();
        case 11:
          return keyMetadata();
        default:
          throw new UnsupportedOperationException("Unknown field ordinal: " + pos);
      }
    }

    @Override
    public String path() {
      return wrapped.path();
    }

    @Override
    public long length() {
      return wrapped.length();
    }

    @Override
    public int partitionSpecId() {
      return wrapped.partitionSpecId();
    }

    @Override
    public ManifestContent content() {
      return wrapped.content();
    }

    @Override
    public long sequenceNumber() {
      return wrapped.sequenceNumber();
    }

    @Override
    public long minSequenceNumber() {
      return wrapped.minSequenceNumber();
    }

    @Override
    public Long snapshotId() {
      return wrapped.snapshotId();
    }

    @Override
    public boolean hasAddedFiles() {
      return wrapped.hasAddedFiles();
    }

    @Override
    public Integer addedFilesCount() {
      return wrapped.addedFilesCount();
    }

    @Override
    public Long addedRowsCount() {
      return wrapped.addedRowsCount();
    }

    @Override
    public boolean hasExistingFiles() {
      return wrapped.hasExistingFiles();
    }

    @Override
    public Integer existingFilesCount() {
      return wrapped.existingFilesCount();
    }

    @Override
    public Long existingRowsCount() {
      return wrapped.existingRowsCount();
    }

    @Override
    public boolean hasDeletedFiles() {
      return wrapped.hasDeletedFiles();
    }

    @Override
    public Integer deletedFilesCount() {
      return wrapped.deletedFilesCount();
    }

    @Override
    public Long deletedRowsCount() {
      return wrapped.deletedRowsCount();
    }

    @Override
    public List<PartitionFieldSummary> partitions() {
      return wrapped.partitions();
    }

    @Override
    public ByteBuffer keyMetadata() {
      return wrapped.keyMetadata();
    }

    @Override
    public ManifestFile copy() {
      return wrapped.copy();
    }
  }

  static Schema entrySchema(Types.StructType partitionType) {
    return wrapFileSchema(dataFileSchema(partitionType));
  }

  static Schema wrapFileSchema(Types.StructType fileSchema) {
    // this is used to build projection schemas
    return new Schema(
        ManifestEntry.STATUS,
        ManifestEntry.SNAPSHOT_ID,
        required(ManifestEntry.DATA_FILE_ID, "data_file", fileSchema));
  }

  private static final Types.NestedField BLOCK_SIZE =
      required(105, "block_size_in_bytes", Types.LongType.get());

  static Types.StructType dataFileSchema(Types.StructType partitionType) {
    return Types.StructType.of(
        DataFile.FILE_PATH,
        DataFile.FILE_FORMAT,
        required(DataFile.PARTITION_ID, DataFile.PARTITION_NAME, partitionType),
        DataFile.RECORD_COUNT,
        DataFile.FILE_SIZE,
        BLOCK_SIZE,
        DataFile.COLUMN_SIZES,
        DataFile.VALUE_COUNTS,
        DataFile.NULL_VALUE_COUNTS,
        DataFile.NAN_VALUE_COUNTS,
        DataFile.LOWER_BOUNDS,
        DataFile.UPPER_BOUNDS,
        DataFile.KEY_METADATA,
        DataFile.SPLIT_OFFSETS,
        DataFile.SORT_ORDER_ID);
  }

  /** Wrapper used to write a ManifestEntry to v1 metadata. */
  static class ManifestEntryWrapper implements ManifestEntry<DataFile>, StructLike {
    private final int size;
    private final DataFileWrapper fileWrapper;
    private ManifestEntry<DataFile> wrapped = null;

    ManifestEntryWrapper() {
      this.size = entrySchema(Types.StructType.of()).columns().size();
      this.fileWrapper = new DataFileWrapper();
    }

    public ManifestEntryWrapper wrap(ManifestEntry<DataFile> entry) {
      this.wrapped = entry;
      return this;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public <T> void set(int pos, T value) {
      throw new UnsupportedOperationException("Cannot modify ManifestEntryWrapper wrapper via set");
    }

    @Override
    public <T> T get(int pos, Class<T> javaClass) {
      return javaClass.cast(get(pos));
    }

    private Object get(int pos) {
      switch (pos) {
        case 0:
          return wrapped.status().id();
        case 1:
          return wrapped.snapshotId();
        case 2:
          DataFile file = wrapped.file();
          if (file != null) {
            return fileWrapper.wrap(file);
          }
          return null;
        default:
          throw new UnsupportedOperationException("Unknown field ordinal: " + pos);
      }
    }

    @Override
    public Status status() {
      return wrapped.status();
    }

    @Override
    public Long snapshotId() {
      return wrapped.snapshotId();
    }

    @Override
    public void setSnapshotId(long snapshotId) {
      wrapped.setSnapshotId(snapshotId);
    }

    @Override
    public Long dataSequenceNumber() {
      return wrapped.dataSequenceNumber();
    }

    @Override
    public void setDataSequenceNumber(long dataSequenceNumber) {
      wrapped.setDataSequenceNumber(dataSequenceNumber);
    }

    @Override
    public Long fileSequenceNumber() {
      return wrapped.fileSequenceNumber();
    }

    @Override
    public void setFileSequenceNumber(long fileSequenceNumber) {
      wrapped.setFileSequenceNumber(fileSequenceNumber);
    }

    @Override
    public DataFile file() {
      return wrapped.file();
    }

    @Override
    public ManifestEntry<DataFile> copy() {
      return wrapped.copy();
    }

    @Override
    public ManifestEntry<DataFile> copyWithoutStats() {
      return wrapped.copyWithoutStats();
    }
  }

  static class DataFileWrapper implements DataFile, StructLike {
    private static final long DEFAULT_BLOCK_SIZE = 64 * 1024 * 1024;

    private final int size;
    private DataFile wrapped = null;

    DataFileWrapper() {
      this.size = dataFileSchema(Types.StructType.of()).fields().size();
    }

    DataFileWrapper wrap(DataFile file) {
      this.wrapped = file;
      return this;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public <T> void set(int pos, T value) {
      throw new UnsupportedOperationException("Cannot modify DataFileWrapper wrapper via set");
    }

    @Override
    public <T> T get(int pos, Class<T> javaClass) {
      return javaClass.cast(get(pos));
    }

    private Object get(int pos) {
      switch (pos) {
        case 0:
          return wrapped.location();
        case 1:
          return wrapped.format() != null ? wrapped.format().toString() : null;
        case 2:
          return wrapped.partition();
        case 3:
          return wrapped.recordCount();
        case 4:
          return wrapped.fileSizeInBytes();
        case 5:
          return DEFAULT_BLOCK_SIZE;
        case 6:
          return wrapped.columnSizes();
        case 7:
          return wrapped.valueCounts();
        case 8:
          return wrapped.nullValueCounts();
        case 9:
          return wrapped.nanValueCounts();
        case 10:
          return wrapped.lowerBounds();
        case 11:
          return wrapped.upperBounds();
        case 12:
          return wrapped.keyMetadata();
        case 13:
          return wrapped.splitOffsets();
        case 14:
          return wrapped.sortOrderId();
      }
      throw new IllegalArgumentException("Unknown field ordinal: " + pos);
    }

    @Override
    public Long pos() {
      return null;
    }

    @Override
    public String manifestLocation() {
      return null;
    }

    @Override
    public int specId() {
      return wrapped.specId();
    }

    @Override
    public FileContent content() {
      return wrapped.content();
    }

    @Override
    public CharSequence path() {
      return wrapped.location();
    }

    @Override
    public FileFormat format() {
      return wrapped.format();
    }

    @Override
    public StructLike partition() {
      return wrapped.partition();
    }

    @Override
    public long recordCount() {
      return wrapped.recordCount();
    }

    @Override
    public long fileSizeInBytes() {
      return wrapped.fileSizeInBytes();
    }

    @Override
    public Map<Integer, Long> columnSizes() {
      return wrapped.columnSizes();
    }

    @Override
    public Map<Integer, Long> valueCounts() {
      return wrapped.valueCounts();
    }

    @Override
    public Map<Integer, Long> nullValueCounts() {
      return wrapped.nullValueCounts();
    }

    @Override
    public Map<Integer, Long> nanValueCounts() {
      return wrapped.nanValueCounts();
    }

    @Override
    public Map<Integer, ByteBuffer> lowerBounds() {
      return wrapped.lowerBounds();
    }

    @Override
    public Map<Integer, ByteBuffer> upperBounds() {
      return wrapped.upperBounds();
    }

    @Override
    public ByteBuffer keyMetadata() {
      return wrapped.keyMetadata();
    }

    @Override
    public List<Long> splitOffsets() {
      return wrapped.splitOffsets();
    }

    @Override
    public Integer sortOrderId() {
      return wrapped.sortOrderId();
    }

    @Override
    public Long dataSequenceNumber() {
      return wrapped.dataSequenceNumber();
    }

    @Override
    public Long fileSequenceNumber() {
      return wrapped.fileSequenceNumber();
    }

    @Override
    public DataFile copy() {
      return wrapped.copy();
    }

    @Override
    public DataFile copyWithStats(Set<Integer> requestedColumnIds) {
      return wrapped.copyWithStats(requestedColumnIds);
    }

    @Override
    public DataFile copyWithoutStats() {
      return wrapped.copyWithoutStats();
    }
  }
}
