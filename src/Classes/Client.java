
package Classes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;


import Server.Request;
import Server.Response;
import Classes.Mause;
import Enums.Action;
import Enums.Direction;

public class Client {
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket("localhost", 12345);
		System.out.println("has connect");
		
		
		ObjectOutputStream outStr = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream inpStr = new ObjectInputStream(socket.getInputStream());
		System.out.println("maked streams");
		Scanner scanner = new Scanner(System.in);
		System.out.println("ВВедите имя играока: ");
		String name = scanner.nextLine();
		Request request = new Request(name);
		try {
			outStr.writeObject(request);
			outStr.flush();
			Mause mause = new Mause();
			
			Response response = (Response) inpStr.readObject();
			while (response.getAction() != Action.WoodmanNotFound && response.getAction() != Action.Finish) {
				Direction dir = mause.NextMove(response.getAction());
				request = new Request(name, dir);
				outStr.writeObject(request);
				outStr.flush();
				response = (Response) inpStr.readObject();
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			inpStr.close();
			outStr.close();
			socket.close();
		}
		
	}
}
