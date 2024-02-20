package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.ferload.clients.KeycloakClientTest._
import bio.ferlab.cqdg.ferload.models.DrsObjectSpec
import jakarta.ws.rs.NotFoundException
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.{Keycloak, KeycloakBuilder}
import org.keycloak.representations.idm.authorization._
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

  def initImport(resourceClient: String, fhirClient: String): Unit = {
    createRealm("CQDG")(client)

    val clientRepresentationResource = createResourceClient(resourceClient, port)
    client.realms.realm("CQDG").clients().create(clientRepresentationResource)
    val clientRepresentationAcl = createAclClient(fhirClient, port)(client)
    client.realms.realm("CQDG").clients().create(clientRepresentationAcl)

    createUser("user1")(client)
  }

  def initDownload(resourceClient: String, fhirClient: String, files: Seq[DrsObjectSpec]): Unit = {
    createRealm("CQDG")(client)
    createUser("user1")(client)

    val clientRepresentationResource = createResourceClient(resourceClient, port)
    val clientRepresentationWithResource = setResourcesToClientRepresentation(clientRepresentationResource, files)
    client.realms.realm("CQDG").clients().create(clientRepresentationWithResource) //create client
    createPolicy("fetch", Set("user1"), "cqdg-resource-server")(client)
    createPermission("fetch", "fetch_study1", Set("study1"), "cqdg-resource-server")(client)
    val clientRepresentationAcl = createAclClient(fhirClient, port)(client)
    client.realms.realm("CQDG").clients().create(clientRepresentationAcl) //create client


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

  private def createResourceClient(clientName: String, port: Int) = {
    val clientRepresentation = new ClientRepresentation()

    clientRepresentation.setId(clientName)
    clientRepresentation.setSecret("SECRET")
    clientRepresentation.setRootUrl(s"http://localhost:$port")

    clientRepresentation.setEnabled(true)
    clientRepresentation.setServiceAccountsEnabled(true)
    clientRepresentation.setAuthorizationServicesEnabled(true)

    clientRepresentation.setStandardFlowEnabled(true)
    clientRepresentation.setDirectAccessGrantsEnabled(true)

    clientRepresentation
  }

  private def createAclClient(clientName: String, port: Int)(implicit keycloakClient: Keycloak) = {
    val clientRepresentation = new ClientRepresentation()

    clientRepresentation.setId(clientName)
    clientRepresentation.setSecret("SECRET")
    clientRepresentation.setRootUrl(s"http://localhost:$port")

    clientRepresentation.setEnabled(true)
    clientRepresentation.setServiceAccountsEnabled(true)
    clientRepresentation.setAuthorizationServicesEnabled(true)

    clientRepresentation.setStandardFlowEnabled(true)
    clientRepresentation.setDirectAccessGrantsEnabled(true)

    //Create Scopes
    val resourceServerRepresentation = new ResourceServerRepresentation()
    val scopeCreate = new ScopeRepresentation("Create")
    //    scopeCreate.setPolicies()
    val scopeRead = new ScopeRepresentation("Read")
    //    scopeRead.setPolicies()
    resourceServerRepresentation.setScopes(List(scopeCreate, scopeRead).asJava)

    //Create Resources
    val resourceRepresentationStudy = new ResourceRepresentation()
    val resourceRepresentationDocument = new ResourceRepresentation()
    resourceRepresentationStudy.setName("ResearchStudy")
    resourceRepresentationDocument.setName("DocumentReference")
    resourceServerRepresentation.setResources(List(resourceRepresentationStudy, resourceRepresentationDocument).asJava)

    clientRepresentation.setAuthorizationSettings(resourceServerRepresentation)

    clientRepresentation
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

  def setResourcesToClientRepresentation(clientRepresentation: ClientRepresentation, files: Seq[DrsObjectSpec]): ClientRepresentation = {
    val uniqueScopes = files.flatMap(f => f.scopes).flatten.toSet

    val scopeRepresentationList = uniqueScopes.map(s => {
      val scopeRepresentation = new ScopeRepresentation()
      scopeRepresentation.setId(s)
      scopeRepresentation.setName(s)
      scopeRepresentation.setDisplayName(s)
      scopeRepresentation
    })

    val resources = files.map(f => {
      val resourceRep = new ResourceRepresentation(f.id)
      f.name.foreach(n => resourceRep.setName(n))
      resourceRep.setUris(f.uris.toSet.asJava)

      f.scopes.foreach(scopes => resourceRep.setScopes(scopeRepresentationList.filter(s => scopes.contains(s.getName)).asJava))
      resourceRep
    }).toList.asJava

    val resourceServerRepresentation = new ResourceServerRepresentation
    resourceServerRepresentation.setResources(resources)

    clientRepresentation.setAuthorizationSettings(resourceServerRepresentation)
    clientRepresentation
  }

  def createPolicy(policyName: String, users: Set[String], client: String)(implicit keycloak: Keycloak) = {
    val authRes = keycloak.realm("CQDG").clients().get(client).authorization()

    val policyRepresentation = new UserPolicyRepresentation()
    policyRepresentation.setId(policyName)
    policyRepresentation.setName(policyName)
    policyRepresentation.setLogic(Logic.POSITIVE)
    policyRepresentation.setUsers(users.asJava)

    authRes.policies().user().create(policyRepresentation)
  }

  def createPermission(policyName: String, permissionName: String, scopes: Set[String], client: String)(implicit keycloak: Keycloak) = {
    val authRes = keycloak.realm("CQDG").clients().get(client).authorization()

    val scopePermissionRepresentation = new ScopePermissionRepresentation
    scopePermissionRepresentation.setId(permissionName)
    scopePermissionRepresentation.setName(permissionName)
    scopePermissionRepresentation.setScopes(scopes.asJava)
    scopePermissionRepresentation.setPolicies(Set(policyName).asJava)

    authRes.permissions().scope().create(scopePermissionRepresentation)
  }



  }