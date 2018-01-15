# 等额本金和等额本息贷款计算


import math

area = float(input('请输入房屋面积(m^2): '))
unit_price = float(input('请输入单价(元): '))
year_interest = float(input('请输入贷款利率(%): '))

# '''
# 等额本金的每月还款情况文件，格式如下：
# 1月,5700.79(元)
# 2月,5691.69(元)
# 3月,5682.59(元)
# 4月,5673.5(元)
# ......
#
# '''
#
#
# # 读取文件内容，即每个月的还款数据
# def get_average_capital():
#     file_path = input('请输入等额本金文件路径: ')
#     average_capital_data = []
#     file_object = open(file_path, 'r')
#     try:
#         for line in file_object:
#             average_capital_data.append(float(line[line.find(',') + 1:line.find('(')]))
#     finally:
#         file_object.close()
#     print(average_capital_data)
#     return average_capital_data


'''
等额本金还款法:
设：A=贷款额 B=贷款月利率 C=贷款月数 D=每月还款额 E=还款利息总和
每月应还本金=贷款额÷还款月数=A÷C
第一月还款利息=剩余本金×月利率=A×B
第二月还款利息:(贷款额-第一月已归还本金额)×月利率=(A-A÷C)×B
第三月还款利息:(A-2×A÷C)×B
...
第C月还款利息:(A-(C-1)×A÷C)×B
求以上和得E=B(AC-[A+2A+3A+...+(C-1)A]÷C)=B(AC-A(C-1)÷2)=A×B×(C+1)÷2
每n月还款:D=本金+第n月利息=A÷C+(A-(n-1)×A÷C)×B=(1+B(C-n+1))×A÷C
'''


def get_average_capital():
    average_capital_data = []
    for i in range(0, 360):
        average_capital_data.append((loan / 360) + ((loan - loan * i / 360) * (year_interest / (12 * 100))))
    return average_capital_data


# 等额本金计算，默认30年
def prepayment_average_capital(year=30):
    arr_float = get_average_capital()
    count = 0
    pay = 0
    # pay = loan - (year / 30) * loan
    for i in arr_float:
        pay += i
        count += 1
        if count == year * 12:
            break
    return pay


'''
等额本息还款法：
设：A=贷款额 B=贷款月利率 C=贷款月数 D=每月还款额 E=还款利息总和
第一月还款利息为【贷款额×月利率】：A×B
第二月还款利息为【[贷款额-第一个月已还本金]×月利率】：〔A－（D－A×B）〕×B＝（A×B－D）×（1＋B）^1＋D
第三月还款利息为：｛A－（D－A×B）－〔D－（A×B－D）×（1＋B）^1－D〕｝×B＝（A×B－D）×（1＋B）^2＋D
第四月还款利息为：＝（A×B－D）×（1＋B）^3＋D
.....
第C月还款利息为：＝（A×B－D）×（1＋B）^（C－1）＋D
求以上和为：E＝（A×B－D）[（1＋B）^0+（1＋B）^1+...+（1＋B）^（C－1）]+C×D=（A×B－D）×〔（1＋B）^C－1〕÷B＋C×D
而利息总和E＝C×D－A，两项E值相等求得
月均还款:D＝A×B×（1＋B）^C÷〔（1＋B）^C－1〕
支付利息总和:E＝C×D-A=C×A×B×（1＋B）^C÷〔（1＋B）^C－1〕－A
还款总额:E+A=C×A×B×（1＋B）^C÷〔（1＋B）^C－1〕

附注:a^b表示a的b次方。
'''


# 等额本息计算，默认30年

def prepayment_average_capital_plus_interest(year=30):
    # pay = loan - (year / 30) * loan
    pay = 0
    month_interest = year_interest / (12 * 100)
    month_pay = (loan * month_interest * math.pow((1 + month_interest), 360)) / (
        math.pow((1 + month_interest), 360) - 1)
    for i in range(1, year * 12 + 1):
        pay += month_pay
    return pay


total = unit_price * area
loan = int((total - total * 0.3) / 10000) * 10000
first_pay = total - loan

print('房屋总价:', total, '贷款额:', loan, '首付款:', first_pay)

print("===============等额本金已还款总额(本金+利息)================")
for j in range(1, 31):
    # print(first_pay + prepayment_average_capital(j), first_pay + prepayment_average_capital_plus_interest(j), total)
    print(first_pay + prepayment_average_capital(j))

print("===============等额本息已还款总额(本金+利息)=================")
for k in range(1, 31):
    print(first_pay + prepayment_average_capital_plus_interest(k))

print("===============等额本息已还款总额-等额本金已还款总额======")
for m in range(1, 31):
    print(prepayment_average_capital_plus_interest(m) - prepayment_average_capital(m))

print("===============一次性还清剩余贷款本金======")
for n in range(1, 31):
    print(loan - (n / 30) * loan)

print("===============总房价=================")
for l in range(1, 31):
    print(total)

print("===============等额本金总还款额======")
for o in range(1, 31):
    t1 = first_pay + prepayment_average_capital(o)
    t2 = loan - (o / 30) * loan
    print(t1 + t2)

print("===============等额本息总还款额======")
for p in range(1, 31):
    t1 = first_pay + prepayment_average_capital_plus_interest(p)
    t2 = loan - (p / 30) * loan
    print(t1 + t2)
# # 输入年份计算等额本金已经付给银行的钱，默认15年(提前还)
# prepayment_average_capital_data = prepayment_average_capital()
# # 输入年份计算等额本息已经付给银行的钱，默认15年(提前还)
# prepayment_average_capital_plus_interest_data = prepayment_average_capital_plus_interest()
#
# print('等额本金总耗资：', first_pay + prepayment_average_capital_data)
# print('等额本息总耗资：', first_pay + prepayment_average_capital_plus_interest_data)
# print('少还利息：', prepayment_average_capital_plus_interest_data - prepayment_average_capital_data)
