import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._

/**
 * Created by mentall on 23.02.15.
 */
//class SprayServiceTest extends Specification with WebUIActor with Specs2RouteTest {
//  var location: String = ""
//
//  "Spray service" should {
//    "return a greeting for GET requests to the root path" in {
//      Get() ~> route ~> check{
//        responseAs[String] must be equalTo(indexPage)
//      }
//    }
//    "return list of actor classes" in {
//      Get("/actor") ~> route ~> check{
//        responseAs[String] must be equalTo(availableActors)
//      }
//    }
//    "create a new actor and answer with his id" in {
//      Put("/actor") ~> route ~> check{
//        responseAs[String] must be equalTo(availableActors)
//      }
//    }
//
//  }
//}
