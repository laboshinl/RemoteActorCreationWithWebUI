import akka.actor.{ActorRef, Actor}

/**
 * Created by mentall on 12.02.15.
 */
class RemoteConnection extends Actor with MyBeautifulOutput{
  var waiter : ActorRef = null

  var remoteSystems = new scala.collection.mutable.HashMap[Long, ActorRef]
  var uniqueId : Long = 0

  out("Remoter started")

  def remote : ActorRef = {
    var r = scala.util.Random.nextInt(remoteSystems.size)+1
    println("I have "+remoteSystems.size+" remote system and i choose "+r+" to send a message")
    remoteSystems(r)
  }

  override def receive: Receive = {
    case ActorTypeToJson(t) => {out("Got request on creation"); waiter = sender; remote ! CreateNewActor(t)}
    case ActorCreated(adr) =>  {out("Checking address"); adr ! CheckAddress}
    case NonexistentActorType =>  {out("Nonexsistent actor type"); waiter ! NonexistentActorType}
    case AddressIsOk =>        {out("Address is ok"); waiter ! ActorCreated(sender)}
    case StopSystem =>         {out("Stopping remote system"); for (r <- remoteSystems.values) r ! StopSystem}
    case ConnectionRequest =>  {out("Connection request"); uniqueId += 1; remoteSystems += ((uniqueId, sender)); sender!Connected}
  }
}
