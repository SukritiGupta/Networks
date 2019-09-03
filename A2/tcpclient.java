import java.io.*;
import java.net.*;

class tcpclient {

	private Socket sendSocket;				private Socket recvSocket;				private BufferedReader in_std;
	private DataOutputStream outSS;			private BufferedReader inSS;			private DataOutputStream outRS;				private BufferedReader inRS;

	public tcpclient(String addr,int port) throws IOException{
		String resp;	String username;
		try{ 
            sendSocket = new Socket(addr,port);								in_std  = new BufferedReader(new InputStreamReader(System.in));  
            outSS = new DataOutputStream(sendSocket.getOutputStream());		inSS = new BufferedReader(new InputStreamReader(sendSocket.getInputStream())); 
        } 
        catch(UnknownHostException u){	System.out.println(u);	} 
        catch(IOException i){	System.out.println(i);	}

        while(true){
        	System.out.println("Enter Username.");					username = in_std.readLine();
	        outSS.writeBytes("REGISTER TOSEND "+username+"\n\n");	resp = inSS.readLine();						System.out.println(resp);
	        if(resp.equals("REGISTER TOSEND "+username+"\n"))break;
        }

        try{ 
            recvSocket = new Socket(addr,port);	 
            outRS = new DataOutputStream(sendSocket.getOutputStream());		inRS = new BufferedReader(new InputStreamReader(sendSocket.getInputStream())); 
        } 
        catch(UnknownHostException u){	System.out.println(u);	} 
        catch(IOException i){	System.out.println(i);	}

        outSS.writeBytes("REGISTER TORECV "+username+"\n\n");	resp = inSS.readLine();						System.out.println(resp);


        String sentence="";		String modifiedsent;
        while(true)
        {
            try{ 
         		sentence = in_std.readLine(); 				outSS.writeBytes(sentence+"\n");			if(sentence.equals("Exit"))break;
	            modifiedsent = inSS.readLine();           	System.out.println("S:"+modifiedsent);       
            } 
            catch(IOException i){	System.out.println(i);	}
        }
        try{ in_std.close();		outSS.close();		inSS.close();		sendSocket.close();	} 
        catch(IOException i){	System.out.println(i);	} 
	}

	public static void main(String[] args) throws IOException {

		tcpclient a = new tcpclient("localhost",6789);	
 	}
}
