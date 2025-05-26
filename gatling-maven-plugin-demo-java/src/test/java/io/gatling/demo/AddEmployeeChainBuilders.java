package io.gatling.demo;

import io.gatling.javaapi.core.ChainBuilder;

import java.util.Random;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.gatling.demo.CommonUtils.HEADERS_JSON;

/**
 * Chaînes d'actions pour le parcours 2 : Ajout d'un employé et détails personnels
 */
public class AddEmployeeChainBuilders {

    /**
     * Parcours 2 : Naviguer vers la page d'ajout d'employé
     */
    public static ChainBuilder navigateToAddEmployee() {
        return exec(
                http("Navigate to Add Employee Page")
                        .get("/web/index.php/pim/addEmployee")
                        .check(status().is(200))
        );
    }

    /**
     * Parcours 2 : Ajouter un nouvel employé
     */
    public static ChainBuilder addNewEmployee() {
        return exec(session -> {
            // Générer un ID d'employé numérique aléatoire
            // Utilisons un nombre entre 10000 et 99999 pour éviter les collisions
            int randomEmployeeId = new Random().nextInt(9000000) + 4000;
            System.out.println(randomEmployeeId);
            return session.set("randomEmployeeId", ( "9" + String.valueOf(randomEmployeeId)));
        })
                .exec(
                        http("Validate Employee ID")
                                .get("/web/index.php/api/v2/core/validation/unique?value=#{randomEmployeeId}&entityName=Employee&attributeName=employeeId")
                                .check(status().is(200))
                )
                .exec(
                        http("Add New Employee")
                                .post("/web/index.php/api/v2/pim/employees")
                                .headers(HEADERS_JSON)
                                .body(StringBody(session -> {
                                    String firstName = session.getString("firstName");
                                    String lastName = session.getString("lastName");
                                    String empId = session.getString("randomEmployeeId");
                                    return "{\"firstName\":\"" + firstName + "\",\"middleName\":\"\",\"lastName\":\"" + lastName + "\",\"empPicture\":null,\"employeeId\":\"" + empId + "\"}";
                                }))
                                .check(status().in(200, 201))
                                .check(jsonPath("$.data.empNumber").saveAs("empNumber"))
                );
    }

    /**
     * Parcours 2 : Compléter les détails de l'employé
     */
    public static ChainBuilder fillEmployeeDetails() {
        return exec(
                http("Access Employee Details Form")
                        .get("/web/index.php/pim/viewPersonalDetails/empNumber/#{empNumber}")
                        .check(status().in(200, 404))
        )
                .pause(CommonUtils.randomPause())
                .exec(
                        http("Save Employee Personal Details")
                                .put("/web/index.php/api/v2/pim/employees/#{empNumber}/personal-details")
                                .headers(HEADERS_JSON)
                                .body(StringBody(session -> {
                                    String firstName = session.getString("firstName");
                                    String lastName = session.getString("lastName");
                                    String dob = session.getString("dob");
                                    String gender = session.getString("gender").equals("M") ? "1" : "2";
                                    //String gender = session.getString("gender");
                                    String nationality = session.getString("nationality");
                                    String maritalStatus = session.getString("maritalStatus");
                                    String empId = session.getString("randomEmployeeId");

                                    return "{"
                                            + "\"lastName\":\"" + lastName + "\","
                                            + "\"firstName\":\"" + firstName + "\","
                                            + "\"middleName\":\"\","
                                            + "\"employeeId\":\"" + empId + "\","
                                            + "\"otherId\":\"\","
                                            + "\"drivingLicenseNo\":\"\","
                                            + "\"drivingLicenseExpiredDate\":null,"
                                            + "\"gender\":\"" + gender + "\","
                                            + "\"maritalStatus\":\"" + maritalStatus + "\","
                                            + "\"birthday\":\"" + dob + "\","
                                            + "\"nationalityId\":\"" + nationality + "\""
                                            + "}";
                                }))
                                .check(status().in(200, 204))
                );
    }
}
