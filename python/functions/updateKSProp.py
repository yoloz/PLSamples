# 读取修改properties文件
# 辅助KStream的配置文件修改
import sys
import os


class Properties:
    def __init__(self, file_name):
        self.file_name = file_name
        self.map = {}
        self.contents = []
        try:
            f_open = open(self.file_name, 'rt', encoding='utf-8')
            for line in f_open:
                line = line.strip()
                self.contents.append(line + '\n')
                if line.find('=') > 0 and not line.startswith('#'):
                    arr = line.split('=')
                    self.map[arr[0].strip()] = arr[1].strip()
        except Exception as e:
            raise e
        else:
            f_open.close()

    def __write(self, contents):
        try:
            tmp_file = open(self.file_name + '.swp', 'wt', encoding='utf-8')
            tmp_file.writelines(contents)
        except Exception as e:
            raise e
        else:
            tmp_file.close()
        # if os.path.exists(file_name):
        # os.remove(file_name)
        os.rename(self.file_name + '.swp', self.file_name)
        if os.path.exists(self.file_name + '.swp'):
            print("tmp_file exist")

    def contains(self, key):
        return key in self.map

    def get(self, key, default_value=''):
        if key in self.map:
            return self.map[key]
        return default_value

    def update(self, key, value):
        self.map[key] = value

    def save(self):
        for key in self.map:
            for index in range(0, len(self.contents)):
                if self.contents[index].find(key) == 0:
                    self.contents.remove(self.contents[index])
                    self.contents.insert(index, key + '=' + self.get(key) + '\n')
        # self.__write([line + '\n' for line in self.contents]) 处理的每行添加换行符
        self.__write(self.contents)


def __load(file_name):
    return Properties(file_name)


def __update(directory='', kafka='', zookeeper='', outputs=None):
    if outputs is None:
        outputs = []
    if len(directory) > 0 and len(kafka) > 0 and len(outputs) >= 2:
        if os.path.isdir(directory):
            main_prop = __load(directory + os.sep + 'main.properties')
            main_prop.update('bootstrap.servers', kafka)
            if len(zookeeper) > 0:
                main_prop.update('ks.zookeeper.url', zookeeper)
            main_prop.save()
            output_prop = __load(directory + os.sep + 'output.properties')
            output_prop.update('output.targets', outputs[0])
            if outputs[0] == 'kafka':
                if len(outputs) == 2:
                    output_prop.update('output.target.kafka.topic', outputs[1])
                elif len(outputs) == 3:
                    output_prop.update('output.target.kafka.address', outputs[1])
                    output_prop.update('output.target.kafka.topic', outputs[2])
                else:
                    raise Exception('output_args error!')
            else:
                output_prop.update('output.target.zbus.address', outputs[1])
                output_prop.update('output.target.zbus.mq', outputs[2])
            output_prop.save()
            print('KS config update success!')
        else:
            raise Exception('path "' + directory + '" does not exist or not directory!')
    else:
        raise Exception('args length error!')


if __name__ == '__main__':
    output_types = ['kafka', 'zbus']
    zookeeperArg = ''
    try:
        # sys.argv[0] is this file
        if len(sys.argv) > 1:
            conf_dir = sys.argv[1]
            kafkaArg = sys.argv[2]
            if len(sys.argv) == 5:
                outputArgs = sys.argv[3:]
            elif len(sys.argv) == 6:
                if str(sys.argv[3]).find(':') > 0:
                    zookeeperArg = sys.argv[3]
                    outputArgs = sys.argv[4:]
                else:
                    outputArgs = sys.argv[3:]
            elif len(sys.argv) == 7:
                zookeeperArg = sys.argv[3]
                outputArgs = sys.argv[4:]
            else:
                raise Exception('args length error!')
        else:
            print('参数格式:目录路径 kafka地址 [zookeeper地址] 输出类型 输出地址[kafka可以不写] 输出目的\n'
                  'path '
                  'ip:port '
                  '[ip:port(/path)] '
                  'type(kafka,zbus) [ip:port] topic(mq)')
            conf_dir = input('abspath:\n')
            kafkaArg = input('kafka_arg:\n')
            zookeeperArg = input('zookeeper_arg:\n')
            outputArgs = input('output_args:\n').split(' ')
        if outputArgs[0] not in output_types:
            raise Exception('output type not in[kafka,zbus]')
        __update(conf_dir, kafkaArg, zookeeperArg, outputArgs)
    except KeyboardInterrupt as e:
        # print('key board interrupt!')
        pass
