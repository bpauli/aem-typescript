package de.bpauli.aem.compiler.ts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.clientlibs.script.CompilerContext;
import com.adobe.granite.ui.clientlibs.script.ScriptCompiler;
import com.adobe.granite.ui.clientlibs.script.ScriptResource;
import jdk.nashorn.api.scripting.NashornScriptEngine;

@Component
@Service(ScriptCompiler.class)
public class TsCompilerImpl implements ScriptCompiler {

    private static final Logger log = LoggerFactory.getLogger(TsCompilerImpl.class);
    private static final String JS_MIME_TYPE = "application/javascript";
    private static final String TS_EXTENSION = "ts";
    private static final String JS_EXTENSION = "js";


    private final NashornScriptEngine js;
    private String tsVersion;

    public TsCompilerImpl() throws IOException, ScriptException {
        long t0 = System.currentTimeMillis();
        this.tsVersion = getTsVersion();
        this.js = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");

        try {
            loadTsLib("tsc.js");
        } catch (final ScriptException e) {
            if (!e.getMessage().contains("Cannot read property \"args\" from undefined")) {
                throw e;
            }
        }

        final Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("jtsc", this);
        bindings.put("jtsc_repackArgs", js.eval("(function () { return arguments; })"));
        loadLocalLib("JVMSystem.js");

        long t1 = System.currentTimeMillis();
        log.info("Initialized TypeScript Compiler in {}ms", t1 - t0);
    }

    public void compile(Collection<ScriptResource> src, Writer dst, CompilerContext ctx) throws IOException {
        long t0 = System.currentTimeMillis();

        final Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        final Object ts = bindings.get("ts");

        for (ScriptResource r : src) {
            Path tempDirectory = Files.createTempDirectory("ts-");
            Path tsFile = Files.createTempFile(tempDirectory, "ts-", ".ts");
            Path jsFile = Files.createTempFile(tempDirectory, "ts-", ".js");
            FileOutputStream outputStream = new FileOutputStream(tsFile.toFile());
            IOUtils.copy(r.getReader(), outputStream);
            try {
                String[] arguments = {tsFile.toString(), "--outFile", jsFile.toString()};
                final Object repackedArguments = js.invokeFunction("jtsc_repackArgs", (Object[]) arguments);
                js.invokeMethod(ts, "executeCommandLine", repackedArguments);
                final Object exitCode = js.eval("ts.sys.exitCode");
                if (exitCode != null && exitCode instanceof Integer) {
                    log.warn(String.valueOf(exitCode));
                }
            } catch (ScriptException | NoSuchMethodException e) {
                log.error(e.getMessage());
            }
            FileInputStream inputStream = new FileInputStream(jsFile.toFile());
            IOUtils.copy(inputStream, dst);
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
        dst.close();
        long t1 = System.currentTimeMillis();
        log.info("Compile TypeScript in {}ms", t1 - t0);
    }

    private String getTsVersion() {
        try (final InputStream in = TsCompilerImpl.class.getResourceAsStream("tsc.properties")) {
            final Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("org.typescriptlang.typescript.version");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public String getName() {
        return TS_EXTENSION;
    }

    public boolean handles(String extension) {
        return StringUtils.equals(extension, TS_EXTENSION) || StringUtils.equals(extension, "." + TS_EXTENSION);
    }

    public String getMimeType() {
        return JS_MIME_TYPE;
    }

    public String getOutputExtension() {
        return JS_EXTENSION;
    }

    private void loadLocalLib(final String filename) throws IOException, ScriptException {
        try (final InputStream in = getClass().getResourceAsStream(filename)) {
            final InputStreamReader reader = new InputStreamReader(in);
            js.eval(reader);
        }
    }

    private void loadTsLib(final String filename) throws IOException, ScriptException {
        js.eval(readTsLib(filename));
    }

    public String readTsLib(final String filename) throws IOException {
        final String tsLibPath = Paths.get("META-INF", "resources", "webjars", "typescript", tsVersion, "lib", filename).toString();
        try (final InputStream in = TsCompilerImpl.class.getClassLoader().getResourceAsStream(tsLibPath)) {
            final InputStreamReader reader = new InputStreamReader(in);
            final BufferedReader buf = new BufferedReader(reader);
            return buf.lines().collect(Collectors.joining("\n"));
        }
    }
}
