package io.gatling.demo;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration; // Ajout de l'import pour Duration

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.demo.CommonUtils.BASE_URL;

/**
 * Simulation de test de performance pour OrangeHRM V4
 * Basé sur le cahier des charges avec la répartition de charge :
 * - Parcours 1 (75%) : Login => Search Employee by name => Employee Contact => Logout
 * - Parcours 2 (20%) : Login => Add Employee => Employee Details => Logout
 * - Parcours 3 (5%)  : Login => Search Employee By Id => Delete Employee => Logout

 * Modifications :
 * - Utilisation de randomSwitch pour la pondération 75/20/5
 * - Augmentation progressive des utilisateurs (stairs) pour identifier le seuil critique
 */
public class OrangeHRMSimulationV4 extends Simulation {

    // Configuration HTTP de base
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .inferHtmlResources()
            .acceptHeader("application/json, text/plain, */*")
            .acceptEncodingHeader("gzip, deflate, br")
            .acceptLanguageHeader("fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
            .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0");

    // Feeders pour les données de test
    private final FeederBuilder<String> employeeNamesFeeder = csv("employee_names_parcours_1.csv").random();
    private final FeederBuilder<String> employeeIdsFeeder = csv("employee_ids.csv").random();
    private final FeederBuilder<String> newEmployeesFeeder = csv("new_employees.csv").random();
    private final FeederBuilder<String> contactDetailsFeeder = csv("employee_contact_details.csv").random();

    // Définition des chaînes d'actions pour chaque parcours
    private final ChainBuilder parcoursComplet = group("Parcours Complet").on(
            exec(CommonChainBuilders.login())
                    .pause(CommonUtils.randomPause())  // Garde les pauses aléatoires
                    .exec(
                            randomSwitch().on(
                                    percent(75.0).then(
                                            group("Parcours 1").on(
                                                    feed(employeeNamesFeeder)
                                                            .exec(SearchEmployeeChainBuilders.searchEmployeeByName())
                                                            .pause(CommonUtils.randomPause())
                                                            .feed(contactDetailsFeeder)
                                                            .exec(SearchEmployeeChainBuilders.viewEmployeeContact())
                                            )
                                    ),
                                    percent(20.0).then(
                                            group("Parcours 2").on(
                                                    feed(newEmployeesFeeder)
                                                            .exec(AddEmployeeChainBuilders.navigateToAddEmployee())
                                                            .pause(CommonUtils.randomPause())
                                                            .exec(AddEmployeeChainBuilders.addNewEmployee())
                                                            .pause(CommonUtils.randomPause())
                                                            .exec(AddEmployeeChainBuilders.fillEmployeeDetails())
                                            )
                                    ),
                                    percent(5.0).then(
                                            group("Parcours 3").on(
                                                    feed(employeeIdsFeeder)
                                                            .exec(DeleteEmployeeChainBuilders.searchEmployeeById())
                                                            .pause(CommonUtils.randomPause())
                                                            .exec(DeleteEmployeeChainBuilders.deleteEmployee())
                                            )
                                    )
                            )
                    )
                    .pause(CommonUtils.randomPause())
                    .exec(CommonChainBuilders.logout())
    );

    // Scénario avec pace() pour contrôler le rythme
    ScenarioBuilder orangeHRMScenario = scenario("OrangeHRM Mixed Load Test")
            .during(Duration.ofSeconds(60)).on(  // Durée totale de 20 minutes
                    pace(Duration.ofSeconds(2))           // Un utilisateur toutes les 2 secondes
                            .exec(parcoursComplet)
            );

    // Configuration de la simulation
    {
        setUp(
                orangeHRMScenario.injectOpen(
                        rampUsers(10).during(Duration.ofSeconds(15))  // 70 utilisateurs sur 2 minutes
                ).protocols(httpProtocol)
        );
    }
}
