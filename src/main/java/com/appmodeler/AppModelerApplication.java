package com.appmodeler;

import com.appmodeler.ui.UiLibraryInterpreter;
import com.appmodeler.ui.runtime.BaseJsRuntime;
import com.appmodeler.ui.runtime.BaseStyleRuntime;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class AppModelerApplication {
    private static final int DEFAULT_PORT = 8091;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path SPECIALTIES = Path.of("config", "specialties");
    private static final Path UI_ROOT = Path.of("config", "ui");
    private static final Path LAYOUTS = UI_ROOT.resolve("layouts");
    private static final Path LIBRARIES = UI_ROOT.resolve("libraries");
    private static final Path SETTINGS = UI_ROOT.resolve("ui-settings.json");
    private static final Path GENERATED = Path.of("runtime", "index.html");

    private static final DomainInterpreter DOMAIN_INTERPRETER = new DomainInterpreter(JSON);
    private static final MenuInterpreter MENU_INTERPRETER = new MenuInterpreter();
    private static final LayoutInterpreter LAYOUT_INTERPRETER = new LayoutInterpreter(LAYOUTS);
    private static final HtmlAssembler HTML_ASSEMBLER = new HtmlAssembler();
    private static final UiLibraryInterpreter UI_LIBRARY_INTERPRETER =
            new UiLibraryInterpreter(LIBRARIES, JSON);

    private static volatile String activeFile;
    private static volatile Map<String, Object> domain = new LinkedHashMap<>();
    private static volatile List<Map<String, Object>> menu = List.of();
    private static volatile Map<String, Object> settings = new LinkedHashMap<>();

    private AppModelerApplication() {
    }

    public static void main(String[] args) throws Exception {
        loadSettings();
        List<Path> files = domainFiles();
        if (files.isEmpty()) {
            throw new IllegalStateException("No *-domain.json files in " + SPECIALTIES.toAbsolutePath());
        }

        activeFile = files.get(0).getFileName().toString();
        loadDomain(activeFile);
        generateUi();

        int port = resolvePort(args);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", AppModelerApplication::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("App Modeler v0.10: http://localhost:" + port);
        System.out.println("Generated UI: " + GENERATED.toAbsolutePath());
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                sendFile(exchange, GENERATED, "text/html; charset=utf-8");
                return;
            }
            if (path.equals("/vue.global.prod.js")) {
                sendResource(exchange, "/static/vue.global.prod.js", "text/javascript; charset=utf-8");
                return;
            }
if (path.equals("/api/domains")) {
                sendDomains(exchange);
                return;
            }
            if (path.equals("/api/layouts")) {
                json(exchange, 200, LAYOUT_INTERPRETER.layouts());
                return;
            }
            if (path.equals("/api/state")) {
                json(exchange, 200, Map.of(
                        "activeFile", activeFile,
                        "domain", domain,
                        "menu", menu,
                        "settings", settings));
                return;
            }
            if (path.equals("/api/data")) {
                sendEntityData(exchange);
                return;
            }
            if (path.equals("/api/apply") && exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                apply(exchange);
                return;
            }
            json(exchange, 404, Map.of("error", "Not found"));
        } catch (Exception error) {
            error.printStackTrace();
            json(exchange, 500, Map.of(
                    "error", error.getMessage() == null
                            ? error.getClass().getSimpleName()
                            : error.getMessage()));
        }
    }

    private static void sendEntityData(HttpExchange exchange) throws IOException {
        String entity = queryParameter(exchange, "entity");
        if (entity == null || entity.isBlank()) {
            json(exchange, 400, Map.of("error", "Missing entity parameter"));
            return;
        }

        Object entitiesObject = domain.get("entities");
        if (!(entitiesObject instanceof Map<?, ?> entities) || !entities.containsKey(entity)) {
            json(exchange, 404, Map.of("error", "Unknown entity: " + entity));
            return;
        }

        Object seedObject = domain.get("seedData");
        if (!(seedObject instanceof Map<?, ?> seedData)) {
            json(exchange, 200, List.of());
            return;
        }

        Object records = seedData.get(entity);
        json(exchange, 200, records instanceof List<?> list ? list : List.of());
    }

    private static String queryParameter(HttpExchange exchange, String name) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                return java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void sendDomains(HttpExchange exchange) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        for (Path file : domainFiles()) {
            String name = file.getFileName().toString();
            result.add(Map.of("file", name, "label", label(name)));
        }
        json(exchange, 200, result);
    }

    private static void apply(HttpExchange exchange) throws IOException {
        Map<String, Object> request = JSON.readValue(exchange.getRequestBody(), new TypeReference<>() {});
        String file = String.valueOf(request.get("domainFile"));
        boolean knownDomain = domainFiles().stream()
                .anyMatch(path -> path.getFileName().toString().equals(file));
        if (!knownDomain) {
            throw new IllegalArgumentException("Unknown domain file: " + file);
        }

        Object requestedSettings = request.get("settings");
        if (requestedSettings instanceof Map<?, ?> map) {
            settings = new LinkedHashMap<>();
            map.forEach((key, value) -> settings.put(String.valueOf(key), value));
            Files.createDirectories(SETTINGS.getParent());
            JSON.writerWithDefaultPrettyPrinter().writeValue(SETTINGS.toFile(), settings);
        }

        loadDomain(file);
        generateUi();
        json(exchange, 200, Map.of("ok", true));
    }

    private static void loadDomain(String file) throws IOException {
        activeFile = file;
        domain = DOMAIN_INTERPRETER.interpret(SPECIALTIES.resolve(file));
        menu = MENU_INTERPRETER.interpret(domain);
    }

    private static void generateUi() throws IOException {
        String layoutName =
                String.valueOf(settings.getOrDefault("contentLayout", "default"));
        LayoutInterpreter.LayoutBlocks layout =
                LAYOUT_INTERPRETER.interpret(layoutName);

        Path selectedLayout = LAYOUTS.resolve(layoutName).normalize();
        if (!Files.isDirectory(selectedLayout)) {
            selectedLayout = LAYOUTS.resolve("default");
        }

        UiLibraryInterpreter.LibraryAssets libraries =
                UI_LIBRARY_INTERPRETER.interpret(selectedLayout);

        String javascript = new BaseJsRuntime().compose(
                selectedLayout, libraries.javascript());
        String css = new BaseStyleRuntime().compose(
                selectedLayout, libraries.css());

        HTML_ASSEMBLER.assemble(GENERATED, css, layout, javascript);
    }

    private static void loadSettings() throws IOException {
        if (Files.exists(SETTINGS)) {
            settings = JSON.readValue(SETTINGS.toFile(), new TypeReference<>() {});
            return;
        }
        settings = new LinkedHashMap<>();
        settings.put("contentLayout", "default");
    }

    private static List<Path> domainFiles() throws IOException {
        if (!Files.isDirectory(SPECIALTIES)) {
            return List.of();
        }
        try (var stream = Files.list(SPECIALTIES)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith("-domain.json") || name.contains("-domain");
                    })
                    .sorted()
                    .toList();
        }
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // Use the default port.
            }
        }
        return DEFAULT_PORT;
    }

    private static String label(String file) {
        return file
                .replace(".json", "")
                .replace("-domain4", "")
                .replace("-domain", "")
                .replace('-', ' ')
                .replace('_', ' ');
    }

    private static void json(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] body = JSON.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void sendFile(HttpExchange exchange, Path path, String contentType) throws IOException {
        byte[] body = Files.readAllBytes(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void sendResource(HttpExchange exchange, String resource, String contentType) throws IOException {
        try (InputStream input = AppModelerApplication.class.getResourceAsStream(resource)) {
            if (input == null) {
                json(exchange, 404, Map.of("error", "Missing resource"));
                return;
            }
            byte[] body = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        }
    }
}
