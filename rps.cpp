// rps.cpp
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <random>
#include <fstream>
#include <cctype>
#include <algorithm>
#include <unistd.h> // для sleep на Unix, на Windows используйте windows.h

using namespace std;

// ANSI-цвета (для терминалов)
const string RESET = "\033[0m";
const string GREEN = "\033[92m";
const string RED = "\033[91m";
const string YELLOW = "\033[93m";
const string BLUE = "\033[94m";
const string CYAN = "\033[96m";
const string BOLD = "\033[1m";

string colorize(const string& text, const string& color) {
    return color + text + RESET;
}

class RPSGame {
public:
    vector<string> moves;
    map<string, vector<string>> rules;
    map<string, string> shortcuts;
    string difficulty;
    int targetWins;
    vector<string> history;
    map<string, int> stats;

    RPSGame(const string& mode, const string& diff, int target)
        : difficulty(diff), targetWins(target) {
        if (mode == "classic") {
            moves = {"камень", "ножницы", "бумага"};
        } else {
            moves = {"камень", "ножницы", "бумага", "ящерица", "Спок"};
        }
        // Правила
        rules["камень"] = {"ножницы", "ящерица"};
        rules["ножницы"] = {"бумага", "ящерица"};
        rules["бумага"] = {"камень", "Спок"};
        rules["ящерица"] = {"бумага", "Спок"};
        rules["Спок"] = {"камень", "ножницы"};
        // Сокращения
        shortcuts["r"] = "камень";
        shortcuts["p"] = "бумага";
        shortcuts["s"] = "ножницы";
        shortcuts["l"] = "ящерица";
        shortcuts["k"] = "Спок";
        shortcuts["1"] = "камень";
        shortcuts["2"] = "ножницы";
        shortcuts["3"] = "бумага";
        shortcuts["4"] = "ящерица";
        shortcuts["5"] = "Спок";
        loadStats();
    }

    void loadStats() {
        ifstream f(getStatsPath());
        if (f) {
            string line;
            while (getline(f, line)) {
                size_t pos = line.find(':');
                if (pos != string::npos) {
                    string key = line.substr(0, pos);
                    int val = stoi(line.substr(pos+1));
                    stats[key] = val;
                }
            }
        }
        if (stats.find("games") == stats.end()) {
            stats["games"] = 0;
            stats["wins"] = 0;
            stats["losses"] = 0;
            stats["draws"] = 0;
        }
    }

    void saveStats() {
        ofstream f(getStatsPath());
        if (f) {
            f << "games:" << stats["games"] << "\n";
            f << "wins:" << stats["wins"] << "\n";
            f << "losses:" << stats["losses"] << "\n";
            f << "draws:" << stats["draws"] << "\n";
        }
    }

    string getStatsPath() {
        const char* home = getenv("HOME");
        if (!home) home = getenv("USERPROFILE");
        return string(home) + "/.rps_stats.txt";
    }

    void displayStats() {
        int total = stats["games"];
        if (total == 0) {
            cout << colorize("Статистика пуста.", YELLOW) << endl;
            return;
        }
        double winRate = (double)stats["wins"] / total * 100;
        cout << colorize("📊 Статистика:", BOLD) << endl;
        cout << "  Всего игр: " << total << endl;
        cout << "  Побед: " << stats["wins"] << endl;
        cout << "  Поражений: " << stats["losses"] << endl;
        cout << "  Ничьих: " << stats["draws"] << endl;
        cout << "  Процент побед: " << winRate << "%" << endl;
    }

    string getComputerMove() {
        random_device rd;
        mt19937 gen(rd());
        uniform_int_distribution<> dis(0, moves.size()-1);
        if (difficulty == "easy") {
            return moves[dis(gen)];
        } else if (difficulty == "medium") {
            if (history.empty()) return moves[dis(gen)];
            // Найти самый частый ход игрока
            map<string, int> freq;
            for (const string& m : history) freq[m]++;
            string mostCommon = history[0];
            int maxCnt = 0;
            for (auto& p : freq) {
                if (p.second > maxCnt) {
                    maxCnt = p.second;
                    mostCommon = p.first;
                }
            }
            // Выбрать ход, который побеждает mostCommon
            for (const string& m : moves) {
                if (find(rules[m].begin(), rules[m].end(), mostCommon) != rules[m].end()) {
                    return m;
                }
            }
            return moves[dis(gen)];
        } else { // hard
            if (history.size() < 2) return moves[dis(gen)];
            string last = history.back();
            // Ищем ход, который побеждает последний ход игрока с вероятностью 60%
            vector<string> winning;
            for (const string& m : moves) {
                if (find(rules[m].begin(), rules[m].end(), last) != rules[m].end()) {
                    winning.push_back(m);
                }
            }
            if (!winning.empty() && (rand() % 100) < 60) {
                return winning[rand() % winning.size()];
            }
            return moves[dis(gen)];
        }
    }

    int determineWinner(const string& player, const string& computer) {
        if (player == computer) return 0; // draw
        if (find(rules[player].begin(), rules[player].end(), computer) != rules[player].end()) {
            return 1; // player wins
        }
        return -1; // computer wins
    }

    void playRound(int& wins, int& losses, int& draws) {
        // Показать ходы
        cout << colorize("\nВаш ход:", BOLD) << endl;
        for (size_t i=0; i<moves.size(); ++i) {
            cout << "  " << i+1 << ". " << moves[i] << endl;
        }
        string input;
        string playerMove;
        while (true) {
            cout << "Введите номер или сокращение (r,p,s,l,k): ";
            cin >> input;
            transform(input.begin(), input.end(), input.begin(), ::tolower);
            if (input == "q") {
                cout << colorize("Выход.", YELLOW) << endl;
                exit(0);
            }
            if (shortcuts.find(input) != shortcuts.end()) {
                string m = shortcuts[input];
                if (find(moves.begin(), moves.end(), m) != moves.end()) {
                    playerMove = m;
                    break;
                }
            }
            if (input.size() == 1 && isdigit(input[0])) {
                int idx = stoi(input) - 1;
                if (idx >= 0 && idx < (int)moves.size()) {
                    playerMove = moves[idx];
                    break;
                }
            }
            cout << colorize("Неверный ввод. Попробуйте снова.", RED) << endl;
        }
        string compMove = getComputerMove();
        int result = determineWinner(playerMove, compMove);
        cout << "\nВаш ход: " << colorize(playerMove, GREEN) << endl;
        cout << "Ход компьютера: " << colorize(compMove, RED) << endl;
        if (result == 1) {
            cout << colorize("✅ Вы победили!", GREEN) << endl;
            wins++;
        } else if (result == -1) {
            cout << colorize("❌ Компьютер победил!", RED) << endl;
            losses++;
        } else {
            cout << colorize("🤝 Ничья!", YELLOW) << endl;
            draws++;
        }
        history.push_back(playerMove);
    }

    void playMatch() {
        int wins = 0, losses = 0, draws = 0;
        int round = 0;
        cout << colorize("\nРежим: " + string((moves.size()==3?"классический":"расширенный")), BLUE) << endl;
        cout << colorize("Сложность: " + difficulty, BLUE) << endl;
        cout << colorize("Игра до " + to_string(targetWins) + " побед.\n", BLUE) << endl;
        while (true) {
            playRound(wins, losses, draws);
            round++;
            if (wins >= targetWins || losses >= targetWins) break;
            if (round >= 20) break;
        }
        if (wins > losses) {
            cout << colorize("\n🏆 Вы выиграли матч со счётом " + to_string(wins) + "-" + to_string(losses) + "!", GREEN) << endl;
        } else if (losses > wins) {
            cout << colorize("\n💔 Компьютер выиграл матч со счётом " + to_string(losses) + "-" + to_string(wins) + ".", RED) << endl;
        } else {
            cout << colorize("\n🤝 Ничья в матче.", YELLOW) << endl;
        }
        // Обновить общую статистику
        stats["games"]++;
        stats["wins"] += (wins > losses) ? 1 : 0;
        stats["losses"] += (losses > wins) ? 1 : 0;
        stats["draws"] += draws;
        saveStats();
    }
};

int main(int argc, char* argv[]) {
    string mode = "extended";
    string difficulty = "medium";
    int targetWins = 1;
    bool showStats = false;
    for (int i=1; i<argc; ++i) {
        string arg = argv[i];
        if (arg == "-m" && i+1 < argc) {
            string val = argv[++i];
            if (val == "classic" || val == "extended") mode = val;
        } else if (arg == "-d" && i+1 < argc) {
            string val = argv[++i];
            if (val == "easy" || val == "medium" || val == "hard") difficulty = val;
        } else if (arg == "-r" && i+1 < argc) {
            targetWins = stoi(argv[++i]);
        } else if (arg == "-s") {
            showStats = true;
        } else if (arg == "-h") {
            cout << "Usage: rps [-m classic|extended] [-d easy|medium|hard] [-r N] [-s]" << endl;
            return 0;
        }
    }
    RPSGame game(mode, difficulty, targetWins);
    if (showStats) {
        game.displayStats();
        return 0;
    }
    cout << colorize("🪨📄✂️  Добро пожаловать в игру!", BOLD) << endl;
    cout << "Вводите номер хода или сокращение (r,p,s,l,k)." << endl;
    cout << "Для выхода нажмите 'q' в любой момент.\n" << endl;
    game.playMatch();
    game.displayStats();
    return 0;
}
