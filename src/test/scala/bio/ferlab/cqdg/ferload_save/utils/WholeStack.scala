package bio.ferlab.cqdg.ferload_save.utils

import bio.ferlab.cqdg.ferload_save.LOGGER

trait WholeStack extends KeycloakServer with FhirServer

trait WholeStackSuite extends KeycloakServer with FhirServer {

}

object StartWholeStack extends App with KeycloakServer with FhirServer {
  LOGGER.info("Whole stack is started")
  while (true) {

  }
}




