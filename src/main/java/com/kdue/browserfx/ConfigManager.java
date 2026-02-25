package com.kdue.browserfx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";
    private static final String APP_NAME = "BrowserFX";

    public static final List<String> AVAILABLE_MODELS = Arrays.asList(
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-3.5-turbo",
            "claude-3-haiku",
            "claude-3-sonnet",
            "gemini-pro"
    );

    public static final List<String> AVAILABLE_LANGUAGES = Arrays.asList(
            "English",
            "Spanish",
            "French",
            "German",
            "Vietnamese",
            "Chinese",
            "Japanese",
            "Korean"
    );

    private final Properties properties;
    private final Path configPath;

    public ConfigManager() {
        properties = new Properties();
        this.configPath = resolveConfigPath();
        load();
    }

    public ConfigManager(String customConfigPath) {
        properties = new Properties();
        this.configPath = Paths.get(customConfigPath);
        load();
    }

    private Path resolveConfigPath() {
        String customPath = System.getProperty("browserfx.config");
        if (customPath != null && !customPath.isBlank()) {
            LOGGER.info("Using custom config path: {}", customPath);
            return Paths.get(customPath);
        }

        String os = System.getProperty("os.name").toLowerCase();
        Path configDir;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                configDir = Paths.get(appData, APP_NAME);
            } else {
                configDir = Paths.get(System.getProperty("user.home"), APP_NAME);
            }
            LOGGER.debug("Windows config dir: {}", configDir);
        } else if (os.contains("mac")) {
            configDir = Paths.get(System.getProperty("user.home"), 
                "Library/Application Support/" + APP_NAME);
            LOGGER.debug("macOS config dir: {}", configDir);
        } else {
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isBlank()) {
                configDir = Paths.get(xdgConfig, APP_NAME.toLowerCase());
            } else {
                configDir = Paths.get(System.getProperty("user.home"), 
                    ".config/" + APP_NAME.toLowerCase());
            }
            LOGGER.debug("Linux config dir: {}", configDir);
        }

        return configDir.resolve(CONFIG_FILE);
    }

    private void load() {
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) {
                try (InputStream is = Files.newInputStream(configPath)) {
                    properties.load(is);
                    LOGGER.info("Configuration loaded from {}", configPath);
                }
            } else {
                createDefaultConfig();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration", e);
        }
    }

    private void createDefaultConfig() throws IOException {
        properties.setProperty("litellm.endpoint", "https://api.litellm.ai");
        properties.setProperty("litellm.model", "gpt-4o-mini");
        properties.setProperty("litellm.api_key", "");
        save();
        LOGGER.info("Default configuration created at {}", configPath);
    }

    public void save() {
        try (OutputStream os = Files.newOutputStream(configPath)) {
            properties.store(os, "BrowserFX Configuration");
            LOGGER.info("Configuration saved to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration", e);
        }
    }

    public String getLitellmEndpoint() {
        return properties.getProperty("litellm.endpoint", "https://api.litellm.ai");
    }

    public String getLitellmModel() {
        return properties.getProperty("litellm.model", "gpt-4o-mini");
    }

    public String getLitellmApiKey() {
        return properties.getProperty("litellm.api_key", "");
    }

    public void setLitellmEndpoint(String endpoint) {
        properties.setProperty("litellm.endpoint", endpoint);
    }

    public void setLitellmModel(String model) {
        properties.setProperty("litellm.model", model);
    }

    public void setLitellmApiKey(String apiKey) {
        properties.setProperty("litellm.api_key", apiKey);
    }

    public boolean isConfigured() {
        String apiKey = getLitellmApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    public Path getConfigPath() {
        return configPath;
    }

    public static String getConfigDirDescription() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return "%APPDATA%\\" + APP_NAME + "\\" + CONFIG_FILE;
        } else if (os.contains("mac")) {
            return "~/Library/Application Support/" + APP_NAME + "/" + CONFIG_FILE;
        } else {
            return "~/.config/" + APP_NAME.toLowerCase() + "/" + CONFIG_FILE;
        }
    }
}
