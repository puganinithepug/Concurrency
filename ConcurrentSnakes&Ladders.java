import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class q2 {

	//parameters
	
	public static int k; // Adder sleep time
        public static int j; // Remover sleep time
        public static int s; // Simulation duration in seconds
        
        public static long startT; //start of a thread's execution
        
        Board board; //board 
        
        List<String> log; //log of operations and timestamps

   public static void main(String[] args){
   if(args.length < 3){
    	System.out.println("Requires 3 arguments t run: adder sleep time in milliseconds, remover sleep time in milliseconds and simulation duration time in seconds to run the Snake and Ladder simulation");
    	return;
    }
    try{
    
    //parse input arguments
    k = Integer.parseInt(args[0]);//ms for adder to sleep
    j = Integer.parseInt(args[1]);//ms for remover to sleep
    s = Integer.parseInt(args[2]);//seconds for game to run
      
    //generate the board
    Board board = new Board();
    
    //create a log of operations performed and timestamps
    List<String> log = Collections.synchronizedList(new ArrayList<>());      
    
    //record time since beginning of execution
    startT = System.currentTimeMillis();
    
     //Pre-populate the adder log with the initial snakes/ladders
     prepopulate(board, log, 16, 6, true);
     prepopulate(board, log, 49, 11, false);
     
     //player adder and remover threads, where adder sleeps for k ms and remover sleeps for j ms 
     Thread player = new Thread(new PlayerThread(board, log));
     Thread adder = new Thread(new AdderThread(board, log, k));
     Thread remover = new Thread(new RemoverThread(board, log, j));
     
     //start the threads
     player.start();
     adder.start();
     remover.start();
     
     //make thread sleep for s seconds, input perceived in ms, *1000 to convert to seconds
     Thread.sleep(s * 1000);   
        
     //interrupt all threads
     player.interrupt();
     adder.interrupt();
     remover.interrupt();
     
     //join threads
     player.join();
     adder.join();
     remover.join();
        
     // access to shred resources - synchronize log access
     //sort the log in terms of thread timestamps and print the operations in chronological order 
     synchronized (log) {
    List<String> sortedLog = new ArrayList<>(log); // Copy to prevent modification issues
    sortedLog.sort(Comparator.comparingLong(entry -> Long.parseLong(entry.split(" ")[0])));
    sortedLog.forEach(System.out::println);
    }

        
     } catch (InterruptedException e) {
     	System.out.println("ERROR " +e);
        e.printStackTrace();
      	}
    }
    
    //method to get the time of thread's start of execution to end 
    //only called at log input, so shared access to startT is by default regulated with logLock
   public synchronized static String getTime() {
        return String.format("%09d", (System.currentTimeMillis() - startT));
      }
    
    public static void prepopulate(Board b, List<String> l, int p1, int p2, boolean bool) {
    //synchronize access to shared resources - board and log
    synchronized (l) {
        synchronized (b) {
            
            b.addSnakeOrLadder(p1, p2, bool);

            //capture time
            String time = getTime();
            
            if(bool){
                l.add(time + " Adder snake " + p1 + " " +p2);
            }else{
               
            l.add(time + " Adder ladder " + p1 + " " +p2);}
                
           }
         }
     }

   //cell class for the board setup
   public static class Cell {
    int x, y;
    Integer snakeHead = null; // Reference to destination cell for a snake
    Integer snakeTail = null;
  
    Integer ladderTop = null; // Reference to destination cell for a ladder
    Integer ladderBase = null;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    // Unique cell ID
    public int getId() {
        return y * 10 + x;
    }
}

//board class to set up game

public static class Board {
    private final Cell[][] grid = new Cell[10][10];
    
    //list of all snakes and ladders, has shared access, so variable should be synchronized
    public Map<Integer, int[]> SnakeLadderCells = Collections.synchronizedMap(new HashMap<>());

    public Board() {
        // Initialize the grid
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                grid[y][x] = new Cell(x, y);
            }
        }
    }
    
    //get the coordinate of cell from its unique id
    public Cell getCell(int id) {
        int x = id % 10;
        int y = id / 10;
        return grid[y][x];
    }

    public boolean addSnakeOrLadder(int startId, int endId, boolean isSnake) {
    
    //lock access to board
    synchronized(this){
    
        Cell start = getCell(startId);
        Cell end = getCell(endId);

        // Validate endpoints for location of snake or ladder
        if (startId == endId || start.snakeHead != null || start.ladderTop != null){ return false;}
        if (end.snakeHead != null || end.ladderTop != null){ return false;}

        if (isSnake && start.y <= end.y){ return false;} // Snake must go down
        if (!isSnake && start.y >= end.y){ return false;} // Ladder must go up
        
        //check if snake or ladder added
        if (isSnake) {
        start.snakeHead = endId;
        end.snakeTail = startId;
        } else {
        start.ladderTop = endId;
        end.ladderBase = startId;}
	
	//synchronize access to shared variable
        synchronized (SnakeLadderCells) {
        	SnakeLadderCells.put(endId, new int[]{isSnake ? 0 : 1, startId});
        } 
        
        return true;
        
        }
    }

    public List<Integer> removeSnakeOrLadder() {
    
    //synchronize access to board
    synchronized(this){
    
    //objects for removal temporary list
    List<Integer> ObjectRemoved = new ArrayList<>();
    
    //synchronize access to shared variable
    synchronized (SnakeLadderCells) {
    
       for (Map.Entry<Integer, int[]> entry : SnakeLadderCells.entrySet()) {
          
          int endId = entry.getKey();
          int[] details = entry.getValue(); // [type, startId]

          if (details[0] == 0) { // Snake to be removed
                ObjectRemoved.add(endId); // Snake head
                ObjectRemoved.add(details[1]); // Snake tail
                ObjectRemoved.add(0); // Type: Snake
          } else { // Ladder to be removed
                ObjectRemoved.add(endId); // Ladder top
                ObjectRemoved.add(details[1]); // Ladder base
                ObjectRemoved.add(1); // Type: Ladder
          }

          // Remove from the board
          Cell end = getCell(endId);
          if (details[0] == 0) { // Snake
                end.snakeTail = null;
                getCell(details[1]).snakeHead = null;
          } else { // Ladder
                end.ladderBase = null;
                getCell(details[1]).ladderTop = null;
          }

          // Remove entry from SnakeLadderCells
          SnakeLadderCells.remove(endId);

          return ObjectRemoved; // Only remove one object at a time
          }
    	}
    }
    return null; // Nothing to remove
    }
    
    }

//player thread class
public static class PlayerThread implements Runnable {
    private final Board board;
    private final List<String> log;
    private final Random rand = new Random();
    //starts at the beginning
    private int position = 0;

    public PlayerThread(Board board, List<String> log) {
        this.board = board;
        this.log = log;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
            //synchronize log access and board access
            synchronized(log){
            
            //choses a random number in range 1-6
                int roll = rand.nextInt(6) + 1;
                
                //advances its position that many cells
                int newPos = position + roll;
		
		//if thread goes to the final position of the grid of farther
                if (newPos >= 100) {
                
                //add log entry that the player won
                    log.add(getTime() + " Player wins");
                    
                    //make thread sleep for 100 ms before restarting
                    Thread.sleep(100);
                    
                    //take thread back to beginning, restart 
                    position = 0;
                    continue;
                }
		
		synchronized(board){
		//update thread position
                position = newPos;
                
                //capture time
                String time = getTime();
                
                //calculate location on the board
                Cell cell = board.getCell(position);
                
                
                //if thread encounters snake head at cell
                if (cell.snakeHead != null) {
                //add entry to log
                    log.add(time + " Player " + position + " " + cell.snakeHead);
                    //change its position accordingly
                    position = cell.snakeHead;
                
                //if thread encounters ladder top
                } else if (cell.ladderTop != null) {
                    log.add(time + " Player " + position + " " + cell.ladderTop);
                    position = cell.ladderTop;
                } else {
                
                //thread encounters nothing that would affect its position
                    log.add(time + " Player " + position);
                }
               
                
              //make thread sleep after move for random duration of 20-50 ms 
              Thread.sleep(20 + rand.nextInt(30));
              
              }}
              
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }  
}

//adder thread class
public static class AdderThread implements Runnable {
    private final Board board;
    private final List<String> log;
    private final Random rand = new Random();
    
    //sleep time is determined by input arg
    private final int sleepTime;

    public AdderThread(Board board, List<String> log, int sleepTime) {
        this.board = board;
        this.log = log;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
            
                //picks a random position on the board
                int start = rand.nextInt(100);
                
                //picks another random position on the board
                int end = rand.nextInt(100);
                
                //picks randomly whether to true  - to place a snake and a ladder otherwise
                boolean isSnake = rand.nextBoolean();
                
                //adds a snake or ladder from start to end cells, determined by isSnake boolean
     		if (board.addSnakeOrLadder(start, end, isSnake)) {
                //capture time
                    String time = getTime();
                    //synchronize log access
                    synchronized (log) {
                        log.add(time + " Adder " + (isSnake ? "snake" : "ladder") + " " + start + " " + end);
                    }
               
            }
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
}

//remover thread class
public static class RemoverThread implements Runnable {
    private final Board board;
    private final List<String> log;
    private final int sleepTime;
    private List<Integer> removedObject = new ArrayList<Integer>();
    private String object = null; 

    public RemoverThread(Board board, List<String> log, int sleepTime) {
        this.board = board;
        this.log = log;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
            //synchronize access to board
            synchronized(board){
            //looks for a snake or ladder on board and removes it
            removedObject = board.removeSnakeOrLadder();}
            if (removedObject != null && removedObject.size() == 3) {
            	object = removedObject.get(2) == 0 ? "snake" : "ladder";	
            } else {
            	continue; // Skip if nothing was removed
            }
            
         if(removedObject!=null){ 
         //capture time
         String time  = getTime();
         String object = (removedObject.get(2) == 0) ? "snake" : "ladder";
        
         //synchronize access to log
         synchronized(log){
            log.add(time + " Remover " + object + " " + removedObject.get(1) + " " + removedObject.get(0));
            }
            
          }
          
          //sleeps for time given by input before restarting
          Thread.sleep(sleepTime);
          
          } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                }
           }
        }
    }
}
   



