// rps.go
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

const (
	reset  = "\033[0m"
	green  = "\033[92m"
	red    = "\033[91m"
	yellow = "\033[93m"
	blue   = "\033[94m"
	cyan   = "\033[96m"
	bold   = "\033[1m"
)

func colorize(text, color string) string {
	return color + text + reset
}

type Stats struct {
	Games  int `json:"games"`
	Wins   int `json:"wins"`
	Losses int `json:"losses"`
	Draws  int `json:"draws"`
}

type Game struct {
	moves      []string
	rules      map[string][]string
	shortcuts  map[string]string
	mode       string
	difficulty string
	targetWins int
	history    []string
	stats      Stats
	statsFile  string
}

func NewGame(mode, difficulty string, targetWins int) *Game {
	g := &Game{
		mode:       mode,
		difficulty: difficulty,
		targetWins: targetWins,
		history:    []string{},
		statsFile:  filepath.Join(os.Getenv("HOME"), ".rps_stats.json"),
	}
	if mode == "classic" {
		g.moves = []string{"камень", "ножницы", "бумага"}
	} else {
		g.moves = []string{"камень", "ножницы", "бумага", "ящерица", "Спок"}
	}
	g.rules = map[string][]string{
		"камень":   {"ножницы", "ящерица"},
		"ножницы":  {"бумага", "ящерица"},
		"бумага":   {"камень", "Спок"},
		"ящерица":  {"бумага", "Спок"},
		"Спок":     {"камень", "ножницы"},
	}
	g.shortcuts = map[string]string{
		"r": "камень", "p": "бумага", "s": "ножницы",
		"l": "ящерица", "k": "Спок",
		"1": "камень", "2": "ножницы", "3": "бумага",
		"4": "ящерица", "5": "Спок",
	}
	g.loadStats()
	return g
}

func (g *Game) loadStats() {
	data, err := os.ReadFile(g.statsFile)
	if err == nil {
		json.Unmarshal(data, &g.stats)
	}
	if g.stats.Games == 0 && g.stats.Wins == 0 && g.stats.Losses == 0 && g.stats.Draws == 0 {
		g.stats = Stats{Games: 0, Wins: 0, Losses: 0, Draws: 0}
	}
}

func (g *Game) saveStats() {
	data, _ := json.MarshalIndent(g.stats, "", "  ")
	os.WriteFile(g.statsFile, data, 0644)
}

func (g *Game) displayStats() {
	if g.stats.Games == 0 {
		fmt.Println(colorize("Статистика пуста.", yellow))
		return
	}
	winRate := float64(g.stats.Wins) / float64(g.stats.Games) * 100
	fmt.Println(colorize("📊 Статистика:", bold))
	fmt.Printf("  Всего игр: %d\n", g.stats.Games)
	fmt.Printf("  Побед: %d\n", g.stats.Wins)
	fmt.Printf("  Поражений: %d\n", g.stats.Losses)
	fmt.Printf("  Ничьих: %d\n", g.stats.Draws)
	fmt.Printf("  Процент побед: %.1f%%\n", winRate)
}

func (g *Game) getComputerMove() string {
	rand.Seed(time.Now().UnixNano())
	if g.difficulty == "easy" {
		return g.moves[rand.Intn(len(g.moves))]
	}
	// medium / hard
	if g.difficulty == "medium" {
		if len(g.history) == 0 {
			return g.moves[rand.Intn(len(g.moves))]
		}
		// Найти самый частый ход игрока
		freq := make(map[string]int)
		for _, m := range g.history {
			freq[m]++
		}
		mostCommon := g.history[0]
		maxCnt := 0
		for m, c := range freq {
			if c > maxCnt {
				maxCnt = c
				mostCommon = m
			}
		}
		// Выбрать ход, который побеждает mostCommon
		for _, m := range g.moves {
			for _, beaten := range g.rules[m] {
				if beaten == mostCommon {
					return m
				}
			}
		}
		return g.moves[rand.Intn(len(g.moves))]
	}
	// hard
	if len(g.history) < 2 {
		return g.moves[rand.Intn(len(g.moves))]
	}
	last := g.history[len(g.history)-1]
	winning := []string{}
	for _, m := range g.moves {
		for _, beaten := range g.rules[m] {
			if beaten == last {
				winning = append(winning, m)
			}
		}
	}
	if len(winning) > 0 && rand.Intn(100) < 60 {
		return winning[rand.Intn(len(winning))]
	}
	return g.moves[rand.Intn(len(g.moves))]
}

func (g *Game) determineWinner(player, computer string) int {
	if player == computer {
		return 0
	}
	for _, beaten := range g.rules[player] {
		if beaten == computer {
			return 1
		}
	}
	return -1
}

func (g *Game) playRound(wins, losses, draws *int) {
	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println(colorize("\nВаш ход:", bold))
	for i, m := range g.moves {
		fmt.Printf("  %d. %s\n", i+1, m)
	}
	var playerMove string
	for {
		fmt.Print("Введите номер или сокращение (r,p,s,l,k): ")
		scanner.Scan()
		input := strings.ToLower(strings.TrimSpace(scanner.Text()))
		if input == "q" {
			fmt.Println(colorize("Выход.", yellow))
			os.Exit(0)
		}
		if val, ok := g.shortcuts[input]; ok {
			if contains(g.moves, val) {
				playerMove = val
				break
			}
		}
		if len(input) == 1 && input[0] >= '1' && input[0] <= '5' {
			idx := int(input[0] - '1')
			if idx >= 0 && idx < len(g.moves) {
				playerMove = g.moves[idx]
				break
			}
		}
		fmt.Println(colorize("Неверный ввод. Попробуйте снова.", red))
	}
	compMove := g.getComputerMove()
	result := g.determineWinner(playerMove, compMove)
	fmt.Printf("\nВаш ход: %s\n", colorize(playerMove, green))
	fmt.Printf("Ход компьютера: %s\n", colorize(compMove, red))
	if result == 1 {
		fmt.Println(colorize("✅ Вы победили!", green))
		(*wins)++
	} else if result == -1 {
		fmt.Println(colorize("❌ Компьютер победил!", red))
		(*losses)++
	} else {
		fmt.Println(colorize("🤝 Ничья!", yellow))
		(*draws)++
	}
	g.history = append(g.history, playerMove)
}

func contains(slice []string, val string) bool {
	for _, v := range slice {
		if v == val {
			return true
		}
	}
	return false
}

func (g *Game) playMatch() {
	wins, losses, draws := 0, 0, 0
	rounds := 0
	modeStr := "классический"
	if len(g.moves) > 3 {
		modeStr = "расширенный"
	}
	fmt.Println(colorize(fmt.Sprintf("\nРежим: %s", modeStr), blue))
	fmt.Println(colorize(fmt.Sprintf("Сложность: %s", g.difficulty), blue))
	fmt.Println(colorize(fmt.Sprintf("Игра до %d побед.\n", g.targetWins), blue))
	for {
		g.playRound(&wins, &losses, &draws)
		rounds++
		if wins >= g.targetWins || losses >= g.targetWins {
			break
		}
		if rounds >= 20 {
			break
		}
	}
	if wins > losses {
		fmt.Println(colorize(fmt.Sprintf("\n🏆 Вы выиграли матч со счётом %d-%d!", wins, losses), green))
	} else if losses > wins {
		fmt.Println(colorize(fmt.Sprintf("\n💔 Компьютер выиграл матч со счётом %d-%d.", losses, wins), red))
	} else {
		fmt.Println(colorize("\n🤝 Ничья в матче.", yellow))
	}
	g.stats.Games++
	if wins > losses {
		g.stats.Wins++
	} else if losses > wins {
		g.stats.Losses++
	}
	g.stats.Draws += draws
	g.saveStats()
}

func main() {
	mode := "extended"
	difficulty := "medium"
	targetWins := 1
	showStats := false
	for i := 1; i < len(os.Args); i++ {
		arg := os.Args[i]
		if arg == "-m" && i+1 < len(os.Args) {
			val := os.Args[i+1]
			if val == "classic" || val == "extended" {
				mode = val
			}
			i++
		} else if arg == "-d" && i+1 < len(os.Args) {
			val := os.Args[i+1]
			if val == "easy" || val == "medium" || val == "hard" {
				difficulty = val
			}
			i++
		} else if arg == "-r" && i+1 < len(os.Args) {
			targetWins, _ = strconv.Atoi(os.Args[i+1])
			i++
		} else if arg == "-s" {
			showStats = true
		} else if arg == "-h" {
			fmt.Println("Usage: rps [-m classic|extended] [-d easy|medium|hard] [-r N] [-s]")
			return
		}
	}
	game := NewGame(mode, difficulty, targetWins)
	if showStats {
		game.displayStats()
		return
	}
	fmt.Println(colorize("🪨📄✂️  Добро пожаловать в игру!", bold))
	fmt.Println("Вводите номер хода или сокращение (r,p,s,l,k).")
	fmt.Println("Для выхода нажмите 'q' в любой момент.\n")
	game.playMatch()
	game.displayStats()
}
