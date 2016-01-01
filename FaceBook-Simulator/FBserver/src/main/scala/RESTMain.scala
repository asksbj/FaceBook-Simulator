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

case class CheckLogin(para: Seq[(String,String)],ctx: RequestContext)
case class GetPageRequest(para: Seq[(String,String)],ctx: RequestContext)
case class PostPageRequest(para: Seq[(String,String)])
case class DeletePageRequest(para: Seq[(String,String)])
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
  