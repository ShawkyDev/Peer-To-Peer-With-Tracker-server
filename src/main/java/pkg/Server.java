package pkg ;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Server {
static String TrackPath="";
static Map<String,List<String>> Track = new HashMap<String,List<String>>();
static Map<Integer,List<String>> hash_users = new HashMap<Integer,List<String>>();
static Map<String,Integer> Username_port = new HashMap<String,Integer>();
static ServerSocket serverSocket;
void update(int hash,String peerName) {
	///useless but won't delete it :D
	try {
	File f = new File(TrackPath);
	FileWriter Fout = new FileWriter(f);
	Scanner Fin = new Scanner(f);
	while(Fin.hasNext())
		;
	}catch(IOException e) {
		System.out.println("Error in update(Tracker)");
		System.out.println(e.getMessage());
	}
	
}
	public static void main(String[] args) {
		try {
			serverSocket = new ServerSocket(4000);
			TrackPath="D:\\Tracker";
			System.out.println("Server is booted up and is waiting for clients to connect.");
			printTheTrackTxt printer = new printTheTrackTxt();
			printer.start();
			while (true) {
				// Accept any Client wants to connect to the server.
				Socket clientSocket = serverSocket.accept();
				System.out.println("A new client [" + clientSocket + "] is connected to the server.");
				// Create a new thread for this client.
				ClientConnection client = new ClientConnection(clientSocket);

				// Start thread.
				client.start();

			}
		} catch (IOException e) {
			System.out.println("Problem with Server Socket.");
		}
	}

	 
	static int hash(String FileName) {
		int ret=0;
		int q=7;// ay qema bs tkon prime w akbr tb3n mn l 26;
		for(int i=0;i<FileName.length();++i)
			ret+=FileName.charAt(i)*Math.pow(q, i);
		return ret;
	}
	static void update_hash_users(String files,String user_name) {
		if(files.equals(" ")||files.length()==0) return ;
		String [] files_names = files.split(" ");
		System.out.println("got:"+files);
		for(int i=0;i<files_names.length;++i) {
			int file_hash = hash(files_names[i]);
			if(hash_users.get(file_hash)==null) {
				hash_users.put(file_hash, new ArrayList<String>());
			}
			hash_users.get(file_hash).add(user_name);
			LinkedHashSet<String> unique_list = new LinkedHashSet<String>(hash_users.get(file_hash));
			hash_users.put(file_hash, new ArrayList<String>(unique_list));
			///this weird part removes Duplicates (and time complexity too :D)
		}
	}
	static class printTheTrackTxt extends Thread{
		public void run() {
			try {
			Scanner sc = new Scanner(System.in);
			while(true) {
			System.out.println("you can Enter P to print the T.txt or U to update it");
			String op = sc.next();
			if(op.charAt(0)=='U'|| op.charAt(0)=='u' ){
			FileWriter fout= new FileWriter(TrackPath+"\\"+"T.txt");
			fout.write("User\t Files\n");
			for(Map.Entry<String,List<String>> entry: Track.entrySet()) {
				fout.write(entry.getKey()+"\t");
				for(String s : entry.getValue()) {
					fout.write(s+" ");
				}
				fout.write("\n");
				fout.close();
			}
			}
			else {
				System.out.println("User\t Files");
				for(Map.Entry<String,List<String>> entry: Track.entrySet()) {
					System.out.print(entry.getKey()+"\t");
					for(String s : entry.getValue()) {
						System.out.print(s+" ");
					}
					System.out.println();
				}	
			}
			}
		}catch(Exception e) {
			System.out.println("Error in printing Track.txt");
			System.out.println(e.getMessage());
		}
		}
	}
	static  class ClientConnection extends Thread {
		final private Socket clientSocket;

		public ClientConnection(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		public void run() {
			String r = "";
			int queryID;
			int clientport;
			String clientUsername="NAN";
			try {

				// Takes input from the client socket.
				DataInputStream in = new DataInputStream(clientSocket.getInputStream());

				// Print output to the client socket.
				DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

				// Send to the client that it is now connected.
				out.writeUTF("connected.");

				// Start communication with client.
					//out.writeUTF("Send your request ID or '-1' to close the connection.");

					// Read the request from the client and output it to Server's console.
					clientport = in.readInt();
					clientUsername = in.readUTF();
					Username_port.put(clientUsername, clientport);
					queryID= in.readInt();
					System.out.println("Client [ " + clientUsername + " ]: " + queryID);
						if(queryID==11) {
							System.out.println("updating " +clientUsername+ " files");
							out.writeUTF("ok");
							r=in.readUTF();
							//System.out.println("update:"+r); 
							update_hash_users(r,clientUsername);
							if(Track.get(clientUsername)==null) {
								Track.put(clientUsername, new ArrayList<String>());
							}
							Track.get(clientUsername).addAll(Arrays.asList(r.split(" ")));//:D
							LinkedHashSet<String> unique_list = new LinkedHashSet<String>(Track.get(clientUsername));
							Track.put(clientUsername, new ArrayList<String>(unique_list));
							// this weird part is for removing duplicates 
							System.out.println("user have :\n"+Track.get(clientUsername));
						}else if(queryID==12) {
							System.out.println(clientUsername+"requesting a file");
							out.writeUTF("ok");
							int fileHash=in.readInt();
							if( hash_users.get(fileHash)!=null&& hash_users.get(fileHash).size()>0) {
							String userWithFile = hash_users.get(fileHash).get(0); 
							int response = Username_port.get(userWithFile);
							out.writeInt(400); //success
							out.writeInt(response);
							//pushing the element to the last (34an myb2a4 hoa kol mara elly byb3t l file )
							hash_users.get(fileHash).remove(0);
							hash_users.get(fileHash).add(userWithFile);
				
							}else {
								out.writeInt(200); //not found
							}
						}else if(queryID==10) {
							boolean taken = false;
							out.writeUTF("ok");
							int requestedPort = in.readInt();
							System.out.println(clientUsername+" is requesting port:"+requestedPort);
							for(Map.Entry<String,Integer> entery : Username_port.entrySet()) {
								if(entery.getValue()==requestedPort) {
									out.writeInt(200);
									taken = true;
									break;
									
								}	
								if(!taken)
								out.writeInt(400);
							}
							
							
						}
						
					
				in.close();
				out.close();
				
			} catch (IOException e) {
				System.out.println("Connection with this client [" + clientUsername+ "] is terminated.");
			}
		}
	}

}
