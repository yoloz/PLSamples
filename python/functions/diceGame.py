# 摇骰子游戏

import random


# 摇骰子函数
def roll_dice(numbers=3, points=None):
    print('<<<<< ROLL THE DICE! >>>>>')
    if points is None:
        points = []
    while numbers > 0:
        point = random.randrange(1, 7)
        points.append(point)
        numbers -= 1
    return points


# 点数转换为大小
def roll_result(total):
    is_big = 11 <= total <= 18
    is_small = 3 <= total <= 10
    if is_big:
        return 'Big'
    elif is_small:
        return 'Small'


# 开始游戏
def start_game():
    print('<<<<<GAME STARTS!>>>>>')
    choices = ['Big', 'Small']
    your_choice = input('Big or Small:')
    if your_choice in choices:
        points = roll_dice()
        total = sum(points)
        if your_choice == roll_result(total):
            print('The points are ', points, ' You win!')
        else:
            print('The points are ', points, 'You lose!')
    else:
        print('Invalid Words')
        start_game()


start_game()
