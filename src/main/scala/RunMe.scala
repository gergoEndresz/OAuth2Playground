import scala.concurrent.Future

object RunMe extends App{

  // Spotify endpoint test
  val clientId = "d76a6461e4fa42eda58420eafb25f76c"
  val clientSecret = "32a3c3a6f5ed475586aefaecd0ae39f0"

  val tokenAcquirer = new SpotifyTokenAcquisitor(clientId, clientSecret)

  private val eventualTokenWrapper: Future[Any] = tokenAcquirer.acquireToken()
  import scala.concurrent.ExecutionContext.Implicits.global

  eventualTokenWrapper.foreach(v =>
    println(v.toString)
  )

  val oneTrustMockEndpoint = "" +
    "https://trial.onetrust.com/api/access/v1/oauth/token"

  val oneTrustMockAcquirer = new  OneTrust(
    "some_client_id",
    "some_credentials",
    oneTrustMockEndpoint
  )

  private val eventualTokenWrapper2: Future[Any] = oneTrustMockAcquirer.acquireToken()

  eventualTokenWrapper2.foreach(v =>
    println(v.toString)
  )
}
