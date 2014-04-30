package Classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;

import Server.*;
import Enums.Action;
import Enums.Direction;
import Exceptions.EmptyFileException;
import Exceptions.InvalidFileException;
import Exceptions.UnexceptableNameException;

public class Actuator {

	private static PrintableWood myWood;
	private static HashSet<Point> starts;
	private static HashSet<Point> finishs;
	public static void main(String[] args) throws IOException {
		System.out.println("START");
		WoodLoader woodLoader = new WoodLoader();
		File fileMap = new File("input.txt");
		File filePoints = new File("points.txt");
		starts = new HashSet<>();
		finishs = new HashSet<>();
		
		Scanner inpStr = new Scanner(new FileInputStream(filePoints));
		ServerSocket sSocket = new ServerSocket(12345);
		try {
			while (inpStr.hasNext()) {
				int x = inpStr.nextInt();
				int y = inpStr.nextInt();
				starts.add(new Point(x,y));
				x = inpStr.nextInt();
				y = inpStr.nextInt();
				finishs.add(new Point (x,y));
				//
				break;
			}
			
			FileInputStream FIS = new FileInputStream(fileMap);
			myWood = woodLoader.printableLoader(FIS, System.out);
			
			System.out.println("wait client");
			clientSession(sSocket.accept());
		} catch (EmptyFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			inpStr.close();
			sSocket.close();
		}
	}
	private static void clientSession(Socket socket) {
		System.out.println("HAS NEW CLIENT!");
		ObjectInputStream inStr = null;
		ObjectOutputStream outStr = null;
		try {
			inStr = new ObjectInputStream(socket.getInputStream());
			outStr = new ObjectOutputStream(socket.getOutputStream());
			try {
				Request request = (Request)inStr.readObject();
				Direction dir;
				String name = request.getName();
				
				if (starts.isEmpty() || finishs.isEmpty())
					throw new RuntimeException("Не хватает точек старта или финиша");
				Point finish = finishs.iterator().next();
				Point start = starts.iterator().next();
				myWood.createWoodman(request.getName(), start, finish);
				Mause mause = new Mause();
				
				Response response = new Response(Action.Ok);
				outStr.writeObject(response);
				while (response.getAction() != Action.Finish && response.getAction() != Action.WoodmanNotFound) {
					request = (Request) inStr.readObject();
					dir = mause.NextMove(response.getAction());
					System.out.println(dir.name());
					response = new Response(myWood.move(name, dir));
					outStr.writeObject(response);
					outStr.flush();
					System.out.println(response.getAction());
				}
			} catch (UnexceptableNameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				inStr.close();
				outStr.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
