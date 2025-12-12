//package com.truongquycode.course_service.config;
//
//import java.util.Map;
//
//import org.apache.kafka.streams.state.RocksDBConfigSetter;   // <-- Kafka Streams API
//import org.rocksdb.Options;
//import org.rocksdb.CompactionStyle;
//import org.rocksdb.BlockBasedTableConfig;
//import org.rocksdb.CompressionType;
//
//public class CustomRocksDBConfig implements RocksDBConfigSetter {
//
//    @Override
//    public void setConfig(String storeName, Options options, Map<String, Object> configs) {
//
//        options.setIncreaseParallelism(8);
//        options.setMaxBackgroundCompactions(8);
//        options.setMaxBackgroundFlushes(4);
//
//        options.setWriteBufferSize(64 * 1024 * 1024);
//        options.setMaxWriteBufferNumber(3);
//
//        options.setCompressionType(CompressionType.LZ4_COMPRESSION);
//        options.setCompactionStyle(CompactionStyle.UNIVERSAL);
//
//        BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
//        tableConfig.setBlockCacheSize(256 * 1024 * 1024L);
//        options.setTableFormatConfig(tableConfig);
//    }
//
//    @Override
//    public void close(String storeName, Options options) {
//        // not required
//    }
//}
