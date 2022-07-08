var fs = require('fs');
var path = require('path');

const constants={
    javaSrcPath : path.join("platforms","android","app","src","main","java"),
    kotlinSrcPath : path.join("platforms","android","app","src","main","kotlin"),
    pluginPackage : path.join("com","outsystems","misnap"),
    pluginID : "com-outsystems-MiSnap",
    pluginSrc : path.join("src","android")
}

module.exports = function (context) {
    
    console.log("Start changing Files!");
    var Q = require("q");
    var deferral = new Q.defer();


    var rawConfig = fs.readFileSync("config.xml", 'ascii');
    var match = /^<widget[\s|\S]* id="([\S]+)".+?>$/gm.exec(rawConfig);
    if(!match || match.length != 2){
        throw new Error("id parse failed");
    }

    const appId = match[1];

    const appFolder = appId.replace(/\./g,"/");

    var pathArray = [{"file":"MainActivity.java","srcPath":constants.javaSrcPath}]
    
    pathArray.forEach((value)=>{
        destFilePath = path.join(value.srcPath,appFolder,value.file)
        srcFilePath = path.join("plugins",constants.pluginID,constants.pluginSrc,value.file)

        if (fs.existsSync(srcFilePath)) {
            var content = fs.readFileSync(srcFilePath, "utf8");
            var toAlterContent = fs.readFileSync(destFilePath, "utf8");

            var variablesToAdd = new RegExp("var (.*)\\n","g");
            var importsToAdd = new RegExp("(import .*\\n)","g");
            var functionsToAddOrAppend = new RegExp("function([\\s|\\S]*)end function","g");

            var currentVariables;
            while(currentVariables = variablesToAdd.exec(content)){
                var current = currentVariables[1];
                toAlterContent = toAlterContent.replace(/CordovaActivity\n{/g,"CordovaActivity\n{\n"+current+"\n")
            }

            var currentImports;
            while(currentImports = importsToAdd.exec(content)){
                var current = currentImports[1];
                toAlterContent = toAlterContent.replace("import android.os.Bundle;","import android.os.Bundle;\n"+current+"\n")
            }

            var currentFunctions;
            while(currentFunctions = functionsToAddOrAppend.exec(content)){
                var current = currentFunctions[1];
                var header = current.split("\n")[0];
                current = current.replace(header, "")
                if(toAlterContent.includes(header)){

                    header = header.replace(")","\\)")
                    header = header.replace("(","\\(")
                    var regexFunction = new RegExp("(super\\.onCreate\\(savedInstanceState\\);)")
                    toAlterContent = toAlterContent.replace(regexFunction,(match,group1)=>{
                        return group1+current
                    })

                }else{
                    toAlterContent = toAlterContent.replace(/CordovaActivity\n{/g,"CordovaActivity\n{\n"+header+"{\n"+current+"}\n")
                }
            }

            fs.writeFileSync(destFilePath, toAlterContent);
            console.log("Finished changing "+path.basename(value.file)+"!");
        }else{
            console.error("Error could not find "+path.basename(value.file)+"!");
        }
    })

    console.log("Finished changing Files!");
    
    deferral.resolve();

    return deferral.promise;
}