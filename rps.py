# rps.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
import json
import random
import argparse
from collections import Counter

# ANSI-цвета
COLORS = {
    'reset': '\033[0m',
    'green': '\033[92m',
    'red': '\033[91m',
    'yellow': '\033[93m',
    'blue': '\033[94m',
    'cyan': '\033[96m',
    'bold': '\033[1m'
}

def colorize(text, color):
    return f"{COLORS.get(color, '')}{text}{COLORS['reset']}"

# Данные для обоих режимов
MOVES = {
    'classic': ['камень', 'ножницы', 'бумага'],
    'extended': ['камень', 'ножницы', 'бумага', 'ящерица', 'Спок']
}

# Правила: что побеждает что (ключ побеждает значения)
RULES = {
    'камень': ['ножницы', 'ящерица'],
    'ножницы': ['бумага', 'ящерица'],
    'бумага': ['камень', 'Спок'],
    'ящерица': ['бумага', 'Спок'],
    'Спок': ['камень', 'ножницы']
}

# Сокращения для ввода
SHORTCUTS = {
    'r': 'камень', 'p': 'бумага', 's': 'ножницы',
    'l': 'ящерица', 'k': 'Спок',  # k - Spock
    '1': 'камень', '2': 'ножницы', '3': 'бумага',
    '4': 'ящерица', '5': 'Спок'
}

class RPSGame:
    def __init__(self, mode='extended', difficulty='medium', target_wins=1):
        self.mode = mode
        self.moves = MOVES[mode]
        self.difficulty = difficulty
        self.target_wins = target_wins
        self.history = []          # ходы игрока (последовательность)
        self.stats = self.load_stats()
        self.wins = 0
        self.losses = 0
        self.draws = 0
        self.rounds_played = 0

    def load_stats(self):
        stats_file = os.path.expanduser('~/.rps_stats.json')
        if os.path.exists(stats_file):
            with open(stats_file, 'r') as f:
                return json.load(f)
        return {'games': 0, 'wins': 0, 'losses': 0, 'draws': 0}

    def save_stats(self):
        stats_file = os.path.expanduser('~/.rps_stats.json')
        with open(stats_file, 'w') as f:
            json.dump(self.stats, f, indent=2)

    def display_stats(self):
        s = self.stats
        total = s['games']
        if total == 0:
            print(colorize("Статистика пуста.", 'yellow'))
            return
        win_rate = (s['wins'] / total) * 100 if total > 0 else 0
        print(colorize("📊 Статистика:", 'bold'))
        print(f"  Всего игр: {total}")
        print(f"  Побед: {s['wins']}")
        print(f"  Поражений: {s['losses']}")
        print(f"  Ничьих: {s['draws']}")
        print(f"  Процент побед: {win_rate:.1f}%")

    def get_computer_move(self):
        moves = self.moves
        if self.difficulty == 'easy':
            return random.choice(moves)
        elif self.difficulty == 'medium':
            # Анализируем частоту ходов игрока
            if not self.history:
                return random.choice(moves)
            counter = Counter(self.history)
            most_common = counter.most_common(1)[0][0]  # самый частый ход игрока
            # Выбираем ход, который побеждает most_common
            # Найдём ход, который побеждает самый частый ход противника
            for move in moves:
                if most_common in RULES.get(move, []):
                    return move
            return random.choice(moves)
        else:  # hard
            # Анализируем последовательности из двух последних ходов
            if len(self.history) < 2:
                return random.choice(moves)
            last_two = tuple(self.history[-2:])
            # Строим словарь переходов (для простоты используем только последний ход)
            # Упрощённо: смотрим, какой ход чаще всего следует за последним ходом игрока
            # Для демонстрации используем более простую эвристику: предсказываем следующий ход как самый частый после последнего
            # На практике можно хранить переходы, но мы упростим: если последний ход был 'камень', вероятно игрок выберет 'ножницы'?
            # Я реализую случайный выбор с учётом предпочтений.
            # Для hard: если игрок часто выбирает камень, компьютер выберет бумагу и т.п.
            # Это уже реализовано в medium, так что hard можно сделать как medium с учётом последнего хода.
            # Сделаем: смотрим последний ход и выбираем контрход к нему с вероятностью 60%, иначе случайно.
            last = self.history[-1]
            # Найдём ход, который побеждает последний ход игрока
            winning_moves = [m for m in moves if last in RULES.get(m, [])]
            if winning_moves and random.random() < 0.6:
                return random.choice(winning_moves)
            else:
                return random.choice(moves)

    def determine_winner(self, player_move, computer_move):
        if player_move == computer_move:
            return 'draw'
        if computer_move in RULES.get(player_move, []):
            return 'player'
        else:
            return 'computer'

    def play_round(self):
        # Показать доступные ходы
        print(colorize("\nВаш ход:", 'bold'))
        for i, move in enumerate(self.moves):
            print(f"  {i+1}. {move}")
        # Получить ввод
        while True:
            choice = input("Введите номер или сокращение (r,p,s,l,k): ").strip().lower()
            if choice == 'q':
                print(colorize("Выход.", 'yellow'))
                sys.exit(0)
            # Проверка по сокращениям
            if choice in SHORTCUTS:
                move = SHORTCUTS[choice]
                if move in self.moves:
                    break
            # Проверка по номеру
            if choice.isdigit():
                idx = int(choice) - 1
                if 0 <= idx < len(self.moves):
                    move = self.moves[idx]
                    break
            print(colorize("Неверный ввод. Попробуйте снова.", 'red'))
        # Ход компьютера
        comp_move = self.get_computer_move()
        # Результат
        result = self.determine_winner(move, comp_move)
        # Вывод
        print(f"\nВаш ход: {colorize(move, 'green')}")
        print(f"Ход компьютера: {colorize(comp_move, 'red')}")
        if result == 'player':
            print(colorize("✅ Вы победили!", 'green'))
            self.wins += 1
        elif result == 'computer':
            print(colorize("❌ Компьютер победил!", 'red'))
            self.losses += 1
        else:
            print(colorize("🤝 Ничья!", 'yellow'))
            self.draws += 1
        self.rounds_played += 1
        self.history.append(move)

    def play_match(self):
        self.wins = 0
        self.losses = 0
        self.draws = 0
        self.rounds_played = 0
        self.history = []
        print(colorize(f"\nРежим: {'классический' if self.mode == 'classic' else 'расширенный'}", 'blue'))
        print(colorize(f"Сложность: {self.difficulty}", 'blue'))
        print(colorize(f"Игра до {self.target_wins} побед.\n", 'blue'))
        while True:
            self.play_round()
            if self.wins >= self.target_wins:
                print(colorize(f"\n🏆 Вы выиграли матч со счётом {self.wins}-{self.losses}!", 'green'))
                break
            if self.losses >= self.target_wins:
                print(colorize(f"\n💔 Компьютер выиграл матч со счётом {self.losses}-{self.wins}.", 'red'))
                break
            if self.rounds_played >= 20:  # защита от бесконечности
                break
        # Обновить общую статистику
        self.stats['games'] += 1
        self.stats['wins'] += 1 if self.wins > self.losses else 0
        self.stats['losses'] += 1 if self.losses > self.wins else 0
        self.stats['draws'] += self.draws
        self.save_stats()

def main():
    parser = argparse.ArgumentParser(description="Rock-Paper-Scissors-Lizard-Spock")
    parser.add_argument('-m', '--mode', choices=['classic', 'extended'], default='extended',
                        help='Режим игры (classic или extended)')
    parser.add_argument('-d', '--difficulty', choices=['easy', 'medium', 'hard'], default='medium',
                        help='Сложность компьютера')
    parser.add_argument('-r', '--rounds', type=int, default=1,
                        help='Количество побед для завершения матча')
    parser.add_argument('-s', '--stats', action='store_true',
                        help='Показать статистику и выйти')
    args = parser.parse_args()

    game = RPSGame(mode=args.mode, difficulty=args.difficulty, target_wins=args.rounds)
    if args.stats:
        game.display_stats()
        return

    print(colorize("🪨📄✂️  Добро пожаловать в игру!", 'bold'))
    print("Вводите номер хода или сокращение (r,p,s,l,k).")
    print("Для выхода нажмите 'q' в любой момент.\n")

    game.play_match()
    game.display_stats()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(colorize("\nВыход.", 'yellow'))
        sys.exit(0)
