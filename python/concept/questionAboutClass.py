# 类属性如果被重新赋值，是否会影响类属性的引用
class TestA:
    attr = 1

    def __init__(self):
        self.attr = 42


obj_a = TestA()
obj_b = TestA()
print(TestA.attr, "====", obj_a.attr, '====', obj_b.attr)  # 输出1 ==== 42 ==== 42
obj_a.attr = 36
print(TestA.attr, "====", obj_a.attr, '====', obj_b.attr)  # 输出1 ==== 36 ==== 42
TestA.attr = 2
print(TestA.attr, "====", obj_a.attr, '====', obj_b.attr)  # 输出2 ==== 36 ==== 42
# 结论
'''
类本身拥有自己的类变量（保存在内存），当一个类的对象被构造时，会将当前类变量拷贝一份给这个对象，通过对象来修改类变量，
并不会影响其他对象的类变量的值，因为大家都有各自的副本，更不会影响类本身所拥有的那个类变量的值；
只有类自己才能改变类本身拥有的类变量的值

'''
print(TestA.__dict__)
'''
{'__module__': '__main__', '__weakref__': <attribute '__weakref__' of 'TestA' objects>,
'__dict__': <attribute '__dict__' of 'TestA' objects>,
'__doc__': None, 'attr': 2, '__init__': <function TestA.__init__ at 0x00BF1420>}
'''
print(obj_a.__dict__)  # {'attr': 36}
print(obj_b.__dict__)  # {'attr': 42}
# 从上面的输出可以看出，类TestA和它的实例obj_a,obj_b拥有各自的attr属性
'''
Python 中属性的引用机制是自外而内，当你创建一个实例后，准备引用属性，这时候编译器会先搜索该实例是否拥有该属性，如果有则引用；
如果没有，将搜索这个实例所属的类是否有这个属性，如果有，则引用，没有那就只能报错了。
'''
