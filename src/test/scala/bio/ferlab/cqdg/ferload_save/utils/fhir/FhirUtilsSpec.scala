package bio.ferlab.cqdg.ferload_save.utils.fhir

import bio.ferlab.cqdg.ferload_save.fhir.FhirUtils
import bio.ferlab.cqdg.ferload_save.utils.FhirServer
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}


class FhirUtilsSpec extends FlatSpec with FhirServer with Matchers with BeforeAndAfterEach{

  "FhirUtil" should "return study version" in {

    val studyVersion = FhirUtils.getStudyVersion("STUDY1")

    studyVersion shouldEqual "2"
  }


  "FhirUtil" should "return documents" in {

    val documents = FhirUtils.requestExportFor("DocumentReference", "STUDY1", "2")

    documents.size shouldEqual 2
  }
}
