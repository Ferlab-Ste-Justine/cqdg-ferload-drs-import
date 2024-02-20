package bio.ferlab.cqdg.ferload.utils

import org.testcontainers.containers.ContainerState
import play.api.libs.json.Json

object Utils {

  def fetchToken(container: ContainerState, client: String, secret: String): String = {
    val tokenResult = container
      .execInContainer("/bin/sh", "-c", s"curl --header 'Content-Type:application/x-www-form-urlencoded' --header 'Accept:application/json' -X POST --data 'client_id=$client&grant_type=client_credentials&client_secret=$secret' http://auth:$AUTH_PORT/realms/$REALM/protocol/openid-connect/token")
      .getStdout

    val tokenExtract = Json.parse(tokenResult)

    (tokenExtract \ "access_token").getOrElse(throw new RuntimeException("missing token")).validate[String].get
  }

  def fetchUserToken(container: ContainerState, client: String, secret: String): String = {
    val tokenResult = container
      .execInContainer("/bin/sh", "-c", s"curl -u $client:$secret  --header 'Content-Type:application/x-www-form-urlencoded' --header 'Accept:application/json' -X POST --data 'password=admin&audience=cqdg-resource-server&grant_type=password&username=user1' http://auth:$AUTH_PORT/realms/$REALM/protocol/openid-connect/token")
      .getStdout

    val tokenExtract = Json.parse(tokenResult)

    (tokenExtract \ "access_token").getOrElse(throw new RuntimeException("missing token")).validate[String].get
  }


}
