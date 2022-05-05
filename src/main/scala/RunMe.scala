import com.typesafe.config.{Config, ConfigFactory}
import oauth.tokenProviders

import scala.concurrent.Future

object RunMe extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  // Spotify endpoint test
  val clientId = "aClientId"
  val clientSecret = "aClientSecret"

  val tokenAcquirer = tokenProviders.SpotifyTokenProvider(clientId, clientSecret)
  //
  //  private val eventualTokenWrapper: Future[Any] = tokenAcquirer.acquireToken()
  //
  //  eventualTokenWrapper.foreach(v =>
  //    println(v.toString)
  //  )
  private val config: Config = ConfigFactory.load("secrets.conf").getConfig("OneTrust")
  val oneTrustMockEndpoint = "" +
    "https://uat-de.onetrust.com/api/access/v1/oauth/token"
  val oneTrustMockAcquirer = tokenProviders.OneTrustTokenProvider(
    config.getString("client_id"),
    config.getString("client_secret"),
    oneTrustMockEndpoint
  )

  private val eventualTokenWrapper2: Future[Any] = oneTrustMockAcquirer.acquireToken()

  eventualTokenWrapper2.foreach(v =>
    println(v.toString)
  )
}
