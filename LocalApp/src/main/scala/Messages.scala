import akka.actor.ActorRef

/**
 * Created by mentall on 12.02.15.
 */
case object CreateAnotherActor
case object StopSystem
case object Gotcha
case class ActorCreated(val adr: ActorRef){
  override def toString = "ActorRef:"+adr+"\nActorPath:"+adr.path
}
case class ActorJson(id: String, ref: String)
