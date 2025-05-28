package orangehrm;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static orangehrm.CommonUtils.HEADERS_JSON;

/**
 * Chaînes d'actions pour le parcours 3 : Recherche et suppression d'employé par ID
 */
public class DeleteEmployeeChainBuilders {

    /**
     * Parcours 3 : Recherche employé par ID
     */
    public static ChainBuilder searchEmployeeById() {
        return exec(CommonChainBuilders.navigateToEmployeeList())
                .pause(CommonUtils.randomPause())
                .exec(session -> {
                    // Débogage pour voir l'ID employé utilisé
                    String employeeId = session.getString("employeeId");
                    System.out.println("Recherche d'employé avec ID: " + employeeId);
                    return session;
                })
                .exec(
                        http("Search Employee by ID")
                                .get("/web/index.php/api/v2/pim/employees?limit=50&offset=0&model=detailed&employeeId=#{employeeId}&includeEmployees=onlyCurrent&sortField=employee.firstName&sortOrder=ASC")
                                .check(status().is(200))
                                // Vérifier si des données sont retournées
                                .check(jsonPath("$.data").transform(data -> {
                                    System.out.println("Résultat de la recherche: " + data);
                                    return data;
                                }).optional())
                                // Sauvegarder l'ID numérique interne (empNumber) pour la suppression
                                .check(jsonPath("$.data[0].empNumber").optional().saveAs("empNumberToDelete"))
                )
                .exec(session -> {
                    // Vérifier si l'ID a été trouvé
                    if (!session.contains("empNumberToDelete") || session.getString("empNumberToDelete") == null) {
                        System.out.println("⚠️ ATTENTION: Employé non trouvé dans la recherche, utilisation de l'ID directement");
                        // Si l'empNumber n'est pas trouvé, utiliser l'ID du feeder directement
                        // car dans votre CSV, les IDs sont déjà au format numérique correct
                        return session.set("empNumberToDelete", session.getString("employeeId"));
                    } else {
                        System.out.println("✓ Employé trouvé avec empNumber: " + session.getString("empNumberToDelete"));
                    }
                    return session;
                });
    }

    /**
     * Parcours 3 : Supprimer un employé
     */
    public static ChainBuilder deleteEmployee() {
        return exec(session -> {
            // Afficher l'ID pour vérification
            String empNumber = session.getString("empNumberToDelete");
            System.out.println("Préparation à la suppression de l'employé avec ID: " + empNumber);
            return session;
        })
                .exec(
                        http("Delete Employee")
                                .delete("/web/index.php/api/v2/pim/employees")
                                .headers(HEADERS_JSON)
                                .header("Priority", "u=0")
                                .body(StringBody(session -> {
                                    String empNumber = session.getString("empNumberToDelete");
                                    String requestBody = "{\"ids\":[" + empNumber + "]}";
                                    System.out.println("Corps de la requête de suppression: " + requestBody);
                                    return requestBody;
                                }))
                                .check(status().in(200, 204))
                )
                .exec(session -> {
                    System.out.println("✓ Suppression terminée avec succès pour l'employé: " + session.getString("empNumberToDelete"));
                    return session;
                });
    }
}
