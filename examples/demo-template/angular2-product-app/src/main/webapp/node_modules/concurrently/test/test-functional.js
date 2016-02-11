// Test basic usage of cli

var path = require('path');
var assert = require('assert');
var run = require('./utils').run;

// If true, output of commands are shown
var DEBUG_TESTS = false;
var TEST_DIR = 'dir/';

// Abs path to test directory
var testDir = path.resolve(__dirname);
process.chdir(path.join(testDir, '..'));

describe('concurrently', function() {
    this.timeout(5000);

    it('help should be succesful', function(done) {
        run('node ./src/main.js --help', {pipe: DEBUG_TESTS})
        .then(function(exitCode) {
            // exit code 0 means success
            assert.strictEqual(exitCode, 0);
            done();
        });
    });

    it('version should be successful', function(done) {
        run('node ./src/main.js -V', {pipe: DEBUG_TESTS})
        .then(function(exitCode) {
            assert.strictEqual(exitCode, 0);
            done();
        });
    });

    it('two successful commands should exit 0', function(done) {
        run('node ./src/main.js "echo" "echo"', {pipe: DEBUG_TESTS})
        .then(function(exitCode) {
            assert.strictEqual(exitCode, 0);
            done();
        });
    });

    it('at least one unsuccessful commands should exit non-zero', function(done) {
        run('node ./src/main.js "echo" "exit 1" "echo"', {pipe: DEBUG_TESTS})
        .then(function(exitCode) {
            assert.notStrictEqual(exitCode, 0);
            done();
        });
    });

    it('--kill-others should kill other commands if one dies', function(done) {
        // This test would timeout if kill others option does not work
        run('node ./src/main.js --kill-others "sleep 1000" "echo" "sleep 1000"', {pipe: DEBUG_TESTS})
        .then(function(exitCode) {
            assert.notStrictEqual(exitCode, 0);
            done();
        });
    });

    it('--success=first should return first exit code', function(done) {
        run('node ./src/main.js -k --success first "echo" "sleep 1000" ', {pipe: DEBUG_TESTS})
        // When killed, sleep returns null exit code
        .then(function(exitCode) {
            assert.strictEqual(exitCode, 0);
            done();
        });
    });

    it('--success=last should return last exit code', function(done) {
        // When killed, sleep returns null exit code
        run('node ./src/main.js -k --success last "echo" "sleep 1000" ', {pipe: DEBUG_TESTS})
        .then(function(exitCode) {
            assert.notStrictEqual(exitCode, 0);
            done();
        });
    });

});

function resolve(relativePath) {
    return path.join(testDir, relativePath);
}

