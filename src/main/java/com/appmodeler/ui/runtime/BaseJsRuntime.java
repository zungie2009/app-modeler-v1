package com.appmodeler.ui.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Composes default.js -> declared libraries -> selected layout.js. */
public final class BaseJsRuntime {

    private static final Path DEFAULT_JS =
            Path.of("config", "ui", "js", "default.js");

    public String compose(
            Path selectedLayout,
            String libraryJavascript) throws IOException {

        StringBuilder result = new StringBuilder();
        result.append(Files.readString(DEFAULT_JS, StandardCharsets.UTF_8));

        if (libraryJavascript != null && !libraryJavascript.isBlank()) {
            result.append("\n\n/* ---- declared UI libraries ---- */\n")
                    .append(libraryJavascript);
        }

        Path layoutJs = selectedLayout.resolve("layout.js");
        if (Files.isRegularFile(layoutJs)) {
            result.append("\n\n/* ---- selected layout.js ---- */\n")
                    .append(Files.readString(layoutJs, StandardCharsets.UTF_8));
        }
        return result.toString();
    }
}
