package commands;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.io.File;


public class CommandS {
	
	private String ip; 
	
	private int port; 
	
	private List<String> files;
	
	
	public CommandS(String ip, int port, List<String> files) { 
		
		this.ip = ip;
		this.port = port;
		this.files = files;
		
	}
	

	private Signature assignFile() throws NoSuchAlgorithmException,KeyStoreException, UnrecoverableKeyException, InvalidKeyException, CertificateException, IOException {
		
		//Set the signature
		Signature signature = Signature.getInstance("SHA256withRSA");   
		

		//Read the KeyStore File
		FileInputStream keyStorefile = new FileInputStream(new File("../src/KeyStore.si027")); 
		
		//Alias from set up in KeyStore file
		String alias = "si027";
		
		//Get the instance of keyStore
		KeyStore kstore = KeyStore.getInstance("PKCS12"); 
		
		//verify password and load KeyStore file
		kstore.load(keyStorefile, "si027marcos&rafael".toCharArray()); 
		
		//Get the private key 
		Key key = kstore.getKey(alias, "si027marcos&rafael".toCharArray()); 
		
		//turn key in instance of private key
		PrivateKey privatekey = (PrivateKey) key; 
		
		//Encrypted digital assign
		signature.initSign(privatekey); 		
		
		return signature;
		
	}
	
	public void sendToServer() throws UnknownHostException, IOException, NoSuchAlgorithmException, SignatureException, ClassNotFoundException, UnrecoverableKeyException, InvalidKeyException, KeyStoreException, CertificateException {
		
		Socket socket = null;
		try {
			 socket = new Socket(this.ip, this.port);
		}
		catch (ConnectException e) {
			System.out.println("Connection refused, please check the Port");
			System.exit(-1);
		}
		catch (UnknownHostException e) {
			
			System.out.println("Connection refused, please check the Host");
			System.exit(-1);
		}
		
		//Send data to server
		ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
		
		//Read from Server
		ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
		
		//Send the option first
		outStream.writeObject("-s");
		
		//Send numbers of files
		outStream.writeObject(this.files.size());
		
		for (String fileName : this.files) {
			
			File fileToRead = new File("../files/" + fileName);
			
			if(fileToRead.exists()) {
				
				//Send true to server know that file exists
				outStream.writeObject(true); 
				
				//Send the file name
				outStream.writeObject(fileName); 
				
				// Verify if file exists in the server
				//File does not exist
				if(!(Boolean) inStream.readObject()) {
					
					//Read the received file 
					FileInputStream fileInStream = new FileInputStream(fileToRead); 
					
					//get signature object 
					Signature signature = assignFile(); 
					
					//get total file length
					int totalFileLength = fileInStream.available();
					
					//send to server exact buffer size
					outStream.writeObject(totalFileLength);

					//byte array for file
					byte[] dataToBytes = new byte[Math.min(totalFileLength, 1024)]; 
					
					//Length of the contents of the read file 
					int contentLength = fileInStream.read(dataToBytes); 
					
					//read files chunk 
					while(contentLength != -1 ) {
						//Hash the data
						signature.update(dataToBytes,0,contentLength);
						//send data to server
						outStream.write(dataToBytes,0,contentLength);
						//continue to read fileInStream
						contentLength = fileInStream.read(dataToBytes);
					}
					
					//send signature to server
					outStream.writeObject(signature.sign());
					fileInStream.close();
					
				}
				//File exist
				else {
					System.out.println("File " + fileName + " is already on the server!");
				}
			}
			else {
				outStream.writeObject(false);
				System.out.println("File " + fileName + " does not exist!");
			}
		}
	}
}
