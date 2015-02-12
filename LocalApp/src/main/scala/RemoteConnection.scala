import akka.actor.{ActorRef, Actor}

/**
 * Created by mentall on 12.02.15.
 */
class RemoteConnection extends Actor with MyBeautifulOutput{
  val remote = context.actorSelection("akka.tcp://HelloRemoteSystem@127.0.0.1:15150/user/RemoteActor")
  var waiter : ActorRef = null

  override def receive: Receive = {
    case CreateAnotherActor => {waiter = sender; remote ! CreateAnotherActor}
    case ActorCreated(adr) =>  adr ! "OK?"
    case Gotcha => {out("started"); waiter ! ActorCreated(sender)}
    case StopSystem =>  remote ! StopSystem
  }
}
