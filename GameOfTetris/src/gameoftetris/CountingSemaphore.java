
public class CountingSemaphore {
	private int signals = 0;

	public int getCount(){
		return signals;
	}
	
	public synchronized void take() {
		this.signals++;
		this.notify();
	}

	public synchronized void release() throws InterruptedException{
	    while(this.signals == 0) wait();
	    this.signals--;
	}
}
