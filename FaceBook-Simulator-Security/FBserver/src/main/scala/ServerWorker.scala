import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats}
import spray.json._
import DefaultJsonProtocol._
import spray.routing.{Route, HttpService, RequestContext}
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import scala.collection.mutable.ArrayBuffer
import java.security.MessageDigest
import scala.util.Random
import javax.crypto._

/**
 * @author Asks
 */
class ServerWorker extends Actor with ActorLogging {
  
  
  def receive = {
    case CreatePublickey(para: Seq[(String,String)],ctx: RequestContext) => {
      var profile = Server.users.get(Integer.parseInt(para(0)._2))
      Server.userpublickey += (Integer.parseInt(para(0)._2) -> BigInt.apply(para(1)._2))
      ctx.complete("complete")
    }
    
    case LoginRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      //var nounce = hex_digest(System.currentTimeMillis.toString()).substring(0, 16)
      var keyGen = KeyGenerator.getInstance("AES")
      keyGen.init(128);
      var key1 = keyGen.generateKey()
      var nounce = key1.getEncoded
      while(nounce(0)<0){
        keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        key1 = keyGen.generateKey()
        nounce = key1.getEncoded
      }
      Server.nounces += (para(0)._2.toInt -> nounce)
      implicit val userFormat = jsonFormat1(Login)
      ctx.complete(Login(nounce))
    }
    
    case CheckLogin(para: Seq[(String,String)],nounce:Array[Byte],ctx: RequestContext) => {
      if(Server.users.contains(Integer.parseInt(para(0)._2))){
        if(nounce.equals(Server.nounces(para(0)._2.toInt)))
          Server.nounces -= (para(0)._2.toInt)
        Server.userpublickey += (Integer.parseInt(para(0)._2) -> BigInt.apply(para(1)._2))
        var numfriends = 0
        var profile = Server.users.get(Integer.parseInt(para(0)._2))   
        //var message = "id:"+profile.get.id+"\nusername:"+profile.get.username+"\ngender:"+profile.get.gender+"\nbirthday:"+profile.get.birthday+"\nlink:"+profile.get.link;
        var fr = profile.get.friend.keySet
        var po = profile.get.post.keySet
        var friend = new Array[Int](fr.size)
        var publickeys = new Array[String](fr.size)
        var post = new Array[Int](po.size)
        var gays = new Array[Int](numfriends)
        
        for(i<-0 to profile.get.friend.size-1){
          friend(i) = fr.toList(i)
          publickeys(i) = Server.userpublickey.get(fr.toList(i)).toString()
          //gays(i) = profile.get.friend.get(fr.toList(i)).get.members.keySet.toList(i)
        }
        for(i<-0 to profile.get.post.size-1){
          post(i) = po.toList(i)
        }
        var RSAaccess_token = new RSAencrypt(1024,BigInt.apply(para(1)._2))
        var access_token = RSAaccess_token.encrypt(profile.get.access_token.getBytes)
        implicit val userFormat = jsonFormat8(user)
        //ctx.complete("Login."+Server.modulus+"\n")
        ctx.complete(user(profile.get.id.toString(),access_token.toString(),profile.get.username,profile.get.email,post,friend,publickeys,Server.modulus.toString()))
      }
      else{
        Server.userpublickey += (Integer.parseInt(para(0)._2) -> BigInt.apply(para(1)._2))
        println("user "+para(0)._2+" has public key "+para(1)._2+"\n")
        Server.users += (para(0)._2.toInt->new Users)
        Server.users(para(0)._2.toInt).id = para(0)._2.toInt
        Server.users(para(0)._2.toInt).access_token = hex_digest(System.currentTimeMillis.toString()+para(0)._2.toInt).substring(0, 16)
        var profile = Server.users.get(para(0)._2.toInt)
        var RSAaccess_token = new RSAencrypt(1024,BigInt.apply(para(1)._2))
        var access_token = RSAaccess_token.encrypt(profile.get.access_token.getBytes)
        implicit val newuserFormat2 = jsonFormat3(newuser)
        ctx.complete(newuser(profile.get.id.toString(),access_token.toString(),Server.modulus.toString()))
      }
    }
    case GetPageRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.pages.contains(Integer.parseInt(para(0)._2))){
        var page = Server.pages.get(Integer.parseInt(para(0)._2))   
        //var message = "id:"+page.get.id+"\nname:"+page.get.name+"\nlink:"+page.get.link
        //ctx.complete("Get page information:\n"+message+"\n")
        var posts: Array[Int] = new Array[Int](page.get.post.size)
        var po = page.get.post.keySet
        for(i<-0 to page.get.post.size-1){
          posts(i) = po.toList(i)
        }
        implicit val getpageFormat2 = jsonFormat3(Getpage)
        ctx.complete(Getpage(page.get.id.toString(),page.get.name,posts))
      }
      //else
        //ctx.complete("Page "+para(0)._2+" does not exist!\n")
      
      this.context.stop(self)
    }
    
    case GetUserPostRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      implicit val getuserpostFormat2 = jsonFormat2(Getuserpost)
      if(Server.friends.contains(Integer.parseInt(para(0)._2)) && Server.users.contains(Integer.parseInt(para(0)._2))){
        if(Server.users(Integer.parseInt(para(0)._2)).friend.contains(Integer.parseInt(para(0)._2))){
          var frlist = Server.users(Integer.parseInt(para(0)._2)).friend(Integer.parseInt(para(0)._2))
          if(frlist.members.size > 0){
            var rand = Random.nextInt(frlist.members.size)
            var frid = frlist.members.keySet.toList(rand)   
            if(Server.users.contains(frid)){
              var frposts = new Array[Int](Server.users(frid).post.size)
              var postkey = Server.users(frid).post.keySet
              for(i<-0 to Server.users(frid).post.size-1){
                frposts(i) = postkey.toList(i)
              }
              ctx.complete(Getuserpost(frid.toString(),frposts))
            }
            else
              ctx.complete(Getuserpost(frid.toString(),new Array[Int](0)))
          }
          else
              ctx.complete(Getuserpost("-1",new Array[Int](0)))
        }
        else
          ctx.complete(Getuserpost("-1",new Array[Int](0)))
        /*var frlist = Server.users(Integer.parseInt(para(0)._2)).friend(Integer.parseInt(para(0)._2))
        var rand = Random.nextInt(frlist.members.size)
        var frid = frlist.members.keySet.toList(rand)   
        if(Server.users.contains(frid)){
          var frposts = new Array[Int](Server.users(frid).post.size)
          var postkey = Server.users(frid).post.keySet
          for(i<-0 to Server.users(frid).post.size-1){
            frposts(i) = postkey.toList(i)
          }
          ctx.complete(Getuserpost(frid.toString(),frposts))
        }
        else
          ctx.complete(Getuserpost(frid.toString(),new Array[Int](0)))*/
      }
      else
        ctx.complete(Getuserpost("-1",new Array[Int](0)))
    }
    
    case GetPostRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.posts.contains(Integer.parseInt(para(0)._2))){
        var post = Server.posts.get(Integer.parseInt(para(0)._2)) 
        //var AESkey = hex_digest(System.currentTimeMillis.toString()).substring(0, 16)
        //var encodemsg = AES.encrypt(AESkey,post.get.message)    
        //var encodekey = new RSAencrypt(1024,Server.userpublickey(para(1)._2.toInt))       
        //println("Server has msg "+post.get.message)
        //println("Server has encodemsg "+encodemsg)  
        println("Server has encodemsg "+post.get.message)
        //var RSAkey = encodekey.encrypt(AESkey)
        //println("Server has modulus "+Server.userpublickey(para(1)._2.toInt))
        //println("Server has AESkey "+AESkey)
        //println("Server has encodekey "+RSAkey+"\n")
        implicit val getpostFormat2 = jsonFormat4(Getpost)
        if(post.get.encodekeys.contains(para(1)._2.toInt)){
          println("Server has encodekey "+post.get.encodekeys(para(1)._2.toInt))
          //ctx.complete(Getpost(post.get.id.toString(),post.get.creator.toString(),encodemsg,RSAkey.toString()))
          ctx.complete(Getpost(post.get.id.toString(),post.get.creator.toString(),post.get.message,post.get.encodekeys(para(1)._2.toInt).toString()))
        }
        
      }
      else
        ctx.complete("Post "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetFriendRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.friends.contains(Integer.parseInt(para(0)._2))){
        var friend = Server.friends.get(Integer.parseInt(para(0)._2)) 
        //var message = "id:"+friend.get.id+"\nname:"+friend.get.name+"\nowner:"+friend.get.owner;
        var friends: Array[Int] = new Array[Int](friend.get.members.size)
        var fr = friend.get.members.keySet
        for(i<-0 to friend.get.members.size-1){
          friends(i) = fr.toList(i)
        }
        implicit val getfriendFormat2 = jsonFormat2(Getfriend)
        ctx.complete(Getfriend(friend.get.id.toString(),friends))
      }
      //else
        //ctx.complete("Friend list "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetProfileRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.users.contains(Integer.parseInt(para(0)._2))){
        var profile = Server.users.get(Integer.parseInt(para(0)._2)) 
        //var message = "id:"+profile.get.id+"\nusername:"+profile.get.username+"\ngender:"+profile.get.gender+"\nbirthday:"+profile.get.birthday+"\nlink:"+profile.get.link;
        //ctx.complete("Get profile information:\n"+message+"\n")
        implicit val getprofileFormat2 = jsonFormat4(Getprofile)
        ctx.complete(Getprofile(profile.get.id.toString(),profile.get.username,profile.get.gender,profile.get.birthday))  
      }
      //else
        //ctx.complete("User "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetAlbumRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.albums.contains(Integer.parseInt(para(0)._2))){
        var album = Server.albums.get(Integer.parseInt(para(0)._2))
        //var message = "id:"+album.get.id+"\nname:"+album.get.name+"\nfrom:"+album.get.from+"\ncreate time:"+album.get.create_time+"\nlink"+album.get.link;
        //ctx.complete("Get album information:\n"+message+"\n")
        var photos: Array[Int] = new Array[Int](album.get.photos.size)
        var ph = album.get.photos.keySet
        for(i<-0 to album.get.photos.size-1){
          photos(i) = ph.toList(i)
        }
        implicit val getalbumFormat2 = jsonFormat3(Getalbum)
        ctx.complete(Getalbum(album.get.id.toString(),album.get.name,photos))
      }
      //else
        //ctx.complete("Album "+para(0)._2+" does not exist!\n")
      this.context.stop(self)
    }
    
    case GetPictureRequest(para: Seq[(String,String)],ctx: RequestContext) => {
      if(Server.photos.contains(Integer.parseInt(para(0)._2))){
        var photo = Server.photos.get(Integer.parseInt(para(0)._2)) 
        //var message = "id:"+photo.get.id+"\nname:"+photo.get.name+"\nalbum:"+photo.get.album+"\nfrom:"+photo.get.from+"\npicture:"+photo.get.picture;     
        //ctx.complete("Get album information:\n"+message+"\n")
        implicit val getphotoFormat2 = jsonFormat3(Getphoto)
        ctx.complete(Getphoto(photo.get.id.toString(),photo.get.name,photo.get.picture))
      }
      //else
        //ctx.complete("Photo "+para(0)._2+" does not exist!\n")
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
      Server.posts += (para(4)._2.toInt->new Posts)
      Server.posts(para(4)._2.toInt).id = para(4)._2.toInt
      //Server.posts(para(4)._2.toInt).message = para(1)._2
      Server.posts(para(4)._2.toInt).create_time = System.currentTimeMillis
      
      var RSAdecrypt = new RSAdecrypt(1024,Server.userpublickey(para(0)._2.toInt),0x10001)
      var RSAat = para(3)._2
      var at = RSAdecrypt.decrypt1(BigInt.apply(RSAat))
      println("at is "+at)
      if(para(0)._1.equals("userid")&& Server.users.contains(para(0)._2.toInt)){
        if(at.equals(Server.users(para(0)._2.toInt).access_token))
          println("true")
        else
          println("false")
        var RSAdecrypt = new RSAdecrypt(1024,Server.modulus,Server.RSAprivatekey)
        var encodekey = para(2)._2
        var AESkey = RSAdecrypt.decrypt(BigInt.apply(encodekey))
        var encodemsg = para(1)._2
        println("\nServer get encode msg "+encodemsg+" "+encodemsg.length())
        println("Server get encode key "+para(2)._2)
        println("Server get AESkey "+AESkey+" length "+AESkey.length)
        for (elem <- AESkey) {  
              print(elem + " , ")  
          }
        var AESdecrypt = AES.decrypt(AESkey, encodemsg)
        //println("server get msg "+AESdecrypt)
        var friends = Server.users(para(0)._2.toInt).friend(para(0)._2.toInt).members
        //friends.keySet
        for(i<-0 to friends.size-1){
          if(Server.userpublickey.contains(friends.keySet.toList(i))){
            var frpublickey = Server.userpublickey(friends.keySet.toList(i))
            var enkey = new RSAencrypt(1024,frpublickey)
            var RSAkey = enkey.encrypt(AESkey)
            Server.posts(para(4)._2.toInt).encodekeys += (friends.keySet.toList(i) -> RSAkey)
          }   
        }
        var enkey = new RSAencrypt(1024,Server.userpublickey(para(0)._2.toInt))
        var RSAkey = enkey.encrypt(AESkey)
        Server.posts(para(4)._2.toInt).encodekeys += (para(0)._2.toInt -> RSAkey)
        
        Server.posts(para(4)._2.toInt).creatortype = "user"
        Server.posts(para(4)._2.toInt).creator = para(0)._2.toInt
        Server.posts(para(4)._2.toInt).message = encodemsg
        Server.users(para(0)._2.toInt).post += (para(4)._2.toInt -> Server.posts(para(4)._2.toInt))
        println("Create post: "+Server.posts(para(4)._2.toInt).id+" by "+Server.posts(para(4)._2.toInt).creatortype+" "+Server.posts(para(4)._2.toInt).creator+"\n")
      }
      else if(para(0)._1.equals("pageid")&& Server.pages.contains(para(0)._2.toInt)){
          Server.posts(Server.postsum).creatortype = "page"
          Server.posts(Server.postsum).creator = para(0)._2.toInt
          Server.pages(para(0)._2.toInt).post += (Server.postsum -> Server.posts(Server.postsum))
          System.out.println("Create post: "+Server.posts(Server.postsum).id+" by "+Server.posts(Server.postsum).creatortype+" "+Server.posts(Server.postsum).creator+"\n")
      }
      //System.out.println("Create post: "+Server.posts(Server.postsum).id+" by "+Server.posts(Server.postsum).creatortype+" "+Server.posts(Server.postsum).creator+"\n")
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
      Server.userpublickey += (Server.usersum -> BigInt.apply(para(0)._2))
      Server.users += (Server.usersum->new Users)
      Server.users(Server.usersum).id = Server.usersum
      Server.users(Server.usersum).access_token = hex_digest(System.currentTimeMillis.toString()+Server.usersum).substring(0, 16)
      Server.friends += (Server.usersum -> new Friends)
      Server.users(Server.usersum).friend += (Server.usersum -> Server.friends(Server.usersum))
      System.out.println("Create user: "+Server.users(Server.usersum).id)
      var RSAaccess_token = new RSAencrypt(1024,BigInt.apply(para(0)._2))
      var access_token = RSAaccess_token.encrypt(Server.users(Server.usersum).access_token.getBytes)
      implicit val newuserFormat = jsonFormat3(newuser)
      //ctx.complete(Server.usersum.toString())
      ctx.complete(newuser(Server.usersum.toString(),access_token.toString(),Server.modulus.toString()))
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
      if(para(1)._1.equals("userid") && Server.users.contains(para(1)._2.toInt)){
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
      if(Server.users.contains(para(1)._2.toInt)){
        if(Server.users(para(1)._2.toInt).friend.contains(para(0)._2.toInt)){
        Server.friends -= para(0)._2.toInt
        Server.users(para(1)._2.toInt).friend -= para(0)._2.toInt
        System.out.println("Dreate friendlist: "+para(0)._2.toInt+" by user "+para(1)._2.toInt)
        }
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
  
  def hex_digest(s: String): String = {
    val sha = MessageDigest.getInstance("SHA-256")
    sha.digest(s.getBytes)
    .foldLeft("")((s: String, b: Byte) => s +
                  Character.forDigit((b & 0xf0) >> 4, 16) +
                  Character.forDigit(b & 0x0f, 16))
   }
}