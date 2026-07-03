#!/usr/bin/env ruby
# rps.rb
# encoding: UTF-8

require 'json'
require 'fileutils'

# ANSI-цвета
COLORS = {
  reset: "\e[0m",
  green: "\e[92m",
  red: "\e[91m",
  yellow: "\e[93m",
  blue: "\e[94m",
  bold: "\e[1m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

RULES = {
  'камень' => ['ножницы', 'ящерица'],
  'ножницы' => ['бумага', 'ящерица'],
  'бумага' => ['камень', 'Спок'],
  'ящерица' => ['бумага', 'Спок'],
  'Спок' => ['камень', 'ножницы']
}

SHORTCUTS = {
  'r' => 'камень', 'p' => 'бумага', 's' => 'ножницы',
  'l' => 'ящерица', 'k' => 'Спок',
  '1' => 'камень', '2' => 'ножницы', '3' => 'бумага',
  '4' => 'ящерица', '5' => 'Спок'
}

class Game
  attr_reader :moves, :difficulty, :target_wins, :history, :stats, :stats_file

  def initialize(mode, difficulty, target_wins)
    @moves = if mode == 'classic'
               ['камень', 'ножницы', 'бумага']
             else
               ['камень', 'ножницы', 'бумага', 'ящерица', 'Спок']
             end
    @difficulty = difficulty
    @target_wins = target_wins
    @history = []
    @stats_file = File.join(Dir.home, '.rps_stats.json')
    load_stats
  end

  def load_stats
    if File.exist?(@stats_file)
      @stats = JSON.parse(File.read(@stats_file))
    else
      @stats = { 'games' => 0, 'wins' => 0, 'losses' => 0, 'draws' => 0 }
    end
  end

  def save_stats
    File.write(@stats_file, JSON.pretty_generate(@stats))
  end

  def display_stats
    if @stats['games'] == 0
      puts colorize("Статистика пуста.", :yellow)
      return
    end
    win_rate = (@stats['wins'].to_f / @stats['games']) * 100
    puts colorize("📊 Статистика:", :bold)
    puts "  Всего игр: #{@stats['games']}"
    puts "  Побед: #{@stats['wins']}"
    puts "  Поражений: #{@stats['losses']}"
    puts "  Ничьих: #{@stats['draws']}"
    puts "  Процент побед: #{win_rate.round(1)}%"
  end

  def computer_move
    if @difficulty == 'easy'
      return @moves.sample
    elsif @difficulty == 'medium'
      if @history.empty?
        return @moves.sample
      end
      freq = Hash.new(0)
      @history.each { |m| freq[m] += 1 }
      most_common = freq.max_by { |_, v| v }[0]
      @moves.each do |m|
        return m if RULES[m].include?(most_common)
      end
      return @moves.sample
    else # hard
      if @history.size < 2
        return @moves.sample
      end
      last = @history.last
      winning = @moves.select { |m| RULES[m].include?(last) }
      if winning.any? && rand < 0.6
        return winning.sample
      end
      return @moves.sample
    end
  end

  def determine_winner(player, computer)
    return 0 if player == computer
    return 1 if RULES[player].include?(computer)
    -1
  end

  def play_round(wins, losses, draws)
    puts colorize("\nВаш ход:", :bold)
    @moves.each_with_index { |m, i| puts "  #{i+1}. #{m}" }
    player_move = nil
    loop do
      print "Введите номер или сокращение (r,p,s,l,k): "
      input = gets.chomp.strip.downcase
      if input == 'q'
        puts colorize("Выход.", :yellow)
        exit
      end
      if SHORTCUTS[input] && @moves.include?(SHORTCUTS[input])
        player_move = SHORTCUTS[input]
        break
      end
      if input.match?(/^[1-5]$/)
        idx = input.to_i - 1
        if idx >= 0 && idx < @moves.size
          player_move = @moves[idx]
          break
        end
      end
      puts colorize("Неверный ввод. Попробуйте снова.", :red)
    end
    comp_move = computer_move
    result = determine_winner(player_move, comp_move)
    puts "\nВаш ход: #{colorize(player_move, :green)}"
    puts "Ход компьютера: #{colorize(comp_move, :red)}"
    if result == 1
      puts colorize("✅ Вы победили!", :green)
      wins.call(wins.call + 1)
    elsif result == -1
      puts colorize("❌ Компьютер победил!", :red)
      losses.call(losses.call + 1)
    else
      puts colorize("🤝 Ничья!", :yellow)
      draws.call(draws.call + 1)
    end
    @history << player_move
  end

  def play_match
    wins = 0
    losses = 0
    draws = 0
    round = 0
    mode_str = @moves.size == 3 ? 'классический' : 'расширенный'
    puts colorize("\nРежим: #{mode_str}", :blue)
    puts colorize("Сложность: #{@difficulty}", :blue)
    puts colorize("Игра до #{@target_wins} побед.\n", :blue)
    while true
      play_round(->(v) { wins = v }, ->(v) { losses = v }, ->(v) { draws = v })
      round += 1
      break if wins >= @target_wins || losses >= @target_wins
      break if round >= 20
    end
    if wins > losses
      puts colorize("\n🏆 Вы выиграли матч со счётом #{wins}-#{losses}!", :green)
    elsif losses > wins
      puts colorize("\n💔 Компьютер выиграл матч со счётом #{losses}-#{wins}.", :red)
    else
      puts colorize("\n🤝 Ничья в матче.", :yellow)
    end
    @stats['games'] += 1
    if wins > losses
      @stats['wins'] += 1
    elsif losses > wins
      @stats['losses'] += 1
    end
    @stats['draws'] += draws
    save_stats
  end
end

def main
  mode = 'extended'
  difficulty = 'medium'
  target_wins = 1
  show_stats = false
  args = ARGV
  i = 0
  while i < args.size
    arg = args[i]
    if arg == '-m' && i+1 < args.size
      val = args[i+1]
      mode = val if ['classic', 'extended'].include?(val)
      i += 1
    elsif arg == '-d' && i+1 < args.size
      val = args[i+1]
      difficulty = val if ['easy', 'medium', 'hard'].include?(val)
      i += 1
    elsif arg == '-r' && i+1 < args.size
      target_wins = args[i+1].to_i
      i += 1
    elsif arg == '-s'
      show_stats = true
    elsif arg == '-h'
      puts "Usage: ruby rps.rb [-m classic|extended] [-d easy|medium|hard] [-r N] [-s]"
      exit
    end
    i += 1
  end
  game = Game.new(mode, difficulty, target_wins)
  if show_stats
    game.display_stats
    exit
  end
  puts colorize("🪨📄✂️  Добро пожаловать в игру!", :bold)
  puts "Вводите номер хода или сокращение (r,p,s,l,k)."
  puts "Для выхода нажмите 'q' в любой момент.\n"
  game.play_match
  game.display_stats
end

main if __FILE__ == $0
