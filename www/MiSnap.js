var exec = require('cordova/exec');

exports.startFacialCapture = function (configs, success, error) {
    exec(success, error, 'MiSnap', 'startFacialCapture', [configs]);
};

exports.requestPermission = function (success, error) {
    exec(success, error, 'MiSnap', 'requestPermission', []);
};
exports.checkPermission = function (success, error) {
    exec(success, error, 'MiSnap', 'checkPermission', []);
};
