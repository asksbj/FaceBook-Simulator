import scala.language.postfixOps
import scala.runtime.ScalaRunTime.stringOf
import scala.util.Random
import org.apache.commons.codec.binary.Base64

//import com.roundeights.hasher.Implicits.byteArrayToHasher
//import com.roundeights.hasher.Implicits.stringToHasher

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES {

  def encrypt(k:String, data: String): String = {
   
    val key = k  
    val skey = key.getBytes("utf-8")
      //(salt + password).sha256.bytes
    val keyspec = new SecretKeySpec(skey, "AES");

    //val iv = new Array[Byte](16);
    val base : Array[Byte] = Array('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p','q','r',
                                   's','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9')
    //val iv: Array[Byte] = Array('.', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', '2')
    val iv: Array[Byte] = new Array[Byte](16)
    for (i<-0 to 15){
      var rand = Random
      iv(i) = base(rand.nextInt(base.length))
    }
    val ivspec = new IvParameterSpec(iv);
    val ivBase64 = Base64.encodeBase64(iv).filterNot("=".toSet)
    //val ivBase64 = Base64.encodeBase64(iv)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
    val encrypted = cipher.doFinal(data.getBytes("utf-8"))
    //new String(Base64().encodeToString(encrypted))
    //val utf8 = decrypted.getBytes("UTF-8")
    //val encrypted = cipher.doFinal(pkcs5Pad(utf8 ++ utf8.md5.hex.getBytes("UTF-8")));

    val encBase64 = Base64.encodeBase64(encrypted);
    //println(new String(encBase64,"UTF-8"))
    new String(ivBase64 ++ encBase64, "UTF-8")
  }

  def decrypt(k:Array[Byte],data: String): String = {
    //val key = k
    //val skey = key.getBytes("utf-8")
    val skey = k
    val keyspec = new SecretKeySpec(skey, "AES");

    val iv = Base64.decodeBase64((data.take(22) + "==").getBytes("utf-8"))
    val ivspec = new IvParameterSpec(iv);
    
    val decoded = Base64.decodeBase64(data.substring(22, data.length()).getBytes("utf-8"))
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);  
    
    val dec = cipher.doFinal(decoded)
    //println(new String(dec,"UTF-8"))
    
    //val decrypted = pkcs5Unpad(dec)

    //val message: String = new String(dec,"utf-8")
      //decrypted.take(decrypted.length - 32)
    //val md5 = decrypted.takeRight(32)

    /*if (message.md5.hex != new String(md5, "UTF-8")) {
      throw new Exception("[error][" + this.getClass().getName() + "] " +
        "Message could not be decrypted correctly.\n" +
        "\tMessage: \"" + new String(message, "UTF-8") + "\"\n" +
        "Hashes are not equal.\n" +
        "\tGenerated hash: " + stringOf(message.md5.hex) + "\n" +
        "\tExpected hash:  " + stringOf(md5) + "\n" +
        "\tGenerated HEX:  " + stringOf(message.md5.bytes.map(_.toHexString)) + "\"\n" +
        "\tExpected HEX:   " + stringOf(md5.map(_.toHexString)) + "\"\n");
    }*/
    new String(dec, "UTF-8")
  }

  def pkcs5Pad(input: Array[Byte], size: Int = 16): Array[Byte] = {
    val padByte: Int = size - (input.length % size);
    return input ++ Array.fill[Byte](padByte)(padByte.toByte);
  }
  
  

}