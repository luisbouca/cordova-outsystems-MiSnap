var exec = require('cordova/exec');

exports.startFacialCapture = function (configs, success, error) {
    exec(success, error, 'MiSnap', 'startFacialCapture', [configs]);
};
