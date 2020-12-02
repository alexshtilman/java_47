package telran.util.concurrent;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingListQueue<E> implements BlockingQueue<E> {
	private static final String NULL_POINTER = "The specified element is null";
	private static final String ILLEGAL_STATE = "The element cannot be added at this time due to capacity restriction";
	private static final String NO_SUCH_ELEMENT = "Can not get element or this queue is empty";
	Integer capacity;
	private Lock monitor = new ReentrantLock();
	private Condition cosumersCondition = monitor.newCondition();
	private Condition producerCondition = monitor.newCondition();
	private Queue<E> queue = new LinkedList<>();

	public BlockingListQueue() {
		this.capacity = Integer.MAX_VALUE;
	}

	public BlockingListQueue(int capacity) {
		this.capacity = capacity;
	}

	/*
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions.
	 * 
	 * Throws: IllegalStateException - if the element cannot be added at this time
	 * due to capacity restrictions NullPointerException - if the specified element
	 * is null not implemented: IllegalArgumentException - if some property of the
	 * specified element prevents it from being added to this queue not implemented:
	 * ClassCastException - if the class of the specified element prevents it from
	 * being added to this queue Returning true upon success and throwing an
	 * IllegalStateException if no space is currently available
	 */
	@Override
	public boolean add(E e) {
		if (e == null)
			throw new NullPointerException(NULL_POINTER);
		try {
			monitor.lock();
			if (capacity == queue.size()) {
				throw new IllegalStateException(ILLEGAL_STATE);
			}
			cosumersCondition.signal();
			return queue.add(e);
		} finally {
			monitor.unlock();
		}

	}

	/*
	 * Retrieves, but does not remove, the head of this queue, or returns null if
	 * this queue is empty.
	 */
	@Override
	public E peek() {
		try {
			monitor.lock();
			if (queue.isEmpty()) {
				return null;
			}
			return queue.element();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Retrieves, but does not remove, the head of this queue. This method differs
	 * from peek only in that it throws an exception if this queue is empty.
	 */
	@Override
	public E element() {
		try {
			monitor.lock();
			if (queue.isEmpty()) {
				throw new NoSuchElementException(NO_SUCH_ELEMENT);
			}
			return queue.element();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Returns true if this collection contains no elements.
	 */
	@Override
	public boolean isEmpty() {
		try {
			monitor.lock();
			return queue.isEmpty();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions. When using a
	 * capacity-restricted queue, this method is generally preferable to add(E),
	 * which can fail to insert an element only by throwing an exception. Returns:
	 * true if the element was added to this queue, else false Throws:
	 * ClassCastException - if the class of the specified element prevents it from
	 * being added to this queue NullPointerException - if the specified element is
	 * null and this queue does not permit null elements IllegalArgumentException -
	 * if some property of this element prevents it from being added to this queue
	 */
	@Override
	public boolean offer(E e) {
		if (e == null)
			throw new NullPointerException(NULL_POINTER);
		try {
			monitor.lock();
			if (capacity == queue.size()) {
				return false;
			}
			queue.add(e);
			cosumersCondition.signal();
			return true;
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Inserts the specified element into this queue, waiting up to the specified
	 * wait time if necessary for space to become available.
	 * 
	 * Throws: IllegalStateException - if the element cannot be added at this time
	 * due to capacity restrictions NullPointerException - if the specified element
	 * is null interruptedException - if interrupted while waiting
	 * ClassCastException - if the class of the specified element prevents it from
	 * being added to this queue Returns: true if successful, or false if the
	 * specified waiting time elapses before space is available
	 */
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		if (e == null)
			throw new NullPointerException(NULL_POINTER);
		try {
			monitor.lock();
			Date deadline = new Date(System.currentTimeMillis() + unit.convert(timeout, TimeUnit.MILLISECONDS));
			boolean stillWaiting = true;
			while (capacity == queue.size()) {
				if (!stillWaiting)
					return false;
				stillWaiting = producerCondition.awaitUntil(deadline);
			}
			queue.add(e);
			return true;
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Retrieves and removes the head of this queue, or returns null if this queue
	 * is empty. Returns: the head of this queue, or null if this queue is empty
	 */
	@Override
	public E poll() {
		
		try {
			monitor.lock();
			if (queue.isEmpty()) {
				return null;
			}
			producerCondition.signal();
			return queue.poll();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Retrieves and removes the head of this queue, waiting up to the specified
	 * wait time if necessary for an element to become available. Returns: the head
	 * of this queue, or null if the specified waiting time elapses before an
	 * element is available
	 */
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {

		try {
			monitor.lock();
			Date deadline = new Date(System.currentTimeMillis() + unit.convert(timeout, TimeUnit.MILLISECONDS));
			boolean stillWaiting = true;
			while (queue.isEmpty()) {
				if (!stillWaiting)
					return null;
				stillWaiting = cosumersCondition.awaitUntil(deadline);
			}
			producerCondition.signal();
			return queue.poll();
		} finally {
			monitor.unlock();
		}
	}

	@Override
	public void put(E e) throws InterruptedException {
		if (e == null)
			throw new NullPointerException(NULL_POINTER);
		try {
			monitor.lock();
			while (queue.size() == capacity) {
				producerCondition.await();
			}
			queue.add(e);
			cosumersCondition.signal();
		} finally {
			monitor.unlock();
		}

	}

	/*
	 * Retrieves and removes the head of this queue. This method differs from poll
	 * only in that it throws an exception if this queue is empty. Throws:
	 * NoSuchElementException - if this queue is empty
	 */
	@Override
	public E remove() {
		try {
			monitor.lock();
			if (queue.isEmpty())
				throw new NoSuchElementException();
			producerCondition.signal();
			return queue.remove();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Removes a single instance of the specified element from this queue, if it is
	 * present. More formally, removes an element e such that o.equals(e), if this
	 * queue contains one or more such elements. Returns true if this queue
	 * contained the specified element (or equivalently, if this queue changed as a
	 * result of the call). Throws: ClassCastException - if the class of the
	 * specified element is incompatible with this queue (optional)
	 * NullPointerException - if the specified element is null (optional)
	 */
	@Override
	public boolean remove(Object o) {
		if (o == null)
			throw new NullPointerException(NULL_POINTER);
		try {
			monitor.lock();
			producerCondition.signal();
			return queue.remove(o);
		} finally {
			monitor.unlock();
		}
	}

	@Override
	public int size() {
		try {
			monitor.lock();
			return queue.size();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * Retrieves and removes the head of this queue, waiting if necessary until an
	 * element becomes available.
	 * 
	 * 
	 */
	@Override
	public E take() throws InterruptedException {
		try {
			monitor.lock();
			while (queue.isEmpty()) {
				cosumersCondition.await();
			}
			producerCondition.signal();
			return queue.remove();
		} finally {
			monitor.unlock();
		}
	}

	/*
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * STOP RIGHT HERE
	 * 
	 * METHODS BELOW ARE NOT EMPLEMENTED
	 * 
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */

	@Override
	public Object[] toArray() {
		// not implemented
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// not implemented
		return null;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		// not implemented
		return false;
	}

	@Override
	public void clear() {
		// not implemented

	}

	@Override
	public boolean contains(Object o) {
		// not implemented
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// not implemented
		return false;
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		// not implemented
		return 0;
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		// not implemented
		return 0;
	}

	@Override
	public Iterator<E> iterator() {
		// not implemented
		return null;
	}

	@Override
	public int remainingCapacity() {
		// not implemented
		return 0;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// not implemented
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// not implemented
		return false;
	}
}
