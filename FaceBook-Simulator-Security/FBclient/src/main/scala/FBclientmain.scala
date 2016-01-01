package FBclient

/**
 * @author Asks
 */
import akka.actor._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.parsing.json.JSON
import java.util.concurrent.TimeUnit
import java.util.ArrayList
import java.util.Collections
import scala.collection.mutable.HashMap
import java.security._
import javax.crypto._

import spray.http._
import spray.json._
import spray.client.pipelining._
import akka.actor.ActorSystem
import spray.httpx.RequestBuilding
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import org.json4s.ToJsonWritable
import org.json4s.{DefaultFormats, Formats}
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

case object Init
case object InitPublickey
case class InitFinish(uid:Int)
case object InitPost
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

case class test(id:String,content:String)
case class UserLongin(id:String,key:String,nounce:Array[Byte],password:String)
case class Postpost(id:String,access_token:String,postid:String,msg:String,key:String)



object FBsclientmain extends App with RequestBuilding{
  
  implicit val system = ActorSystem("FBclient")
  implicit val executionContext = system.dispatcher
  //val json4sFormats = DefaultFormats
  
  val pipeline : HttpRequest => Future[HttpResponse] = sendReceive
  
    var IPaddress = "192.168.142.1"
      //args(0)
    Global.numRequest = 10
    Global.clients = 10
    var ServerAddress = "http://"+IPaddress + ":2559/"    
    Global.ServerAddress = ServerAddress
    println(ServerAddress)
    var id = 1
    var context = "test"
    
    implicit val testFormat = jsonFormat2(test)
    val result = pipeline(Post(ServerAddress+"me",test("1","test")))
    
    /*("http://something.com/login", s"""{
        "email": "$email",
        "password": "$password"
    }""".asJson.asJsObject)*/
    var address = ""
    var code: String = _
    result.foreach {
      response =>
        println(s"\nRequest completed with status ${response.status}")
        val b = JSON.parseFull(response.entity.asString)  
        b match{
          case Some(map: Map[String, Any]) => {
            println(map)
            code= map("content").toString()
            
          }  
          case None => println("Parsing failed")  
          case other => println("Unknown data structure: " + other)
        }
        println(code)
        //println(response.entity.asString)

        println(" IPaddress of Server: " + IPaddress + "\n Client Users: " + Global.clients)
        System.setProperty("java.net.preferIPv4Stack", "true")
        
    }
  var clientmaster = system.actorOf(Props(new ClientMaster(Global.clients, address)), name = "clientmaster")
  clientmaster ! UserConnection
  //clientmaster ! Init

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
    var userinitfinish = 0
    var initid = 0

    def receive = {

      case SendRequest => {

        println(self.path + " In requestnow")
        self ! Create
      }
      
      case Init => {
         if (initid < clients) {
          clientusers(initid) = system.actorOf(Props(new ClientUser(initid,self)), name = "user" + initid)
          clientusers(initid) ! InitPublickey
          initid += 1
          context.system.scheduler.scheduleOnce(200 milliseconds,self,Init)
         }
      }
      
      case InitFinish(uid:Int) => {
        userinitfinish += 1
        println("user "+uid+" has finished init")
        if(userinitfinish == clients-1){
          userlist += uid
          println("publickey init finish")
          self ! CreateUsers
        }
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
          context.system.scheduler.scheduleOnce(200 milliseconds,self,UserConnection)
        }
        //else
          //self ! CreateUsers
      }
      
      case CreateUsers => {
        if (ccurid < clients) {
          var id = ccurid+clients
          clientusers(id) = system.actorOf(Props(new ClientUser(-1,self)), name = "user" + id)   
          clientusers(id) ! Create
          ccurid += 1
          context.system.scheduler.scheduleOnce(200 milliseconds,self,CreateUsers)
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
    //var postlist:ArrayBuffer[String] = new ArrayBuffer[String]()
    var postlist:List[Int] = _
    var friendlist:List[Int] = _
    var msg = "This is new post created by " + uid
    var encodemsg : String = _
    //var AESkey = hex_digest(System.currentTimeMillis.toString()).substring(0, 16)
    var AESkey: String =  _
    var modulus: BigInt = _
    var access_token: String =_
    var initpost = 0
    var postfiends: List[Int] = _
    var friendkeys:List[BigInt] = _
    
    var random = new Random
    private var p = BigInt.probablePrime(1024 / 2, random) 
    private var q = BigInt.probablePrime(1024 / 2, random)
    private var phi = (p - 1) * (q - 1)
    var usermodulus = p * q
    //var usermodulus: BigInt = _
    var userRSApublic : BigInt= 0x10001
    //var userRSAprivate = userRSApublic.modInverse(phi)
    var userRSAprivate = userRSApublic.modInverse(phi)
    val publicKey = 0x10001
    var friendpublickeys: List[String] = _
    
    def FaceBookLogin()={
      var nounce = Array[Byte](16)
      var result1 = pipeline(Get(ServerAddress + "me/login?id="+uid))
      result1.foreach {
        response =>
          val info = JSON.parseFull(response.entity.asString)
          info match{
          case Some(map: Map[String, Any]) => {
            println(map)
            //nounce = map("nounce").toString()   
            nounce = map("nounce").asInstanceOf[Array[Byte]]
            }
          case None => println("Parsing failed")  
          case other => println("Unknown data structure: " + other)
        }
          
      }
      implicit val loginFormat = jsonFormat4(UserLongin)
      var password = "123"
      //var result = pipeline(Get(ServerAddress + "me?id=" + uid))
      var result2 = pipeline(Get(ServerAddress + "me",UserLongin(uid.toString(),usermodulus.toString(),nounce,password)))
      result2.foreach {
        response =>
          println(s"\nRequest completed with status ${response.status}")
          val info = JSON.parseFull(response.entity.asString) 
          info match{
          case Some(map: Map[String, Any]) => {
            println(map)
            var RSAat= BigInt.apply(map("access_token").toString())
            var RSAdecrypt = new RSAdecrypt(1024,usermodulus,userRSAprivate)
            access_token = RSAdecrypt.decrypt1(RSAat)
            //access_token = map("access_token").toString()
            modulus = BigInt.apply(map("modulus").toString())
            if(map.contains("friend") && map.contains("post")){
              if(map("friend").isInstanceOf[List[_]])
                friendlist = map("friend").asInstanceOf[List[Int]]
              if(map("post").isInstanceOf[List[_]])
                postlist = map("post").asInstanceOf[List[Int]]
              if(map("publickeys").isInstanceOf[List[_]])
                friendpublickeys = map("publickeys").asInstanceOf[List[String]]
            }
            if(friendlist != null)
              friendlist = uid :: friendlist
          }  
          case None => println("Parsing failed")  
          case other => println("Unknown data structure: " + other)
        }

          self ! InitPost
      }

    }

    def FaceBookAPIGet()={
      if(uid != -1){
        var r = Random
      var indexofmessage = r.nextInt(6)
      indexofmessage = 1
      if(indexofmessage == 0){
        var rd = Random.nextInt(Global.pagesum)
        var result = pipeline(Get(ServerAddress + "page?id=" + rd))
        printResult(result)
      }
      if (indexofmessage == 1 && postlist != null && friendlist != null) {
        if(friendlist.length>0){
          var rd = Random.nextInt(friendlist.length)
          var friendid = (friendlist(rd).toString()).split('.')
          if(friendid(0) == uid){
            postfiends = postlist
          }
          else{
            var result = pipeline(Get(ServerAddress + "user/post?id=" + friendid(0)))
            result.foreach {
              response =>
                  println(s"\nRequest completed with status ${response.status}")
                  val info = JSON.parseFull(response.entity.asString) 
                  info match{
                    case Some(map: Map[String, Any]) => {
                       println(map)
                       if(map("post").isInstanceOf[List[_]])
                        postfiends = map("post").asInstanceOf[List[Int]]
                       println("postfiends "+postfiends)
                   }  
                   case None => println("Parsing failed")  
                   case other => println("Unknown data structure: " + other)
                 }
            }
          }
                
          if(postfiends != null){
            if(postfiends.length>0){
              var rd = Random.nextInt(postfiends.length)
             var postid = (postfiends(rd).toString()).split('.')
             var result = pipeline(Get(ServerAddress + "post?id=" + postid(0)+"&userid="+uid))
             result.foreach {
              response =>
                  println(s"\nRequest completed with status ${response.status}")
                  val info = JSON.parseFull(response.entity.asString) 
                  info match{
                    case Some(map: Map[String, Any]) => {
                       println(map)
                       var RSAdecrypt = new RSAdecrypt(1024,usermodulus,userRSAprivate)
                       var encodekey = BigInt.apply(map("key").toString())
                       var AESkey = RSAdecrypt.decrypt(encodekey)
                       var encodemsg = map("msg").toString()
                       println("Client "+uid+" get encode msg "+encodemsg+" "+encodemsg.length())
                       println("Client "+uid+" get encode key "+map("key").toString())
                       println("Client "+uid+" get AESkey "+AESkey)
                       var AESdecrypt = AES.decrypt(AESkey, encodemsg)
                       println("Client "+uid+" get msg "+AESdecrypt+"\n")
                   }  
                   case None => println("Parsing failed")  
                   case other => println("Unknown data structure: " + other)
                 }
           }
            }
          
          //printResult(result)
        }
        }
     
      } 
      if (indexofmessage == 2 && friendlist != null) {
        if(friendlist.length > 0){
          var rd = Random.nextInt(friendlist.length)
          var friendid = (friendlist(rd).toString()).split('.')
          var result = pipeline(Get(ServerAddress + "friend_list?id=" + friendid(0)))
          printResult(result)
        }
        //var rd = Random.nextInt(Global.friendlistsum)
        
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
      var indexofmessage = r.nextInt(2)
      indexofmessage = 1
      if(indexofmessage == 0){
        pipeline(Post(ServerAddress + "page"))
        Global.pagesum += 1
      }
      if (indexofmessage == 1) {
        initpost += 1
        var rd = Random.nextInt(10)
        var keyGen = KeyGenerator.getInstance("AES")
          var key1 = keyGen.generateKey()
          var AESKEY = key1.getEncoded
          while(AESKEY(0)<0){
            keyGen = KeyGenerator.getInstance("AES")
            key1 = keyGen.generateKey()
            AESKEY = key1.getEncoded
          }
          for (elem <- AESKEY) {  
             // print(elem+",")
              print(elem.toString() + " , ")  
          }
          println("AESkey is "+AESKEY+" length "+AESKEY.length)
          encodemsg = AES.encrypt(AESKEY,msg)    
          var encodekey = new RSAencrypt(1024,modulus)       
          println("client "+uid+" has msg "+msg)
          println("client "+uid+" has encodemsg "+encodemsg+"\n") 
          var RSAat = new RSAencrypt(1024,usermodulus)
          var at = RSAat.encrypt1(access_token, userRSAprivate)
          var RSAkey = encodekey.encrypt(AESKEY,publicKey)
        implicit val postFormat = jsonFormat5(Postpost)
        //pipeline(Post(ServerAddress + "post?userid=" + uid +"&msg=" + encodemsg+"&encodekey="+RSAkey))
          pipeline(Post(ServerAddress + "post",Postpost(uid.toString(),at.toString(),(uid*100+initpost).toString(),encodemsg,RSAkey.toString())))
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
      case InitPublickey=>{
        var random = new Random
       var p = BigInt.probablePrime(1024 / 2, random) 
       var q = BigInt.probablePrime(1024 / 2, random)
       var phi = (p - 1) * (q - 1)
       usermodulus = p * q
       userRSAprivate = userRSApublic.modInverse(phi)
       var result = pipeline(Post(ServerAddress + "user/me?id="+uid+"&publickey="+usermodulus))
       println("user "+uid+" privatekey "+userRSAprivate)
       master ! InitFinish(uid)
      }  
      
      case InitPost => {
        if(initpost<10){
          msg = "This is new post created by " + uid
          //AESkey = hex_digest(System.currentTimeMillis.toString()).substring(0, 16)
          /*var ekey = new SecureRandom()
          var byte = new Array[Byte](16)
          ekey.nextBytes(byte)*/
          //AESkey = new String(byte,"GB2312")
          var keyGen = KeyGenerator.getInstance("AES")
          keyGen.init(128)
          var key1 = keyGen.generateKey()
          var AESKEY = key1.getEncoded
          while(AESKEY(0)<0){
            keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(128);
            key1 = keyGen.generateKey()
            AESKEY = key1.getEncoded
          }
          for (elem <- AESKEY) {  
             // print(elem+",")
              print(elem.toString() + " , ")  
          }
          println("AESkey is "+AESKEY+" length "+AESKEY.length)
          encodemsg = AES.encrypt(AESKEY,msg)    
          var encodekey = new RSAencrypt(1024,modulus)       
          println("client "+uid+" has msg "+msg)
          println("client "+uid+" has encodemsg "+encodemsg+"\n") 
          var RSAat = new RSAencrypt(1024,usermodulus)
          var at = RSAat.encrypt1(access_token, userRSAprivate)
          var RSAkey = encodekey.encrypt(AESKEY,publicKey) 
          println("access_token is "+access_token)
          println("at is "+at)
          implicit val postFormat = jsonFormat5(Postpost)
          var result = pipeline(Post(ServerAddress + "post",Postpost(uid.toString(),at.toString(),(uid*100+initpost).toString(),encodemsg,RSAkey.toString())))
          Global.postsum += 1
          postlist = (uid*100+initpost) :: postlist
          initpost += 1
          
          //println("user "+uid+" create a new post\n")
          context.system.scheduler.scheduleOnce(200 milliseconds,self,InitPost)
        }
        else{
          println("user "+uid+" has postlist"+postlist)
          self ! GetSchedule
          self ! PostSchedule
          self ! DeleteSchedule
          master ! InitFinish(uid)
        }
      }
      
    case Login => {
        FaceBookLogin
      }
      
      case Create => {
        var random = new Random
        var p = BigInt.probablePrime(1024 / 2, random) 
        var q = BigInt.probablePrime(1024 / 2, random)
        var phi = (p - 1) * (q - 1)
        usermodulus = p * q
        userRSAprivate = userRSApublic.modInverse(phi)
        var result = pipeline(Post(ServerAddress + "user?publickey="+usermodulus))
        //println("Create new user:"+id+"\n")
        /*result.foreach {
        response =>
          //println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        uid = response.entity.asString.toInt
        println("Create new user:"+uid+"\n")*/
        result.foreach {
        response =>
          val info = JSON.parseFull(response.entity.asString) 
          info match{
          case Some(map: Map[String, Any]) => {
            println(map)
            uid = Integer.parseInt(map("id").toString())
            modulus= BigInt.apply(map("modulus").toString())
            var RSAat= BigInt.apply(map("access_token").toString())
            var RSAdecrypt = new RSAdecrypt(1024,usermodulus,userRSAprivate)
            access_token = RSAdecrypt.decrypt(RSAat).toString()
            //access_token = map("access_token").toString()
            println("New userid "+uid+" has been created!")
            println("We have modulus "+modulus+"\n")
            
          }  
          case None => println("Parsing failed")  
          case other => println("Unknown data structure: " + other)
        }
        self ! GetSchedule
        self ! PostSchedule
        self ! DeleteSchedule
       }
        
        Global.usersum +=1
        
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
          context.system.scheduler.scheduleOnce(200 milliseconds, self, MakeGetRequest)
        }
        else
          self ! Terminate       
      }
      
      case PostSchedule => {
        postrequestcount+=1
        if((deleterequestcount+getrequestcount+postrequestcount)<=Global.numRequest){
          context.system.scheduler.scheduleOnce(800 milliseconds, self, MakePostRequest)
        }
        else
          self ! Terminate       
      }
      
      case DeleteSchedule => {
        deleterequestcount+=1
        if((deleterequestcount+getrequestcount+postrequestcount)<=Global.numRequest){
          context.system.scheduler.scheduleOnce(1500 milliseconds, self, MakeDeleteRequest)
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
        //response =>
          //println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        response =>
                  println(s"\nRequest completed with status ${response.status}")
                  val info = JSON.parseFull(response.entity.asString) 
                  info match{
                    case Some(map: Map[String, Any]) => {
                       println(map)
                   }  
                   case None => println("Parsing failed")  
                   case other => println("Unknown data structure: " + other)
                 }   
      }
    }
    
    def hex_digest(s: String): String = {
    val sha = MessageDigest.getInstance("SHA-256")
    sha.digest(s.getBytes)
    .foldLeft("")((s: String, b: Byte) => s +
                  Character.forDigit((b & 0xf0) >> 4, 16) +
                  Character.forDigit(b & 0x0f, 16))
   }
    
    /*def translate(msg: String): String = {
      println(msg)
      var message = msg.replaceAll("\\+", "%2B")
      message = message.replaceAll("\\=", "%3D")
      message
    }*/
  }
  
  object Global {

    var i = 0
    var IP = ""
    var ServerAddress = ""
    var clients = 0
    var numRequest = 0
    var Runningtime = 0
    var usersum = 5
    var pagesum = 100
    var postsum = -1
    var friendlistsum = 20
    var albumsum = 2000
    var photosum = 10000
    var privatekeys = HashMap.empty[Int,BigInt]
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

