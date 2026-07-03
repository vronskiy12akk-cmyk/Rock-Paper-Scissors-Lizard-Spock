// rps.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Linq;

class RPSGame
{
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "green" => "\x1b[92m",
            "red" => "\x1b[91m",
            "yellow" => "\x1b[93m",
            "blue" => "\x1b[94m",
            "bold" => "\x1b[1m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    private List<string> moves;
    private Dictionary<string, List<string>> rules;
    private Dictionary<string, string> shortcuts;
    private string difficulty;
    private int targetWins;
    private List<string> history = new List<string>();
    private Stats stats;
    private string statsFile;

    class Stats
    {
        public int games { get; set; }
        public int wins { get; set; }
        public int losses { get; set; }
        public int draws { get; set; }
    }

    public RPSGame(string mode, string diff, int target)
    {
        difficulty = diff;
        targetWins = target;
        rules = new Dictionary<string, List<string>>()
        {
            {"камень", new List<string>{"ножницы","ящерица"}},
            {"ножницы", new List<string>{"бумага","ящерица"}},
            {"бумага", new List<string>{"камень","Спок"}},
            {"ящерица", new List<string>{"бумага","Спок"}},
            {"Спок", new List<string>{"камень","ножницы"}}
        };
        shortcuts = new Dictionary<string, string>()
        {
            {"r","камень"},{"p","бумага"},{"s","ножницы"},
            {"l","ящерица"},{"k","Спок"},
            {"1","камень"},{"2","ножницы"},{"3","бумага"},
            {"4","ящерица"},{"5","Спок"}
        };
        if (mode == "classic")
            moves = new List<string>{"камень","ножницы","бумага"};
        else
            moves = new List<string>{"камень","ножницы","бумага","ящерица","Спок"};
        statsFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".rps_stats.json");
        LoadStats();
    }

    void LoadStats()
    {
        if (File.Exists(statsFile))
        {
            string json = File.ReadAllText(statsFile);
            stats = JsonSerializer.Deserialize<Stats>(json);
        }
        if (stats == null) stats = new Stats();
    }

    void SaveStats()
    {
        string json = JsonSerializer.Serialize(stats);
        File.WriteAllText(statsFile, json);
    }

    void DisplayStats()
    {
        if (stats.games == 0)
        {
            Console.WriteLine(Colorize("Статистика пуста.", "yellow"));
            return;
        }
        double winRate = (double)stats.wins / stats.games * 100;
        Console.WriteLine(Colorize("📊 Статистика:", "bold"));
        Console.WriteLine($"  Всего игр: {stats.games}");
        Console.WriteLine($"  Побед: {stats.wins}");
        Console.WriteLine($"  Поражений: {stats.losses}");
        Console.WriteLine($"  Ничьих: {stats.draws}");
        Console.WriteLine($"  Процент побед: {winRate:F1}%");
    }

    string GetComputerMove()
    {
        Random rnd = new Random();
        if (difficulty == "easy")
            return moves[rnd.Next(moves.Count)];
        if (difficulty == "medium")
        {
            if (history.Count == 0)
                return moves[rnd.Next(moves.Count)];
            var freq = new Dictionary<string, int>();
            foreach (var m in history)
                freq[m] = freq.ContainsKey(m) ? freq[m] + 1 : 1;
            string mostCommon = history[0];
            int maxCnt = 0;
            foreach (var kv in freq)
                if (kv.Value > maxCnt) { maxCnt = kv.Value; mostCommon = kv.Key; }
            foreach (var m in moves)
                if (rules[m].Contains(mostCommon))
                    return m;
            return moves[rnd.Next(moves.Count)];
        }
        // hard
        if (history.Count < 2)
            return moves[rnd.Next(moves.Count)];
        string last = history[history.Count - 1];
        var winning = new List<string>();
        foreach (var m in moves)
            if (rules[m].Contains(last))
                winning.Add(m);
        if (winning.Count > 0 && rnd.NextDouble() < 0.6)
            return winning[rnd.Next(winning.Count)];
        return moves[rnd.Next(moves.Count)];
    }

    int DetermineWinner(string player, string computer)
    {
        if (player == computer) return 0;
        if (rules[player].Contains(computer)) return 1;
        return -1;
    }

    void PlayRound(ref int wins, ref int losses, ref int draws)
    {
        Console.WriteLine(Colorize("\nВаш ход:", "bold"));
        for (int i = 0; i < moves.Count; i++)
            Console.WriteLine($"  {i+1}. {moves[i]}");
        string playerMove = null;
        while (true)
        {
            Console.Write("Введите номер или сокращение (r,p,s,l,k): ");
            string input = Console.ReadLine().Trim().ToLower();
            if (input == "q")
            {
                Console.WriteLine(Colorize("Выход.", "yellow"));
                Environment.Exit(0);
            }
            if (shortcuts.ContainsKey(input) && moves.Contains(shortcuts[input]))
            {
                playerMove = shortcuts[input];
                break;
            }
            if (input.Length == 1 && char.IsDigit(input[0]))
            {
                int idx = int.Parse(input) - 1;
                if (idx >= 0 && idx < moves.Count)
                {
                    playerMove = moves[idx];
                    break;
                }
            }
            Console.WriteLine(Colorize("Неверный ввод. Попробуйте снова.", "red"));
        }
        string compMove = GetComputerMove();
        int result = DetermineWinner(playerMove, compMove);
        Console.WriteLine($"\nВаш ход: {Colorize(playerMove, "green")}");
        Console.WriteLine($"Ход компьютера: {Colorize(compMove, "red")}");
        if (result == 1)
        {
            Console.WriteLine(Colorize("✅ Вы победили!", "green"));
            wins++;
        }
        else if (result == -1)
        {
            Console.WriteLine(Colorize("❌ Компьютер победил!", "red"));
            losses++;
        }
        else
        {
            Console.WriteLine(Colorize("🤝 Ничья!", "yellow"));
            draws++;
        }
        history.Add(playerMove);
    }

    public void PlayMatch()
    {
        int wins = 0, losses = 0, draws = 0;
        int round = 0;
        string modeStr = moves.Count == 3 ? "классический" : "расширенный";
        Console.WriteLine(Colorize($"\nРежим: {modeStr}", "blue"));
        Console.WriteLine(Colorize($"Сложность: {difficulty}", "blue"));
        Console.WriteLine(Colorize($"Игра до {targetWins} побед.\n", "blue"));
        while (true)
        {
            PlayRound(ref wins, ref losses, ref draws);
            round++;
            if (wins >= targetWins || losses >= targetWins) break;
            if (round >= 20) break;
        }
        if (wins > losses)
            Console.WriteLine(Colorize($"\n🏆 Вы выиграли матч со счётом {wins}-{losses}!", "green"));
        else if (losses > wins)
            Console.WriteLine(Colorize($"\n💔 Компьютер выиграл матч со счётом {losses}-{wins}.", "red"));
        else
            Console.WriteLine(Colorize("\n🤝 Ничья в матче.", "yellow"));
        stats.games++;
        if (wins > losses) stats.wins++;
        else if (losses > wins) stats.losses++;
        stats.draws += draws;
        SaveStats();
    }

    static void Main(string[] args)
    {
        string mode = "extended";
        string difficulty = "medium";
        int targetWins = 1;
        bool showStats = false;
        for (int i = 0; i < args.Length; i++)
        {
            if (args[i] == "-m" && i+1 < args.Length)
            {
                string val = args[++i];
                if (val == "classic" || val == "extended") mode = val;
            }
            else if (args[i] == "-d" && i+1 < args.Length)
            {
                string val = args[++i];
                if (val == "easy" || val == "medium" || val == "hard") difficulty = val;
            }
            else if (args[i] == "-r" && i+1 < args.Length)
            {
                targetWins = int.Parse(args[++i]);
            }
            else if (args[i] == "-s")
            {
                showStats = true;
            }
            else if (args[i] == "-h")
            {
                Console.WriteLine("Usage: rps [-m classic|extended] [-d easy|medium|hard] [-r N] [-s]");
                return;
            }
        }
        var game = new RPSGame(mode, difficulty, targetWins);
        if (showStats)
        {
            game.DisplayStats();
            return;
        }
        Console.WriteLine(Colorize("🪨📄✂️  Добро пожаловать в игру!", "bold"));
        Console.WriteLine("Вводите номер хода или сокращение (r,p,s,l,k).");
        Console.WriteLine("Для выхода нажмите 'q' в любой момент.\n");
        game.PlayMatch();
        game.DisplayStats();
    }
}
