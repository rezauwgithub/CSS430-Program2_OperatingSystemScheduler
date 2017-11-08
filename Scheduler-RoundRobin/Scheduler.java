

import java.util.*;


public class Scheduler extends Thread {

	private Vector queue;
	private int timeSlice;
	private static final int DEFAULT_TIME_SLICE = 1000;
	
	private static final int DEFAULT_MAX_THREADS = 10000;
	private boolean[] tids;
	private int nextId = 0;
	
	

	// Used as a helper function for all constructors
	public void constructObj(int quantum, int maxThreads) {
		timeSlice = quantum;
		queue = new Vector();

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
		
		for (int tid = 0; tid < maxThreads; tid++) {
			tids[tid] = false;
		}		
	}


	// Finds a tid[] array element whose value is false,
	// and returns its index as a new thread ID.
	private int getNewTid() {
		for (int i = 0; i < tids.length; i++) {
			int index = ((nextId + i) % tids.length);
			if (tids[index] == false) {
				tids[index] = true;
				
				nextId = ((index + 1) % tids.length);
				
				return index;
			}
		}


		return -1;
	}


	// Sets the corresponding tid[] elements, (i.e., tids[tid]) false.
	// The return value is false if tids[tid] is already false,
	// (i.e., if this tid has not been used), otherwise true;
	private boolean returnTid(int tid) {
		if ((tid >= 0) && (tid < tids.length) && (tids[tid] == true)) {
			tids[tid] = false;
			
			return true;
		}
		

		return false;
	}

	// Finds the current thread's TCB from the active thread queues and returns it.
	public TCB getMyTcb() {
		Thread currentThread = Thread.currentThread();
		
		synchronized(queue) {
			for (int i = 0; i < queue.size(); i++) { 
				TCB returnedThreadTCB = (TCB)queue.elementAt(i);
				
				Thread returnedThread = returnedThreadTCB.getThread();
				if (returnedThread == currentThread) {
					return returnedThreadTCB;
				}
			}
			
			
			return null;
		}	
	}


	// Returns the length of the tids[] array, 
	// (i.e., the available number of threads).
	public int getMaxThreads() {
		return tids.length;	
	}


	// Puts the scheduler to sleep for a given time quantum.
	private void schedulerSleep() {
		try {
			Thread.sleep((long)timeSlice);		
	
		} catch (InterruptedException ex) {}		
	}


	// Allocates a new TCB to this thread t and adds the TCB to the active thread queue.
    // This new TCB receives the calling thread's id as its parent id.
	public TCB addThread(Thread t) {
		TCB returnedTCB = getMyTcb();
		
		int returnedTid = -1;
		if (returnedTCB != null) {
			returnedTid = returnedTCB.getTid();
		}

		int newTid = getNewTid();
		if (newTid == -1) {
			return null;
		}

		
		TCB newTCB = new TCB(t, newTid, returnedTid);
		
		
		if (returnedTCB != null) {
			for (int i = 0; i < 32; i++) {
				newTCB.ftEnt[i] = returnedTCB.ftEnt[i];
				if (newTCB.ftEnt[i] != null) {
					newTCB.ftEnt[i].count++;
				}
			}
		
		}


		queue.add(newTCB);
		return newTCB;
	}


	// Find the current thread's TCB from the active thread queue and marks its TCB
	// as terminated. The actual deletion of a terminated TCB is performed inside
	// the run() method, (in order to prevent race conditions).
	public boolean deleteThread() {
		TCB myTCB = getMyTcb();
		if (myTCB != null) {
			return myTCB.setTerminated();
		}
		else {
			return false;
		}
	}


	// Puts the calling thread to sleep for a given time quantum.
	public void sleepThread(int quantumMillisecond) {
		try {
			sleep((long)quantumMillisecond);
		} catch (InterruptedException ex) {}		

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


		while (true) {
			try {
				if (queue.size() != 0) {
					TCB firstTCB = (TCB)queue.firstElement();
					if (!firstTCB.getTerminated()) {
						currentThread = firstTCB.getThread();
						if (currentThread != null) {
							if (currentThread.isAlive()) {
								currentThread.resume();
							}
							else {
								currentThread.start();
							}
						}

						
						schedulerSleep();
						
						synchronized(queue) {
							if (
								(currentThread != null) &&
								(currentThread.isAlive())) {

								currentThread.suspend();
							}

							queue.remove(firstTCB);
							queue.add(firstTCB);
						}
					}
					else {
						queue.remove(firstTCB);
						returnTid(firstTCB.getTid());
					}
				}
			} catch (NullPointerException ex) {}
		}
	
	}
}
