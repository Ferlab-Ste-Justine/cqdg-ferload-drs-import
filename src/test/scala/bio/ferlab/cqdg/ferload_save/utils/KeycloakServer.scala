package bio.ferlab.cqdg.ferload_save.utils

import bio.ferlab.cqdg.ferload_save.utils.containers.{FerloadContainer, KeycloakContainer}
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.{Keycloak, KeycloakBuilder}
import org.keycloak.representations.idm.{ClientRepresentation, CredentialRepresentation, RealmRepresentation, UserRepresentation}
import org.scalatest.TestSuite
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._

trait KeycloakServer {
  val (keycloakPort, isNewK) = KeycloakContainer.startIfNotRunning()
  val keycloakBaseUrl = s"http://localhost:$keycloakPort"

  val (ferloadPort, isNewFerload) = FerloadContainer.apply(keycloakPort).startIfNotRunning()
  val ferloadBaseUrl = s"http://localhost:$ferloadPort"

  implicit val keycloakClient: Keycloak =
    KeycloakBuilder.builder()
      .serverUrl(keycloakBaseUrl)
      .realm("master")
      .clientId("admin-cli")
      .grantType(OAuth2Constants.PASSWORD)
      .username("admin")
      .password("admin")
      .build()

  createRealm("CQDG")
  createClient("cqdg-resource-server")
  createUser("user1")

  private def createRealm(realmName: String): Unit = {
    val realmRep = new RealmRepresentation()
    realmRep.setId(realmName)
    realmRep.setRealm(realmName)
    realmRep.setEnabled(true)
    keycloakClient.realms.create(realmRep)
  }

  private def createClient(clientName: String): Unit = {
    val clientRepresentation = new ClientRepresentation()

    clientRepresentation.setId(clientName)
    clientRepresentation.setSecret("SECRET")
    clientRepresentation.setRootUrl(s"http://localhost:$keycloakPort")

    clientRepresentation.setEnabled(true)
    clientRepresentation.setAuthorizationServicesEnabled(false) //FIXME
    clientRepresentation.setStandardFlowEnabled(true)
    clientRepresentation.setDirectAccessGrantsEnabled(true)

    keycloakClient.realms.realm("CQDG").clients().create(clientRepresentation)
  }

  private def createUser(userName: String): Unit = {
    val userRepresentation = new UserRepresentation();

    userRepresentation.setId(userName)
    userRepresentation.setUsername(userName)
    userRepresentation.setEmail(s"$userName@gmail.com")
    userRepresentation.setEmailVerified(true)
    userRepresentation.setEnabled(true)

    val credentialRepresentation = new CredentialRepresentation()
    credentialRepresentation.setValue("admin")
    credentialRepresentation.setSecretData("admin")
    userRepresentation.setCredentials(List(credentialRepresentation).asJava)

    keycloakClient.realm("CQDG").users().create(userRepresentation);
  }

}

trait KeycloakServerSuite extends KeycloakServer with TestSuite {

}

object StartKeycloakServer extends App with KeycloakServer {
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)
  LOGGER.info("Keycloak Server is started")
  while (true) {

  }
}
