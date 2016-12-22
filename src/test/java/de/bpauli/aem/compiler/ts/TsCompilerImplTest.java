package de.bpauli.aem.compiler.ts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.granite.ui.clientlibs.script.CompilerContext;
import com.adobe.granite.ui.clientlibs.script.ScriptResource;

import static org.junit.Assert.assertEquals;

public class TsCompilerImplTest {

    @Test
    public void testGreeting() throws Exception {
        runCompilerTest("greeting");
    }

    @Test
    public void testRayTracer() throws Exception {
        runCompilerTest("raytracer");
    }


    private void runCompilerTest(String filename) throws Exception {
        CompilerContext ctx = Mockito.mock(CompilerContext.class);

        StringWriter out = new StringWriter();
        TsCompilerImpl compiler = new TsCompilerImpl();
        List<ScriptResource> resources = new ArrayList<>();
        resources.add(new ScriptResource() {
            @Override
            public String getName() {
                return filename;
            }

            @Override
            public Reader getReader() throws IOException {
                try {
                    return readFile(filename + ".ts");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public long getSize() {
                return 0;
            }
        });

        compiler.compile(resources, out, ctx);
        out.close();
        Reader reader = readFile(filename + ".js");
        BufferedReader buf = new BufferedReader(reader);
        String matchJavaScript = buf.lines().collect(Collectors.joining("\n"));
        String compiledTypeScript = out.toString().trim();
        assertEquals(matchJavaScript, compiledTypeScript);
    }

    private Reader readFile(String fileName) throws Exception {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
        return new InputStreamReader(in);

    }
}