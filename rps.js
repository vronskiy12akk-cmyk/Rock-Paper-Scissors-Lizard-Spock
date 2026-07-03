// rps.js
#!/usr/bin/env node
'use strict';

const readline = require('readline');
const fs = require('fs');
const path = require('path');
const os = require('os');

const COLORS = {
    reset: '\x1b[0m',
    green: '\x1b[92m',
    red: '\x1b[91m',
    yellow: '\x1b[93m',
    blue: '\x1b[94m',
    cyan: '\x1b[96m',
    bold: '\x1b[1m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

const RULES = {
    'камень': ['ножницы', 'ящерица'],
    'ножницы': ['бумага', 'ящерица'],
    'бумага': ['камень', 'Спок'],
    'ящерица': ['бумага', 'Спок'],
    'Спок': ['камень', 'ножницы']
};

const SHORTCUTS = {
    'r': 'камень', 'p': 'бумага', 's': 'ножницы',
    'l': 'ящерица', 'k': 'Спок',
    '1': 'камень', '2': 'ножницы', '3': 'бумага',
    '4': 'ящерица', '5': 'Спок'
};

class Game {
    constructor(mode, difficulty, targetWins) {
        this.mode = mode;
        this.moves = mode === 'classic' ? ['камень','ножницы','бумага'] :
                                      ['камень','ножницы','бумага','ящерица','Спок'];
        this.difficulty = difficulty;
        this.targetWins = targetWins;
        this.history = [];
        this.statsFile = path.join(os.homedir(), '.rps_stats.json');
        this.stats = this.loadStats();
    }

    loadStats() {
        try {
            return JSON.parse(fs.readFileSync(this.statsFile, 'utf8'));
        } catch {
            return { games: 0, wins: 0, losses: 0, draws: 0 };
        }
    }

    saveStats() {
        fs.writeFileSync(this.statsFile, JSON.stringify(this.stats, null, 2));
    }

    displayStats() {
        if (this.stats.games === 0) {
            console.log(colorize('Статистика пуста.', 'yellow'));
            return;
        }
        const winRate = (this.stats.wins / this.stats.games) * 100;
        console.log(colorize('📊 Статистика:', 'bold'));
        console.log(`  Всего игр: ${this.stats.games}`);
        console.log(`  Побед: ${this.stats.wins}`);
        console.log(`  Поражений: ${this.stats.losses}`);
        console.log(`  Ничьих: ${this.stats.draws}`);
        console.log(`  Процент побед: ${winRate.toFixed(1)}%`);
    }

    getComputerMove() {
        if (this.difficulty === 'easy') {
            return this.moves[Math.floor(Math.random() * this.moves.length)];
        }
        if (this.difficulty === 'medium') {
            if (this.history.length === 0) {
                return this.moves[Math.floor(Math.random() * this.moves.length)];
            }
            const freq = {};
            for (const m of this.history) freq[m] = (freq[m] || 0) + 1;
            let mostCommon = this.history[0];
            let maxCount = 0;
            for (const [m, c] of Object.entries(freq)) {
                if (c > maxCount) {
                    maxCount = c;
                    mostCommon = m;
                }
            }
            for (const m of this.moves) {
                if (RULES[m].includes(mostCommon)) return m;
            }
            return this.moves[Math.floor(Math.random() * this.moves.length)];
        }
        // hard
        if (this.history.length < 2) {
            return this.moves[Math.floor(Math.random() * this.moves.length)];
        }
        const last = this.history[this.history.length - 1];
        const winning = [];
        for (const m of this.moves) {
            if (RULES[m].includes(last)) winning.push(m);
        }
        if (winning.length > 0 && Math.random() < 0.6) {
            return winning[Math.floor(Math.random() * winning.length)];
        }
        return this.moves[Math.floor(Math.random() * this.moves.length)];
    }

    determineWinner(player, computer) {
        if (player === computer) return 0;
        if (RULES[player].includes(computer)) return 1;
        return -1;
    }

    async playRound(rl, wins, losses, draws) {
        console.log(colorize('\nВаш ход:', 'bold'));
        this.moves.forEach((m, i) => console.log(`  ${i+1}. ${m}`));
        const ask = () => new Promise(resolve => {
            rl.question('Введите номер или сокращение (r,p,s,l,k): ', resolve);
        });
        let playerMove = null;
        while (true) {
            const input = (await ask()).trim().toLowerCase();
            if (input === 'q') {
                console.log(colorize('Выход.', 'yellow'));
                process.exit(0);
            }
            if (SHORTCUTS[input] && this.moves.includes(SHORTCUTS[input])) {
                playerMove = SHORTCUTS[input];
                break;
            }
            if (/^[1-5]$/.test(input)) {
                const idx = parseInt(input) - 1;
                if (idx >= 0 && idx < this.moves.length) {
                    playerMove = this.moves[idx];
                    break;
                }
            }
            console.log(colorize('Неверный ввод. Попробуйте снова.', 'red'));
        }
        const compMove = this.getComputerMove();
        const result = this.determineWinner(playerMove, compMove);
        console.log(`\nВаш ход: ${colorize(playerMove, 'green')}`);
        console.log(`Ход компьютера: ${colorize(compMove, 'red')}`);
        if (result === 1) {
            console.log(colorize('✅ Вы победили!', 'green'));
            wins.value++;
        } else if (result === -1) {
            console.log(colorize('❌ Компьютер победил!', 'red'));
            losses.value++;
        } else {
            console.log(colorize('🤝 Ничья!', 'yellow'));
            draws.value++;
        }
        this.history.push(playerMove);
    }

    async playMatch() {
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
        const wins = { value: 0 };
        const losses = { value: 0 };
        const draws = { value: 0 };
        let round = 0;
        const modeStr = this.moves.length === 3 ? 'классический' : 'расширенный';
        console.log(colorize(`\nРежим: ${modeStr}`, 'blue'));
        console.log(colorize(`Сложность: ${this.difficulty}`, 'blue'));
        console.log(colorize(`Игра до ${this.targetWins} побед.\n`, 'blue'));
        while (true) {
            await this.playRound(rl, wins, losses, draws);
            round++;
            if (wins.value >= this.targetWins || losses.value >= this.targetWins) break;
            if (round >= 20) break;
        }
        rl.close();
        if (wins.value > losses.value) {
            console.log(colorize(`\n🏆 Вы выиграли матч со счётом ${wins.value}-${losses.value}!`, 'green'));
        } else if (losses.value > wins.value) {
            console.log(colorize(`\n💔 Компьютер выиграл матч со счётом ${losses.value}-${wins.value}.`, 'red'));
        } else {
            console.log(colorize('\n🤝 Ничья в матче.', 'yellow'));
        }
        this.stats.games++;
        if (wins.value > losses.value) this.stats.wins++;
        else if (losses.value > wins.value) this.stats.losses++;
        this.stats.draws += draws.value;
        this.saveStats();
    }
}

async function main() {
    const args = process.argv.slice(2);
    let mode = 'extended';
    let difficulty = 'medium';
    let targetWins = 1;
    let showStats = false;
    for (let i = 0; i < args.length; i++) {
        if (args[i] === '-m' && i+1 < args.length) {
            const val = args[++i];
            if (val === 'classic' || val === 'extended') mode = val;
        } else if (args[i] === '-d' && i+1 < args.length) {
            const val = args[++i];
            if (['easy','medium','hard'].includes(val)) difficulty = val;
        } else if (args[i] === '-r' && i+1 < args.length) {
            targetWins = parseInt(args[++i]);
        } else if (args[i] === '-s') {
            showStats = true;
        } else if (args[i] === '-h') {
            console.log('Usage: node rps.js [-m classic|extended] [-d easy|medium|hard] [-r N] [-s]');
            process.exit(0);
        }
    }
    const game = new Game(mode, difficulty, targetWins);
    if (showStats) {
        game.displayStats();
        return;
    }
    console.log(colorize('🪨📄✂️  Добро пожаловать в игру!', 'bold'));
    console.log('Вводите номер хода или сокращение (r,p,s,l,k).');
    console.log('Для выхода нажмите \'q\' в любой момент.\n');
    await game.playMatch();
    game.displayStats();
}

main().catch(console.error);
