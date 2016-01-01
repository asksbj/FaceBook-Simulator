import akka.actor._
import akka.util._
import akka.pattern._
import java.util.ArrayList;
import scala.concurrent._
import spray.routing.{Route, HttpService}
import spray.httpx.Json4sSupport
import org.json4s.{DefaultFormats, Formats}
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import spray.http.MediaTypes._
import spray.http._
import spray.json._

case class test(id:String,content:String)

class FBRest(ServerAddress: String) extends Actor with Service {
  
  implicit def actorRefFactory = context
  actorRefFactory.actorOf(Props(new ServerMaster()),"ServerMaster")
  def receive = runRoute(rout)
  
}

case class Answer(code: String, content:String)

object MasterJsonProtocol extends DefaultJsonProtocol {
  implicit val anwserFormat = jsonFormat2(Answer)
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
            respondWithMediaType(`application/json`){
              ctx => sw ! GetPageRequest(para,ctx)
            }
            

         }
      }~
      path("user"/"post"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx =>sw ! GetUserPostRequest(para,ctx)       
            //complete("Get post: " + params);
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
        //parameterSeq{ params =>
            //val para = params
        entity(as[UserLongin]) { login =>
            var para = Seq[(String,String)]()
            para = para:+("id",login.id)
            para = para:+("key",login.key)
            para = para:+("password",login.password)
            var nounce = login.nounce
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx => sw ! CheckLogin(para,nounce,ctx)
         
            complete("from client : " + para);
         }
      }~
      path("me"/"login"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            actorRefFactory.actorSelection("ServerMaster") ! Create(1)
            ctx => sw ! LoginRequest(para,ctx)

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
          //parameterSeq{ params =>
            //val para = params
         entity(as[Postpost]) { post =>
            var para = Seq[(String,String)]()
            para = para:+("userid",post.id)
            para = para:+("msg",post.msg)
            para = para:+("encodekey",post.key)
            para = para:+("access_token",post.access_token)
            para = para:+("postid",post.postid)
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            sw ! PostPostRequest(para)
            complete("Post post: " + para);
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
      }~
      path("me") {
        entity(as[test]) { test =>
          println(test.id)
          println(test.content)
          complete(Answer("1","test"))
        }
      }~
      path("user"/"me"){
        parameterSeq{ params =>
            val para = params
            val sw: ActorRef = actorRefFactory.actorOf(Props(new ServerWorker()))
            ctx => sw ! CreatePublickey(para,ctx)
            //complete("Post photo: " + params);
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