import oauth.tokenProviders

import scala.concurrent.Future

object RunMe extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  // Spotify endpoint test
  val clientId = "d76a6461e4fa42eda58420eafb25f76c1"
  val clientSecret = "32a3c3a6f5ed475586aefaecd0ae39f0"

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
