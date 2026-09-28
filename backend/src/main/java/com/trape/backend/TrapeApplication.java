package com.trape.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TrapeApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(TrapeApplication.class, args);
    }

    /**
     * Automatically loads .env file properties into System properties if not already set by OS environment.
     */
    private static void loadDotenv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("backend/.env");
        }
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not load .env file: " + e.getMessage());
            }
        }
    }
}
