package orangehrm;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static orangehrm.CommonUtils.BASE_URL;

/**
 * Simulation de test de performance pour OrangeHRM
 * Basé sur le cahier des charges avec les 3 parcours utilisateurs suivants :
 * - Parcours 1 (75%) : Login => Search Employee by name => Employee Contact => Logout
 * - Parcours 2 (20%) : Login => Add Employee => Employee Details => Logout
 * - Parcours 3 (5%)  : Login => Search Employee By Id => Delete Employee => Logout
 */
public class OrangeHRMSimulationV3 extends Simulation {

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

    // Parcours 1: Login => Search Employee by name => Employee Contact => Logout (75%)
    private final ScenarioBuilder searchEmployeeByNameScenario = scenario("Search Employee by Name Scenario")
            .exec(CommonChainBuilders.login())
            .pause(CommonUtils.randomPause())
            .feed(employeeNamesFeeder)
            .exec(SearchEmployeeChainBuilders.searchEmployeeByName())
            .pause(CommonUtils.randomPause())
            .feed(contactDetailsFeeder)
            .exec(SearchEmployeeChainBuilders.viewEmployeeContact())
            .pause(CommonUtils.randomPause())
            .exec(CommonChainBuilders.logout());

    // Parcours 2: Login => Add Employee => Employee Details => Logout (20%)
    private final ScenarioBuilder addEmployeeScenario = scenario("Add Employee Scenario")
            .exec(CommonChainBuilders.login())
            .pause(CommonUtils.randomPause())
            .feed(newEmployeesFeeder)
            .exec(AddEmployeeChainBuilders.navigateToAddEmployee())
            .pause(CommonUtils.randomPause())
            .exec(AddEmployeeChainBuilders.addNewEmployee())
            .pause(CommonUtils.randomPause())
            .exec(AddEmployeeChainBuilders.fillEmployeeDetails())
            .pause(CommonUtils.randomPause())
            .exec(CommonChainBuilders.logout());

    // Parcours 3: Login => Search Employee By id => Delete Employee => Logout (5%)
    private final ScenarioBuilder deleteEmployeeScenario = scenario("Delete Employee Scenario")
            .exec(CommonChainBuilders.login())
            .pause(CommonUtils.randomPause())
            .feed(employeeIdsFeeder)
            .exec(DeleteEmployeeChainBuilders.searchEmployeeById())
            .pause(CommonUtils.randomPause())
            .exec(DeleteEmployeeChainBuilders.deleteEmployee())
            .pause(CommonUtils.randomPause())
            .exec(CommonChainBuilders.logout());

    // Configuration de la simulation avec répartition de charge
    {
        // Pour un test initial avec un seul utilisateur par scénario
        //setUp(
                //searchEmployeeByNameScenario.injectOpen(atOnceUsers(1)).protocols(httpProtocol)
                //addEmployeeScenario.injectOpen(atOnceUsers(2)).protocols(httpProtocol)
                //deleteEmployeeScenario.injectOpen(atOnceUsers(1)).protocols(httpProtocol)
        //);

        // Une fois que le test fonctionne, vous pourrez décommenter cette section pour la répartition de charge réelle

        setUp(
                //searchEmployeeByNameScenario.injectOpen(rampUsers(15).during(30)).protocols(httpProtocol) // 75%
                //addEmployeeScenario.injectOpen(rampUsers(4).during(30)).protocols(httpProtocol),          // 20%
                //deleteEmployeeScenario.injectOpen(rampUsers(1).during(2)).protocols(httpProtocol)         // 5%


                // Scénario 1 : Recherche par nom (75%)
                searchEmployeeByNameScenario.injectOpen(
                        //constantUsersPerSec(1).during(20),    // Palier 1: 1 utilisateur/sec pendant 2 minutes
                        rampUsersPerSec(0).to(10).during(20),   // Rampe de 1 à 2 utilisateurs/sec pendant une minute
                        constantUsersPerSec(5).during(20)     // Palier 2: 2 utilisateurs/sec, deux minutes
                ).protocols(httpProtocol),

                // Scénario 2 : Ajout d'employé (20%)
                addEmployeeScenario.injectOpen(
                        //constantUsersPerSec(0.2).during(20),  // Palier 1: 1 utilisateur toutes les 2 sec pendant 2 minutes
                        rampUsersPerSec(1).to(2).during(20), // Rampe de 0.5 à 1 utilisateur/sec pendant 1 minute
                        constantUsersPerSec(1).during(20)     // Palier 2: 1 utilisateur/sec pendant 2 minutes
                ).protocols(httpProtocol),

                // Scénario 3 : Suppression d'employé (5%)
                deleteEmployeeScenario.injectOpen(
                        constantUsersPerSec(2).during(20)   // Constant : 1 utilisateur toutes les 5 sec pendant 5 minutes
                ).protocols(httpProtocol)
        );

    }
}
