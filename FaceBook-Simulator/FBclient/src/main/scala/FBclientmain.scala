package FBclient

/**
 * @author Asks
 */
import akka.actor._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.Future
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import java.util.ArrayList
import java.util.Collections
import spray.http._
import spray.client.pipelining._
import akka.actor.ActorSystem
import spray.httpx.RequestBuilding

case object SendRequest
case object Create
case object Login
case object CreateUsers
case object UserConnection
case object GetSchedule
case object PostSchedule
case object DeleteSchedule
case object MakeGetRequest
case object MakePostRequest
case object MakeDeleteRequest
case object Terminate
case object ForceTerminate
case class FinishWork(uid: Int)


object FBsclientmain extends App with RequestBuilding{
  
  implicit val system = ActorSystem("FBclient")
  implicit val executionContext = system.dispatcher
  val pipeline : HttpRequest => Future[HttpResponse] = sendReceive
  
    var IPaddress = args(0)
	Global.clients = args(1).toInt
    Global.numRequest = args(2).toInt
    
    var ServerAddress = "http://"+IPaddress + ":2559/"    
    Global.ServerAddress = ServerAddress
    println(ServerAddress)
    val result = pipeline(Get(ServerAddress+"page?id=100"))
    var address = ""
    result.foreach {
      response =>
        println(response.entity.asString)

        println(" IPaddress of Server: " + IPaddress + "\n Client Users: " + Global.clients)
        System.setProperty("java.net.preferIPv4Stack", "true")
        
    }
  var clientmaster = system.actorOf(Props(new ClientMaster(Global.clients, address)), name = "clientmaster")
        //clientmaster ! SendRequest
        clientmaster ! UserConnection

  class ClientMaster(clients: Int, IPaddress: String) extends Actor {
    Global.IP = IPaddress
    println("Master reference defined " + IPaddress)
    var clientusers: Array[ActorRef] = new Array[ActorRef](2*clients)
    var start: Long = 0
    var end: Long = 0
    var userlist:ArrayBuffer[Long]=ArrayBuffer[Long]() 
    var users = 2*clients
    var curid = 0
    var ccurid = 0
    var userfinish = 0

    def receive = {

      case SendRequest => {

        println(self.path + " In requestnow")
        self ! Create
      }

      case UserConnection => {
        if (curid < clients) {
          clientusers(curid) = system.actorOf(Props(new ClientUser(curid,self)), name = "user" + curid)
          userlist += curid
          clientusers(curid) ! Login
          //clientusers(curid) ! GetSchedule
          //clientusers(curid) ! PostSchedule
          //clientusers(curid) ! DeleteSchedule
          curid += 1
          context.system.scheduler.scheduleOnce(100 milliseconds,self,UserConnection)
        }
        else
          self ! CreateUsers
      }
      
      case CreateUsers => {
        if (ccurid < clients) {
          var id = ccurid+clients
          clientusers(id) = system.actorOf(Props(new ClientUser(-1,self)), name = "user" + id)   
          clientusers(id) ! Create
          ccurid += 1
          context.system.scheduler.scheduleOnce(100 milliseconds,self,CreateUsers)
        }
        
      }
      /*case getDuration(timing) => {

        Global.Runningtime = timing
        println("Runningtime: " + Global.Runningtime)
        var avgtweetspersec = 5787
        val Avgtweetsperuserforduration = ((5787 * Global.Runningtime) / clients).toInt
        Global.frequency = (Global.Runningtime / Avgtweetsperuserforduration).toInt
        if (Global.option == "Regular")
          sender ! getFrequency(Global.frequency)
        else
          sender ! getFrequency(1)
      }*/
      case FinishWork(uid)=> {
        userfinish+=1

        if(userfinish == users){
          println("\nAll clients have finished request")
          context.stop(self)
          context.system.shutdown
          System.exit(0) 
        }
      }

    }
   
  }
  
  class ClientUser(userid: Int, master: ActorRef) extends Actor {

    var uid: Int = userid
    var ServerAddress = Global.ServerAddress
    var getrequestcount = 0
    var postrequestcount = 0
    var deleterequestcount = 0
    var postlist:ArrayBuffer[String] = new ArrayBuffer[String]()
    var friendlist:ArrayBuffer[String] = new ArrayBuffer[String]()
    
    def FaceBookLogin()={
      var result = pipeline(Get(ServerAddress + "me?id=" + uid))
      printResult(result)
      self ! GetSchedule
      self ! PostSchedule
      self ! DeleteSchedule
      
    }

    def FaceBookAPIGet()={
      if(uid != -1){
        var r = Random
      var indexofmessage = r.nextInt(6)
      if(indexofmessage == 0){
        var rd = Random.nextInt(Global.pagesum)
        var result = pipeline(Get(ServerAddress + "page?id=" + rd))
        printResult(result)
      }
      if (indexofmessage == 1) {
        var rd = Random.nextInt(Global.postsum)
        var result = pipeline(Get(ServerAddress + "post?id=" + rd))
        printResult(result)
      } 
      if (indexofmessage == 2) {
        var rd = Random.nextInt(Global.friendlistsum)
        var result = pipeline(Get(ServerAddress + "friend_list?id=" + rd))
        printResult(result)
      } 
      if (indexofmessage == 3) {
        var rd = Random.nextInt(Global.usersum)
        var result = pipeline(Get(ServerAddress + "profile?id=" + rd))
        printResult(result)
       }
      if (indexofmessage == 4) {
        var rd = Random.nextInt(Global.albumsum)
        var result = pipeline(Get(ServerAddress + "album?id=" + rd))
        printResult(result)
      } 
      if (indexofmessage == 5) {
        var rd = Random.nextInt(Global.photosum)
        var result = pipeline(Get(ServerAddress + "album/photo?id=" + rd))
        printResult(result)
      }
      }  
    }
    
    def FaceBookAPIPost()={
      if(uid != -1){
      var r = Random
      var indexofmessage = r.nextInt(5)
      if(indexofmessage == 0){
        pipeline(Post(ServerAddress + "page"))
        Global.pagesum += 1
      }
      if (indexofmessage == 1) {
        var rd = Random.nextInt(10)
        if(rd == 0){
          var pageid = uid % Global.pagesum
          pipeline(Post(ServerAddress + "post?pageid=" + pageid))
        }   
        else{
          pipeline(Post(ServerAddress + "post?userid=" + uid))
        }
        Global.postsum += 1
      } 
      if (indexofmessage == 2) {
        pipeline(Post(ServerAddress + "friend_list?id=" + uid))
        Global.friendlistsum += 1
      } 
      if (indexofmessage == 3) {
        pipeline(Post(ServerAddress + "album?id=" + uid))
        Global.albumsum += 1
      } 
      if (indexofmessage == 4) {
        var rd = Random.nextInt(Global.albumsum)
        pipeline(Post(ServerAddress + "photo?userid=" + uid + "&albumid="+rd))
        Global.photosum += 1
      }
      }
    }
    
    def FaceBookAPIDelete()={
      if(uid != -1){
      var r = Random
      var indexofmessage = r.nextInt(6)
      if(indexofmessage == 0){
        var pageid = uid % Global.pagesum
        pipeline(Delete(ServerAddress + "page?id="+pageid))
        self ! ForceTerminate
      }
      if (indexofmessage == 1) {
        var rd = Random.nextInt(10)   
        if(rd == 0){
          var rand = Random.nextInt(Global.postsum)
          var id = uid % Global.pagesum
          pipeline(Delete(ServerAddress + "post?id=" + rand + "&pageid="+id))
        }   
        else{
          var rand = Random.nextInt(Global.postsum)
          pipeline(Delete(ServerAddress + "post?id=" + rand + "&userid="+uid))
        }
      } 
      if (indexofmessage == 2) {
        var rand = Random.nextInt(Global.friendlistsum)
        pipeline(Delete(ServerAddress + "friend_list?id=" + rand + "&userid="+uid))
      } 
      if (indexofmessage == 3) {
        var rand = Random.nextInt(Global.albumsum)
        pipeline(Delete(ServerAddress + "album?id=" + rand+ "&userid="+uid))
      } 
      if (indexofmessage == 4) {
        var rand = Random.nextInt(100)
        var rd = Random.nextInt(Global.photosum)
        pipeline(Delete(ServerAddress + "photo?id=" + rand + "&albumid="+rd))
      }
      if (indexofmessage == 5) {
        pipeline(Delete(ServerAddress + "user?id="+uid))
        self ! ForceTerminate
      }
      }
    }
    
    def receive = {
      
      case Login => {
        FaceBookLogin
      }
      
      case Create => {
        var result = pipeline(Post(ServerAddress + "user"))
        //println("Create new user:"+id+"\n")
        result.foreach {
        response =>
          //println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        uid = response.entity.asString.toInt
        println("Create new user:"+uid+"\n")
       }
        
        Global.usersum +=1
        self ! GetSchedule
        self ! PostSchedule
        self ! DeleteSchedule
      }

      case MakeGetRequest => {
        //context.system.scheduler.schedule(Duration.create(1000, TimeUnit.MILLISECONDS),
        //Duration.create(Global.frequency, TimeUnit.SECONDS)) {
          FaceBookAPIGet
        //}
        self ! GetSchedule
        
      }
      
      case MakePostRequest => {
        FaceBookAPIPost
        self ! PostSchedule
      }
      
      case MakeDeleteRequest => {
        FaceBookAPIDelete
        self ! DeleteSchedule
      }
      
      case GetSchedule => {
        getrequestcount+=1
        if((deleterequestcount+getrequestcount+postrequestcount)<=Global.numRequest){
          context.system.scheduler.scheduleOnce(100 milliseconds, self, MakeGetRequest)
        }
        else
          self ! Terminate       
      }
      
      case PostSchedule => {
        postrequestcount+=1
        if((deleterequestcount+getrequestcount+postrequestcount)<=Global.numRequest){
          context.system.scheduler.scheduleOnce(400 milliseconds, self, MakePostRequest)
        }
        else
          self ! Terminate       
      }
      
      case DeleteSchedule => {
        deleterequestcount+=1
        if((deleterequestcount+getrequestcount+postrequestcount)<=Global.numRequest){
          context.system.scheduler.scheduleOnce(800 milliseconds, self, MakeDeleteRequest)
        }
        else
          self ! Terminate       
      }
      
      case Terminate => {
        System.out.println("User "+uid+" has finished its operation. "+"Make get: "+getrequestcount+" post: "+postrequestcount+" dele: "+deleterequestcount+"\n")
        master ! FinishWork(uid)
        this.context.stop(self)
      }
      
      case ForceTerminate => {
        System.out.println("User "+uid+" has deleted itself\n")
        master ! FinishWork(uid)
        this.context.stop(self)
      }
    }
    def printResult(Result:Future[HttpResponse]) {
      Result.foreach {
        response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
          
      }
    }
  }
  
  object Global {

    var i = 0
    var IP = ""
    var ServerAddress = ""
    var clients = 0
    var numRequest = 0
    var Runningtime = 0
    var usersum = 1000
    var pagesum = 100
    var postsum = 2000
    var friendlistsum = 2000
    var albumsum = 2000
    var photosum = 10000
  }
  
  object User{
    case class User (
        id: String,           // The id of this person's user account
        email: String,        // The person's primary email address
        gender: String,       // The gender selected by this person, male or female
        first_name: String,   // The person's first name
        last_name: String,    // The person's last name
        verified: Boolean = false,    // Indicates whether the account has been verified
        middle_name: Option[String] = None,  // The person's middle name
        birthday: Option[String] = None,  // The person's birthday. MM/DD/YYYY
        link: Option[String] = None,     // A link to the person's Timeline
        locale: Option[String] = None,   // The person's locale
        timezone: Option[Float] = None // The person's current timezone offset from UTC
        //location: Option[Page] = None)  
     )// The person's current location
  }
}

