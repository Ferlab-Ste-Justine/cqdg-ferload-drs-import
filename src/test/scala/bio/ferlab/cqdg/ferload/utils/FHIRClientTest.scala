package bio.ferlab.cqdg.ferload.utils

import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.{Keycloak, KeycloakBuilder}
import org.keycloak.representations.idm.{ClientRepresentation, CredentialRepresentation, RealmRepresentation, UserRepresentation}

import scala.jdk.CollectionConverters._

object FHIRClientTest{
  def init(port: Int): Unit = {
    implicit val client: Keycloak =  KeycloakBuilder.builder()
      .serverUrl(s"http://localhost:$port")
      .realm("master")
      .clientId("admin-cli")
      .grantType(OAuth2Constants.PASSWORD)
      .username("admin")
      .password("admin")
      .build()

    createRealm("CQDG")
    createClient("cqdg-resource-server", port)
    createUser("user1")

  }

  private def createRealm(realmName: String)(implicit keycloakClient: Keycloak) = {
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

}