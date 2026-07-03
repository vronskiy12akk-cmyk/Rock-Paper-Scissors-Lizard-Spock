// rps.java
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class rps {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";
    private static final String RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[93m";
    private static final String BLUE = "\u001B[94m";
    private static final String BOLD = "\u001B[1m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    private static final Map<String, List<String>> RULES = new HashMap<>();
    static {
        RULES.put("камень", Arrays.asList("ножницы", "ящерица"));
        RULES.put("ножницы", Arrays.asList("бумага", "ящерица"));
        RULES.put("бумага", Arrays.asList("камень", "Спок"));
        RULES.put("ящерица", Arrays.asList("бумага", "Спок"));
        RULES.put("Спок", Arrays.asList("камень", "ножницы"));
    }

    private static final Map<String, String> SHORTCUTS = new HashMap<>();
    static {
        SHORTCUTS.put("r", "камень");
        SHORTCUTS.put("p", "бумага");
        SHORTCUTS.put("s", "ножницы");
        SHORTCUTS.put("l", "ящерица");
        SHORTCUTS.put("k", "Спок");
        SHORTCUTS.put("1", "камень");
        SHORTCUTS.put("2", "ножницы");
        SHORTCUTS.put("3", "бумага");
        SHORTCUTS.put("4", "ящерица");
        SHORTCUTS.put("5", "Спок");
    }

    static class Stats {
        int games, wins, losses, draws;
    }

    private List<String> moves;
    private String difficulty;
    private int targetWins;
    private List<String> history = new ArrayList<>();
    private Stats stats = new Stats();
    private String statsFile;

    public rps(String mode, String difficulty, int targetWins) {
        this.difficulty = difficulty;
        this.targetWins = targetWins;
        if (mode.equals("classic")) {
            moves = Arrays.asList("камень", "ножницы", "бумага");
        } else {
            moves = Arrays.asList("камень", "ножницы", "бумага", "ящерица", "Спок");
        }
        statsFile = System.getProperty("user.home") + "/.rps_stats.json";
        loadStats();
    }

    private void loadStats() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(statsFile)));
            // Упрощённый парсинг JSON (без библиотеки)
            String[] parts = content.split(",");
            for (String p : parts) {
                if (p.contains("\"games\":")) {
                    stats.games = Integer.parseInt(p.replaceAll("\\D+", ""));
                } else if (p.contains("\"wins\":")) {
                    stats.wins = Integer.parseInt(p.replaceAll("\\D+", ""));
                } else if (p.contains("\"losses\":")) {
                    stats.losses = Integer.parseInt(p.replaceAll("\\D+", ""));
                } else if (p.contains("\"draws\":")) {
                    stats.draws = Integer.parseInt(p.replaceAll("\\D+", ""));
                }
            }
        } catch (Exception e) {
            // файл не существует или повреждён
        }
    }

    private void saveStats() {
        try {
            String json = "{\"games\":" + stats.games + ",\"wins\":" + stats.wins +
                          ",\"losses\":" + stats.losses + ",\"draws\":" + stats.draws + "}";
            Files.write(Paths.get(statsFile), json.getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения статистики.");
        }
    }

    private void displayStats() {
        if (stats.games == 0) {
            System.out.println(colorize("Статистика пуста.", YELLOW));
            return;
        }
        double winRate = (double) stats.wins / stats.games * 100;
        System.out.println(colorize("📊 Статистика:", BOLD));
        System.out.println("  Всего игр: " + stats.games);
        System.out.println("  Побед: " + stats.wins);
        System.out.println("  Поражений: " + stats.losses);
        System.out.println("  Ничьих: " + stats.draws);
        System.out.printf("  Процент побед: %.1f%%\n", winRate);
    }

    private String getComputerMove() {
        Random rand = new Random();
        if (difficulty.equals("easy")) {
            return moves.get(rand.nextInt(moves.size()));
        }
        if (difficulty.equals("medium")) {
            if (history.isEmpty()) {
                return moves.get(rand.nextInt(moves.size()));
            }
            Map<String, Integer> freq = new HashMap<>();
            for (String m : history) freq.put(m, freq.getOrDefault(m, 0) + 1);
            String mostCommon = history.get(0);
            int maxCnt = 0;
            for (Map.Entry<String, Integer> e : freq.entrySet()) {
                if (e.getValue() > maxCnt) {
                    maxCnt = e.getValue();
                    mostCommon = e.getKey();
                }
            }
            for (String m : moves) {
                if (RULES.get(m).contains(mostCommon)) return m;
            }
            return moves.get(rand.nextInt(moves.size()));
        }
        // hard
        if (history.size() < 2) {
            return moves.get(rand.nextInt(moves.size()));
        }
        String last = history.get(history.size() - 1);
        List<String> winning = new ArrayList<>();
        for (String m : moves) {
            if (RULES.get(m).contains(last)) winning.add(m);
        }
        if (!winning.isEmpty() && rand.nextDouble() < 0.6) {
            return winning.get(rand.nextInt(winning.size()));
        }
        return moves.get(rand.nextInt(moves.size()));
    }

    private int determineWinner(String player, String computer) {
        if (player.equals(computer)) return 0;
        if (RULES.get(player).contains(computer)) return 1;
        return -1;
    }

    private void playRound(Scanner scanner, int[] wins, int[] losses, int[] draws) {
        System.out.println(colorize("\nВаш ход:", BOLD));
        for (int i = 0; i < moves.size(); i++) {
            System.out.println("  " + (i+1) + ". " + moves.get(i));
        }
        String playerMove = null;
        while (true) {
            System.out.print("Введите номер или сокращение (r,p,s,l,k): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("q")) {
                System.out.println(colorize("Выход.", YELLOW));
                System.exit(0);
            }
            if (SHORTCUTS.containsKey(input) && moves.contains(SHORTCUTS.get(input))) {
                playerMove = SHORTCUTS.get(input);
                break;
            }
            if (input.matches("^[1-5]$")) {
                int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < moves.size()) {
                    playerMove = moves.get(idx);
                    break;
                }
            }
            System.out.println(colorize("Неверный ввод. Попробуйте снова.", RED));
        }
        String compMove = getComputerMove();
        int result = determineWinner(playerMove, compMove);
        System.out.println("\nВаш ход: " + colorize(playerMove, GREEN));
        System.out.println("Ход компьютера: " + colorize(compMove, RED));
        if (result == 1) {
            System.out.println(colorize("✅ Вы победили!", GREEN));
            wins[0]++;
        } else if (result == -1) {
            System.out.println(colorize("❌ Компьютер победил!", RED));
            losses[0]++;
        } else {
            System.out.println(colorize("🤝 Ничья!", YELLOW));
            draws[0]++;
        }
        history.add(playerMove);
    }

    public void playMatch() {
        Scanner scanner = new Scanner(System.in);
        int[] wins = {0}, losses = {0}, draws = {0};
        int round = 0;
        String modeStr = moves.size() == 3 ? "классический" : "расширенный";
        System.out.println(colorize("\nРежим: " + modeStr, BLUE));
        System.out.println(colorize("Сложность: " + difficulty, BLUE));
        System.out.println(colorize("Игра до " + targetWins + " побед.\n", BLUE));
        while (true) {
            playRound(scanner, wins, losses, draws);
            round++;
            if (wins[0] >= targetWins || losses[0] >= targetWins) break;
            if (round >= 20) break;
        }
        scanner.close();
        if (wins[0] > losses[0]) {
            System.out.println(colorize("\n🏆 Вы выиграли матч со счётом " + wins[0] + "-" + losses[0] + "!", GREEN));
        } else if (losses[0] > wins[0]) {
            System.out.println(colorize("\n💔 Компьютер выиграл матч со счётом " + losses[0] + "-" + wins[0] + ".", RED));
        } else {
            System.out.println(colorize("\n🤝 Ничья в матче.", YELLOW));
        }
        stats.games++;
        if (wins[0] > losses[0]) stats.wins++;
        else if (losses[0] > wins[0]) stats.losses++;
        stats.draws += draws[0];
        saveStats();
    }

    public static void main(String[] args) {
        String mode = "extended";
        String difficulty = "medium";
        int targetWins = 1;
        boolean showStats = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-m") && i+1 < args.length) {
                String val = args[++i];
                if (val.equals("classic") || val.equals("extended")) mode = val;
            } else if (arg.equals("-d") && i+1 < args.length) {
                String val = args[++i];
                if (val.equals("easy") || val.equals("medium") || val.equals("hard")) difficulty = val;
            } else if (arg.equals("-r") && i+1 < args.length) {
                targetWins = Integer.parseInt(args[++i]);
            } else if (arg.equals("-s")) {
                showStats = true;
            } else if (arg.equals("-h")) {
                System.out.println("Usage: java rps [-m classic|extended] [-d easy|medium|hard] [-r N] [-s]");
                return;
            }
        }
        rps game = new rps(mode, difficulty, targetWins);
        if (showStats) {
            game.displayStats();
            return;
        }
        System.out.println(colorize("🪨📄✂️  Добро пожаловать в игру!", BOLD));
        System.out.println("Вводите номер хода или сокращение (r,p,s,l,k).");
        System.out.println("Для выхода нажмите 'q' в любой момент.\n");
        game.playMatch();
        game.displayStats();
    }
}
