import io.gatling.core.Predef._
import io.gatling.core.feeder._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class SomeSimulation extends Simulation {
  var host: String = null
  var feeder: BatchableFeederBuilder[String] = null

  val httpConf = http

  // for local run:
  val env = "DEV01"
  val usersCount = 1
  val scenarioType = "RAMP_UP" // RAMP_UP / CONSTANT_USERS
  val rampUpTime = 1 // seconds
  val constantLoadTime = 1 // minutes

  env match {
    case "DEV01" =>
      host = "https://jsonplaceholder.typicode.com"
      feeder = csv("Feeder.csv").circular

    case "DEV04" =>
      host = "https://jsonplaceholder.typicode.com"
      feeder = csv("Feeder.csv").circular
  }

  val scn = scenario("Scenario Name")
    .feed(feeder)

    .exec(http("Simple GET call; check parameter exists in response")
      .get(host + "/posts/1")
      .header("accept-language", "en")
      .check(jsonPath("$.userId").exists))
    .pause(1)

    .exec(http("GET call and extract values from response")
      .get(host + "/posts/1")
      // save values from response in newly created parameter
      .check(jsonPath("$.userId").saveAs("userId")))
    .pause(1)
    //
    .exec(http("POST call and use values extracted in the previous response")
      .post(host + "/posts")
      .header("Content-type", "application/json")
      .body(StringBody(
        """{
      	    "title": "${param1FromFeederFile}",
      	    "body": "${param2FromFeederFile}",
      	    "userId": "${userId}"
      	    }"""))
      .check(jsonPath("$.id").exists))
    .pause(1)

  scenarioType match {
    case "RAMP_UP" =>
      setUp(scn.inject(rampUsers(usersCount) during (rampUpTime seconds))).protocols(httpConf)

    case "CONSTANT_USERS" =>
      setUp(scn.inject(
        rampConcurrentUsers(0) to (usersCount) during (rampUpTime seconds),
        constantConcurrentUsers(usersCount) during (constantLoadTime minutes))).protocols(httpConf)
  }
}
