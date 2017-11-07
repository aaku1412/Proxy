import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Web {
	public static final int MAXINPUTSIZE = 65535;
	private static final int MAXOUTPUTSIZE = 200000000; //200 MB 
	private static final int HTTPPORT = 80;
	public static void main(String[] args) throws IOException {
		Web p = new Web();
		ServerSocket s = p.init(new Integer(args[0]));
		InetAddress ipAddr;
		try {
			ipAddr = InetAddress.getLocalHost();
			System.out.println(ipAddr.getHostAddress());
		}
		catch (UnknownHostException e) 
		{	e.printStackTrace();    }
		try{
			p.go(s);
		}
		catch(Exception e)
		{	e.printStackTrace();	}	
	}	

	public ServerSocket init(int port) {    
		System.out.println("CSC656 project by group W4 (mrs63@njit.edu)");
		ServerSocket s;
		try {
			s = new ServerSocket();
			s.bind(new InetSocketAddress(port));
			return s;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void go(ServerSocket s) {
		byte[] buffer = new byte[2048];  // We assume the largest size of a request will be 2kb
		int totalBytes;
		int bytesRead;
		int bytesReadSoFar = 0; // To track how many got read so far
		Socket requestedServerSocket; 	 
		Socket clientSocket;
		try{
			while (true) {
				System.out.println("Waiting for a client connection...");
				clientSocket = s.accept();

				System.out.println("Client connection received!");
				try {
					bytesRead = clientSocket.getInputStream().read(buffer, 0, buffer.length); // First request read from client
			
					bytesReadSoFar = bytesRead; 
					totalBytes = getTotalBytes(buffer);
					
					if (bytesRead != -1) {
						if (totalBytes <= MAXINPUTSIZE) {
							System.out.println("Total bytes: " + totalBytes);
							System.out.print("LOG: requested for (" + parse(buffer) + ").\n");
							
							requestedServerSocket = dnslookup(parse(buffer));
							
							if(requestedServerSocket != null) {
									requestedServerSocket.getOutputStream().write(buffer, 0, bytesRead); 
									while (!isSocksClosed(clientSocket, requestedServerSocket) && bytesReadSoFar < totalBytes) { 
										System.out.println("\nMessage wrote to client");
										if ((bytesRead = clientSocket.getInputStream().read(buffer, 0, buffer.length)) == -1)
										{
											closeSocks(requestedServerSocket, clientSocket);
										}
										bytesReadSoFar += bytesRead; // this statement will increment the bytes read and while have us exit the while loop after bytesReadSoFar = totalBytes
										
									}
									System.out.println("\n\nFlushing the request to the server");
									if (!isSocksClosed(clientSocket, requestedServerSocket)) {
									requestedServerSocket.getOutputStream().flush();
									try {
										if ((bytesRead = requestedServerSocket.getInputStream().read(buffer, 0, buffer.length)) != -1) {
											totalBytes = getTotalBytes(buffer);
											bytesReadSoFar = bytesRead;

											if (totalBytes <= MAXOUTPUTSIZE) {    
												System.out.println("Receiving response...");
												System.out.println("Size of total bytes: " + totalBytes);
												
												clientSocket.getOutputStream().write(buffer, 0, bytesRead);
												while (!isSocksClosed(clientSocket, requestedServerSocket) && bytesReadSoFar < totalBytes)
												{
													if ((bytesRead = requestedServerSocket.getInputStream().read(buffer, 0, buffer.length)) == -1) {
														closeSocks(clientSocket, requestedServerSocket);
													}
													clientSocket.getOutputStream().write(buffer, 0, bytesRead);
													bytesReadSoFar += bytesRead;
												}
												if(!isSocksClosed(requestedServerSocket, clientSocket)) {
													System.out.println("Flushed the response from the server to the client.");
													clientSocket.getOutputStream().flush();
													System.out.println("Sockets Closed!");
													closeSocks(clientSocket, requestedServerSocket);
												}
											}
											else {
												System.out.println("First response from server was too big. Closed connection.");
												closeSocks(clientSocket, requestedServerSocket);
											}
										} 
										else {
											closeSocks(clientSocket, requestedServerSocket); 
											System.out.println("No response from the server after writing the request.");
										}
									}
									catch (IOException e) {
										closeSocks(requestedServerSocket, clientSocket);
										System.out.println("Server disconnected from the socket. Closed the connection.");
									}
								}
							}
							else {
								clientSocket.close();
								System.out.println("***BAD URL***\n");
							}
						}    
						else {
							System.out.println("First request was too big. Closed the client.");
							clientSocket.close(); 
						}
					}
					else {
						System.out.println("No response from client after accepting the connection. Closed the client.");
						clientSocket.close();
					}
				}
				catch (IOException e) {
					clientSocket.close();
					System.out.println("Client disconnectioned from socket after accepting the connection. Closed the client.");
				}
			}
		}
		catch (IOException e)
		{	e.printStackTrace();	}
	}

	private boolean isSocksClosed(Socket c, Socket s)
	{    
		try{
			return s.isClosed() && c.isClosed();      
		}
		catch(Exception e)
		{	e.printStackTrace();	}
		return false;

	}

	private void closeSocks(Socket s,Socket c) {
		try{
			s.close();
			c.close();
		}
		catch(IOException e)
		{	e.printStackTrace();	}
		
	}
	// Parses url from host name from http request messages. If not found it returns null.
	public String parse(byte[] array){    
		Charset cSet = Charset.forName("US-ASCII");
		char[] cArray = cSet.decode(ByteBuffer.wrap(array)).array();
		int urlLength = 0;
		int indexOfUrl = 0;
		try{
		for (int i = 0; i < cArray.length - 5; i++) {
			if (cArray[i] == 'H' && cArray[i+1] == 'o' && cArray[i+2] == 's' && cArray[i+3] == 't' && cArray[i+4] == ':' && cArray[i+5] == ' ') {
				indexOfUrl = i + 6;
				for (int j = indexOfUrl; j < cArray.length ; j++) {
					if ((cArray[j] != '\n') && (cArray[j] != '\r'))
						urlLength++;
					else {
						j = cArray.length - 1;
						i = cArray.length - 1;
					}
				}
			}
		}
		if (urlLength >= 1)
		{	
			String ch1 = new String(cArray, indexOfUrl, urlLength);
			if(ch1.equals("www.torrentz.eu")||ch1.equals("torrentz.eu")||ch1.equals("makemoney.com")||ch1.equals("www.makemoney.com")||ch1.equals("lottoforever.com")||ch1.equals("www.lottoforever.com"))
				return null;
			else
				return ch1;
		}
		else
			return null;
	}
	catch(IllegalArgumentException e)
	{	e.printStackTrace();	}
	return null;
	}

	public Socket dnslookup(String url) {
		try {
			if(url!=null) {
				Socket sock = new Socket();
				InetAddress[] address = InetAddress.getAllByName(url);
				for(int i=0;i<address.length;i++) {
					sock.connect(new InetSocketAddress(address[i].getHostAddress(), HTTPPORT));
					if(sock.isConnected())
					{
						System.out.println("Connected to ip: "+address[i].getHostAddress());
						System.out.println("Got connection");
						return sock;
					}
					sock.close();
				}
			}
		}
		catch (IOException |IllegalArgumentException e) 
		{	e.printStackTrace();	}
		return null;
	}
	// returns content-length from http header. If it is not found it returns 0
	private int parseContentLength(byte[] array, int headerSize) {
		Charset cSet = Charset.forName("US-ASCII");
		char[] cArray = cSet.decode(ByteBuffer.wrap(array)).array();
		int urlLength = 0, indexOfUrl = 0;
		try{
		for (int i = 0; i < headerSize - 7; i++) {
			if (cArray[i] == 'L' && cArray[i+1] == 'e' && cArray[i+2] == 'n' && cArray[i+3] == 'g' && cArray[i+4] == 't' && cArray[i+5] == 'h' && cArray[i+6] == ':' && cArray[i+7] == ' ') {
				indexOfUrl = i + 8;
				for (int j = indexOfUrl; j < cArray.length - 1; j++) {
					if ((cArray[j] != '\n') && (cArray[j] != '\r'))
						urlLength++;
					else {
						j = cArray.length - 1;
						i = cArray.length - 1; 
					}
				}
			}
		}
		if (urlLength >= 1)
			return new Integer(new String(cArray, indexOfUrl, urlLength));
		else
			return 0;
		}
		catch(IllegalArgumentException e)
		{	e.printStackTrace();	}
		return 0;
	}
	private int getTotalBytes(byte[] array) {
		int contentLength;
		int totalHeaderBytes = 0;  
		Charset cSet = Charset.forName("US-ASCII");
		char[] cArray = cSet.decode(ByteBuffer.wrap(array)).array();
		try{
		for (int i = 0; i < array.length - 2; i++) {
			if (cArray[i] == '\n' && cArray[i+1] == '\r' && cArray[i+2] == '\n') {
				totalHeaderBytes = i + 3; // finds last index and adds 1 since array index starts at 0
				i = array.length - 2;
			}
		}
		contentLength = parseContentLength(array, totalHeaderBytes);
		System.out.println("Total header bytes: " + totalHeaderBytes);
		System.out.println("Content length: " + contentLength);
		if (contentLength == 0) // returns header bytes if no body exists
			return totalHeaderBytes;
		else    // returns header bytes plus body bytes plus 2 bytes for /r and /n character
			return totalHeaderBytes + contentLength;
		}
		catch(IllegalArgumentException e)
		{	e.printStackTrace();	}
		return 0;
	}
}