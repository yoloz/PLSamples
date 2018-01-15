#!/usr/bin/python
# -*- coding: UTF-8 -*-


# 通过logstash的endpoint api(http)获取plugin的输出信息

from urllib import request
from urllib import error
import time

port = int(input("输入端口号:"))

'''
{"host":"node14","version":"5.3.0","http_address":"127.0.0.1:9600","id":"c0453696-9e73-41a4-ab1f-d023aed55305","name":"node14",
"pipeline":{"events":{"duration_in_millis":0,"in":0,"filtered":0,"out":0},
"plugins":{"inputs":[],"filters":[],"outputs":[{"id":"cfe486ff44842fc5bd38ae4c77210f8fe527af0a-2","name":"stdout"}]},
"reloads":{"last_error":null,"successes":0,"last_success_timestamp":null,"last_failure_timestamp":null,"failures":0},
"queue":{"type":"memory"},"id":"main"}}
{"host":"node14","version":"5.3.0","http_address":"127.0.0.1:9600","id":"c0453696-9e73-41a4-ab1f-d023aed55305","name":"node14",
"pipeline":{"events":{"duration_in_millis":237,"in":2,"filtered":2,"out":2},
"plugins":{"inputs":[],"filters":[],"outputs":[{"id":"bde6dd17d70023b1bdb8dde5673ee2554ec5b629-2","events":{"in":2},"name":"kafka"}]},
"reloads":{"last_error":null,"successes":0,"last_success_timestamp":null,"last_failure_timestamp":null,"failures":0},
"queue":{"type":"memory"},"id":"main"}}
'''
'''
"outputs":[{"id":"cfe486ff44842fc5bd38ae4c77210f8fe527af0a-2","name":"stdout"}
"outputs":[{"id":"6629819fd9180c62a53c297889d5f4f123b3124a-2","events":{"in":437},"name":"kafka"}]
"outputs":[{"id":"caf4e277e5f70e6e2827bc10a41018769bb84ae9-2","events":{"duration_in_millis":453192,"in":936029,"out":935529},"name":"stdout"}]
"outputs":[{"id":"6629819fd9180c62a53c297889d5f4f123b3124a-2","events":{"duration_in_millis":14751,"in":113237,"out":113112},"name":"kafka"}]
'''


def get_request():
    param = '/_node/stats/pipeline'
    url = 'http://127.0.0.1:' + str(port)
    full_url = url + param
    data = request.urlopen(full_url).read().decode('UTF-8')
    output_index = data.find('outputs')
    output = data[output_index - 1:data.find(']', output_index) + 1]
    out_event_index = output.find('out', 10)
    out_event = output[out_event_index + len('out:') + 1:output.find('}', out_event_index)]
    time_mill_index = output.find('duration_in_millis')
    time_mill = output[time_mill_index + len('duration_in_millis:') + 1:output.find(',', time_mill_index)]
    print(int(out_event) * 1000 / int(time_mill))


def re_exe(inc=10):
    while True:
        try:
            get_request()
            time.sleep(inc)
        # except ConnectionRefusedError as msg:
        #     print(msg)
        #     break
        except ConnectionRefusedError:  # 连接关闭
            # break
            pass
        except error.URLError:  # 连接关闭
            # break
            pass
        except ValueError:  # outputs内容是变化的，见文中注释
            pass


re_exe()
