# encoding=utf8
from .zbus3 import SingleBroker, Consumer

broker = SingleBroker(host='10.68.23.11', port=15555)

consumer = Consumer(broker=broker, mq='MyMQ')
while True:
    msg = consumer.recv()
    if msg is None:
        continue
    print(msg)
