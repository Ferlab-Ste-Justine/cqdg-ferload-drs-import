package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.ferload.clients.KeycloakClientTest.{createClient, createRealm, createUser}
import bio.ferlab.cqdg.ferload.model.DrsObject
import bio.ferlab.cqdg.ferload.models.DrsObjectSpec
import jakarta.ws.rs.NotFoundException
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.{Keycloak, KeycloakBuilder}
import org.keycloak.representations.idm.{ClientRepresentation, CredentialRepresentation, RealmRepresentation, UserRepresentation}

import scala.jdk.CollectionConverters._

case class KeycloakClientTest (host: String, port: Int){

  val client: Keycloak =  KeycloakBuilder.builder()
    .serverUrl(s"http://$host:$port")
    .realm("master")
    .clientId("admin-cli")
    .grantType(OAuth2Constants.PASSWORD)
    .username("admin")
    .password("admin")
    .build()

  def init(): Unit = {
    createRealm("CQDG")(client)
    createClient("cqdg-resource-server", port)(client)
    createUser("user1")(client)
  }

}

object KeycloakClientTest{

  private def createRealm(realmName: String)(implicit keycloakClient: Keycloak): Unit = {
    val realmRep = new RealmRepresentation()
    realmRep.setId(realmName)
    realmRep.setRealm(realmName)
    realmRep.setEnabled(true)
    keycloakClient.realms.create(realmRep)
  }

  private def createClient(clientName: String, port: Int)(implicit keycloakClient: Keycloak): Unit = {
    val clientRepresentation = new ClientRepresentation()

    clientRepresentation.setId(clientName)
    clientRepresentation.setSecret("SECRET")
    clientRepresentation.setRootUrl(s"http://localhost:$port")

    clientRepresentation.setEnabled(true)
    clientRepresentation.setAttributes(
      Map(
        "backchannel.logout.revoke.offline.tokens"-> "false",
        "backchannel.logout.session.required"-> "true",
        "backchannel.logout.url"-> "",
        "display.on.consent.screen"-> "false",
        "login_theme"-> "",
        "oauth2.device.authorization.grant.enabled"-> "false",
        "oidc.ciba.grant.enabled" -> "false",
      ).asJava
    )
    clientRepresentation.setServiceAccountsEnabled(true)
    clientRepresentation.setAuthorizationServicesEnabled(true)

    clientRepresentation.setStandardFlowEnabled(true)
    clientRepresentation.setDirectAccessGrantsEnabled(true)

    keycloakClient.realms.realm("CQDG").clients().create(clientRepresentation)
  }
  def createUser(userName: String)(implicit keycloakClient: Keycloak): Unit = {
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

  def getClientResources()(implicit keycloak: Keycloak): List[DrsObjectSpec] = {
    try {
      val client = keycloak.realm("CQDG").clients().get("cqdg-resource-server")
      val resources = client.authorization().resources().resources()

      resources.asScala.toList.map(DrsObjectSpec.apply)
    } catch {
      case e: NotFoundException =>
        List.empty[DrsObjectSpec]
      case e => throw e
    }
  }


}