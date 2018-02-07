# python updateKSProp.py 配置文件路径 kafka地址 [zookeeper地址] 输出类型(kafka,zbus) 输出地址(kafka可以不写) 输出主题(队列)
python3 updateKSProp.py
#python3 updateKSProp.py /home/kstreamTest/test 10.68.23.11:9092 10.68.23.11:2181 kafka test2
#python3 updateKSProp.py /home/kstreamTest/test 10.68.23.11:9092  kafka test1
#python3 updateKSProp.py /home/kstreamTest/test 10.68.23.11:9092  kafka 10.68.23.11:9092 test2
#python3 updateKSProp.py /home/kstreamTest/test 10.68.23.11:9092  zbus 10.68.23.11:15555 ids_realtime1
