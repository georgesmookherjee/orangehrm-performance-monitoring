package io.gatling.demo;

import java.util.Map;
import java.util.Random;

/**
 * Constantes et utilitaires partagés entre tous les fichiers de simulation
 */
public class CommonUtils {
    // Configuration de l'environnement
    public static final String BASE_URL = "http://localhost:8060";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "HGKJH$$kjiu1236";

    // Variables pour les temps de pause aléatoires (en secondes)
    public static final int MIN_PAUSE_TIME = 1;
    public static final int MAX_PAUSE_TIME = 5;

    // En-têtes JSON standards pour les requêtes
    public static final Map<CharSequence, String> HEADERS_JSON = Map.ofEntries(
            Map.entry("Content-Type", "application/json"),
            Map.entry("Origin", BASE_URL),
            Map.entry("Sec-Fetch-Dest", "empty"),
            Map.entry("Sec-Fetch-Mode", "cors"),
            Map.entry("Sec-Fetch-Site", "same-origin")
    );

    // Méthode utilitaire pour générer un temps de pause aléatoire
    public static int randomPause() {
        return MIN_PAUSE_TIME + new Random().nextInt(MAX_PAUSE_TIME - MIN_PAUSE_TIME + 1);
    }
}