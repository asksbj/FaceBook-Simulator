import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.routing.{Route, HttpService, RequestContext}
import spray.http.MediaTypes._
import scala.collection.mutable.ArrayBuffer


/**
 * @author Asks
 */
class ServerWorker extends Actor with ActorLogging {
  
  
  def receive = {
    case CheckLogin(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.users.contains(Integer.parseInt(para(0)._2))){
        var profile = Server.users.get(Integer.parseInt(para(0)._2))   
        var message = "id:"+profile.get.id+"\nusername:"+profile.get.username+"\ngender:"+profile.get.gender+
                    "\nbirthday:"+profile.get.birthday+"\nlink:"+profile.get.link;
        ctx.complete("Login. User information:\n"+message+"\n")
      }
      else
        Server.users += (para(0)._2.toInt->new Users)
        Server.users(para(0)._2.toInt).id = para(0)._2.toInt
        ctx.complete("User "+para(0)._2+" created!\n")
    }
    case GetPageRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.pages.contains(Integer.parseInt(para(0)._2))){
        var page = Server.pages.get(Integer.parseInt(para(0)._2))   
        var message = "id:"+page.get.id+"\nname:"+page.get.name+"\nlink:"+page.get.link
        ctx.complete("Get page information:\n"+message+"\n")
      }
      else
        ctx.complete("Page "+para(0)._2+" does not exist!\n")
      
      this.context.stop(self)
    }
    
    case GetPostRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.posts.contains(Integer.parseInt(para(0)._2))){
        var post = Server.posts.get(Integer.parseInt(para(0)._2)) 
        var message = "id:"+post.get.id+"\ncreator:"+post.get.creator+"\ncreatortype:"+post.get.creatortype+"\ncreate time:"+post.get.create_time;
        ctx.complete("Get post information:\n"+message+"\n")
      }
      else
        ctx.complete("Post "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetFriendRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.friends.contains(Integer.parseInt(para(0)._2))){
        var friend = Server.friends.get(Integer.parseInt(para(0)._2)) 
        var message = "id:"+friend.get.id+"\nname:"+friend.get.name+"\nowner:"+friend.get.owner;
        ctx.complete("Get friendlist information:\n"+message+"\n")
      }
      else
        ctx.complete("Friend list "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetProfileRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.users.contains(Integer.parseInt(para(0)._2))){
        var profile = Server.users.get(Integer.parseInt(para(0)._2)) 
        var message = "id:"+profile.get.id+"\nusername:"+profile.get.username+"\ngender:"+profile.get.gender+
                    "\nbirthday:"+profile.get.birthday+"\nlink:"+profile.get.link;
        ctx.complete("Get profile information:\n"+message+"\n")
      }
      else
        ctx.complete("User "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetAlbumRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.albums.contains(Integer.parseInt(para(0)._2))){
        var album = Server.albums.get(Integer.parseInt(para(0)._2))
        var message = "id:"+album.get.id+"\nname:"+album.get.name+"\nfrom:"+album.get.from+"\ncreate time:"+album.get.create_time+"\nlink"+album.get.link;
        ctx.complete("Get page information:\n"+message+"\n")
      }
      else
        ctx.complete("Album "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetPictureRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.photos.contains(Integer.parseInt(para(0)._2))){
        var photo = Server.photos.get(Integer.parseInt(para(0)._2)) 
        var message = "id:"+photo.get.id+"\nname:"+photo.get.name+"\nalbum:"+photo.get.album+"\nfrom:"+photo.get.from+"\npicture:"+photo.get.picture;
        ctx.complete("Get page information:\n"+message+"\n")
      }
      else
        ctx.complete("Photo "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case PostPageRequest(para: Seq[(String,String)]) => {
      Server.pagesum += 1
      Server.pages += (Server.pagesum->new Pages)
      Server.pages(Server.pagesum).id = Server.pagesum
      System.out.println("Create page: "+Server.pages(Server.pagesum).id)
      this.context.stop(self)
    }
    
    case PostPostRequest(para: Seq[(String,String)]) => {
      Server.postsum += 1
      Server.posts += (Server.postsum->new Posts)
      Server.posts(Server.postsum).id = Server.postsum
      if(para(0)._1.equals("userid")&& Server.users.contains(para(0)._2.toInt)){
        Server.posts(Server.postsum).creatortype = "user"
        Server.posts(Server.postsum).creator = para(0)._2.toInt
        Server.users(para(0)._2.toInt).post += (Server.postsum -> Server.posts(Server.postsum))
      }
      else if(para(0)._1.equals("pageid")&& Server.pages.contains(para(0)._2.toInt)){
          Server.posts(Server.postsum).creatortype = "page"
          Server.posts(Server.postsum).creator = para(0)._2.toInt
          Server.pages(para(0)._2.toInt).post += (Server.postsum -> Server.posts(Server.postsum))
      }
      System.out.println("Create post: "+Server.posts(Server.postsum).id+" by "+Server.posts(Server.postsum).creatortype+" "+Server.posts(Server.postsum).creator)
      this.context.stop(self)
    }
    
    case PostFriendListRequest(para: Seq[(String,String)]) => {
	  if(Server.users.contains(para(0)._2.toInt)){
      Server.friendlistsum += 1
      Server.friends += (Server.friendlistsum->new Friends)
      Server.friends(Server.friendlistsum).id = Server.friendlistsum
      Server.friends(Server.friendlistsum).owner = para(0)._2.toInt
      Server.users(para(0)._2.toInt).friend += (Server.friendlistsum -> Server.friends(Server.friendlistsum))
      System.out.println("Create friendlist: "+Server.friends(Server.friendlistsum).id+" by user "+Server.friends(Server.friendlistsum).owner)
	  }
      this.context.stop(self)
    }
    
    //case PostFriendRequest
    
    case CreateUserRequest(para: Seq[(String,String)],ctx: RequestContext) => {  
      Server.users += (Server.usersum->new Users)
      Server.users(Server.usersum).id = Server.usersum
      System.out.println("Create user: "+Server.users(Server.usersum).id)
      ctx.complete(Server.usersum.toString())
      Server.usersum += 1   
      this.context.stop(self)
    }
    
    case PostAlbumRequest(para: Seq[(String,String)]) => {
	  if(Server.users.contains(para(0)._2.toInt)){
      Server.albumsum += 1
      Server.albums += (Server.albumsum->new Albums)
      Server.albums(Server.albumsum).id = Server.albumsum
      Server.albums(Server.albumsum).from = para(0)._2.toInt
      System.out.println("Create album: "+Server.albums(Server.albumsum).id+" by user "+Server.albums(Server.albumsum).from)
	  }
      this.context.stop(self)
    }
    
    case PostPictureRequest(para: Seq[(String,String)]) => {
      Server.photosum += 1
      Server.photos += (Server.photosum->new Photos)
      Server.photos(Server.photosum).id = Server.photosum
      Server.photos(Server.photosum).from = para(0)._2.toInt
      Server.photos(Server.photosum).album = para(1)._2.toInt
      System.out.println("Create album: "+Server.photos(Server.photosum).id+" by user "+Server.photos(Server.photosum).from+" in album "+Server.photos(Server.photosum).album)
      this.context.stop(self)
    }
    
    case DeletePageRequest(para: Seq[(String,String)]) => {
      Server.pages -= para(0)._2.toInt
      System.out.println("Dreate page: "+para(0)._2.toInt)
      this.context.stop(self)
    }
    
    case DeletePostRequest(para: Seq[(String,String)]) => {    
      if(para(1)._1.equals("userid")){
        if(Server.users(para(1)._2.toInt).post.contains(para(0)._2.toInt)){
          Server.posts -= para(0)._2.toInt
          Server.users(para(1)._2.toInt).post -= para(0)._2.toInt
          System.out.println("Dreate post: "+Server.posts(Server.postsum).id+" by user "+para(1)._2.toInt)
        }  
      }
      else if(para(1)._1.equals("pageid")&&Server.pages.contains(para(0)._2.toInt)){
        if(Server.pages(para(1)._2.toInt).post.contains(para(0)._2.toInt)){
          Server.posts -= para(0)._2.toInt
          Server.pages(para(0)._2.toInt).post -= para(0)._2.toInt
          System.out.println("Dreate post: "+Server.posts(Server.postsum).id+" by page "+para(1)._2.toInt)
        }   
      }
      
      this.context.stop(self)
    }
    
    case DeleteFriendListRequest(para: Seq[(String,String)]) => {
      if(Server.users(para(1)._2.toInt).friend.contains(para(0)._2.toInt)){
        Server.friends -= para(0)._2.toInt
        Server.users(para(1)._2.toInt).friend -= para(0)._2.toInt
        System.out.println("Dreate friendlist: "+para(0)._2.toInt+" by user "+para(1)._2.toInt)
      }
      this.context.stop(self)
    }
    
    //case PostFriendRequest
    
    case DeleteUserRequest(para: Seq[(String,String)]) => {
      Server.users -= para(0)._2.toInt
      System.out.println("Delete user: "+para(0)._2.toInt)
      this.context.stop(self)
    }
    
    case DeleteAlbumRequest(para: Seq[(String,String)]) => {
      Server.albums -= para(0)._2.toInt
      System.out.println("Delete album: "+para(0)._2.toInt+" by user "+para(1)._2.toInt)
      this.context.stop(self)
    }
    
    case DeletePictureRequest(para: Seq[(String,String)]) => {
      Server.photos -= para(0)._2.toInt
      System.out.println("Delete album: "+para(0)._2.toInt+" by user "+para(1)._2.toInt)
      this.context.stop(self)
    }
    
    

      
    }
}