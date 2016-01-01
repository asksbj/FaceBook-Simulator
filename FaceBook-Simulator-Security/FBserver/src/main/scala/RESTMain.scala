import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import akka.actor._
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import spray.can.Http
import spray.routing.{Route, HttpService, RequestContext}

import scala.concurrent.duration._
//import spray.can.Http.RegisterChunkHandle
/**
 * @author Asks
 */
case object Initiate
case class GetConnected(client: ActorRef)
case class Create(clients: Int)
case class CreatePublickey(para: Seq[(String,String)],ctx: RequestContext)

case class LoginRequest(para: Seq[(String,String)],ctx: RequestContext)
case class CheckLogin(para: Seq[(String,String)],nounce:Array[Byte],ctx: RequestContext)
case class GetPageRequest(para: Seq[(String,String)],ctx: RequestContext)
case class PostPageRequest(para: Seq[(String,String)])
case class DeletePageRequest(para: Seq[(String,String)])
case class GetUserPostRequest(para: Seq[(String,String)],ctx: RequestContext)
case class GetPostRequest(para: Seq[(String,String)],ctx: RequestContext)
case class PostPostRequest(para: Seq[(String,String)])
case class DeletePostRequest(para: Seq[(String,String)])
case class GetFriendRequest(para: Seq[(String,String)],ctx: RequestContext)
case class PostFriendListRequest(para: Seq[(String,String)])
case class DeleteFriendListRequest(para: Seq[(String,String)])
case class GetProfileRequest(para: Seq[(String,String)],ctx: RequestContext)
case class CreateUserRequest(para: Seq[(String,String)],ctx: RequestContext)
case class DeleteUserRequest(para: Seq[(String,String)])
case class GetAlbumRequest(para: Seq[(String,String)],ctx: RequestContext)
case class PostAlbumRequest(para: Seq[(String,String)])
case class DeleteAlbumRequest(para: Seq[(String,String)])
case class GetPictureRequest(para: Seq[(String,String)],ctx: RequestContext)
case class PostPictureRequest(para: Seq[(String,String)])
case class DeletePictureRequest(para: Seq[(String,String)])

case class user(id:String,access_token:String,username:String,email:String,post:Array[Int], friend: Array[Int], publickeys: Array[String], modulus:String)
case class newuser(id:String,access_token:String,modulus:String)
case class Login(nounce:Array[Byte])
case class UserLongin(id:String,key:String,nounce:Array[Byte],password:String)
case class Getpage(id:String,name:String,post:Array[Int])
case class Getuserpost(id:String,post:Array[Int])
case class Getpost(id:String,creator:String,msg:String,key:String)
case class Getfriend(id:String,friend:Array[Int])
case class Getprofile(id:String,name:String,gender:String,birthday:String)
case class Getalbum(id:String,name:String,photo:Array[Int])
case class Getphoto(id:String,name:String,picture:String)
case class Postpost(id:String,access_token:String,postid:String,msg:String,key:String)

case class ReturnPage(userid: Int, pageid: Int)
object RESTMain extends App {
  override def main(args: Array[String]): Unit={
    val hostIP = InetAddress.getLocalHost.getHostAddress()
    println(hostIP)
    val config = ConfigFactory.parseString("""
      akka {
         actor {
             provider = "akka.remote.RemoteActorRefProvider"
               }
         remote {
             enabled-transports = ["akka.remote.netty.tcp"]
         netty.tcp {
             hostname = """ + hostIP + """
             port = 0
                   }
               }
          }
      """) 
    implicit val system = ActorSystem("FBserver")
    //implicit val system = ActorSystem("FBserver", ConfigFactory.load(config))
    val service = system.actorOf(Props(new FBRest(hostIP)), "FBRest")

    
    IO(Http) ! Http.Bind(service, interface = hostIP, port = 2559)
    println("Listening on port 2559, waiting for requests")
    
  }  
}
  