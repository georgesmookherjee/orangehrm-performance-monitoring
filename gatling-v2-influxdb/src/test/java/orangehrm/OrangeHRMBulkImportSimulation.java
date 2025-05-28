package orangehrm;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Simulation pour l'import massif de 2000 employés dans OrangeHRM
 * Utilise le fichier employee_names.csv pour les données de base des employés
 */
public class OrangeHRMBulkImportSimulation extends Simulation {

    // Configuration de l'environnement
    private static final String BASE_URL = "http://localhost:8060"; // Remplacez par l'URL de votre instance OrangeHRM
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "HGKJH$$kjiu1236";
    // IDs des administrateurs à ne pas supprimer - à ajuster selon la configuration
    // private static final String ADMIN_IDS = "[1]"; // Admin par défaut a généralement l'ID 1

    // Configuration HTTP de base
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .inferHtmlResources()
            .acceptHeader("application/json, text/plain, */*")
            .acceptEncodingHeader("gzip, deflate, br")
            .acceptLanguageHeader("fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
            .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0");

    private final Map<CharSequence, String> headersJson = Map.ofEntries(
            Map.entry("Content-Type", "application/json"),
            Map.entry("Origin", BASE_URL),
            Map.entry("Sec-Fetch-Dest", "empty"),
            Map.entry("Sec-Fetch-Mode", "cors"),
            Map.entry("Sec-Fetch-Site", "same-origin")
    );

    // Feeder pour les données d'employés
    private final FeederBuilder<String> employeesFeeder = csv("bulk_init.csv").queue();

    // Scénario pour ajouter tous les employés
    private final ScenarioBuilder bulkAddEmployeeScenario = scenario("Bulk Add Employees Scenario")
            .exec(login())
            .pause(Duration.ofMillis(30))
            .repeat(220).on(feed(employeesFeeder)
                    .exec(navigateToAddEmployee())
                    .pause(Duration.ofMillis(30)) // Pause courte entre chaque ajout
                    .exec(addNewEmployee())
                    .pause(Duration.ofMillis(30))
            )
            .exec(logout());

    // Scénario 2 : Supprimer tous les employés (sauf les administrateurs)
    private final ScenarioBuilder bulkDeleteEmployeeScenario = scenario("Bulk Delete Employees Scenario")
            .exec(login())
            .pause(1)
            .exec(getAllEmployeeIds())
            .exec(session -> {
                System.out.println("Employés récupérés, prêt à les supprimer");
                return session;
            })
            .exec(deleteAllEmployees())
            .exec(logout());

    // Configuration de la simulation
    {
        setUp(
                bulkAddEmployeeScenario.injectOpen(atOnceUsers(1)).protocols(httpProtocol)
                //bulkDeleteEmployeeScenario.injectOpen(atOnceUsers(1)).protocols(httpProtocol)
        );
    }

    // Étapes du parcours utilisateur

    // Étape 1 : Login
    private ChainBuilder login() {
        return exec(http("Open Login Page")
                        .get("/web/index.php/auth/login")
                        .check(status().is(200))
                        .check(regex(":token=\"&quot;(.*?)&quot;").saveAs("csrf_token"))
        )
                .exec(session -> {
                    // Débogage : afficher le token pour vérification
                    System.out.println("csrf_token: " + session.getString("csrf_token"));
                    return session;
                })
                .exec(http("Submit Login")
                                .post("/web/index.php/auth/validate")
                                .formParam("_token", "#{csrf_token}") // À remplacer par un token valide ou un extracteur dynamique
                                .formParam("username", ADMIN_USERNAME)
                                .formParam("password", ADMIN_PASSWORD)
                                .check(status().in(200, 302))
                );
    }

    // Étape 2 : Naviguer vers la page d'ajout d'employé
    private ChainBuilder navigateToAddEmployee() {
        return exec(http("Navigate to Add Employee Page")
                        .get("/web/index.php/pim/addEmployee")
                        .check(status().is(200))
        );
    }

    // Étape 3 : Ajouter un nouvel employé
    private ChainBuilder addNewEmployee() {
        return exec(http("Validate Employee ID")
                        .get("/web/index.php/api/v2/core/validation/unique?value=#{employeeId}&entityName=Employee&attributeName=employeeId")
                        .check(status().is(200))
        )
                .exec(http("Add New Employee")
                                .post("/web/index.php/api/v2/pim/employees")
                                .headers(headersJson)
                                .body(StringBody(session -> {
                                    String firstName = session.getString("firstName");
                                    String lastName = session.getString("lastName");
                                    String empId = session.getString("employeeId");
                                    return "{\"firstName\":\"" + firstName + "\",\"middleName\":\"\",\"lastName\":\"" + lastName + "\",\"empPicture\":null,\"employeeId\":\"" + empId + "\"}";
                                }))
                                .check(status().in(200, 201))
                                .check(jsonPath("$.data.empNumber").optional().saveAs("empNumber"))
                );
    }

    // Étape pour récupérer les IDs de tous les employés, version corrigée
    private ChainBuilder getAllEmployeeIds() {
        return exec(http("Navigate to Employee List")
                .get("/web/index.php/pim/viewEmployeeList")
                .check(status().is(200))
        )
                .exec(http("Get All Employees")
                        .get("/web/index.php/api/v2/pim/employees?limit=0&offset=0&model=detailed&includeEmployees=onlyCurrent&sortField=employee.firstName&sortOrder=ASC")
                        .check(status().is(200))
                        // Extraire tous les IDs des employés sous forme de chaînes et les stocker
                        .check(jsonPath("$.data[*].empNumber").findAll().saveAs("allEmployeeIds"))
                        // Vérifier la présence du total et le sauvegarder avec une valeur par défaut
                        .check(jsonPath("$.meta.total").optional().saveAs("totalEmployeesRaw"))
                )
                .exec(session -> {
                    // Récupérer les IDs et les convertir en Integer en toute sécurité
                    java.util.List<?> rawEmpIds = session.getList("allEmployeeIds");
                    java.util.List<Integer> empIds = new java.util.ArrayList<>();

                    // Convertir chaque ID en Integer
                    for (Object rawId : rawEmpIds) {
                        try {
                            if (rawId instanceof String) {
                                empIds.add(Integer.parseInt((String) rawId));
                            } else if (rawId instanceof Integer) {
                                empIds.add((Integer) rawId);
                            } else if (rawId != null) {
                                empIds.add(Integer.parseInt(rawId.toString()));
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Impossible de convertir l'ID: " + rawId);
                        }
                    }

                    // Définir une valeur par défaut pour totalEmployees si elle est nulle
                    String totalRaw = session.getString("totalEmployeesRaw");
                    int totalEmployees = 0;
                    try {
                        totalEmployees = (totalRaw != null) ? Integer.parseInt(totalRaw) : empIds.size();
                    } catch (NumberFormatException e) {
                        totalEmployees = empIds.size();
                    }

                    System.out.println("Total des employés trouvés: " + totalEmployees);
                    System.out.println("IDs des employés: " + empIds);

                    // Mettre à jour la session avec les IDs convertis
                    return session.set("allEmployeeIdsConverted", empIds)
                            .set("totalEmployees", totalEmployees);
                });
    }

    // Étape pour supprimer tous les employés (sauf les administrateurs), version corrigée
    private ChainBuilder deleteAllEmployees() {
        return exec(session -> {
            java.util.List<Integer> allEmpIds;

            // Vérifier si la liste d'IDs convertis existe
            if (session.contains("allEmployeeIdsConverted")) {
                allEmpIds = session.getList("allEmployeeIdsConverted");
            } else {
                // Si la liste n'existe pas, créer une liste vide
                System.out.println("⚠️ Aucun ID d'employé trouvé, création d'une liste vide");
                allEmpIds = new java.util.ArrayList<>();
            }

            // Filtrer pour exclure les IDs d'administrateurs (généralement ID 1)
            java.util.List<Integer> empIdsToDelete = new java.util.ArrayList<>();
            for (Integer id : allEmpIds) {
                // Exclure l'ID 1 qui est généralement l'admin principal
                if (id != 1) {
                    empIdsToDelete.add(id);
                }
            }

            System.out.println("IDs à supprimer: " + empIdsToDelete);

            // Stocker la liste des IDs à supprimer
            return session.set("employeeIdsToDelete", empIdsToDelete);
        })
                .doIf(session -> {
                    java.util.List<Integer> idsToDelete = session.getList("employeeIdsToDelete");
                    return idsToDelete != null && !idsToDelete.isEmpty();
                }).then(
                        foreach("#{employeeIdsToDelete}", "empNumberToDelete").on(
                                exec(http("Delete Employee #{empNumberToDelete}")
                                        .delete("/web/index.php/api/v2/pim/employees")
                                        .headers(headersJson)
                                        .body(StringBody(session -> {
                                            String empId = session.getString("empNumberToDelete");
                                            return "{\"ids\":[" + empId + "]}";
                                        }))
                                        .check(status().in(200, 204))
                                )
                                        .pause(Duration.ofMillis(50)) // Pause courte entre chaque suppression
                        )
                );
    }

    // Étape 4: Logout
    private ChainBuilder logout() {
        return exec(http("Logout")
                        .get("/web/index.php/auth/logout")
                        .check(status().in(200, 302))
        );
    }
}