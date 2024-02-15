package bio.ferlab.cqdg.ferload

import bio.ferlab.cqdg.etl.clients.FerloadClient
import bio.ferlab.cqdg.ferload.clients.{FerloadClientTest, FhirClientTest, KeycloakClientTest}
import bio.ferlab.cqdg.ferload.models.DrsObjectSpec
import bio.ferlab.cqdg.ferload.utils.Utils.fetchToken
import bio.ferlab.cqdg.ferload.utils._
import ca.uhn.fhir.rest.client.api.IGenericClient
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import java.io.File

class FerloadImportSpec extends FlatSpec with Matchers with BeforeAndAfterEach with TestContainerForAll {

  implicit val study: String = "STUDY1"

  val expectedOutput = Seq(
    DrsObjectSpec(),
    DrsObjectSpec(id = "doc2", size = Some(3555072L), name = Some("S14358.cram.crai"), mime_type = Some("CRAI"), uris = List("s3://cqdg-qa-file-import/jmichaud/study1/dataset_data1/1002/S14358.cram.crai")),
  )

  override val containerDef =
    DockerComposeContainer.Def(
      new File("src/test/resources/docker-compose.yml"),
      tailChildContainers = true,
      exposedServices = Seq(
        ExposedService("auth_1", AUTH_PORT), //todo implement wait strategy
        ExposedService("fhir_1", FHIR_PORT), //todo implement wait strategy
        ExposedService("ferload_1", FERLOAD_PORT), //todo implement wait strategy
      )
    )

  //TODO implement containers creating and provisioning in Before and stop in After
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

      // Create and Provision Keycloak
      val keycloakClientTest = KeycloakClientTest("localhost", authPort)
      keycloakClientTest.init(RESOURCE_CLIENT, FHIR_CLIENT)

      // Create and Provision FHIR
      val tokenFHIR = fetchToken(composedContainers.container.getContainerByServiceName("ferload_1").get(), FHIR_CLIENT, FHIR_CLIENT_SECRET)
      implicit val fhirClient: IGenericClient = FhirClientTest.buildClient("localhost", fhirPort, tokenFHIR)
      FhirClientTest.init()

      // Create Ferload Client
      val tokenFerload = fetchToken(composedContainers.container.getContainerByServiceName("ferload_1").get(), RESOURCE_CLIENT, RESOURCE_CLIENT_SECRET)
      implicit val ferloadClient: FerloadClient = new FerloadClientTest(tokenFerload, "localhost", ferloadPort)


      val result = FerloadImport.run(study)

      result.isValid shouldBe true

      val keycloakResources = KeycloakClientTest.getClientResources()(keycloakClientTest.client)

      keycloakResources should contain allElementsOf expectedOutput
    }
  }
}
