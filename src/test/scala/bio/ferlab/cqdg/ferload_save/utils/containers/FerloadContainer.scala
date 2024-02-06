package bio.ferlab.cqdg.ferload_save.utils.containers

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

case class FerloadContainer(keycloakPort: Int) extends OurContainer {
  val name = "ferload-test"
  val port = 9090

  val ferloadEnv: Map[String, String] = Map(
    "AUTH_CLIENT_ID" -> "cqdg-resource-server",
    "AUTH_CLIENT_SECRET" -> "SECRET", //From keycloak client
    "AUTH_REALM" -> "CQDG",
    "AUTH_URL" -> s"http://localhost:$keycloakPort",
    "DRS_ID" -> "user1",
    "DRS_NAME" -> "DRS_NAME",
    "DRS_ORGANIZATION_NAME" -> "DRS_ORGANIZATION_NAME",
    "DRS_ORGANIZATION_URL" -> "DRS_ORGANIZATION_URL",
    "DRS_SELF_HOST" -> "DRS_SELF_HOST",
    "FERLOAD_CLIENT_ID" -> "user1",
    "FERLOAD_CLIENT_METHOD" -> "password",
    "AWS_REGION" -> "us-est-1",
  )

  val container: GenericContainer = GenericContainer(
    "ferlabcrsj/ferload:2.0.1",
    command = Seq("start-dev"),
    waitStrategy = Wait.forHttp("/status").withStartupTimeout(Duration.ofSeconds(60)),
    exposedPorts = Seq(port),
    labels = Map("name" -> name),
    env = ferloadEnv
  )
}
