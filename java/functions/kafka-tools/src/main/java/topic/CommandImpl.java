package topic;

import kafka.admin.TopicCommand;
import kafka.tools.ConsoleConsumer;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.security.JaasUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Created on 17-2-7.
 */
public class CommandImpl {

    private static void initZkSecurity(Properties proper) {
        if (proper.containsKey(JaasUtils.ZK_SASL_CLIENT))
            System.setProperty(JaasUtils.ZK_SASL_CLIENT, proper.getProperty(JaasUtils.ZK_SASL_CLIENT));
        if (proper.containsKey(JaasUtils.ZK_LOGIN_CONTEXT_NAME_KEY))
            System.setProperty(JaasUtils.ZK_LOGIN_CONTEXT_NAME_KEY, proper.getProperty(JaasUtils.ZK_LOGIN_CONTEXT_NAME_KEY));
    }

    private static void performKafSecurity(Properties properties) {
        if (properties.getProperty(Command.security_protocol).contains("SASL")) {
            System.setProperty("java.security.krb5.conf", properties.getProperty("java.security.krb5.conf"));
            System.setProperty("java.security.auth.login.config", properties.getProperty("java.security.auth.login.config"));
        }
    }

    static void testConnect(Properties proper) throws Exception {
        if (!proper.containsKey("topic")) throw new Exception("topic not configured......");
        if (!proper.containsKey("bootstrap.servers")) throw new Exception("bootstrap.servers not configured......");
        KafkaProducer<String, String> producer = null;
        try {
            if (proper.getProperty("topic").equals(getTopic(proper))) {
                System.out.println("topic " + proper.getProperty("topic") + " is exist...");
            } else {
                createTopic(proper);
            }
            performKafSecurity(proper);
            proper.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            proper.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producer = new KafkaProducer<>(proper);
            producer.send(new ProducerRecord<>(proper.getProperty("topic"), "test available...")).get();
            String[] args = new String[]{"--bootstrap-server", proper.getProperty("bootstrap.servers"), "--topic",
                    proper.getProperty("topic"), "--max-messages", "1", "--from-beginning"};
            ConsoleConsumer.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (producer != null) producer.close();
//            deleteTopic(proper);
        }
        System.out.println("Kafka is ok...");
    }

    /**
     * 创建主题
     * "kafka-topics.sh --config retention.ms=86400000 --create --partitions 2 --replication-factor 2 --topic test --zookeeper node11,node12,node14/kafka"
     *
     * @param proper 参数
     * @throws Exception error
     */
    private static void createTopic(Properties proper) throws Exception {
        if (!proper.containsKey("zookeeper")) throw new Exception("zookeeper not configured......");
        if (!proper.containsKey("topic")) throw new Exception("topic not configured......");
        initZkSecurity(proper);
        int session_timeout = Integer.parseInt(proper.getProperty("zk_session_timeout", "30000"));
        int connect_timeout = Integer.parseInt(proper.getProperty("zk_connection_timeout", "30000"));
        List<String> argList = new ArrayList<>();
        argList.add("--create");
        String zkConnect = proper.getProperty("zookeeper");
        argList.add("--partitions");
        argList.add(proper.getProperty("partitions", "1"));
        argList.add("--replication-factor");
        argList.add(proper.getProperty("replication-factor", "1"));
        argList.add("--topic");
        argList.add(proper.getProperty("topic"));
        Set<String> keys = proper.stringPropertyNames();
        keys.stream().filter(key -> key.contains("config-")).forEach(key -> {
            String value = proper.getProperty(key);
            key = key.substring(key.indexOf("config-") + 7);
            argList.add("--config");
            argList.add(key + "=" + value);
        });
        String[] args = argList.toArray(new String[argList.size()]);
        System.out.println(Arrays.toString(args));
        ZkUtils zkUtils = null;
        try {
            zkUtils = ZkUtils.apply(zkConnect, session_timeout, connect_timeout, JaasUtils.isZkSecurityEnabled());
            TopicCommand.TopicCommandOptions options = new TopicCommand.TopicCommandOptions(args);
            TopicCommand.createTopic(zkUtils, options);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zkUtils != null) zkUtils.close();
        }
    }

    private static String getTopic(Properties proper) throws Exception {
        if (!proper.containsKey("zookeeper")) throw new Exception("zookeeper not configured......");
        PrintStream console = System.out;
        String result = "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bo);
        System.setOut(ps);
        initZkSecurity(proper);
        int session_timeout = Integer.parseInt(proper.getProperty("zk_session_timeout", "30000"));
        int connect_timeout = Integer.parseInt(proper.getProperty("zk_connection_timeout", "30000"));
        String zkConnect = proper.getProperty("zookeeper");
        String[] args = new String[]{"--topic", proper.getProperty("topic")};
        ZkUtils zkUtils = null;
        try {
            zkUtils = ZkUtils.apply(zkConnect, session_timeout, connect_timeout, JaasUtils.isZkSecurityEnabled());
            TopicCommand.TopicCommandOptions options = new TopicCommand.TopicCommandOptions(args);
            TopicCommand.listTopics(zkUtils, options);
            result = bo.toString().replaceAll("\r|\n", "");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zkUtils != null) zkUtils.close();
            ps.close();
        }
        System.setOut(console);
        return result;
    }

    static void deleteTopic(Properties proper) throws Exception {
        if (!proper.containsKey("zookeeper")) throw new Exception("zookeeper not configured......");
        if (!proper.containsKey("topic")) throw new Exception("topic not configured......");
        initZkSecurity(proper);
        int session_timeout = Integer.parseInt(proper.getProperty("zk_session_timeout", "30000"));
        int connect_timeout = Integer.parseInt(proper.getProperty("zk_connection_timeout", "30000"));
        String zkConnect = proper.getProperty("zookeeper");
        String[] args = new String[]{"--topic", proper.getProperty("topic")};
        ZkUtils zkUtils = null;
        try {
            zkUtils = ZkUtils.apply(zkConnect, session_timeout, connect_timeout, JaasUtils.isZkSecurityEnabled());
            TopicCommand.TopicCommandOptions options = new TopicCommand.TopicCommandOptions(args);
            TopicCommand.deleteTopic(zkUtils, options);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zkUtils != null) zkUtils.close();
        }
    }

    static void performance(Properties proper) throws Exception {
        if (!proper.containsKey("topic")) throw new Exception("topic not configured......");
//        String result = "";
//        PrintStream console = System.out;
//        ByteArrayOutputStream bo = new ByteArrayOutputStream();
//        try (PrintStream ps = new PrintStream(bo)) {
        if ("producer".equals(proper.getProperty("type"))) {
            if (proper.getProperty("topic").equals(getTopic(proper))) {
                System.out.println("topic " + proper.getProperty("topic") + " is exist...");
            } else {
                createTopic(proper);
            }
//                System.setOut(ps);
            List<String> argsList = new ArrayList<>();
            argsList.add("--topic");
            argsList.add(proper.getProperty("topic"));
            argsList.add("--num-records");
            argsList.add(proper.getProperty("num-records", "100000"));
            argsList.add("--record-size");
            argsList.add(proper.getProperty("record-size", "1024"));
            argsList.add("--throughput");
            argsList.add(proper.getProperty("throughput", "2"));
            argsList.add("--producer-props");
            Set<String> keys = proper.stringPropertyNames();
            keys.stream().filter(key -> key.contains("props-")).forEach(key -> {
                String value = proper.getProperty(key);
                key = key.substring(key.indexOf("props-") + 6);
                argsList.add(key + "=" + value);
            });
            String[] args = argsList.toArray(new String[argsList.size()]);
            org.apache.kafka.tools.ProducerPerformance.main(args);
//                String[] results = bo.toString().split(",");
//                result = results[0] + " " + results[1];
        }
//        }
//        System.setOut(console);
//        return result;
    }

    public static void main(String[] args) throws Exception {
//        Properties properties = new Properties();
//        properties.load(new FileInputStream("E:\\projects\\GitHub\\BigData\\Kafka\\src\\com\\unimas\\kafka\\Template.properties"));
//        deleteTopic(properties);
//        System.out.println(testConnect(properties));
//        System.out.println(performance(properties));
    }


}
