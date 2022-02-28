import chalk from 'chalk';
/** The minimum width, in characters, of each size column */
const SIZE_COLUMN_WIDTH = 11;
/** Generic Object.entries() alphabetical sort by keys. */
function entriesSort([filenameA], [filenameB]) {
    return filenameA.localeCompare(filenameB);
}
/** Pretty-prints number of bytes as "XXX KB" */
function formatSize(size) {
    let kb = Math.round((size / 1000) * 100) / 100;
    if (kb >= 1000) {
        kb = Math.floor(kb);
    }
    let color;
    if (kb < 15) {
        color = 'green';
    }
    else if (kb < 30) {
        color = 'yellow';
    }
    else {
        color = 'red';
    }
    return chalk[color](`${kb} KB`.padEnd(SIZE_COLUMN_WIDTH));
}
function formatDelta(delta) {
    const kb = Math.round(delta * 100) / 100;
    const color = delta > 0 ? 'red' : 'green';
    return chalk[color](`Δ ${delta > 0 ? '+' : ''}${kb} KB`);
}
function formatFileInfo(filename, stats, padEnd, isLastFile) {
    const lineGlyph = chalk.dim(isLastFile ? '└─' : '├─');
    const lineName = filename.padEnd(padEnd);
    const fileStat = formatSize(stats.size);
    const gzipStat = formatSize(stats.gzip);
    const brotliStat = formatSize(stats.brotli);
    const lineStat = fileStat + gzipStat + brotliStat;
    let lineDelta = '';
    if (stats.delta) {
        lineDelta = chalk.dim('[') + formatDelta(stats.delta) + chalk.dim(']');
    }
    // Trim trailing whitespace (can mess with formatting), but keep indentation.
    return `    ` + `${lineGlyph} ${lineName} ${lineStat} ${lineDelta}`.trim();
}
function formatFiles(files, padEnd) {
    const strippedFiles = files.map(([filename, stats]) => [
        filename.replace(/^common\//, ''),
        stats,
    ]);
    return strippedFiles
        .map(([filename, stats], index) => formatFileInfo(filename, stats, padEnd, index >= files.length - 1))
        .join('\n');
}
export function printStats(dependencyStats) {
    let output = '';
    const { direct, common } = dependencyStats;
    const allDirect = Object.entries(direct).sort(entriesSort);
    const allCommon = Object.entries(common).sort(entriesSort);
    const maxFileNameLength = [...allCommon, ...allDirect].reduce((max, [filename]) => Math.max(filename.length, max), 'web_modules/'.length) + 1;
    output +=
        `  ⦿ ${chalk.bold('web_modules/'.padEnd(maxFileNameLength + 4))}` +
            chalk.bold.underline('size'.padEnd(SIZE_COLUMN_WIDTH - 2)) +
            '  ' +
            chalk.bold.underline('gzip'.padEnd(SIZE_COLUMN_WIDTH - 2)) +
            '  ' +
            chalk.bold.underline('brotli'.padEnd(SIZE_COLUMN_WIDTH - 2)) +
            `\n`;
    output += `${formatFiles(allDirect, maxFileNameLength)}\n`;
    if (Object.values(common).length > 0) {
        output += `  ⦿ ${chalk.bold('web_modules/common/ (Shared)')}\n`;
        output += `${formatFiles(allCommon, maxFileNameLength)}\n`;
    }
    return `\n${output}\n`;
}
