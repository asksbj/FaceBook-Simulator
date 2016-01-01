1. Team members
Name: Sijie Dong    UFID: 02147344
Nmae: Fengbo Zheng   UFID: 46183966

2.  How to run
This project consists of two parts: the server and the client.
First, choose one machine as the server end. Run sbt under the folder /FBserver, 
then type the command ¡°run¡±:
> run
Now, the server is working. At first, it will initialize some information used in the 
FaceBook api, creating some users, pages etc. Then, it will bind to the port 2559 and 
begin listening to this port and wait for the clients to connect to.
Second, use another machine as the client end. Run sbt under the folder /FBclient, 
the type the run command as follows:
> run   serverIP   numofclients   numofrequests
Here serverIP refers to the IP address of the server machine, numofclients stands for 
the number of clients assumed to run the simulator and numofrequests stands for 
the number of requests each client assumed to make.
For example:
> run   192.168.0.1   100   10
This command means our client machine wants to connect to the server machine 
whose IP address is 192.168.0.1. In this case, we will have 100 existing FaceBook users and create 100 new users. Each user will make 10 requests (Get, Post, Delete) to the server.

3. The system can hold 10 thousands users sending request at the same time. Each user and page will have 5 posts in max. One user have 10 friendlists and each friendlist will have 10 friends. One user have 2 albums and in each album, there are 5 photos.