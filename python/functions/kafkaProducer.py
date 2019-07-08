#coding=utf8
import os
import time
import re
import random
import base64
#模拟生产者发送kafka信息
from pykafka import KafkaClient
#定义一个kafka发送端
def send_message(host):
	#ip为对应的logstash虚拟机的IP
	client = KafkaClient(hosts=host+":9092",broker_version="1.0.1")
	topic = client.topics["dac-event-log".encode()]
	producer = topic.get_producer()
	messages='{"event_type_id":152549100,"src_ip":"192.168.14.122","dst_ip":"192.168.14.123","src_mac":"000002EC0307","dst_mac":"000000140286","src_port":59785,"dst_port":53,"event_count":1,"start_time":1559722826,"security_id":5,"security_type":"木马后门","event_level":30,"event_level_name":"中危","popular":2,"danger_value":2,"key_device":4,"key_service":1,"attack_id":7003,"attack_type":"有害程序","protocol":"DNS","subject":"DNS_木马_暗云III连接","message":"bmljPTMyMTtkbnNuYW1lPW1zLm1haW1haTY2Ni5jb207","dev_ip":"192.168.2.7","dev_type":"IDS"}'
	for t in range(1):
		message=messages
		#修改时间为当前时间
		time1='\"start_time\":'+str(int(time.time()))
		a1=re.sub('\"start_time\":\d{10}',time1,message)
		if "DNS" in a1:
			response_domain='venssssssssuseye.com'
			b1='nic=0;请求域名='+response_domain+';'
			d=base64.b64encode(b1.encode("gbk"))
			message='"message":"'+d.decode("gbk")+'"'
			a1=re.sub('"message":".*?"',message,a1)
		#修改IP为威胁情报库IP
		#src='"src_ip":"'+random.choice(attack_ip)+'"'
		#修改srcIP
		#a1=re.sub('"src_ip":".*?"',src,a1)
		print(a1)
		time.sleep(0.3)
		producer.produce(bytes(a1,encoding="utf8"))
#启动发送message操作
#while True:
send_message("127.0.0.1")
