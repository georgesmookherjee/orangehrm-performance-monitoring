package orangehrm;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Simulation Hello World ultra-simple pour tester Gatling 3.10.5
 * - Fait un simple GET sur httpbin.org
 * - 1 utilisateur pendant 10 secondes
 * - Parfait pour valider le setup !
 */
public class HelloWorldSimulation extends Simulation {

    // Configuration HTTP simple
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://httpbin.org")
            .acceptHeader("application/json")
            .userAgentHeader("Gatling Hello World Test");

    // Scénario ultra-simple
    private final ScenarioBuilder helloWorldScenario = scenario("Hello World Test")
            .exec(
                    http("GET Request")
                            .get("/get")
                            .check(status().is(200))
                            .check(jsonPath("$.url").exists())
            )
            .pause(Duration.ofSeconds(1));

    // Configuration de la simulation
    {
        setUp(
                helloWorldScenario.injectOpen(
                        atOnceUsers(1)  // 1 utilisateur en une fois
                ).protocols(httpProtocol)
        ).maxDuration(Duration.ofSeconds(30));  // Maximum 30 secondes
    }
}