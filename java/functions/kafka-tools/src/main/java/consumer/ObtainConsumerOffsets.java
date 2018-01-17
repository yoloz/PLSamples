package consumer;

import kafka.coordinator.GroupMetadataManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 指定消费的group，topic后，从__consumer_offsets中查询出group的消费情况,从中也可以得出消费结束时间
 * Created on 17-4-17.
 */
public class ObtainConsumerOffsets {

    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("未添加配置文件...");
            System.exit(-1);
        }
        if (Files.notExists(Paths.get(args[0]))) {
            System.out.println("配置文件：" + args[0] + "不存在...");
            System.exit(-1);
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        props.put("group.id", System.currentTimeMillis() + "");
        props.put("key.deserializer", ByteArrayDeserializer.class.getName());
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        ObtainConsumerOffsets.ConsumeOffsets consumeOffsets = new ObtainConsumerOffsets().new ConsumeOffsets(props);
        Runtime.getRuntime().addShutdownHook(new Thread(consumeOffsets::destroy));
        consumeOffsets.consume();
    }

    class ConsumeOffsets {
        private final AtomicBoolean closed = new AtomicBoolean(false);
        //        private boolean pauseFlag = true;
//        private int count = 0;
        private Map<String, Long> caches = new HashMap<>();
        private Map<String, Long> stopSet = new HashMap<>();
        private final String offsets_topic = "__consumer_offsets";
        private final int numPartition = 50;

        private Properties props;
        private String consumeGroup;
        private String consumeTopic;
//        private int intervalTime;

        private KafkaConsumer<byte[], byte[]> consumer;
        private ByteArrayOutputStream byteArrayOutputStream;
        private PrintStream printStream;
        private FileChannel fileChannel;

        ConsumeOffsets(Properties properties) {
            this.consumeGroup = properties.getProperty("consumeGroup");
            this.consumeTopic = properties.getProperty("consumeTopic");
//            this.intervalTime = Integer.parseInt(properties.getProperty("intervalTime", "60"));
            properties.remove("consumeGroup");
            properties.remove("consumeTopic");
            properties.remove("intervalTime");
            this.props = properties;
        }

        void consume() {
            int partition = Math.abs(this.consumeGroup.hashCode() % this.numPartition);
            String outputFile = this.props.getProperty("outputPath", System.getProperty("user.dir")) + File.separator +
                    this.consumeGroup + "_" + simpleDateFormat.format(new Date());
            this.props.remove("outputPath");
            GroupMetadataManager.OffsetsMessageFormatter offsetsMessageFormatter =
                    new GroupMetadataManager.OffsetsMessageFormatter();
            try {
                this.consumer = new KafkaConsumer<>(this.props);
                this.byteArrayOutputStream = new ByteArrayOutputStream();
                this.printStream = new PrintStream(this.byteArrayOutputStream, true);
                this.fileChannel = FileChannel.open(Paths.get(outputFile), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                TopicPartition topicPartition = new TopicPartition(this.offsets_topic, partition);
                this.consumer.assign(Collections.singletonList(topicPartition));
                while (!closed.get()) {
                    ConsumerRecords<byte[], byte[]> records = this.consumer.poll(60000);
//                    System.out.println("poll=========" + simpleDateFormat.format(new Date()) + "=======" + records.count());
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        offsetsMessageFormatter.writeTo(record, this.printStream);
                        String content = this.byteArrayOutputStream.toString("utf-8");
                        this.byteArrayOutputStream.reset();
                        if (content.contains(this.consumeGroup + "," + this.consumeTopic)) {
                            String[] s1 = content.split("::");
                            String s2 = s1[0].substring(1, s1[0].length() - 1);
                            String[] s3 = s1[1].split(",");
                            long offset = Long.valueOf(s3[0].substring(s3[0].lastIndexOf("[") + 1));
                            Date tmp = new Date(Long.valueOf(s3[2].substring(11)));
                            if (!this.caches.containsKey(s2)) {
                                this.caches.put(s2, offset);
                                this.fileChannel.write(ByteBuffer.wrap(
                                        (s2 + " " + offset + " " + simpleDateFormat.format(tmp) + System.getProperty("line.separator"))
                                                .getBytes("utf-8")));
                            } else {
                                if (offset != this.caches.get(s2)) {
                                    this.caches.put(s2, offset);
                                } else {
                                    if (!this.stopSet.containsKey(s2) || this.stopSet.get(s2) != offset) {
                                        this.stopSet.put(s2, offset);
                                        this.fileChannel.write(ByteBuffer.wrap(
                                                (s2 + " " + this.caches.get(s2) + " " + simpleDateFormat.format(tmp) + System.getProperty("line.separator"))
                                                        .getBytes("utf-8")));
                                    }
//                                    if (this.stopSet.size() == this.caches.size()) { //存在重复消费的情况，会在没有全部消费完的时候size达到partition
//                                        this.destroy();
//                                        System.exit(0);
//                                    }
                                }
                            }
                        }
                    }
//                    if (this.pauseFlag && count == 0) {
//                        System.out.println("pause=========" + simpleDateFormat.format(new Date()) + "=======");
//                        this.consumer.pause(Collections.singletonList(topicPartition));
//                        new Thread(new Timing(this.intervalTime, this)).start();
//                        count++;
//                    } else {
//                        System.out.println("resume=========" + simpleDateFormat.format(new Date()) + "=======");
//                        this.consumer.resume(Collections.singletonList(topicPartition));
//                        this.pauseFlag = true;
//                        this.count = 0;
//                    }
                }
            } catch (WakeupException e) {
                // Ignore exception if closing
                if (!closed.get()) throw e;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.destroy();
            }
        }

        void destroy() {
            try {
//in a separate thread, the consumer can be shutdown by setting the closed flag and waking up the consumer.
                this.closed.set(true);
                this.consumer.wakeup();
//                if (this.consumer != null) {
//                    this.consumer.close();
//                }
                if (this.printStream != null) {
                    this.printStream.flush();
                    this.printStream.close();
                }
                if (this.byteArrayOutputStream != null) {
                    this.byteArrayOutputStream.flush();
                    this.byteArrayOutputStream.close();
                }
                if (this.fileChannel != null) {
                    this.fileChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    class Timing implements Runnable {
//        private int interval;
//        private ConsumeOffsets consumeOffsets;
//
//        Timing(int interval, ConsumeOffsets consumeOffsets) {
//            this.interval = interval;
//            this.consumeOffsets = consumeOffsets;
//        }
//
//        @Override
//        public void run() {
//            if (this.consumeOffsets != null) {
////                this.consumeOffsets.pauseFlag = false;
//                try {
//                    Thread.sleep(this.interval * 1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                this.consumeOffsets.pauseFlag = false;
//            }
//        }
//    }


//    private String millToString() {
//        return simpleDateFormat.format(new Date());
//    }
}
