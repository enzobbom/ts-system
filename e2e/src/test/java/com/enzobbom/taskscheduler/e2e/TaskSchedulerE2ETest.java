package com.enzobbom.taskscheduler.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

class TaskSchedulerE2ETest {
    private static final String USERS = "/api/v1/users";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String TASKS = "/api/v1/tasks";

    private static final String PASSWORD = "E2ePassword123!";
    private static final String TIME_ZONE = "Europe/Dublin";

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = System.getenv()
                .getOrDefault("E2E_BASE_URL", "http://127.0.0.1:8083");

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteNotificationLifecycle() {
        String email = uniqueEmail();

        createUser(email);

        String authorization = login(email);

        Instant scheduledDateTime = Instant.now()
                .plus(Duration.ofMinutes(30))
                .truncatedTo(ChronoUnit.SECONDS);

        String taskId = createTask(
                authorization,
                "Notification E2E task",
                "Task that should go through the notification lifecycle",
                scheduledDateTime
        );

        await()
                .atMost(Duration.ofSeconds(45))
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() ->
                        given()
                                .header("Authorization", authorization)
                                .when()
                                .get(TASKS)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("SUCCESS"))
                                .body("code", equalTo(200))
                                .body("data.id", hasItem(taskId))
                                .body(
                                        "data.find { it.id == '%s' }.notificationStatus"
                                                .formatted(taskId),
                                        equalTo("NOTIFIED")
                                )
                );

        deleteTask(authorization, taskId);
        deleteUser(authorization);
    }

    @Test
    void shouldCompleteUserProfileLifecycle() {
        String email = uniqueEmail();

        createUser(email);

        String authorization = login(email);

        // Verify initial persisted user
        given()
                .header("Authorization", authorization)
                .when()
                .get(USERS + "/me")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.name", equalTo("E2E Test User"))
                .body("data.email", equalTo(email))
                .body("data.address.street", equalTo("Praça da Sé"))
                .body("data.address.number", equalTo("123"))
                .body("data.address.complement", equalTo("E2E"))
                .body("data.address.city", equalTo("São Paulo"))
                .body("data.address.neighbourhood", equalTo("Sé"))
                .body("data.address.state", equalTo("SP"))
                .body("data.address.cep", equalTo("01001-000"))
                .body("data.phone.countryCode", equalTo("55"))
                .body("data.phone.number", not(emptyOrNullString()));

        // Update user
        String updateUserBody = """
            {
              "name": "Updated E2E User"
            }
            """;

        given()
                .header("Authorization", authorization)
                .contentType(ContentType.JSON)
                .body(updateUserBody)
                .when()
                .patch(USERS + "/me")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.name", equalTo("Updated E2E User"))
                .body("data.email", equalTo(email));

        // Update address
        String updateAddressBody = """
            {
              "street": "Avenida Paulista",
              "number": "1000",
              "complement": "Apartment 101",
              "city": "São Paulo",
              "neighbourhood": "Bela Vista",
              "state": "SP",
              "cep": "01310-100"
            }
            """;

        given()
                .header("Authorization", authorization)
                .contentType(ContentType.JSON)
                .body(updateAddressBody)
                .when()
                .patch(USERS + "/me/address")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.street", equalTo("Avenida Paulista"))
                .body("data.number", equalTo("1000"))
                .body("data.complement", equalTo("Apartment 101"))
                .body("data.city", equalTo("São Paulo"))
                .body("data.neighbourhood", equalTo("Bela Vista"))
                .body("data.state", equalTo("SP"))
                .body("data.cep", equalTo("01310-100"));

        // Update phone
        String updatedPhoneNumber = uniquePhoneNumber();

        String updatePhoneBody = """
            {
              "countryCode": "353",
              "number": "%s"
            }
            """.formatted(updatedPhoneNumber);

        given()
                .header("Authorization", authorization)
                .contentType(ContentType.JSON)
                .body(updatePhoneBody)
                .when()
                .patch(USERS + "/me/phone")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.countryCode", equalTo("353"))
                .body("data.number", equalTo(updatedPhoneNumber));

        // Verify the complete persisted state
        given()
                .header("Authorization", authorization)
                .when()
                .get(USERS + "/me")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.name", equalTo("Updated E2E User"))
                .body("data.email", equalTo(email))
                .body("data.address.street", equalTo("Avenida Paulista"))
                .body("data.address.number", equalTo("1000"))
                .body("data.address.complement", equalTo("Apartment 101"))
                .body("data.address.city", equalTo("São Paulo"))
                .body("data.address.neighbourhood", equalTo("Bela Vista"))
                .body("data.address.state", equalTo("SP"))
                .body("data.address.cep", equalTo("01310-100"))
                .body("data.phone.countryCode", equalTo("353"))
                .body("data.phone.number", equalTo(updatedPhoneNumber));

        deleteUser(authorization);
    }

    @Test
    void shouldCompleteTaskCrudLifecycle() {
        String email = uniqueEmail();

        createUser(email);

        String authorization = login(email);

        Instant scheduledDateTime = Instant.now()
                .plus(Duration.ofHours(2))
                .truncatedTo(ChronoUnit.SECONDS);

        String taskId = createTask(
                authorization,
                "Initial E2E task",
                "Initial task description",
                scheduledDateTime
        );

        given()
                .header("Authorization", authorization)
                .when()
                .get(TASKS)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.id", hasItem(taskId))
                .body(
                        "data.find { it.id == '%s' }.name".formatted(taskId),
                        equalTo("Initial E2E task")
                )
                .body(
                        "data.find { it.id == '%s' }.description".formatted(taskId),
                        equalTo("Initial task description")
                );

        String updateBody = """
                {
                  "name": "Updated E2E task",
                  "description": "Updated task description"
                }
                """;

        given()
                .header("Authorization", authorization)
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .patch(TASKS + "/" + taskId)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.id", equalTo(taskId))
                .body("data.name", equalTo("Updated E2E task"))
                .body("data.description", equalTo("Updated task description"));

        given()
                .header("Authorization", authorization)
                .when()
                .get(TASKS)
                .then()
                .statusCode(200)
                .body(
                        "data.find { it.id == '%s' }.name".formatted(taskId),
                        equalTo("Updated E2E task")
                )
                .body(
                        "data.find { it.id == '%s' }.description".formatted(taskId),
                        equalTo("Updated task description")
                );

        deleteTask(authorization, taskId);

        given()
                .header("Authorization", authorization)
                .when()
                .get(TASKS)
                .then()
                .statusCode(200)
                .body("data.id", not(hasItem(taskId)));

        deleteUser(authorization);
    }

    private static void createUser(String email) {
        String body = """
                {
                  "name": "E2E Test User",
                  "email": "%s",
                  "password": "%s",
                  "address": {
                    "street": "Praça da Sé",
                    "number": "123",
                    "complement": "E2E",
                    "city": "São Paulo",
                    "neighbourhood": "Sé",
                    "state": "SP",
                    "cep": "01001-000"
                  },
                  "phone": {
                    "countryCode": "55",
                    "number": "%s"
                  }
                }
                """.formatted(email, PASSWORD, uniquePhoneNumber());

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(USERS)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("code", equalTo(200))
                .body("data.email", equalTo(email));
    }

    private static String login(String email) {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, PASSWORD);

        Response response =
                given()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .when()
                        .post(LOGIN)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("SUCCESS"))
                        .body("code", equalTo(200))
                        .body("data.userEmail", equalTo(email))
                        .body("data.token", startsWith("Bearer "))
                        .extract()
                        .response();

        return response.jsonPath().getString("data.token");
    }

    private static String createTask(
            String authorization,
            String name,
            String description,
            Instant scheduledDateTime
    ) {
        String body = """
                {
                  "name": "%s",
                  "description": "%s",
                  "scheduledDateTime": "%s",
                  "timeZoneId": "%s"
                }
                """.formatted(
                name,
                description,
                scheduledDateTime,
                TIME_ZONE
        );

        Response response =
                given()
                        .header("Authorization", authorization)
                        .contentType(ContentType.JSON)
                        .body(body)
                        .when()
                        .post(TASKS)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("SUCCESS"))
                        .body("code", equalTo(200))
                        .body("data.name", equalTo(name))
                        .body("data.description", equalTo(description))
                        .body("data.timeZoneId", equalTo(TIME_ZONE))
                        .body("data.id", not(emptyOrNullString()))
                        .body("data.notificationStatus", notNullValue())
                        .extract()
                        .response();

        return response.jsonPath().getString("data.id");
    }

    private static void deleteTask(String authorization, String taskId) {
        given()
                .header("Authorization", authorization)
                .when()
                .delete(TASKS + "/" + taskId)
                .then()
                .statusCode(204);
    }

    private static void deleteUser(String authorization) {
        given()
                .header("Authorization", authorization)
                .when()
                .delete(USERS + "/me")
                .then()
                .statusCode(204);
    }

    private static String uniqueEmail() {
        return "e2e-" + UUID.randomUUID() + "@example.com";
    }

    private static String uniquePhoneNumber() {
        long suffix = Math.floorMod(
                UUID.randomUUID().getLeastSignificantBits(),
                1_000_000_000L
        );

        return "11%09d".formatted(suffix);
    }
}
