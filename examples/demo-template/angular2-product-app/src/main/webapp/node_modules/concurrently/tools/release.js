#!/usr/bin/env node

// Release automation script inspired by
// https://github.com/geddski/grunt-release

var fs = require('fs');
var path = require('path');

var _ = require('lodash');
var S = require('string');
var shell = require('shelljs');
var Mustache = require('mustache');
var semver = require('semver');
var program = require('commander');
var Promise = require('bluebird');


// Message templates use https://github.com/janl/mustache.js
var config = {
    releaseMessage: 'Release {{ version }}',
    backToDevMessage: 'Bump to dev version',
    bumpType: 'patch',
    files: ['package.json'],
    readmeFile: 'README.md',
    indentation: 4,

    // If true, don't execute anything, just tell what would have been done
    dryRun: false,

    // If true, don't push commits/tags or release to npm
    noPush: false,
    consolePrefix: '->',
    devSuffix: '-dev'
};

var projectRoot = path.join(__dirname, '..');
process.chdir(projectRoot);

function main() {
    parseArgs();
    config = mergeArgsToDefaults(config);

    if (config.dryRun) status('Dry run\n');

    var newVersion = bumpVersion(config.files, config.bumpType);

    _gitBranchName()
    .then(function(stdout) {
        if (stdout.trim().toLowerCase() !== 'master') {
            throw new Error('You should be in master branch before running the script!');
        }

        return gitAdd([config.readmeFile]);
    })
    .then(function() {
        return gitAdd(config.files);
    })
    .then(function() {
        var message = Mustache.render(config.releaseMessage, {
            version: newVersion
        });

        return gitCommit(message);
    })
    .then(function() {
        return gitTag(newVersion);
    })
    .then(function() {
        return gitPushTag(newVersion);
    })
    .then(npmPublish)
    .then(function() {
        bumpVersion(config.files, 'dev');
        return gitAdd(config.files.concat(config.readmeFile));
    })
    .then(function() {
        return gitCommit(config.backToDevMessage);
    })
    .then(function() {
        gitPush();
    })
    .then(function() {
        console.log('');
        status('Release successfully done!');
    })
    .catch(function(err) {
        console.error('\n!! Releasing failed');
        console.trace(err);
        process.exit(2);
    });
}

function parseArgs() {
    program
        .usage('bump');

    program.on('--help', function() {
        console.log('  Example usage:');
        console.log('');
        console.log('  $ ./release.js minor');
    });

    program.parse(process.argv);
}

function mergeArgsToDefaults(config) {
    if (program.args[0]) {
        config.bumpType = program.args[0];

        if (!_.contains(['major', 'minor', 'patch'], config.bumpType)) {
            console.error('Error:', config.bumpType, 'is not a valid bump type');
            process.exit(1);
        }
    }

    return config;
}

function status( /* arguments */ ) {
    var args = Array.prototype.slice.call(arguments);
    console.log(config.consolePrefix, args.join(' '));
}

function run(cmd, msg) {
    // All calls are actually synchronous but eventually some task
    // will need async stuff, so keep them promises
    return new Promise(function(resolve, reject) {
        if (msg) {
            status(msg);
        }

        if (config.dryRun) {
            return resolve();
        }

        var exec = shell.exec(cmd);
        var success = exec.code === 0;

        if (success) {
            resolve(exec.output);
        } else {
            var errMsg = 'Error executing: `' + cmd + '`\nOutput:\n' + exec.output;
            var err = new Error(errMsg);
            reject(err);
        }
    });
}

// Task functions
// All functions should return promise

// Bumps version in specified files.
// Files are assumed to contain JSON data which has "version" key following
// semantic versioning
function bumpVersion(files, bumpType) {
    status('Bump', bumpType, 'version to files:', files.join(' '));
    if (config.dryRun) return '[not available in dry run]';

    var newVersion;
    var originalVersion;
    files.forEach(function(fileName) {
        var filePath = path.join(projectRoot, fileName);

        var data = JSON.parse(fs.readFileSync(filePath));
        originalVersion = data.version;
        var currentVersion = data.version;
        if (!semver.valid(currentVersion)) {
            var msg = 'Invalid version ' + currentVersion +
                ' in file ' + fileName;
            var err = new Error(msg);
            throw err;
        }

        if (S(currentVersion).endsWith(config.devSuffix)) {
            currentVersion = S(currentVersion).chompRight(config.devSuffix).s;
        }

        if (bumpType === 'dev') {
            newVersion = currentVersion + config.devSuffix;
        } else {
            newVersion = semver.inc(currentVersion, bumpType);
        }
        data.version = newVersion;

        var content = JSON.stringify(data, null, config.indentation);
        fs.writeFileSync(filePath, content);

        status('Bump', originalVersion, '->', newVersion, 'in',
            fileName);
    });

    bumpReadmeVersion(originalVersion, newVersion, bumpType);

    return newVersion;
}

function bumpReadmeVersion(oldVersion, newVersion, bumpType) {
    if (bumpType === 'dev') {
        // Don't bump readme version in to dev version
        return;
    }

    var oldReleaseVersion = oldVersion;
    if (S(oldReleaseVersion).endsWith(config.devSuffix)) {
        oldReleaseVersion = S(oldReleaseVersion).chompRight(config.devSuffix).s;
    }

    status('Replace readme version', oldReleaseVersion, '->', newVersion);
    if (config.dryRun) return;

    var filePath = path.join(projectRoot, config.readmeFile);
    var content = fs.readFileSync(filePath, {encoding: 'utf-8'});

    // Update visible version
    var re = new RegExp('Version: ' + oldReleaseVersion, 'g');
    var newContent = content.replace(re, 'Version: ' + newVersion);

    // Replace link to previous stable
    re = new RegExp('tree/[0-9]\\.[0-9]\\.[0-9]');
    newContent = newContent.replace(re, 'tree/' + oldReleaseVersion);

    fs.writeFileSync(filePath, newContent);
}

function gitAdd(files) {
    var cmd = 'git add ' + files.join(' ');
    var msg = 'Staged ' + files.length + ' files';
    return run(cmd, msg);
}

function gitCommit(message) {
    var cmd = 'git commit -m "' + message + '"';
    var msg = 'Commit files';
    return run(cmd, msg);
}

function gitTag(name) {
    var cmd = 'git tag ' + name;
    var msg = 'Created a new git tag: ' + name;
    return run(cmd, msg);
}

function gitPush() {
    if (config.noPush) return;

    var cmd = 'git push';
    var msg = 'Push to remote';
    return run(cmd, msg);
}

function gitPushTag(tagName) {
    if (config.noPush) return;

    var cmd = 'git push origin ' + tagName;
    var msg = 'Push created git tag to remote';
    return run(cmd, msg);
}

function npmPublish() {
    if (config.noPush) return;

    var cmd = 'npm publish';
    var msg = 'Publish to npm';
    return run(cmd, msg);
}

function _gitBranchName() {
    var cmd = 'git rev-parse --abbrev-ref HEAD';
    return run(cmd, false);
}


main();
