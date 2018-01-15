# 提前还贷计算


import math

total_month = 360  # 还款期限
ten_thousand = 10, 000  # 每年提前还款一万
twenty_thousand = 20, 000  # 每年提前还款二万
thirty_thousand = 30, 000  # 每年提前还款三万
area = float(input('请输入房屋面积(m^2): '))
unit_price = float(input('请输入单价(元): '))
year_rate = float(input('请输入贷款利率(%): '))

month_rate = year_rate / (12 * 100)  # 月利率
total = unit_price * area  # 房屋实际额度
loan = int((total - total * 0.3) / 10000) * 10000  # 贷款额度整数，零头首付的时候交掉
first_pay = total - loan  # 首付

print('房屋总价:', total, '贷款额:', loan, '首付款:', first_pay, '月利率:', month_rate)

'''
等额本金还款法:
设：A=贷款额 B=贷款月利率 C=贷款月数 D=每月还款额 E=还款利息总和
每月应还本金=贷款额÷还款月数=A÷C
第一月还款利息=剩余本金×月利率=A×B
第二月还款利息:(贷款额-第一月已归还本金额)×月利率=(A-(A÷C))×B
第三月还款利息:(A-2×(A÷C))×B
...
第C月还款利息:(A-(C-1)×(A÷C))×B
求以上和得E=B(AC-[A+2A+3A+...+(C-1)A]÷C)=B(AC-A(C-1)÷2)=A×B×(C+1)÷2
每n月还款:D=本金+第n月利息=A÷C+(A-(n-1)×(A÷C))×B=(1+B(C-n+1))×A÷C
'''
# for i in range(1, total_month + 1):
#     month_interest = (loan - (i - 1) * (loan / total_month)) * month_rate  # 每月应还利息
#     pay_month = (1 + month_rate * (total_month - i + 1)) * loan / total_month  # 每月还款总额
#     print(pay_month, month_interest, pay_month - month_interest)

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
'''合同中提前还款条约限定前三十六个月内提前还款收取违约金，故三十六个月之后开始提前还款计划，提前还款本金最低数额为一万元'''

temp_total_interest = 0
for i in range(1, total_month + 1):
    pay_month = (loan * month_rate * math.pow(1 + month_rate, total_month)) / (
        math.pow(1 + month_rate, total_month) - 1)  # 每月还款额
    month_interest = (loan * month_rate - pay_month) * math.pow(1 + month_rate, i - 1) + pay_month  # 每月应还利息
    if (i <= 36):
        temp_total_interest += month_interest
        if (i == 36):
            print(temp_total_interest)
            # print(pay_month, month_interest)
