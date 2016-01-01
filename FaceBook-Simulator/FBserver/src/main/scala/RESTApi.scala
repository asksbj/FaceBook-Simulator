import akka.actor._
import akka.util._
import akka.pattern._
import scala.concurrent._
import spray.routing.{Route, HttpService}
import spray.httpx.Json4sSupport
import org.json4s.{DefaultFormats, Formats}
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import spray.http.MediaTypes._
import spray.http._


class FBRest(ServerAddress: String) extends Actor with Service {
  
  implicit def actorRefFactory = context
  actorRefFactory.actorOf(Props(new ServerMaster()),"ServerMaster")
  def receive = runRoute(rout)
  
}


trait Service extends HttpService with Json4sSupport {
  
  val json4sFormats = DefaultFormats
  val rout = {
    get {
      path("page") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            //actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            //sw ! GetPageRequest(para) 
            //HttpEntity(ContentTypes.`application/json`, result)
            //complete("Get page: " + params);
            ctx => sw ! GetPageRequest(para,ctx)

         }
      }~
      path("post"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx =>sw ! GetPostRequest(para,ctx)       
            //complete("Get post: " + params);
         }
      }~
      path("friend_list"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            ctx => sw ! GetFriendRequest(para,ctx)
             
            //complete("Get friends: " + params);
         }
      }~
      path("profile"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            ctx => sw ! GetProfileRequest(para,ctx)
             
            //complete("Get profile: " + params);
        }
      }~
      path("me"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx => sw ! CheckLogin(para,ctx)
         
            complete("from client : " + para);
         }
      }~
      path("album") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx => sw ! GetAlbumRequest(para,ctx)
             
            //complete("Get album: " + params)
         }
      }~
      path("album"/"photo") {
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            //actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            ctx => sw ! GetPictureRequest(para,ctx)
            
            /*respondWithMediaType(`application/json`){
              complete{
                 "Get photo: " + params
              }
            }*/
             
            //complete("Get photo: " + params);
         }
      }     
    }~ 
    post{
      path("page") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            //actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            sw ! PostPageRequest(para)
            complete("Post page: " + params);
         }
      }~
      path("post") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! PostPostRequest(para)
            complete("Post post: " + params);
         }
      }~
      path("friend_list") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! PostFriendListRequest(para)
            complete("Post friendlist: " + params);
         }
      }~
      path("user") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx =>sw ! CreateUserRequest(para,ctx)
            //complete("Create user: " + params);
         }
      }~
      path("album") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! PostAlbumRequest(para)
            complete("Post album: " + params);
         }
      }~
      path("photo") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! PostPictureRequest(para)
            complete("Post photo: " + params);
         }
      }
    }~
    delete{
      path("page") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            //actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            sw ! DeletePageRequest(para)
            complete("Delete page: " + params);
         }
      }~
      path("post") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! DeletePostRequest(para)
            complete("Delete post: " + params);
         }
      }~
      path("friend_list") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! DeleteFriendListRequest(para)
            complete("Delete friendlist: " + params);
         }
      }~
      path("user") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! DeleteUserRequest(para)
            complete("Delete user: " + params);
         }
      }~
      path("album") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! DeleteAlbumRequest(para)
            complete("Delete album: " + params);
         }
      }~
      path("photo") { 
          parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! DeletePictureRequest(para)
            complete("Delete photo: " + params);
         }
      }
    }

    //https://www.facebook.com/feeds/page.php?id=xxxxxxxxxxx&format=rss20

  }
}