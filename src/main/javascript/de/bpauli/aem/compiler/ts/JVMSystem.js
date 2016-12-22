/* globals ts, jtsc, Java */

ts.sys = (function getSystem() {
    "use strict";

    var executingFilePathMagic = "$$_EXECUTING_FILE_PATH_$$";

    var JavaString = Java.type("java.lang.String");
    var System = Java.type("java.lang.System");
    var Files = Java.type("java.nio.file.Files");
    var Paths = Java.type("java.nio.file.Paths");

    var useCaseSensitiveFileNames = !isWindows();

    function isWindows() {
        var osName = System.getProperty("os.name");
        return osName && osName.startsWith("Windows");
    }

    function fileExists(path) {
        return Files.isRegularFile(Paths.get(path));
    }

    function directoryExists(path) {
        return Files.isDirectory(Paths.get(path));
    }

    function getCanonicalPath(path) {
        return useCaseSensitiveFileNames ? path : path.toLowerCase();
    }

    function print(s) {
        System.out.print(s);
    }

    function readFile(path, encoding) {
        if (path.startsWith(executingFilePathMagic)) {
            return jtsc.readTsLib(path.substr(executingFilePathMagic.length));
        }
        var bytes = Files.readAllBytes(Paths.get(path));
        if (encoding) {
            return new JavaString(bytes, encoding);
        }
        else if (bytes.length < 2) {
            return new JavaString(bytes, "UTF-8");
        }
        else {
            /* jshint bitwise: false */
            var byte0 = bytes[0] & 0xFF;
            var byte1 = bytes[1] & 0xFF;
            var charset = byte0 === 0xFF && byte1 === 0xFE || byte0 === 0xFE && byte1 === 0xFF ? "UTF-16" : "UTF-8";
            /* jshint bitwise: true */
            return new JavaString(bytes, charset);
        }
    }

    function writeFile(path, data, writeByteOrderMark) {
        if (writeByteOrderMark) {
            data = "\uFEFF" + data;
        }
        Files.write(Paths.get(path), new JavaString(data).getBytes("UTF-8"));
    }

    function resolvePath(path) {
        return Paths.get(path).toAbsolutePath().toString();
    }

    function createDirectory(path) {
        Files.createDirectories(Paths.get(path));
    }

    function getExecutingFilePath() {
        return executingFilePathMagic + "/tsc.js";
    }

    function getCurrentDirectory() {
        return Paths.get("").toAbsolutePath().toString();
    }

    return {
        newLine: System.lineSeparator(),
        useCaseSensitiveFileNames: useCaseSensitiveFileNames,
        write: print,
        readFile: readFile,
        writeFile: writeFile,
        watchFile: undefined,
        watchDirectory: undefined,
        resolvePath: resolvePath,
        fileExists: fileExists,
        directoryExists: directoryExists,
        createDirectory: createDirectory,
        getExecutingFilePath: getExecutingFilePath,
        getCurrentDirectory: getCurrentDirectory,
        readDirectory: undefined,
        getModifiedTime: undefined,
        createHash: undefined,
        getMemoryUsage: undefined,
        exitCode: 0,
        exit: function (exitCode) {
            this.exitCode = exitCode;
        }
    };

}());