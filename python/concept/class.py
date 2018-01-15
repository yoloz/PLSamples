class Employee:
    """所有员工的基类"""  # 类文档字符串
    empCount = 0

    def __init__(self, name, salary):
        self.name = name
        self.salary = salary
        Employee.empCount += 1

    def display_count(self):
        print("Total Employee %d" % Employee.empCount)

    def display_employee(self):
        print("Name : ", self.name, ", Salary: ", self.salary)


"创建 Employee 类的第一个对象"
emp1 = Employee("Zara", 2000)
"创建 Employee 类的第二个对象"
emp2 = Employee("Manni", 5000)
emp1.display_employee()
emp2.display_employee()
print("Total Employee %d" % Employee.empCount)

emp1.age = 7  # 添加一个 'age' 属性
emp1.age = 8  # 修改 'age' 属性
del emp1.age  # 删除 'age' 属性
'''
    getattr(obj, name[, default]) : 访问对象的属性。
    hasattr(obj,name) : 检查是否存在一个属性。
    setattr(obj,name,value) : 设置一个属性。如果属性不存在，会创建一个新属性。
    delattr(obj, name) : 删除属性。
'''
print(hasattr(emp1, 'age'))  # 如果存在 'age' 属性返回 True。
print(setattr(emp1, 'age', 8))  # 添加属性 'age' 值为 8
print(getattr(emp1, 'age'))  # 返回 'age' 属性的值
print(delattr(emp1, 'age'))  # 删除属性 'age'

'''
    __dict__ : 类的属性（包含一个字典，由类的数据属性组成）
    __doc__ :类的文档字符串
    __name__: 类名
    __module__: 类定义所在的模块（类的全名是'__main__.className'，如果类位于一个导入模块mymod中，那么className.__module__ 等于 mymod）
    __bases__ : 类的所有父类构成元素（包含了一个由所有父类组成的元组）
'''
print("Employee.__doc__:", Employee.__doc__)
print("Employee.__name__:", Employee.__name__)
print("Employee.__module__:", Employee.__module__)
print("Employee.__bases__:", Employee.__bases__)
print("Employee.__dict__:", Employee.__dict__)

# 垃圾回收
'''
同Java语言一样，Python使用了引用计数这一简单技术来追踪内存中的对象。
在Python内部记录着所有使用中的对象各有多少引用。
一个内部跟踪变量，称为一个引用计数器。
当对象被创建时， 就创建了一个引用计数， 当这个对象不再需要时， 也就是说， 这个对象的引用计数变为0 时， 它被垃圾回收。
但是回收不是"立即"的， 由解释器在适当的时机，将垃圾对象占用的内存空间回收。
a = 40      # 创建对象  <40>
b = a       # 增加引用， <40> 的计数
c = [b]     # 增加引用.  <40> 的计数

del a       # 减少引用 <40> 的计数
b = 100     # 减少引用 <40> 的计数
c[0] = -1   # 减少引用 <40> 的计数
垃圾回收机制不仅针对引用计数为0的对象，同样也可以处理循环引用的情况。
循环引用指的是，两个对象相互引用，但是没有其他变量引用他们。这种情况下，仅使用引用计数是不够的。
Python 的垃圾收集器实际上是一个引用计数器和一个循环垃圾收集器。
作为引用计数的补充， 垃圾收集器也会留心被分配的总量很大（及未通过引用计数销毁的那些）的对象。
在这种情况下， 解释器会暂停下来， 试图清理所有未引用的循环。
'''


# 析构函数 __del__ ，__del__在对象销毁的时候被调用，当对象不再被使用时，__del__方法运行：
class Point:
    def __init__(self, x=0, y=0):
        self.x = x
        self.y = y

    def __del__(self):
        class_name = self.__class__.__name__
        print(class_name, "销毁")


pt1 = Point()
pt2 = pt1
pt3 = pt1
print(id(pt1), id(pt2), id(pt3))  # 打印对象的id
del pt1
del pt2
del pt3

# 注意：通常你需要在单独的文件中定义一个类

'''
在python中继承中的一些特点：

    1：在继承中基类的构造（__init__()方法）不会被自动调用，它需要在其派生类的构造中亲自专门调用。
    2：在调用基类的方法时，需要加上基类的类名前缀，且需要带上self参数变量。区别于在类中调用普通函数时并不需要带上self参数
    3：Python总是首先查找对应类型的方法，如果它不能在派生类中找到对应的方法，它才开始到基类中逐个查找。（先在本类中查找调用的方法，找不到才去基类中找）。

如果在继承元组中列了一个以上的类，那么它就被称作"多重继承" 。

语法：

派生类的声明，与他们的父类类似，继承的基类列表跟在类名之后，如下所示：

class SubClassName (ParentClass1[, ParentClass2, ...]):
   'Optional class documentation string'         # 类文档字符串
   class_suite                                   # 类体
'''


class Parent:  # 定义父类
    parentAttr = 100

    def __init__(self):
        print("调用父类构造函数")

    def parent_method(self):
        print('调用父类方法 parent method')

    def set_attr(self, attr):
        Parent.parentAttr = attr

    def get_attr(self):
        print("父类属性 :", Parent.parentAttr)


class Child(Parent):  # 定义子类
    def __init__(self):
        Parent.__init__(self)  # 调用基类的构造函数
        print("调用子类构造方法")
        Parent.__init__(self)  # 调用基类的构造函数

    def child_method(self):
        print('调用子类方法 child method')

    def parent_method(self):
        print('调用子类方法 parent method')
        Parent.parent_method(self)  # 调用基类的函数


c = Child()  # 实例化子类
c.child_method()  # 调用子类的方法
c.parent_method()  # 调用子类方法
c.set_attr(200)  # 再次调用父类的方法
c.get_attr()  # 再次调用父类的方法

'''
类属性与方法
类的私有属性

__private_attrs：两个下划线开头，声明该属性为私有，不能在类地外部被使用或直接访问。在类内部的方法中使用时 self.__private_attrs。

类的方法
在类地内部，使用def关键字可以为类定义一个方法，与一般函数定义不同，类方法必须包含参数self,且为第一个参数

类的私有方法
__private_method：两个下划线开头，声明该方法为私有方法，不能在类地外部调用。在类的内部调用 self.__private_methods

'''


class JustCounter:
    __secretCount = 0  # 私有变量
    publicCount = 0  # 公开变量

    def count(self):
        self.__secretCount += 1
        self.publicCount += 1
        print(self.__secretCount)


counter = JustCounter()
counter.count()
counter.count()
print(counter.publicCount)
'''
print(counter.__secretCount)  # 报错，实例不能访问私有变量

Python不允许实例化的类访问私有数据，
但你可以使用 object._className__attrName 访问属性，
将如下代码替换以上代码的最后一行代码：
'''
print(counter._JustCounter__secretCount)
