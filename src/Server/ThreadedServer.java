package Server;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Scanner;

import Classes.Point;
import Classes.PrintableWood;
import Classes.WoodLoader;
import Enums.Action;
import Enums.Direction;
import Exceptions.EmptyFileException;
import Exceptions.InvalidFileException;
import Exceptions.UnexceptableNameException;

public class ThreadedServer {

	private static PrintableWood myWood;
	private static HashSet<Point> starts;
	private static HashSet<Point> finishs;
	private HashSet<ThreadedClient> clients;
	
	public class Timer extends Thread {
		private HashSet<ThreadedClient> clientsSet;
		public Timer(HashSet<ThreadedClient> set) {
		clientsSet = set;
		}
		public void run() {
			while(true){
				for (ThreadedClient i: clientsSet)
					synchronized (i) {
						i.notify();
					}
					
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) { }
				if (clientsSet.size() != 0) {
					myWood.print();
				}
			}
				
		}
			
	}
	
	public class ThreadedClient extends Thread {
		
		private Socket socket;
		
		private ThreadedClient(Socket s) {
			socket = s;
		}
		
		@Override
		public void run() {
			Point finish = null;
			Point start = null;
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
					
					synchronized (starts) {
						if (starts.isEmpty() || finishs.isEmpty())
							throw new RuntimeException("Не хватает точек старта или финиша");
						finish = finishs.iterator().next();
						start = starts.iterator().next();
					}
					
					synchronized (myWood) {
						myWood.createWoodman(request.getName(), start, finish);
					}
					
					synchronized (clients) {
						clients.add(this);
					}
					
					
					Response response = new Response(Action.Ok);
					outStr.writeObject(response);
					outStr.flush();
					while (response.getAction() != Action.Finish && response.getAction() != Action.WoodmanNotFound) {
						request = (Request) inStr.readObject();
						dir = request.getDirection(); 
						//System.out.println(dir.name());
						synchronized (this) {
							response = new Response(myWood.move(name, dir));
							this.wait();
						}
						
						outStr.writeObject(response);
						outStr.flush();
						//System.out.println(response.getAction());
						
					}
				} catch (UnexceptableNameException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					close(inStr);
					close(outStr);
					finishs.add(finish);
					starts.add(start);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				close(socket);
				clients.remove(this);
			}
		}
	}
	
	
	public ThreadedServer() {
		starts = new HashSet<>();
		finishs = new HashSet<>();
		File fileMap = new File("input.txt");
		File filePoints = new File("points.txt");
		Scanner inpStr = null;
		try {
			inpStr = new Scanner(new FileInputStream(filePoints));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
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
		} finally {
			inpStr.close();
		}
		FileInputStream FIS = null;
		try {
			FIS = new FileInputStream(fileMap);
			WoodLoader woodLoader = new WoodLoader();
			myWood = woodLoader.printableLoader(FIS, System.out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (EmptyFileException e) {
			e.printStackTrace();
		} catch (InvalidFileException e) {
			e.printStackTrace();
		} finally {
			close(FIS);
		}
	}
	
	public void start() {
		clients = new HashSet<>();
		ServerSocket sSocket = null;
		try {
			sSocket = new ServerSocket(12345);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Timer timer = new Timer(clients);
		timer.start();
		try {
			while (true) {
				Socket newSocket = null;
				try { 
					newSocket = sSocket.accept();
				} catch (SocketTimeoutException e) {
					continue;
				}
				ThreadedClient newClient = new ThreadedClient(newSocket);
				newClient.start();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
			close(sSocket);
		}
		
	}
	public void close (Closeable c) {
		try {
			c.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main (String[] args) {
		ThreadedServer ser = new ThreadedServer();
		ser.start();
	}
}
