
public class Mutex {
	private boolean signal = true;

	public synchronized void take() {
		this.signal = true;
		this.notify();
	}

	public synchronized void release() throws InterruptedException{
		while(!this.signal) wait();
		this.signal = false;
	}
}
