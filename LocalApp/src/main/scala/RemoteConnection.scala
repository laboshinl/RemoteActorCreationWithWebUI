import akka.actor.{ActorRef, Actor}

/**
 * Created by mentall on 12.02.15.
 */
class RemoteConnection extends Actor with MyBeautifulOutput{
  val remote = context.actorSelection("akka.tcp://HelloRemoteSystem@127.0.0.1:15150/user/RemoteActor")
  var waiter : ActorRef = null

  override def receive: Receive = {
    case ActorTypeToJson(t) => {out("Got request on creation"); waiter = sender; remote ! CreateNewActor(t)}
    case ActorCreated(adr) =>  {out("Checking address"); adr ! CheckAddress}
    case AddressIsOk =>        {out("Adress is ok"); waiter ! ActorCreated(sender)}
    case StopSystem =>         {out("Stopping remote system"); remote ! StopSystem}
  }
}
