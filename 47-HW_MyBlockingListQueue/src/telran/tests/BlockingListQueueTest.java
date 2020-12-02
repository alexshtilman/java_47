package telran.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import telran.util.concurrent.BlockingListQueue;

class BlockingListQueueTest {
	BlockingListQueue<String> myQueue;
	BlockingListQueue<String> myEmptyQueue;
	BlockingListQueue<String> myFullQueue;
	final static String[] testMessages = { "Hello", "world", "from", "Blocking", "List", "Queue", "Test" };

	@BeforeEach
	void setup() {
		myQueue = new BlockingListQueue<String>(8);
		myEmptyQueue = new BlockingListQueue<String>(3);
		myFullQueue = new BlockingListQueue<String>(7);
		// method addAll not implemented by the task
		for (String msg : testMessages) {
			myQueue.add(msg);
		}
		for (int i = 0; i < 7; i++) {
			myFullQueue.add(testMessages[i]);
		}
	}

	@Test
	void testAdd() {
		assertThrows(NullPointerException.class, () -> myQueue.add(null));
		assertDoesNotThrow(() -> myQueue.add("more data"));
		assertThrows(IllegalStateException.class, () -> myQueue.add("to much data"));
	}

	@Test
	void testPeek() {
		assertEquals("Hello", myQueue.peek());
		assertEquals("Hello", myQueue.peek());
		assertNotEquals("from", myFullQueue.peek());
		assertEquals("Hello", myQueue.peek());
		assertNull(myEmptyQueue.peek());
	}

	@Test
	void testElement() {
		assertEquals("Hello", myQueue.element());
		assertNotEquals("from", myFullQueue.element());
		assertThrows(NoSuchElementException.class, () -> myEmptyQueue.element());
	}

	@Test
	void testIsEmpty() {
		assertFalse(myQueue.isEmpty());
		assertFalse(myFullQueue.isEmpty());
		assertTrue(myEmptyQueue.isEmpty());
	}

	@Test
	void testOffer() {
		assertTrue(myQueue.offer("new Data"));
		assertFalse(myFullQueue.offer("new Data"));
		assertThrows(NullPointerException.class, () -> myEmptyQueue.offer(null));
	}

	@Test
	void testOfferCondition() {
		assertThrows(NullPointerException.class, () -> myEmptyQueue.offer(null, 3, TimeUnit.MILLISECONDS));
		assertDoesNotThrow(() -> myQueue.offer("extra data", 3, TimeUnit.MILLISECONDS));
		assertEquals(8, myQueue.size());
		// myQueue is full

		// start blocking producer
				// wait until it is started
				// wait small timeout, verify that producer still waiting
				// consume one message
				// join producer
				// verify it's results
		new Thread(() -> myQueue.poll()).start();

		new Thread(() -> assertDoesNotThrow(() -> assertTrue(myQueue.offer("extra data", 50, TimeUnit.MILLISECONDS))))
				.start();
	}

	@Test
	void testPoll() {
		assertNull(myEmptyQueue.poll());
		assertEquals("Hello", myQueue.poll());
		assertEquals("world", myQueue.poll());
	}

	@Test
	void testPollCondition() {
		assertDoesNotThrow(() -> {
			new Thread(() -> {
				assertDoesNotThrow(() -> Thread.sleep(45));
				myEmptyQueue.add("data");
			}).start();
			assertEquals("data", myEmptyQueue.poll(50, TimeUnit.MILLISECONDS));
		});

		assertDoesNotThrow(() -> {
			new Thread(() -> {
				assertDoesNotThrow(() -> Thread.sleep(55));
				myEmptyQueue.add("data2");
			}).start();
			assertNull(myEmptyQueue.poll(50, TimeUnit.MILLISECONDS));
		});
	}

	@Test
	void testPutCondition() {
		assertThrows(NullPointerException.class, () -> myQueue.put(null));
		assertDoesNotThrow(() -> myQueue.put("data"));
		assertEquals(8, myQueue.size());
		// myQueue is full
		assertDoesNotThrow(() -> {
			new Thread(() -> {
				assertDoesNotThrow(() -> Thread.sleep(200));
				myQueue.remove();
			}).start();
			myQueue.put("data");
		});
	}

	@Test
	void testRemove() {
		assertEquals("Hello", myQueue.remove());
		assertNotEquals("Hello", myQueue.remove());
		assertThrows(NoSuchElementException.class, () -> myEmptyQueue.remove());
	}

	@Test
	void testRemoveObject() {
		assertTrue(myQueue.remove("Blocking"));
		assertFalse(myQueue.remove("Blocking"));
		assertFalse(myEmptyQueue.remove("Blocking"));
		assertThrows(NullPointerException.class, () -> myEmptyQueue.remove(null));
	}

	@Test
	void tesTake() {
		assertDoesNotThrow(() -> {
			new Thread(() -> {
				assertDoesNotThrow(() -> Thread.sleep(200));
				myEmptyQueue.add("data");
			}).start();
			assertEquals("data", myEmptyQueue.take());
		});
	}
}
