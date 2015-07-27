package wok.eip

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.Aggregator
import org.springframework.integration.annotation.CorrelationStrategy
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.AggregatorSpec
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.RouterSpec
import org.springframework.integration.dsl.channel.MessageChannels
import org.springframework.integration.dsl.support.Consumer
import org.springframework.integration.dsl.support.GenericHandler
import org.springframework.integration.router.ExpressionEvaluatingRouter
import org.springframework.integration.samples.cafe.Delivery
import org.springframework.integration.samples.cafe.Drink
import org.springframework.integration.samples.cafe.Order
import org.springframework.integration.samples.cafe.OrderItem
import org.springframework.integration.stream.CharacterStreamWritingMessageHandler
import org.springframework.integration.transformer.GenericTransformer
import org.springframework.stereotype.Component

import com.google.common.util.concurrent.Uninterruptibles

//@Configuration
//@EnableIntegration
//@IntegrationComponentScan
class CafeFlow {

	private final AtomicInteger hotDrinkCounter = new AtomicInteger();

	private final AtomicInteger coldDrinkCounter = new AtomicInteger();

	@MessagingGateway
	public interface Cafe {

		@Gateway(requestChannel = "orders.input")
		void placeOrder(Order order);
	}

	@Component
	public static class CafeAggregator {

		@Aggregator
		public Delivery output(List<Drink> drinks) {
			return new Delivery(drinks);
		}

		@CorrelationStrategy
		public Integer correlation(Drink drink) {
			return drink.getOrderNumber();
		}
	}

	//	@Bean
	//	public IntegrationFlow convert() {
	//		IntegrationFlows.from("orders.input")
	//				.split("payload.items", (Consumer) null)
	//				.channel(MessageChannels.executor(Executors.newCachedThreadPool()))
	//				.route("payload.iced",
	//				new Consumer<RouterSpec<ExpressionEvaluatingRouter>>() {
	//
	//					@Override
	//					public void accept(RouterSpec<ExpressionEvaluatingRouter> spec) {
	//						spec.channelMapping("true", "iced")
	//								.channelMapping("false", "hot");
	//					}
	//				})
	//				.get();
	//	}
	@Bean
	@SuppressWarnings("unchecked")
	public IntegrationFlow orders() {                                      // 13
		return IntegrationFlows.from("orders.input")                         // 14
				.split("payload.items", (Consumer) null)                           // 15
				.channel(MessageChannels.executor(Executors.newCachedThreadPool()))// 16
				.route("payload.iced", new Consumer<RouterSpec<ExpressionEvaluatingRouter>>() {

					@Override
					public void accept(RouterSpec<ExpressionEvaluatingRouter> spec) {
						spec.channelMapping("true", "iced")
								.channelMapping("false", "hot");                         // 19
					}

				})
				.get();                                                            // 20
	}

	@Bean
	public IntegrationFlow icedFlow() {                                    // 21
		return IntegrationFlows.from(MessageChannels.queue("iced", 10))      // 22
				.handle(new GenericHandler<MyOrderItem>() {                          // 23

					@Override
					public Object handle(MyOrderItem payload, Map<String, Object> headers) {
						Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
						System.out.println(Thread.currentThread().getName()
								+ " prepared cold drink #" + coldDrinkCounter.incrementAndGet()
								+ " for order #" + payload.getOrderNumber() + ": " + payload);
						return payload;                                                // 24
					}

				})
				.channel("output")                                                 // 25
				.get();
	}

	@Bean
	public IntegrationFlow hotFlow() {                                     // 26
		return IntegrationFlows.from(MessageChannels.queue("hot", 10))
				.handle(new GenericHandler<MyOrderItem>() {

					@Override
					public Object handle(MyOrderItem payload, Map<String, Object> headers) {
						Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);    // 27
						System.out.println(Thread.currentThread().getName()
								+ " prepared hot drink #" + hotDrinkCounter.incrementAndGet()
								+ " for order #" + payload.getOrderNumber() + ": " + payload);
						return payload;
					}

				})
				.channel("output")
				.get();
	}

	@Bean
	public IntegrationFlow resultFlow() {                                  // 28
		return IntegrationFlows.from("output")                               // 29
				.transform(new GenericTransformer<MyOrderItem, Drink>() {            // 30

					@Override
					public Drink transform(MyOrderItem orderItem) {
						return new Drink(orderItem.getOrderNumber(),
								orderItem.getDrinkType(),
								orderItem.isIced(),
								orderItem.getShots());                                       // 31
					}

				})
				.aggregate(new Consumer<AggregatorSpec>() {                        // 32

					@Override
					public void accept(AggregatorSpec aggregatorSpec) {
						aggregatorSpec.processor(cafeAggregator, null);                // 33
					}

				}, null)
				.handle(CharacterStreamWritingMessageHandler.stdout())             // 34
				.get();
	}

	class MyOrderItem extends OrderItem {
		Integer getOrderNumber() {
			1234
		}
	}
}
