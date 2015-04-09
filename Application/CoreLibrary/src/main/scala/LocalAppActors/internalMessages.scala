package LocalAppActors

import java.io.Serializable
import java.util.UUID

/**
 * Created by baka on 06.04.15.
 */



case class ActorTypeToJson(var actorType: String) extends Serializable
case class IdToJson(Id : String) extends Serializable


case object NoRouters