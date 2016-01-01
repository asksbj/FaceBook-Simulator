package FBclient

import scala.util.Random
/**
 * @author Asks
 */
class RSAencrypt(val n: Int, modulus: BigInt) {



  //modulus = p * q
  //val publicKey = 0x10001 //Classic 2^16 + 1
  


  def encrypt(message: Array[Byte],publicKey:BigInt) = BigInt.apply(message).modPow(publicKey, modulus) //Functions to handle encryption and decryption
  def encrypt1(message: String,publicKey:BigInt) = BigInt.apply(message.getBytes).modPow(publicKey, modulus) 
   //In order to use these with strings, you must cast into BigInt and back

  //override def toString = "Bitlength: " + n + "\npublic: 0x" + publicKey.toString(16) + "\nprivate: 0x" + privateKey.toString(16) + "\nmodulus: 0x" + modulus.toString(16)
  
}

class RSAdecrypt(n: Int,modulus: BigInt, privateKey: BigInt){
  def decrypt(message: BigInt) = message.modPow(privateKey, modulus).toByteArray
  def decrypt1(message: BigInt) = new String(message.modPow(privateKey, modulus).toByteArray)
}