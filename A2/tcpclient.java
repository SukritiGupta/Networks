import java.io.*;
import java.net.*;
import java.util.regex.Matcher; 
import java.util.regex.Pattern;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey; 


public class tcpclient {

	private Socket sendSocket;				
    private Socket recvSocket;				
    private BufferedReader in_std;
	private DataOutputStream outSS;			
    private BufferedReader inSS;			
    private DataOutputStream outRS;				
    private BufferedReader inRS;
    private KeyPair keys;
    private int mode_of_op;

	public tcpclient(String addr,int port, int mode) throws IOException
    {
		String resp;	
        String username;
        mode_of_op=mode;

		try
        { 
            this.sendSocket = new Socket(addr,port);								
            this.in_std  = new BufferedReader(new InputStreamReader(System.in));  
            this.outSS = new DataOutputStream(sendSocket.getOutputStream());		
            this.inSS = new BufferedReader(new InputStreamReader(sendSocket.getInputStream())); 
        } 
        catch(UnknownHostException u)
        {	
            System.out.println(u);	
        } 
        catch(IOException i)
        {	
            System.out.println(i);	
        }

        while(true)
        {
        	System.out.println("Enter Username:");					
            username = this.in_std.readLine();
	        this.outSS.writeBytes("REGISTER TOSEND "+username+"\n");	
            resp = this.inSS.readLine();						
            System.out.println(resp);
	        if((resp.split(" ",2))[0].equals("REGISTERED"))
                break;
        }

        try
        { 
            this.recvSocket = new Socket(addr,port);	 
            this.outRS = new DataOutputStream(recvSocket.getOutputStream());		
            this.inRS = new BufferedReader(new InputStreamReader(recvSocket.getInputStream())); 
        } 
        catch(UnknownHostException u)
        {	
            System.out.println(u);	
        } 
        catch(IOException i)
        {	
            System.out.println(i);	
        }

        if (mode==1) 
        {
            try{
            keys = CryptographyExample.generateKeyPair();}
            catch(Exception e)
            {
                System.out.println("Unable to generateKeyPair");
            }

            PublicKey publicKey = keys.getPublic();
            String temp=java.util.Base64.getEncoder().encodeToString((publicKey.getEncoded()));

            this.outSS.writeBytes("REGISTER keyPUB "+username+" "+temp+"\n");            
        }

        this.outRS.writeBytes("REGISTER TORECV "+username+"\n");	
        resp = this.inRS.readLine();						
        System.out.println(resp);

        System.out.println("\n|--------------- Connected to Server ---------------|\n");

        ClientSender t1 = new ClientSender(this.sendSocket,this.inSS,this.outSS,mode,this.keys);    		
        Thread t11 = new Thread(t1);	
        t11.start();
        
        ClientReceiver t2 = new ClientReceiver(this.recvSocket,this.inRS,this.outRS,mode, this.keys);    	
        Thread t22 = new Thread(t2);	
        t22.start();
	}

	public static void main(String[] args) throws IOException {





        String srvrIP="localhost";
        // tcpclient a = new tcpclient(srvrIP,6789,1); 
		tcpclient a = new tcpclient(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));	
        // cin port, mode, addr....
 	}
}

class ClientSender implements Runnable{

    private BufferedReader inSS;      
    private DataOutputStream outSS;         
    private Socket s; 
    private int mode_of_op;
    private KeyPair keys;

    public ClientSender(Socket s, BufferedReader dis, DataOutputStream dos, int mode, KeyPair key)
    { 
        this.s = s;         
        this.inSS = dis;            
        this.outSS = dos; 
        this.mode_of_op=mode;
        this.keys=key;

    }

    public void run()
    { 
        BufferedReader in_std  = new BufferedReader(new InputStreamReader(System.in));          
        String inp,resp,recvr,msg;          
        String [] l;
        
        while(true)
        {
            try
            {    
                while(true)
                {
                    inp = in_std.readLine();        
                    if(inp.charAt(0)=='@')
                        break;
                    if(inp.substring(0,10).equals("UNREGISTER"))
                    {
                        this.outSS.writeBytes("UNREGISTER");     
                        this.s.close();         
                        this.inSS.close();          
                        this.outSS.close();     
                        return;
                    }
                }
                l = inp.split(" ",2);                       
                recvr = (l[0]).substring(1);  
                System.out.println(recvr);
                System.out.println(l[0]+l[1]);
                msg = l[1];

                if (mode_of_op==1) 
                {
                    this.outSS.writeBytes("Fetch_key "+recvr+"\n");
                    System.out.println("Fetch_key called");
                    resp = this.inSS.readLine();
                    System.out.println("*"+ resp+"*");

                    if (!(resp.equals("ERROR 102 Unable to send")))
                    {
                        try
                        {
                            msg=java.util.Base64.getEncoder().encodeToString(CryptographyExample.encrypt(   java.util.Base64.getDecoder().decode(resp) , (l[1]).getBytes()  ));
                        }
                        catch(Exception e)
                        {
                            System.out.println("Problem in encryption \n");
                            System.out.println(e);
                        }
                    }
                }

                this.outSS.writeBytes("SEND "+recvr+"\n"+"Content-length: "+msg.length()+"\n\n"+msg+"\n"); 
                resp = this.inSS.readLine();				
                System.out.println("S: "+ resp);  

            } 
            catch(IOException i)
            {   
                System.out.println(i);  
            }
        } 
    }
}


class ClientReceiver implements Runnable{

    private BufferedReader inRS;      
    private DataOutputStream outRS;         
    private Socket s; 
    private int mode_of_op;
    private KeyPair keys;

    public ClientReceiver(Socket s, BufferedReader dis, DataOutputStream dos, int mode, KeyPair key)
    { 
        this.s = s;         
        this.inRS = dis;            
        this.outRS = dos; 
        this.mode_of_op=mode;
        this.keys=key;
    }

    public void run()
    { 
        String h1,h2,h3,h4,sendr;       
        String [] l;        
        Pattern p1 = Pattern.compile("^FORWARD [a-zA-Z0-9]+$");         
        Pattern p2 = Pattern.compile("^Content-length: [0-9]+$");

        while (true)  
        { 
            try 
            {
            	h1 = this.inRS.readLine();		
                if(h1.equals("UNREGISTER"))
                {
                    this.s.close();         
                    this.inRS.close();          
                    this.outRS.close();     
                    return;
                }

                h2 = this.inRS.readLine();			
                h3 = this.inRS.readLine();			
                h4 = this.inRS.readLine();	       
    			
                sendr = (h1.split(" ",2))[1];

                if(p1.matcher(h1).matches() && p2.matcher(h2).matches())
                {
                    this.outRS.writeBytes("RECEIVED " + sendr+"\n");
                }
                else
                {
                    this.outRS.writeBytes("ERROR 103 Header incomplete\n");
                }
                if(mode_of_op==1)
                {
                    try
                    {
                        h4=new String(CryptographyExample.decrypt(keys.getPrivate().getEncoded(),    java.util.Base64.getDecoder().decode(h4)     ));
                    }
                    catch(Exception e)
                    {
                        System.out.println("Problem in decryption \n");
                    }
                }
                System.out.println(sendr+": "+h4);

            }   
            catch (IOException e) 
            {
                e.printStackTrace();
            } 
        }  
    }
}



