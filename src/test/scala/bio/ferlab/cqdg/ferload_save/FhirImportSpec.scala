package bio.ferlab.cqdg.ferload_save

import bio.ferlab.cqdg.ferload_save.utils.WholeStackSuite
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class FhirImportSpec extends FlatSpec with WholeStackSuite with Matchers with BeforeAndAfterEach {
  val study = "STU0000001"


  "run" should "return no errors" in {
//    withS3Objects { (inputPrefix, _) =>
//      addObjectToBucket(inputPrefix, objects)


      1 shouldEqual 1
//    }
  }
}
