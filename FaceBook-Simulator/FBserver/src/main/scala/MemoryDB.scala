import akka.actor._
import scala.collection.mutable.HashMap


/**
 * @author Asks
 */
object Server {
    var usersum = 1000
    var pagesum = 100
    var postsum = 2000
    var friendlistsum = 2000
    var albumsum = 2000
    var photosum = 10000
    var endid = 0
    var startid = 0
    var counter = 0
    var frequency = 0
    var totalusers = 0
    var maxclients = 0
    var Runningtime = 0
    var relationcounter = 0
    var messageprocessed = 0
    var clientref: Array[ActorRef] = _
    var users = HashMap.empty[Int, Users]
    var pages = HashMap.empty[Int, Pages]
    var posts = HashMap.empty[Int, Posts]
    var friends = HashMap.empty[Int, Friends]
    var albums = HashMap.empty[Int, Albums]
    var photos = HashMap.empty[Int, Photos]
   

}

class Users{
  var id:Int = _
  var username: String = "username"
  var first_name: String = "first"
  var last_name: String = "last"
  var middle_name: String = "middle"
  var gender: String = "male"
  var email: String = "sijie@ufl.edu"
  var birthday: String = "1990/01/01"
  var post = HashMap.empty[Int, Posts]
  var friend = HashMap.empty[Int, Friends]
  var postsum = post.size
  var friendsum = friend.size
  var link: String = "http://facebook.com/user"
  var albumsum = 0
}

class Pages{
  var id:Int = _
  var name: String = "pagename"
  var post = HashMap.empty[Int, Posts]
  var postsum = post.size
  var link: String = "http://facebook.com/page"
}

class Posts{
  var id:Int = _
  var creator: Int = _
  var creatortype: String = _
  var message: String = "This is a test post."
  var create_time: String = "2015/11/11"
}

class Friends{
  var id: Int = _
  var name: String = "UF friends"
  var owner: Int = _
  var members = HashMap.empty[Int, Users]
}

class Albums{
  var id: Int = _
  var name: String = "MyAlbum"
  var count = 0
  var from: Int = _
  var create_time: String = "2015/11/11"
  var link: String = "http://facebook.com/album"
  var photos = HashMap.empty[Int, Photos]
}

class Photos{
  var id: Int = _
  var album: Int = _
  var from: Int = _
  var name: String = "MyPhoto"
  var picture: String = "Picture.png"
  var link: String = "http://facebook.com/photo"
}