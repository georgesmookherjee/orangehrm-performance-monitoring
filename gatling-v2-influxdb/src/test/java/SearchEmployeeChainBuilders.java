package io.gatling.demo;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.gatling.demo.CommonUtils.HEADERS_JSON;

/**
 * Chaînes d'actions pour le parcours 1 : Recherche d'employé par nom et visualisation des coordonnées
 */
public class SearchEmployeeChainBuilders {

    /**
     * Parcours 1 : Recherche employe par nom
     */
    public static ChainBuilder searchEmployeeByName() {
        // Utiliser directement la méthode navigateToEmployeeList de la classe CommonChainBuilders
        return exec(CommonChainBuilders.navigateToEmployeeList())
                .pause(CommonUtils.randomPause())
                .exec(session -> {
                    // Log pour indiquer la recherche d'employé par nom
                    // Ne pas redéclarer les variables firstName et lastName
                    System.out.println("🔍 Recherche de l'employé par nom : " +
                            session.getString("firstName") + " " +
                            session.getString("lastName"));
                    return session;
                })
                .exec(http("Search Employee by Name")
                        .get("/web/index.php/api/v2/pim/employees?nameOrId=#{firstName}&includeEmployees=onlyCurrent")
                        .check(status().is(200))
                        .check(jsonPath("$.data[0].empNumber").optional().saveAs("empNumber"))
                        .check(jsonPath("$.data[0].firstName").optional().saveAs("foundFirstName"))
                        .check(jsonPath("$.data[0].lastName").optional().saveAs("foundLastName"))
                )
                .exec(session -> {
                    // Log pour indiquer si l'employé a été trouvé ou non
                    if (session.contains("empNumber")) {
                        // Ne pas redéclarer les variables
                        System.out.println("Employé trouvé : " +
                                session.getString("foundFirstName") + " " +
                                session.getString("foundLastName") +
                                " (empNumber: " + session.getString("empNumber") + ")");
                    } else {
                        System.out.println("Aucun employé trouvé avec ce nom");
                    }
                    return session;
                });
    }

    /**
     * Parcours 1 : Voir et modifier les coordonnées de l'employé
     */
    public static ChainBuilder viewEmployeeContact() {
        return exec(session -> {
            // Si l'employé n'a pas été trouvé, utiliser un ID par défaut
            if (!session.contains("empNumber")) {
                System.out.println("Utilisation d'un ID par défaut (3) car l'employé n'a pas été trouvé");
                return session.set("empNumber", "3"); // ID par défaut
            }
            return session;
        })
                .exec(
                        http("View Employee Details")
                                .get("/web/index.php/pim/viewPersonalDetails/empNumber/#{empNumber}")
                                .check(status().in(200, 404))
                                // Optionnel: extraire l'employeeId s'il n'est pas déjà dans la session
                                //.check(jsonPath("$.data.employeeId").optional().saveAs("employeeId"))
                )
                .pause(CommonUtils.randomPause())
                .exec(session -> {
                    // Log indiquant la consultation des coordonnées
                    String empNumber = session.getString("empNumber");
                    // Récupérer l'employeeId s'il existe dans la session
                    //String employeeId = session.contains("employeeId") ? session.getString("employeeId") : "N/A";
                    System.out.println("Accès à la page de coordonnées de l'empNumber: " + empNumber );
                    return session;
                })
                .exec(
                        http("Access Employee Contact Details Page")
                                .get("/web/index.php/pim/contactDetails/empNumber/#{empNumber}")
                                .check(status().in(200, 404))
                )
                .pause(CommonUtils.randomPause())
                // Récupérer les coordonnées actuelles et l'employeeId si pas encore disponible
                .exec(
                        http("Get Current Contact Details")
                                .get("/web/index.php/api/v2/pim/employee/#{empNumber}/contact-details")
                                .check(status().in(200, 404))
                                // Si employeeId n'est pas encore extrait, essayer ici
                                //.check(jsonPath("$.meta.employeeId").optional().saveAs("employeeId"))
                )
                .pause(CommonUtils.randomPause())
                .exec(session -> {
                    // Récupérer les informations de l'employé trouvé
                    String firstName = "";
                    String lastName = "";

                    // Essayer de récupérer le nom trouvé lors de la recherche
                    if (session.contains("foundFirstName") && session.contains("foundLastName")) {
                        // Protection contre les valeurs nulles
                        firstName = session.getString("foundFirstName");
                        firstName = (firstName != null) ? firstName.toLowerCase() : "";

                        lastName = session.getString("foundLastName");
                        lastName = (lastName != null) ? lastName.toLowerCase() : "";
                    } else {
                        // Si pas trouvé, utiliser les valeurs par défaut de la recherche
                        firstName = session.contains("firstName") ? session.getString("firstName") : "user";
                        firstName = (firstName != null) ? firstName.toLowerCase() : "user";

                        lastName = session.contains("lastName") ? session.getString("lastName") : "default";
                        lastName = (lastName != null) ? lastName.toLowerCase() : "default";
                    }

                    // Nettoyer les noms (enlever les espaces, accents, etc.)
                    firstName = firstName.replaceAll("[^a-z]", "");
                    lastName = lastName.replaceAll("[^a-z]", "");

                    // Générer un timestamp
                    String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);

                    // Créer l'email avec prénom+nom+timestamp
                    String newEmail = firstName + lastName + timestamp + "@example.com";

                    // Mettre à jour la session avec le nouvel email
                    return session.set("workEmail", newEmail);
                })
                .exec(session -> {
                    // Log pour indiquer la modification des coordonnées
                    String empNumber = session.getString("empNumber");
                    //String employeeId = session.contains("employeeId") ? session.getString("employeeId") : "N/A";
                    String street = session.getString("street");
                    String city = session.getString("city");
                    String state = session.getString("state");
                    String zipCode = session.getString("zipCode");
                    String country = session.getString("country");
                    String mobile = session.getString("mobile");
                    String workEmail = session.getString("workEmail");

                    System.out.println("Modification des coordonnées pour l'empNumber: " + empNumber);
                    System.out.println("   - Adresse: " + street + ", " + city + ", " + state + " " + zipCode);
                    System.out.println("   - Pays: " + country);
                    System.out.println("   - Téléphone: " + mobile);
                    System.out.println("   - Email: " + workEmail);

                    return session;
                })
                // Sauvegarde des nouvelles coordonnées avec le chemin API correct et au format correct
                .exec(
                        http("Save Employee Contact Details")
                                .put("/web/index.php/api/v2/pim/employee/#{empNumber}/contact-details")
                                .headers(HEADERS_JSON)
                                .body(StringBody(session -> {
                                    String street = session.getString("street");
                                    String city = session.getString("city");
                                    String state = session.getString("state");
                                    String zipCode = session.getString("zipCode");
                                    String country = session.getString("country");
                                    String mobile = session.getString("mobile");
                                    String workEmail = session.getString("workEmail");

                                    return "{"
                                            + "\"street1\":\"" + street + "\","
                                            + "\"street2\": null,"  // null au lieu de chaîne vide
                                            + "\"city\":\"" + city + "\","
                                            + "\"province\":\"" + state + "\","
                                            + "\"zipCode\":\"" + zipCode + "\","
                                            + "\"countryCode\":\"" + country + "\","  // Utiliser countryCode et garder les guillemets
                                            + "\"homeTelephone\": null,"  // null au lieu de chaîne vide
                                            + "\"mobile\":\"" + mobile + "\","
                                            + "\"workTelephone\": null,"  // null au lieu de chaîne vide
                                            + "\"workEmail\":\"" + workEmail + "\","
                                            + "\"otherEmail\": null"  // null au lieu de chaîne vide
                                            + "}";
                                }))
                                .check(status().in(200, 201, 204))
                )
                .exec(session -> {
                    // Log de confirmation que les coordonnées ont été sauvegardées
                    String empNumber = session.getString("empNumber");
                    //String employeeId = session.contains("employeeId") ? session.getString("employeeId") : "N/A";
                    System.out.println("Coordonnées sauvegardées avec succès pour l'empNumber: " + empNumber + " " + session.getString("foundFirstName") + " " + session.getString("foundLastName"));
                    return session;
                });
    }
}
