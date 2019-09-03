import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
	
public class tcpserver {

	public tcpserver(int port) throws IOException{
		ServerSocket srvr = new ServerSocket(port);

		while(true){
			Socket soc = null;
			try{
				soc = srvr.accept();		DataOutputStream outToClient = new DataOutputStream(soc.getOutputStream());		
				BufferedReader inFromClient  = new BufferedReader(new InputStreamReader(soc.getInputStream()));

				String msg = inFromClient.readLine();
				if((msg.substring(0,15)).equals("REGISTER TOSEND")){
					if((msg.substring(16)).matches("[A-Za-z0-9]+")){
						outToClient.writeBytes("REGISTERED TOSEND "+msg.substring(16));

						System.out.println("New client "+ soc.getLocalAddress()+":"+soc.getLocalPort()+"_"+soc.getPort()+" is connected.");
						Thread t = new ClientHandler(soc, inFromClient, outToClient); 		t.start();
					}
					else{
						outToClient.writeBytes("ERROR 100 Malformed username\n");
						soc.close();
					}
				}
				else if((msg.substring(0,15)).equals("REGISTER TORECV")){
					if((msg.substring(16)).matches("[A-Za-z0-9]+")){
						outToClient.writeBytes("REGISTERED TORECV "+msg.substring(16));
						
						System.out.println("New client "+ soc.getLocalAddress()+":"+soc.getLocalPort()+"_"+soc.getPort()+" is connected.");
						Thread t = new ClientHandler(soc, inFromClient, outToClient); 		t.start();
					}
					else{
						outToClient.writeBytes("ERROR 100 Malformed username\n");
						soc.close();
					}
				}
				else{
					outToClient.writeBytes("ERROR 101 No user registered\n");
					soc.close();
				}
				
			}
			catch (Exception e){ 
                soc.close(); 
                e.printStackTrace(); 
            }
		}
	}

	public static void main(String[] args) throws IOException{
		tcpserver server = new tcpserver(6789);		
	}
}

class ClientHandler extends Thread{

	final BufferedReader inFromClient; 		final DataOutputStream outToClient; 		final Socket s; 
    public ClientHandler(Socket s, BufferedReader dis, DataOutputStream dos){ 
        this.s = s; 		this.inFromClient = dis; 			this.outToClient = dos; 
    }

    public void run(){ 
        String msg; 	String response; 
        while (true)  
        { 
            try { 
                msg = inFromClient.readLine(); 
                if(msg.equals("Exit")){  
                    System.out.println("Client " + this.s.getLocalAddress()+":"+this.s.getLocalPort()+"_"+this.s.getPort()+" sends exit...Closing this connection.");			this.s.close();			break; 
                } 

                System.out.println("Msg from client "+this.s.getLocalAddress()+":"+this.s.getLocalPort()+"_"+this.s.getPort()+" is :" + msg);
				response = msg.toUpperCase() + "\n";
				outToClient.writeBytes(response);

            }	catch (IOException e) {e.printStackTrace();} 
        } 
        try{ 	this.inFromClient.close(); 		this.inFromClient.close();		}
        catch(IOException e){	e.printStackTrace();	} 
    }
}


