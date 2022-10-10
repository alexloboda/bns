package ctlab.mc5;

import org.apache.commons.io.IOUtils;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class VersionProvider implements CommandLine.IVersionProvider {

    @Override
        public String[] getVersion() throws Exception {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            try (InputStream is = classloader.getResourceAsStream("ctlab/mc5/Version")) {
                return new String[]{IOUtils.toString(is, StandardCharsets.UTF_8)};
            }
        }
    }


