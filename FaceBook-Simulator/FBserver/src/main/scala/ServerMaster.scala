import akka.actor._
import scala.util.Random
import java.util.ArrayList
import java.util.Collections
import scala.concurrent.duration._
import scala.util.control.Breaks._
import java.util.concurrent.TimeUnit
import akka.routing.RoundRobinRouter
//import org.json4s.native.Serialization
import scala.collection.mutable.HashMap
import com.typesafe.config.ConfigFactory
//import org.json4s.native.Serialization._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.SynchronizedBuffer
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * @author Asks
 */
class ServerMaster extends Actor{
  println("create")
  var users: Int = 0
  //var db = new HashMap[String, String]
  //val serverworker = context.actorOf(Props[ServerWorker].withRouter(RoundRobinRouter(s_actors)), name = "ServerWorker")
  for(i<-0 to Server.usersum-1){
    //Server.users.keySet(i)
    Server.users += (i -> new Users)
    Server.users.get(i).get.id = i
  }
  
  println("Users init")

  
  for(i<-0 to Server.pagesum-1){
    Server.pages += (i -> new Pages)
    Server.pages.get(i).get.id = i
  }
  
  println("Pages init")
    
  for(i<-0 to Server.postsum-1){
    Server.posts += (i -> new Posts)
    Server.posts.get(i).get.id = i
    var rand:Int= Random.nextInt(10)
    if(rand != 0){
      var rd:Int= Random.nextInt(Server.usersum)
      Server.posts.get(i).get.creatortype = "user"
      Server.posts.get(i).get.creator = rd
      Server.users.get(rd).get.post += (i -> Server.posts(i))
    }
    else{
      var rd:Int= Random.nextInt(Server.pagesum)
      Server.posts.get(i).get.creatortype = "page"
      Server.posts.get(i).get.creator = rd
      Server.pages.get(rd).get.post += (i -> Server.posts(i))
    }  
  }
  println("Posts init")
  
  for(i<-0 to Server.friendlistsum-1){
    Server.friends += (i -> new Friends)
    Server.friends.get(i).get.id = i  
    var rd:Int= Random.nextInt(Server.usersum)
    Server.friends.get(i).get.owner = rd
    Server.users.get(rd).get.friend += (i -> Server.friends(i))
    
    for(j<-0 to 10){
      var rand:Int= Random.nextInt(Server.usersum)
      while(rand == rd || Server.friends.get(i).get.members.contains(rand))
        rand = Random.nextInt(Server.usersum)
      Server.friends.get(i).get.members += (rand -> Server.users(rand))
    }
  }
  println("Friends init")
  
   for(i<-0 to Server.albumsum-1){
    Server.albums += (i -> new Albums)
    Server.albums.get(i).get.id = i
    var rd:Int= Random.nextInt(Server.usersum)
    Server.albums.get(i).get.from = rd
    
   }
   println("Albums init")

   
   for(i<-0 to Server.photosum-1){
    Server.photos += (i -> new Photos)
    Server.photos.get(i).get.id = i
    var rd:Int= Random.nextInt(Server.albumsum)
    Server.photos.get(i).get.album = rd
    var rand: Int= Random.nextInt(Server.usersum)
    Server.photos.get(i).get.from = rand
   }
   println("Photos init")
   println("Initialization process has finished")
  
  def receive = {
    
    case Create(clientid) => { 
      //val serverworker = context.actorOf(Props[ServerWorker].withRouter(RoundRobinRouter(clients)), name = "ServerWorker")
      Server.totalusers += 1
      Server.endid += 1
    }
      
    
  }
}


