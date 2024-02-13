package bio.ferlab.cqdg.ferload

import bio.ferlab.cqdg.etl.clients.FerloadClient
import bio.ferlab.cqdg.ferload.clients.{FerloadClientTest, FhirClientTest, KeycloakClientTest}
import bio.ferlab.cqdg.ferload.models.DrsObjectSpec
import bio.ferlab.cqdg.ferload.utils.{AUTH_PORT, FERLOAD_PORT, FHIR_PORT, REALM, RESOURCE_CLIENT, RESOURCE_CLIENT_SECRET}
import ca.uhn.fhir.context.{FhirContext, PerformanceOptionsEnum}
import ca.uhn.fhir.rest.client.api.{IGenericClient, ServerValidationModeEnum}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import play.api.libs.json.Json

import java.io.File

class FerloadImportSpec extends FlatSpec with Matchers with BeforeAndAfterEach with TestContainerForAll {

  implicit val study: String = "STUDY1"

  val expectedOutput = Seq(
    DrsObjectSpec(),
    DrsObjectSpec(id = "doc2", name = Some("doc2.cram"), uris = List("s3://path_to_doc2")),
  )

  override val containerDef =
    DockerComposeContainer.Def(
      new File("src/test/resources/docker-compose.yml"),
      tailChildContainers = true,
      exposedServices = Seq(
        ExposedService("auth_1", AUTH_PORT), //todo implement wait strategy
        ExposedService("fhir_1", FHIR_PORT), //todo implement wait strategy
        ExposedService("ferload_1", FERLOAD_PORT), //todo implement wait strategy
      ),
      localCompose = true //TODO remove
    )

  "run" should "return no errors" in {
    withContainers { composedContainers =>

      //Ambassador container exposed ports
      val authPort = composedContainers.getServicePort("auth_1", AUTH_PORT)
      val fhirPort = composedContainers.getServicePort("fhir_1", FHIR_PORT)
      val ferloadPort = composedContainers.getServicePort("ferload_1", FERLOAD_PORT)

      //Ensure all containers are up and running
      assert(authPort > 0)
      assert(fhirPort > 0)
      assert(ferloadPort > 0)

      //Provision Keycloak
      val keycloakClientTest = KeycloakClientTest("localhost", authPort)
      keycloakClientTest.init()

      //Provision FHIR
      val fhirBaseUrl = s"http://localhost:$fhirPort/fhir"
      val fhirContext: FhirContext = FhirContext.forR4()
      fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING)
      fhirContext.getRestfulClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER)

      implicit val fhirClient: IGenericClient = fhirContext.newRestfulGenericClient(fhirBaseUrl)

      FhirClientTest.init("localhost", fhirPort)

      val tokenResult = composedContainers.container.getContainerByServiceName("ferload_1").get()
        .execInContainer("/bin/sh", "-c", s"curl --header 'Content-Type:application/x-www-form-urlencoded' --header 'Accept:application/json' -X POST --data 'client_id=$RESOURCE_CLIENT&grant_type=client_credentials&client_secret=$RESOURCE_CLIENT_SECRET' http://auth:$AUTH_PORT/realms/$REALM/protocol/openid-connect/token")
        .getStdout

      val tokenExtract = Json.parse(tokenResult)

      val token = (tokenExtract \ "access_token").getOrElse(throw new RuntimeException("missing token")).validate[String].get


      implicit val ferloadClient: FerloadClient = new FerloadClientTest(token, "localhost", ferloadPort)


      val result = FerloadImport.run(study)

      result.isValid shouldBe true

      val keycloakResources = KeycloakClientTest.getClientResources()(keycloakClientTest.client)

      keycloakResources should contain allElementsOf expectedOutput
    }
  }
}
