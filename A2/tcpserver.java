import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 


class sc{
    public Socket soc;		
    public BufferedReader dis;		
    public DataOutputStream dos;

    public sc(Socket s1, BufferedReader s2, DataOutputStream s3) 
    {
       this.soc = s1;		
       this.dis = s2;			
       this.dos=s3;
    }
    public Socket s() 
    { 
        return this.soc; 
    }
    public BufferedReader ris() 
    { 
        return this.dis; 
    }
    public DataOutputStream ros() 
    { 
        return this.dos; 
    }
}


public class tcpserver {

	public static Map< String, sc> ss;
    public static Map< String, sc> rs;
	public static Map< String, String> publickeymap;

	public tcpserver(int port) throws IOException
    {

		ServerSocket srvr = new ServerSocket(port);				
        ss = new HashMap<>();				
        rs = new HashMap<>();
		
		while(true)
        {
			Socket soc = null;

			try
            {
				soc = srvr.accept();		
                DataOutputStream outToClient = new DataOutputStream(soc.getOutputStream());		
				BufferedReader inFromClient  = new BufferedReader(new InputStreamReader(soc.getInputStream()));
				ClientHandler t1 = new ClientHandler(soc, inFromClient, outToClient); 				
                Thread t = new Thread(t1);				
                t.start();

			}
			catch (Exception e)
            { 
                soc.close(); 				
                e.printStackTrace(); 
            }
		}
	}

	public static void main(String[] args) throws IOException
    {
		tcpserver server = new tcpserver(6789);		
	}
}

class ClientHandler implements Runnable{

	private BufferedReader instrm; 		
    private DataOutputStream outstrm; 		
    private Socket s; 			
    private String usr;

    public ClientHandler(Socket s, BufferedReader dis, DataOutputStream dos)
    { 
        this.s = s; 		
        this.instrm = dis; 			
        this.outstrm = dos; 
    }

    public void run()
    { 
        String msg,usr,hdr; 	
        String response; 

        try 
        { 
            msg = this.instrm.readLine();		
            usr = msg.substring(16);		
            hdr=msg.substring(0,15);

            if(hdr.equals("REGISTER TOSEND"))
            {
				if(usr.matches("[A-Za-z0-9]+"))
                {
					sc t1 = new sc(this.s,this.instrm,this.outstrm);			
					this.outstrm.writeBytes("REGISTERED TOSEND "+usr+"\n");			
                    tcpserver.ss.put(usr,t1);			
                    this.usr=usr;		
                    trnsmit();				
				}
				else
                {
					this.outstrm.writeBytes("ERROR 100 Malformed username\n");			
				}
			}
			else if(hdr.equals("REGISTER TORECV"))
            {
				if(usr.matches("[A-Za-z0-9]+"))
                {	
					sc t2 = new sc(this.s,this.instrm,this.outstrm);			
					this.outstrm.writeBytes("REGISTERED TORECV "+usr+"\n");			
                    tcpserver.rs.put(usr,t2);			
                    this.usr=usr;	
				}
				else
                {
					this.outstrm.writeBytes("ERROR 100 Malformed username\n");
				}
			}
            else if(hdr.equals("REGISTER keyPUB"))
            {
                if(usr.matches("[A-Za-z0-9]+"))
                {
                    tcpserver.publickeymap.put(  (usr.split(" ",2)[0]) , (usr.split(" ",2)[1])  );
                }
                else
                {
                    this.outstrm.writeBytes("ERROR 100 Malformed username\n");
                }
            }
			else
            {
				this.outstrm.writeBytes("ERROR 101 No user registered\n");			
                this.s.close();
			}
        }	
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
    }

    public void trnsmit()
    {
    	BufferedReader instream = this.instrm;						
        DataOutputStream outstream = this.outstrm;					
        String h1,h2,h3,h4,resp,rcvr;    
    	Pattern p1 = Pattern.compile("^SEND [a-zA-Z0-9]+$");		
        Pattern p2 = Pattern.compile("^Content-length: [0-9]+$");
    	DataOutputStream rcvrin;									
        BufferedReader rcvrout;										

    	while(true)
        {
    		try 
            {
    			h1 = instream.readLine();

                if(h1.equals("UNREGISTER"))
                {
                    tcpserver.rs.get(this.usr).dos.writeBytes("UNREGISTER");
                    tcpserver.rs.get(this.usr).soc.close();             
                    tcpserver.rs.get(this.usr).dis.close();           
                    tcpserver.rs.get(this.usr).dos.close();

                    tcpserver.ss.get(this.usr).soc.close();             
                    tcpserver.ss.get(this.usr).dis.close();           
                    tcpserver.ss.get(this.usr).dos.close();

                    tcpserver.ss.remove(this.usr);                      
                    tcpserver.rs.remove(this.usr);                    
                    return;                 
                }
                if(((h1.split(" ",2))[0]).equals("Fetch_key"))	
                {
                    rcvr = (h1.split(" ",2))[1];
                    if(!tcpserver.publickeymap.containsKey(rcvr))
                    {
                        System.out.println("Client ["+rcvr+"] not registered");
                        outstream.writeBytes("ERROR 102 Unable to send\n");
                    }
                    else
                        outstream.writeBytes(tcpserver.publickeymap.get(rcvr)+"\n");
                    continue;
                }

                h2 = instream.readLine();		
                h3 = instream.readLine();		
                h4 = instream.readLine();	

    			rcvr = (h1.split(" ",2))[1];	

                System.out.println("["+this.usr+"] to ["+rcvr+"]: "+h4);
    			
    			if(!tcpserver.rs.containsKey(rcvr))
                {
    				System.out.println("Client ["+rcvr+"] not registered");
    				outstream.writeBytes("ERROR 102 Unable to send\n");
    			}

    			else if(p1.matcher(h1).matches() && p2.matcher(h2).matches())
                {
    				rcvrin= tcpserver.rs.get(rcvr).dos;							
                    rcvrout = tcpserver.rs.get(rcvr).dis;
                    rcvrin.writeBytes("FORWARD "+this.usr+"\n"+h2+"\n\n"+h4+"\n");
                    resp = rcvrout.readLine();		

                    if(resp.equals("RECEIVED "+this.usr))outstream.writeBytes("SENT "+rcvr+"\n");
                    else outstream.writeBytes("ERROR 102 Unable to send\n");
                }
                else{
                    outstream.writeBytes("ERROR 103 Header incomplete\n");
                }
    		}catch (Exception e) {e.printStackTrace();}

    	}
    }
}

