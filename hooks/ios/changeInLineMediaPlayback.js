var fs = require('fs');
var path = require('path');

const constants={
    cordovaSrcPath : path.join("platforms","ios","CordovaLib","Classes"),
    pluginID : path.join("com","outsystems","misnap")
}

module.exports = function (context) {
    
    console.log("Start changing Files!");
    let Q = require("q");
    let deferral = new Q.defer();


    const fileToChangePath = path.join(constants.cordovaSrcPath,"Private","Plugins","CDVWebViewEngine","CDVWebViewEngine.m");
    let content = fs.readFileSync(fileToChangePath,"utf8");

    const regexWkWebviewChanger = /\[settings.*AllowInlineMediaPlayback.*\]/g;

    content = content.replace(regexWkWebviewChanger,"YES")

    fs.writeFileSync(fileToChangePath,content);

    console.log("Finished changing Files!");
    
    deferral.resolve();

    return deferral.promise;
}