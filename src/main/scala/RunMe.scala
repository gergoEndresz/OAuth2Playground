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

  val oneTrustMockEndpoint = "" +
    "https://trial.onetrust.com/api/access/v1/oauth/token"
  val oneTrustMockAcquirer = tokenProviders.OneTrustTokenProvider(
    "some_client_id",
    "some_credentials",
    oneTrustMockEndpoint
  )

  private val eventualTokenWrapper2: Future[Any] = oneTrustMockAcquirer.acquireToken()

  eventualTokenWrapper2.foreach(v =>
    println(v.toString)
  )
}
