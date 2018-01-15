# 定义函数及参数使用
def trapezoid_area(base_up, base_down, height=3):
    return 1 / 2 * (base_up + base_down) * height


print(trapezoid_area(1, 2))

print(trapezoid_area(base_up=1, base_down=2))

print(trapezoid_area(1, 2, height=15))


# 写文件
def text_create(name, msg):
    path = 'E:\\'
    file_path = path + name + '.txt'
    file = open(file_path, 'w')
    file.write(msg)
    file.close()
    print('Done')


text_create('hello', 'hello world')


# 逻辑判断
def account_login():
    password = input('Password:')
    if password == '12345':
        print('Login success!')
    else:
        print('Wrong password or invalid input!')
        account_login()


account_login()

# 字符循环
for s in 'Hello world':
    print(s)

# 9*9乘法表
for i in range(1, 10):
    for j in range(1, 10):
        print('{} X {} = {}'.format(i, j, i * j))

# 列表
Weekday = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']
print(Weekday[0])
all_in_list = [1, 1.0, 'a word', 'print(1)', True, [1, 2], (1, 2), {'key': 'value'}]  # 最后两项是元组和字典类型
fruit = ['pineapple', 'pear']
# 插入操作
fruit.insert(1, 'grade')
print(fruit)
fruit[0:0] = ['Orange']
print(fruit)
fruit.append('last')  # 尾部添加一个新的元素
# 删除操作
fruit.remove('grade')
print(fruit)
del fruit[0:2]
print(fruit)
# 添加多个
fruit.extend(['1', '2'])
print(fruit)

# 字典
NASDAQ_code = {'BIDU': 'Baidu', 'SINA': 'Sina', 'YOKU': 'Youku'}
# 添加
NASDAQ_code['YOKU'] = 'Youku'
print(NASDAQ_code)
# 添加多个元素
NASDAQ_code.update({'FB': 'Facebook', 'TSLA': 'Tesla'})
# 删除
del NASDAQ_code['FB']
# 字典不可被切片
# NASDAQ_code[1:2] is wrong

# 元组，元组不可修改
letters = ('a', 1, 'b')
print(letters[0])

# 集合，每一个集合中的元素是无序的，不重复的任意对象，集合不能被切片也不能被索引
a_set = {'a', 1, 2}
a_set.add(3)
a_set.discard(3)
"""
       Remove an element from a set if it is a member.
       If the element is not a member, do nothing.
"""
# 列表操作
num_list = [6, 2, 7, 4, 1, 3, 5]
print(sorted(num_list))
print(sorted(num_list, reverse=True))
str_list = ['a', 'b', 'c', 'd']
for a, b in zip(num_list, str_list):
    print(b, 'is', a)
