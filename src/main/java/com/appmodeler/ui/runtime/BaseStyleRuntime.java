package com.appmodeler.ui.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Composes default.css -> declared libraries -> selected layout.css. */
public final class BaseStyleRuntime {

    private static final Path DEFAULT_CSS =
            Path.of("config", "ui", "css", "default.css");

    public String compose(
            Path selectedLayout,
            String libraryCss) throws IOException {

        StringBuilder result = new StringBuilder();
        result.append(Files.readString(DEFAULT_CSS, StandardCharsets.UTF_8));

        if (libraryCss != null && !libraryCss.isBlank()) {
            result.append("\n\n/* ---- declared UI libraries ---- */\n")
                    .append(libraryCss);
        }

        Path layoutCss = selectedLayout.resolve("layout.css");
        if (Files.isRegularFile(layoutCss)) {
            result.append("\n\n/* ---- selected layout.css ---- */\n")
                    .append(Files.readString(layoutCss, StandardCharsets.UTF_8));
        }
        return result.toString();
    }
}
