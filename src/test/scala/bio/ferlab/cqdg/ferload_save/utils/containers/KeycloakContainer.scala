package bio.ferlab.cqdg.ferload_save.utils.containers

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

case object KeycloakContainer extends OurContainer {
  val name = "keycloak-test"
  val port = 8080
  val keycloakAdmin = "admin"
  val keycloakPassword = "admin"
  val container: GenericContainer = GenericContainer(
    "quay.io/keycloak/keycloak:23.0.4",
    command = Seq("start-dev"),
    waitStrategy = Wait.forHttp("/").withStartupTimeout(Duration.ofSeconds(60)),
    exposedPorts = Seq(port),
    labels = Map("name" -> name),
    env = Map("KEYCLOAK_ADMIN" -> keycloakAdmin, "KEYCLOAK_ADMIN_PASSWORD" -> keycloakPassword, "KEYCLOAK_USER" -> keycloakAdmin, "KEYCLOAK_PASSWORD" -> keycloakPassword)
  )
}
