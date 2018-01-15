#!/usr/bin/python
# -*- coding: UTF-8 -*-


import os
import platform
import socket
import string
import struct
import sys
import time
import random

import datetime

# 依照source格式生成随机数据
source = "begin_time='2016-11-30 13:36:34' end_time='2016-11-30 13:36:36' alive_time=2 src_mac=265F10E90700 " \
         "dst_mac=6BB5030F0900 src_ip=36.16.60.8 dst_ip=192.168.6.200 src_port=3575 dst_port=80 trans_protocol_id=6 " \
         "service_id=16 list_type=2 up_byte_count=6192 down_byte_count=960 byte_count=7152 up_packet_count=7 " \
         "down_packet_count=2 packet_count=9 src_country_id=1 dst_country_id=0 src_province_id=13 dst_province_id=0 " \
         "src_domain='root,境内' dst_domain='root,未分组' "


# 从2000年到现在随机获取两个时间值
class RandomTime:
    __st = "2000-01-01 00:00:00"
    __et = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time()))

    @staticmethod
    def __str2seconds(str_):
        d = datetime.datetime.strptime(str_, "%Y-%m-%d %H:%M:%S")
        return time.mktime(d.timetuple())

    @staticmethod
    def __seconds2str(sec_):
        return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(int(sec_)))

    def get_time(self):
        sts = self.__str2seconds(self.__st)
        ets = self.__str2seconds(self.__et)
        rt = random.sample(range(int(sts), int(ets)), 2)
        rt.sort()
        return [self.__seconds2str(rt[0]), self.__seconds2str(rt[1])]


arr_data = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F']

lower_str = []
for s in string.ascii_lowercase:
    lower_str.append(s)

arr_address = ['境内', '未分组']

# 示例数据的key值
tuple_key = (source[0:source[0:32].find('=') + 1], source[33:source[33:63].find('=') + 34])
arr_source = []
for s in source[64:-1].split(' '):
    arr_source.append(s[0:s.find('=') + 1])
tuple_key += tuple(arr_source)

total = int(input('输入产生样例数据总数量(整数): '))


# 获取脚本文件的当前路径
def cur_file_dir():
    # 获取脚本路径
    path = sys.path[0]
    # 判断为脚本文件还是py2exe编译后的文件，如果是脚本文件，则返回的是脚本的目录，如果是py2exe编译后的文件，则返回的是编译后的文件路径
    if os.path.isdir(path):
        return path
    elif os.path.isfile(path):
        return os.path.dirname(path)


def one_message(arr_t):
    message = ''
    message += tuple_key[0]  # begin_time
    message += "'" + arr_t[0] + "'" + ' '
    message += tuple_key[1]  # end_time
    message += "'" + arr_t[1] + "'" + ' '
    message += tuple_key[2]  # alive_time
    message += str(random.choice(range(1, 10))) + ' '
    message += tuple_key[3]  # src_mac
    message += ''.join(random.sample(arr_data, 12)) + ' '
    message += tuple_key[4]  # dst_mac
    message += ''.join(random.sample(arr_data, 12)) + ' '
    message += tuple_key[5]  # src_ip
    message += socket.inet_ntoa(struct.pack('>I', random.randint(1, 0xffffffff))) + ' '
    message += tuple_key[6]  # dst_ip
    message += socket.inet_ntoa(struct.pack('>I', random.randint(1, 0xffffffff))) + ' '
    message += tuple_key[7]  # src_port
    message += str(random.choice(range(1024, 65535))) + ' '
    message += tuple_key[8]  # dst_port
    message += str(random.choice(range(1024, 65535))) + ' '
    message += tuple_key[9]  # trans_protocol_id
    message += str(random.choice(range(1, 10))) + ' '
    message += tuple_key[10]  # service_id
    message += str(random.choice(range(1, 100000000))) + ' '
    message += tuple_key[11]  # list_type
    message += str(random.choice(range(1, 10000))) + ' '
    message += tuple_key[12]  # up_byte_count
    up_byte_count = random.choice(range(100, 1024 * 1024 * 6))
    message += str(up_byte_count) + ' '
    message += tuple_key[13]  # down_byte_count
    down_byte_count = random.choice(range(100, 1024 * 1024 * 6))
    message += str(down_byte_count) + ' '
    message += tuple_key[14]  # byte_count
    message += str(up_byte_count + down_byte_count) + ' '
    message += tuple_key[15]  # up_packet_count
    up_packet_count = random.choice(range(1, 2000))
    message += str(up_packet_count) + ' '
    message += tuple_key[16]  # down_packet_count
    down_packet_count = random.choice(range(1, 2000))
    message += str(down_packet_count) + ' '
    message += tuple_key[17]  # packet_count
    message += str(up_packet_count + down_packet_count) + ' '
    message += tuple_key[18]  # src_country_id
    message += str(random.choice(range(0, 224))) + ' '
    message += tuple_key[19]  # dst_country_id
    message += str(random.choice(range(0, 224))) + ' '
    message += tuple_key[20]  # src_province_id
    message += str(random.choice(range(0, 100))) + ' '
    message += tuple_key[21]  # dst_province_id
    message += str(random.choice(range(0, 100))) + ' '
    message += tuple_key[22]  # src_domain
    message += "'" + (''.join(random.sample(lower_str, 4)) + ',' + random.choice(arr_address)) + "'" + ' '
    message += tuple_key[23]  # dst_domain
    message += "'" + (''.join(random.sample(lower_str, 4)) + ',' + random.choice(arr_address)) + "'"
    if platform.system() == "Windows":
        message += '\r\n'
    elif platform.system() == "Linux":
        message += '\n'
    else:
        message += '\r'
    return message


output_file = cur_file_dir() + os.path.sep + 'sampledata'
print('数据文件：' + output_file)
print("开始............" + time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time())))
target_file = open(output_file, 'a', -1)
try:
    if total > 0:
        rand_time = RandomTime()
        for i in range(0, total):
            target_file.write(one_message(rand_time.get_time()))
            # if i % 1000 == 0:
            #     print("flush.....1000...." + time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time())))
            #     target_file.flush()
    else:
        print("数据量需为大于零的整数值............")
        exit(-1)
finally:
    target_file.close()
print("结束............" + time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time())))
