package wok.eip

import java.util.concurrent.atomic.AtomicInteger

class QuoteSubscriptionRegistry {
	Map subscriptions = Collections.synchronizedMap(new HashMap<String, Integer>())
	Set tickerQueue = Collections.synchronizedSet(new HashSet())

	void subscribe(String ticker) {
		while (1) {
			subscriptions.putIfAbsent ticker, 0
			Integer oldVal = subscriptions.get ticker
			if (subscriptions.replace ( ticker, oldVal, oldVal + 1 ))
				break
		}
	}
	
	void unsubscribe(ticker){
		while (1) {
			Integer oldVal = subscriptions.get ticker
			if (!oldVal)
				break
			if (subscriptions.replace(ticker, oldVal, oldVal -1))
				break
		}
		subscriptions.remove ticker, 0
	}
	
	Set take(Integer n) {
		if (tickerQueue.size() == 0)
			tickerQueue.addAll(subscriptions.keySet())
		
		Set taken = tickerQueue.take(n)
		taken.each { ticker -> tickerQueue.remove ticker }
		taken
	}
}
