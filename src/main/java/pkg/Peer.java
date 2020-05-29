package pkg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import jdk.javadoc.doclet.Reporter;
public class Peer {
	public static int port;
	public static int TrackerPort;
	public static String path;
	public static String username;
	static Map<Integer,String> hash_to_fileName = new HashMap<Integer,String>();
	static Map<String,Integer> username_port = new HashMap<String,Integer>();
	//static Map<String,String> username_dir = new HashMap<String,String>();
	static Scanner console = new Scanner (System.in);
	public static void main(String[] args) {
		try {
			init();
			System.out.println("Enter username :");
			String []ans= new String[5] ;
			ans[0]=console.next();
			System.out.println("Enter Port: (must be unique):");
			ans[1]=console.next();
			if(username_port.get(ans[0])!=null) {
				username=ans[0];
				System.out.println("already Registerd");
				port=username_port.get(ans[0]);
				System.out.println("your port is :"+port);
				path="D:\\peersDir\\"+username;
			}else {
				System.out.println("unregisterd user");
				while(username_port.get(ans[0])!=null) {
					System.out.println("username already exist enter a unique one");
					ans[0]=console.next();
				}
				username=ans[0];
				while(Taken(Integer.parseInt(ans[1]))) {
					System.out.println("this port already in use Enter another one");
					ans[1]=console.next();
				}
				port=Integer.parseInt(ans[1]);
				path="D:\\peersDir\\"+username;
				File newFolder = new File(path);
				if(newFolder.mkdir()) {
					System.out.println("A Directory made for "+username);
					System.out.println("Location :"+path);
				}
						
			}
			
			ServerThread Serve = new ServerThread();
			Serve.start();
			updateTracker();
			while(true) {
			System.out.println("Enter file to get or 'u' to update your files record:");
			String f=console.next();
			if(f.equals("u")) {
				updateTracker();
				System.out.println("updated!");
			}else {
			int OtherPeerPort = portOfUserWithFile(f);
			System.out.println("port is :"+OtherPeerPort);
			if(OtherPeerPort >4000){
			System.out.println("Downloading file :"+f);
			getFileFromPeer(OtherPeerPort,f);
			updateTracker();
			System.out.println("auto updated!");
			}
			
			else {
				System.out.println("file not found yet");
			}
			}
			}
		} catch (Exception e) {
			System.out.println("Error in Peer main");
			System.out.println(e.getMessage());
		}
		
		

	}
	public static boolean Taken(int ReqPort) {
		try {
		if(ReqPort<=4003) return true ;
		Socket client = new Socket(InetAddress.getByName("localhost"),TrackerPort);
		DataInputStream in = new DataInputStream(client.getInputStream());
		DataOutputStream out= new DataOutputStream(client.getOutputStream());
		String s=in.readUTF();//ok :D
		out.writeInt(00000); //No port Set yet
		out.writeUTF(username);
		out.writeInt(10);
		s=in.readUTF();
		out.writeInt(ReqPort);
		int r = in.readInt();
		System.out.println("r="+r);
		return r!=400;
		}catch(Exception ex) {
			System.out.println("Error in Taken()");
			System.out.println(ex.getMessage());
			return !false;
		}
	}
	public static void init() {
		TrackerPort=4000;
		username_port.put("user1", 4001);
		username_port.put("user2", 4002);
		username_port.put("user3", 4003);
	}
	public static void updateTracker() {
		try {
			Socket client = new Socket(InetAddress.getByName("localhost"),TrackerPort);
			DataInputStream in = new DataInputStream(client.getInputStream());
			DataOutputStream out= new DataOutputStream(client.getOutputStream());
			String s=in.readUTF();
			System.out.println("Server says:"+s);
			out.writeInt(port);
			out.writeUTF(username);
			out.writeInt(11);
			s=in.readUTF();
			if(s.equalsIgnoreCase("ok")) {
				s="";
				File folder = new File(path);
				File [] files = folder.listFiles();
				for(File f : files) {
					if(f.isFile()) {
						s+=f.getName()+" ";
					}
				}
				
				out.writeUTF(s);
			//System.out.println("Server Said:"+reply);
			
				
			}
			out.close();
			in.close();
		}catch(Exception ex) {
			System.out.println("Error in updateTracker()");
			System.out.println(ex.getMessage());
			
		}
	}
	public static void getFileFromPeer(int peerPort , String fileName) {
		try {
			Socket Server = new Socket(InetAddress.getByName("localhost"),peerPort);
			DataInputStream in = new DataInputStream(Server.getInputStream());
			DataOutputStream out= new DataOutputStream(Server.getOutputStream());
			out.writeUTF(username);
			out.writeInt(hash(fileName));
			int response=in.readInt();
			if(response==400) {
			int sz = in.readInt();
			byte [] buff = new byte[sz];
			FileOutputStream fos = new FileOutputStream(path+"\\"+fileName);
			BufferedOutputStream bos= new BufferedOutputStream(fos);
			int byteRead = in.read(buff,0,buff.length);
			bos.write(buff,0,byteRead);
			bos.close();
			}
			Server.close();

		}catch(IOException ex) {
			System.out.println("Error in  getFileFromPeer();");
			System.out.println(ex.getMessage());
		}
			
	}
	public static int portOfUserWithFile(String file_name) {
		int file_hash=hash(file_name);
		try {
		Socket client = new Socket(InetAddress.getByName("localhost"),TrackerPort);
		DataInputStream in = new DataInputStream(client.getInputStream());
		DataOutputStream out= new DataOutputStream(client.getOutputStream());
		String s=in.readUTF();
		System.out.println("Server says: "+s);
		out.writeInt(port);
		out.writeUTF(username);
		out.writeInt(12); //queryID 
		s=in.readUTF();
		if(s.equalsIgnoreCase("ok")) {
			out.writeInt(file_hash);
			int reply = in.readInt();
			if(reply==200) return -1;
			
			return in.readInt();
		}
		}catch(IOException ex) {
			System.out.println("Error in  portOfUserWithFile()");
			System.out.println(ex.getMessage());
		}
		return -1;
		
	}
	public static int connectToPort(int port) {
		int replyPort ;
		try {
			Socket client = new Socket(InetAddress.getByName("localhost"),port);
			DataInputStream in = new DataInputStream(client.getInputStream());
			DataOutputStream out= new DataOutputStream(client.getOutputStream());
			Scanner console = new Scanner(System.in);
			String s=in.readUTF();
			System.out.println("Server says:"+s);
			if(s.equalsIgnoreCase("which file you want ?")) {
			String query =console.nextLine();
			out.writeUTF(hash(query)+"");
			if(query.equalsIgnoreCase("close")) {
				client.close();
				System.out.println("closed client");
			}
			
			int reply = in.readInt();
			System.out.println("Server Said:"+reply);
			if(reply==400) {
				
			}
			}
			replyPort=1;
		}catch(Exception ex) {
			System.out.println("Eror in Client");
			return -1;
		}
		return replyPort;
	}
	static int hash(String FileName) {
		int ret=0;
		int q=7;// ay qema bs tkon prime w akbr tb3n mn l 26;
		for(int i=0;i<FileName.length();++i)
			ret+=FileName.charAt(i)*Math.pow(q, i);
		return ret;
	}
	
	static void fillMap() {
		File folder = new File(path);
		File [] files = folder.listFiles();
		for(File f : files) {
			if(f.isFile()) {
				hash_to_fileName.put(hash(f.getName()), f.getName());
			}
		}
	}

	public static class ServerThread extends Thread{
	public void run() {
		try {
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("server is on now at: "+port);
		while(true) {
			Socket client = serverSocket.accept();
			Pserver per = new Pserver(client , path);
			System.out.println("new client connected to peer"+username);
			per.start();
		}
		}catch(Exception e) {
			System.out.println("error in serverThread");
			System.out.println(e.getMessage());
		}
	}
	}
	
	 static class Pserver extends Thread{
		private String path;
		private Socket client;
		public void setScoket(Socket s) {
			client=s;
		}
		public Pserver(Socket s,String p) {
			client=s;
			path=p;
			fill_the_map();
		}
		public Pserver(String p) {
			path=p;
			fill_the_map();
			
		}
		boolean WeGot(int hash) {
			File folder = new File(path);
			File [] files = folder.listFiles();
			for(File f : files)
				if(f.isFile())
					if(hash(f.getName())==hash)
						return true;
			return false;
			
		}
		void fill_the_map() {
			File folder = new File(path);
			File [] files = folder.listFiles();
			for(File f : files) {
				if(f.isFile()) {
					System.out.println(hash(f.getName())+"-"+f.getName());
					hash_to_fileName.put(hash(f.getName()),f.getName());
				}
			}
			
		}
		public void run() {
			try {
				
				String query="";
				String PeerUsername="";
				int fileHash;
				DataInputStream in = new DataInputStream(client.getInputStream());
				DataOutputStream out = new DataOutputStream(client.getOutputStream());
				//Scanner console = new Scanner(System.in);
				System.out.println("peer connected to another peer thread");
				PeerUsername=in.readUTF();
				//out.writeUTF("Which file you want ?");
				fileHash= in.readInt();
				System.out.println(" peer : "+PeerUsername+"requetd file which hash = "+fileHash);
						if(hash_to_fileName.get(fileHash)!=null) {
							out.writeInt(400); //found
							File respons = new File(path+"//"+hash_to_fileName.get(fileHash));
							int sz=(int)respons.length();
							byte[] buffer = new byte[sz];
							out.writeInt(sz);
							BufferedInputStream bis = new BufferedInputStream(new FileInputStream(respons));
							bis.read(buffer,0,buffer.length);
							out.write(buffer);
							out.flush();
						}else
							out.writeInt(200); //error
						
				in.close();
				out.close();
				client.close();
				
			}catch(Exception ex) {
				System.out.println("error in Peer Server");
				System.out.println(ex.getMessage());
			}
		}
	 }
	

}
