'use strict';

var module = angular.module('keycloak.controllers', [ 'keycloak.services' ]);

Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};




function randomString(len) {
    var charSet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    var randomString = '';
    for (var i = 0; i < len; i++) {
        var randomPoz = Math.floor(Math.random() * charSet.length);
        randomString += charSet.substring(randomPoz,randomPoz+1);
    }
    return randomString;
}

function getAvailableRoles(roles, systemRoles){
    var complement = [];

    for (var i = 0; i < roles.length; i++){
        var roleName = roles[i].name;

        if (systemRoles.indexOf(roleName) < 0){
            complement.push(roleName);
        }
    }

    return complement;
}
