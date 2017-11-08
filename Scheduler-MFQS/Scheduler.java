

import java.util.Vector;

public class Scheduler extends Thread {

	private static final int DEFAULT_TIME_SLICE = 1000;
	private static final int DEFAULT_MAX_THREADS = 10000;
	
	
	private Vector queue0;
	private Vector queue1;
	private Vector queue2;
	
	private int timeSlice;
	private int zeroSlice;
	private int twoSlice;
	
	private boolean[] tids;
	
	private int nextID = 0;
	
	
	// Used as a helper function for all constructors
	public void constructObj(int quantum, int maxThreads) {
		timeSlice = quantum;
		zeroSlice = (quantum / 2);
		twoSlice = (quantum * 2);
		
		queue0 = new Vector();
		queue1 = new Vector();
		queue2 = new Vector();

		initTid(maxThreads);
	}

	
	
	// All constructors use the constructObj(int quantum, int maxThreads) 
	// to contruct the object.
	public Scheduler() {
		constructObj(DEFAULT_TIME_SLICE, DEFAULT_MAX_THREADS);
	}
	
	public Scheduler(int quantum) {
		constructObj(quantum, DEFAULT_MAX_THREADS);
	}
	
	public Scheduler(int quantum, int maxThreads) {
		constructObj(quantum, maxThreads);
	}		
	
	
	
	// Allocates the tid[] array with a maxThreads number of elements
    private void initTid(int maxThreads) {
        tids = new boolean[maxThreads];

        for(int tid = 0; tid < maxThreads; tid++) {
            tids[tid] = false;
        }

    }

	
	// Finds a tid[] array element whose value is false,
	// and returns its index as a new thread ID.
    private int getNewTid() {
        for(int i = 0; i < tids.length; i++) {
            int index = (nextID + i) % tids.length;
			
            if(!tids[index]) {
                tids[index] = true;
                nextID = (index + 1) % tids.length;
				
                return index;
            }
        }
		

        return -1;
    }

	
	// Sets the corresponding tid[] elements, (i.e., tids[tid]) false.
	// The return value is false if tids[tid] is already false,
	// (i.e., if this tid has not been used), otherwise true;
    private boolean returnTid(int tid) {
        if((tid >= 0) && (tid < tids.length) && tids[tid]) {
			
            tids[tid] = false;
			
            return true;
			
        } else {
            return false;
        }
    }

	
    // Finds the current thread's TCB from the active thread queues and returns it.
    public TCB getMyTcb() {
        Thread currentThread = Thread.currentThread();

        synchronized(queue0) {
            for(int i = 0; i < queue0.size(); ++i) {
                TCB currentThreadsTCB = (TCB)queue0.elementAt(i);
				
                Thread returnedThread = currentThreadsTCB.getThread();
                if(returnedThread == currentThread) {
                    return currentThreadsTCB;
                }
            }
        }
        synchronized(queue1) {
            for(int i = 0; i < queue1.size(); ++i) {
                TCB currentThreadsTCB = (TCB)queue1.elementAt(i);
				
                Thread returnedThread = currentThreadsTCB.getThread();
                if(returnedThread == currentThread) {
                    return currentThreadsTCB;
                }
            }
        }
        synchronized(queue2) {
            for(int i = 0; i < queue2.size(); ++i) {
                TCB currentThreadsTCB = (TCB)queue2.elementAt(i);
				
                Thread returnedThread = currentThreadsTCB.getThread();
                if(returnedThread == currentThread) {
                    return currentThreadsTCB;
                }
            }
        }
		
		
        return null;
    }

	
	// Returns the length of the tids[] array, 
	// (i.e., the available number of threads).
    public int getMaxThreads() {
        return tids.length;
    }


	// Puts the scheduler to sleep for a given time quantum.
    private void schedulerSleep(int timeQuantum) {
        try {
            Thread.sleep((long)timeQuantum);
			
        } catch (InterruptedException iex) {
            
        }

    }
	

    // Allocates a new TCB to this thread t and adds the TCB to the active thread queue.
    // This new TCB receives the calling thread's id as its parent id.
    public TCB addThread(Thread t) {
        t.setPriority(2);// New threads start out at top of queue

        TCB myTCB = getMyTcb();
		
		int pid = -1;
        if (myTCB != null) {
			pid = myTCB.getTid();
		}
		
        int newTid = getNewTid();
        if(newTid == -1) {
            return null;
			
        } else {
            TCB newTCB = new TCB(t, newTid, pid);

            queue0.add(newTCB);
            return newTCB;
        }
    }

	
	// Find the current thread's TCB from the active thread queue and marks its TCB
	// as terminated. The actual deletion of a terminated TCB is performed inside
	// the run() method, (in order to prevent race conditions).
    public boolean deleteThread( ) {
        TCB myTCB = getMyTcb( );
        if (myTCB != null ) {
            return myTCB.setTerminated( );
			
		}
        else {
            return false;
		}
    }

	
	
	// Puts the calling thread to sleep for a given time quantum.
    public void sleepThread(int quantumMillisecond) {
        try {
            sleep((long)quantumMillisecond);
			
        } catch (InterruptedException iex) {
            
        }

    }

	
	// This is the heart of Scheduler. The difference from the lecture slides includes:
	// (1) retrieving a new available TCB rather than a thread from teh active thread list,
	// (2) deleting it if it has been marked as "terminated", and
	// (3) starting the thread if it has not yet been started. Other than this difference,
	// the Scheduler repeats retrieving a new available TCB from the list, raising up the 
	// corresponding thread's priority, yielding CPU to this thread with sleep(),
	// and lowering the thread's priority.
    public void run() {
		
        Thread currentThread = null;
        setPriority(6);

		while(true) {
			try {
				// Handle Threads in queue0 first
				if(queue0.size() != 0) {
					TCB firstTCB = (TCB)queue0.firstElement();

					//check for end of life status
					if(!firstTCB.getTerminated()) {
						currentThread = firstTCB.getThread();

						if(currentThread != null) {
							//if already started
							if(currentThread.isAlive()) {
								//resume
								currentThread.setPriority(2);
							} else {//not started yet
								currentThread.start();
								currentThread.setPriority(2);
							}
						}

						//wait half quantum
						schedulerSleep(zeroSlice);

						synchronized(queue0) {
							if(currentThread != null && currentThread.isAlive()) {
								currentThread.setPriority(4);//Thread taking too long, suspend
							}
							//Thread has taken too long, move to lower queue
							queue0.remove(firstTCB);
							queue1.add(firstTCB);
						}
					} else {//Thread has completed, remove and return
						queue0.remove(firstTCB);
						returnTid(firstTCB.getTid());
					}
				}
				else if (queue1.size() != 0){//queue1 lower priority than queue0
					TCB firstTCB = (TCB)queue1.firstElement();
					if(!firstTCB.getTerminated()) {
						currentThread = firstTCB.getThread();
						if(currentThread != null) {
							if (currentThread.isAlive()) {
								currentThread.setPriority(2);
							} else {
								currentThread.start();
								currentThread.setPriority(2);
							}
						}

						//check every half quantum to see if queue0 NOT empty
						schedulerSleep(zeroSlice);

						if(queue0.size() != 0){
							currentThread.setPriority(4);//suspend Thread if queue0 NOT empty
							continue;
						}

						//second round
						schedulerSleep(zeroSlice);

						if(queue0.size() != 0){
							currentThread.setPriority(4);
							continue;
						}

						synchronized(queue1) {//Thread has taken too long
							if(currentThread != null && currentThread.isAlive()) {
								currentThread.setPriority(4);//suspend
							}

							//move Thread to lower priority queue
							queue1.remove(firstTCB);
							queue2.add(firstTCB);
						}
					} else {
						queue1.remove(firstTCB);
						returnTid(firstTCB.getTid());
					}
				}
				else if (queue2.size() != 0){//lower priority than queue0 and queue1
					TCB firstTCB = (TCB)queue2.firstElement();
					if(!firstTCB.getTerminated()) {
						currentThread = firstTCB.getThread();
						if(currentThread != null) {
							if(currentThread.isAlive()) {
								currentThread.setPriority(2);
							} else {
								currentThread.start();
								currentThread.setPriority(2);
							}
						}

						//check if queue0 and queue1 are NOT empty every half quantum
						for(int x = 0; x < 4; x++){
							schedulerSleep(zeroSlice);

							if(queue0.size() != 0){
								currentThread.setPriority(4);//suspend if queue NOT empty
								continue;
							}

							if(queue1.size() != 0){
								currentThread.setPriority(4);//suspend if queue NOT empty
								continue;
							}
						}

						synchronized(queue2) {
							if(currentThread != null && currentThread.isAlive()) {
								currentThread.setPriority(4);//thread taken too long, suspend
							}

						}
					} else {
						queue2.remove(firstTCB);
						returnTid(firstTCB.getTid());
					}
				}
			} catch (NullPointerException var6) {
				;
			}
		}
    }
}

