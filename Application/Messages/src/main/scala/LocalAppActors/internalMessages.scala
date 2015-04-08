package LocalAppActors

import java.io.Serializable
import java.util.UUID

/**
 * Created by baka on 06.04.15.
 */


case class PlanActorTermination(actorId: String)
case class PlanActorCreation(actorType : String)
case object PlanMachineStart
case class PlanMachineTermination(vmId : String)
case class ActorIdAndMessageToJson(var id: String, var msg: String) extends Serializable
case class ActorTypeToJson(var actorType: String) extends Serializable
case class IdToJson(Id : String) extends Serializable


case object NoRouters