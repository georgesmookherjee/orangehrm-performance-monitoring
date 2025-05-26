package io.gatling.demo;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.gatling.demo.CommonUtils.*;

/**
 * Chaînes d'actions communes à tous les parcours
 */
public class CommonChainBuilders {

    /**
     * Étape commune : Login
     */
    public static ChainBuilder login() {
        return exec(
                http("Open Login Page")
                        .get("/web/index.php/auth/login")
                        .check(status().is(200))
                        .check(regex(":token=\"&quot;(.*?)&quot;").saveAs("csrf_token"))
        )
                .exec(session -> {
                    // Débogage : afficher le token pour vérification
                    System.out.println("csrf_token: " + session.getString("csrf_token"));
                    return session;
                })
                .exec(
                        http("Submit Login")
                                .post("/web/index.php/auth/validate")
                                .formParam("_token", "#{csrf_token}")
                                .formParam("username", ADMIN_USERNAME)
                                .formParam("password", ADMIN_PASSWORD)
                                .check(status().in(200, 302))
                );
    }

    /**
     * Étape commune : Logout
     */
    public static ChainBuilder logout() {
        return exec(
                http("Logout")
                        .get("/web/index.php/auth/logout")
                        .check(status().in(200, 302))
        );
    }

    /**
     * Étape commune : Naviguer vers la liste des employés
     */
    public static ChainBuilder navigateToEmployeeList() {
        return exec(
                http("Navigate to Employee List")
                        .get("/web/index.php/pim/viewPimModule")
                        .check(status().is(200))
        );
    }
}
