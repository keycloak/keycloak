"use strict";

var devIp = require("../lib/dev-ip");
var respNone = require("./fixtures/resp-none");
var respSingle = require("./fixtures/resp-single");
var respMultiple = require("./fixtures/resp-multiple");
var sinon = require("sinon");
var assert = require("chai").assert;
var os = require("os");

// From the resp files
var match1 = "10.104.103.181";
var match2 = "10.104.100.12";

var regex = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;

describe("Getting the IP with a single result", function () {
    var osStub;
    var result;
    before(function () {
        osStub = sinon.stub(os, "networkInterfaces").returns(respSingle);
    });
    beforeEach(function () {
        result = devIp(null);
    });
    after(function () {
        osStub.restore();
    });
    it("should return the IP when a single match found", function () {
        var expected = match1;
        assert.deepEqual(result, [expected]);
    });
    it("should return a string matching the regex", function () {
        var actual = regex.exec(result);
        assert.isNotNull(actual);
    });
});

describe("Getting the IP with Multiple results", function () {
    var osStub;
    var result;
    before(function () {
        osStub = sinon.stub(os, "networkInterfaces").returns(respMultiple);
    });
    beforeEach(function () {
        result = devIp(null);
    });
    after(function () {
        osStub.restore();
    });
    it("should return an array of results", function () {
        assert.equal(result[0], match1);
        assert.equal(result[1], match2);
    });
    it("should return a string matching the regex", function () {
        var actual = regex.exec(result[0]);
        assert.isNotNull(actual);
        actual = regex.exec(result[1]);
        assert.isNotNull(actual);
    });
});

describe("Getting the IP with no results", function () {
    var osStub;
    var result;
    before(function () {
        osStub = sinon.stub(os, "networkInterfaces").returns(respNone);
    });
    after(function () {
        osStub.restore();
    });
    it("should return empty array", function () {
        var actual = devIp();
        assert.isArray(actual);
        assert.equal(actual.length, 0);
    });
});
