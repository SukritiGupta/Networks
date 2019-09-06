import java.io.*;
import java.net.*;
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 


public class tcpclient {

	private Socket sendSocket;				private Socket recvSocket;				private BufferedReader in_std;
	private DataOutputStream outSS;			private BufferedReader inSS;			private DataOutputStream outRS;				private BufferedReader inRS;

	public tcpclient(String addr,int port) throws IOException{
		String resp;	String username;
		try{ 
            this.sendSocket = new Socket(addr,port);								this.in_std  = new BufferedReader(new InputStreamReader(System.in));  
            this.outSS = new DataOutputStream(sendSocket.getOutputStream());		this.inSS = new BufferedReader(new InputStreamReader(sendSocket.getInputStream())); 
        } 
        catch(UnknownHostException u){	System.out.println(u);	} 
        catch(IOException i){	System.out.println(i);	}

        while(true){
        	System.out.println("Enter Username:");					username = this.in_std.readLine();
	        this.outSS.writeBytes("REGISTER TOSEND "+username+"\n");	resp = this.inSS.readLine();						System.out.println(resp);
	        if((resp.split(" ",2))[0].equals("REGISTERED"))break;
        }

        try{ 
            this.recvSocket = new Socket(addr,port);	 
            this.outRS = new DataOutputStream(recvSocket.getOutputStream());		this.inRS = new BufferedReader(new InputStreamReader(recvSocket.getInputStream())); 
        } 
        catch(UnknownHostException u){	System.out.println(u);	} 
        catch(IOException i){	System.out.println(i);	}

        this.outRS.writeBytes("REGISTER TORECV "+username+"\n");	resp = this.inRS.readLine();						System.out.println(resp);

        System.out.println("\n|--------------- Connected to Server ---------------|\n");

        ClientSender t1 = new ClientSender(this.sendSocket,this.inSS,this.outSS);    		Thread t11 = new Thread(t1);	t11.start();
        ClientReceiver t2 = new ClientReceiver(this.recvSocket,this.inRS,this.outRS);    	Thread t22 = new Thread(t2);	t22.start();
	}

	public static void main(String[] args) throws IOException {

        String srvrIP="localhost";
		tcpclient a = new tcpclient(srvrIP,6789);	
 	}
}

class ClientSender implements Runnable{

    private BufferedReader inSS;      private DataOutputStream outSS;         private Socket s; 
    public ClientSender(Socket s, BufferedReader dis, DataOutputStream dos){ 
        this.s = s;         this.inSS = dis;            this.outSS = dos; 
    }

    public void run(){ 
        BufferedReader in_std  = new BufferedReader(new InputStreamReader(System.in));          String inp,resp,recvr,msg;          String [] l;
        while(true)
        {
            try{    
                while(true){
                    inp = in_std.readLine();        if(inp.charAt(0)=='@')break;
                }
                l = inp.split(" ",2);                       recvr = (l[0]).substring(1);                msg = l[1];
                this.outSS.writeBytes("SEND "+recvr+"\n"+"Content-length: "+msg.length()+"\n\n"+msg+"\n"); 
                resp = this.inSS.readLine();				System.out.println("S: "+ "Message Sent");       
            } 
            catch(IOException i){   System.out.println(i);  }
        } 
    }
}


class ClientReceiver implements Runnable{

    private BufferedReader inRS;      private DataOutputStream outRS;         private Socket s; 
    public ClientReceiver(Socket s, BufferedReader dis, DataOutputStream dos){ 
        this.s = s;         this.inRS = dis;            this.outRS = dos; 
    }

    public void run(){ 
        String h1,h2,h3,h4,sendr;       String [] l;        
        Pattern p1 = Pattern.compile("^FORWARD [a-zA-Z0-9]+$");         Pattern p2 = Pattern.compile("^Content-length: [0-9]+$");

        while (true)  
        { 
            try {
            	h1 = this.inRS.readLine();			h2 = this.inRS.readLine();			h3 = this.inRS.readLine();			h4 = this.inRS.readLine();	       
    			sendr = (h1.split(" ",2))[1];

                if(p1.matcher(h1).matches() && p2.matcher(h2).matches()){
                    this.outRS.writeBytes("RECEIVED " + sendr+"\n");
                }
                else{
                    this.outRS.writeBytes("ERROR 103 Header incomplete\n");
                }
                System.out.println(sendr+": "+h4);

            }   catch (IOException e) {e.printStackTrace();} 
        }  
    }
}



