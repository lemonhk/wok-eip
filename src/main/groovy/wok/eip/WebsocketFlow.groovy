package wok.eip

import java.util.concurrent.Executors

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.channel.MessageChannels
import org.springframework.integration.metadata.SimpleMetadataStore
import org.springframework.integration.store.SimpleMessageStore
import org.springframework.integration.websocket.IntegrationWebSocketContainer
import org.springframework.integration.websocket.ServerWebSocketContainer
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.socket.messaging.StompSubProtocolHandler

@Configuration
@EnableIntegration
@IntegrationComponentScan
class WebsocketFlows {
	//Server side
	@Bean
	IntegrationWebSocketContainer serverWebSocketContainer() {SimpleMetadataStore
		return new ServerWebSocketContainer("/endpoint").withSockJs();
	}
	
	//<bean id="webSocketSessionStore" class="org.springframework.integration.metadata.SimpleMetadataStore"/>
	@Bean
	SimpleMetadataStore webSocketSessionStore() {
		new SimpleMetadataStore()
	}
	//<bean id="chatMessagesStore" class="org.springframework.integration.store.SimpleMessageStore"/>
	@Bean
	SimpleMessageStore chatMessageStore() {
		new SimpleMessageStore()
	}
	/*
	<util:map id="chatRoomSessions" value-type="java.util.List">
		<entry key="room1" value="#{new java.util.ArrayList()}"/>
		<entry key="room2" value="#{new java.util.ArrayList()}"/>
	</util:map>
	*/
	
	@Bean
	MessageHandler webSocketOutboundAdapter() {
		return new WebSocketOutboundMessageHandler(serverWebSocketContainer());
	}
	
	//<bean id="stompSubProtocolHandler" class="org.springframework.web.socket.messaging.StompSubProtocolHandler"/>
	@Bean
	StompSubProtocolHandler stompSubProtocolHandler() {
		new StompSubProtocolHandler()
	}
	
	@Bean(name = "webSocketFlow.input")
	MessageChannel requestChannel() {
		return new DirectChannel();
	}
	
	@Bean
	IntegrationFlow webSocketFlow() {
		Set<String> sessionKeys = serverWebSocketContainer().getSessions().keySet()
		sessionKeys.collect() { s ->
			def m = MessageBuilder.createMessage("Payload", [:])
			m = MessageBuilder.fromMessage(m).setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, s)
			.build()
		}
		IntegrationFlows.from("webSocketFlow.input")
		.channel(MessageChannels.executor(Executors.newCachedThreadPool()))
		.handle(webSocketOutboundAdapter())
		.get()
//		return f -> {
//			Function<Message , Object> splitter = m -> serverWebSocketContainer()
//					.getSessions()
//					.keySet()
//					.stream()
//					.map(s -> MessageBuilder.fromMessage(m)
//							.setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, s)
//							.build())
//					.collect(Collectors.toList());
//			f.split( Message.class, splitter)
//					.channel(c -> c.executor(Executors.newCachedThreadPool()))
//					.handle(webSocketOutboundAdapter());
//		};
	}
	
	@RequestMapping("/hi/{name}")
	public void send(@PathVariable String name) {
		requestChannel().send(MessageBuilder.withPayload(name).build());
	}
}

@MessagingGateway
@Controller
public interface WebSocketGateway {

	@MessageMapping("/greeting")
	@SendToUser("/queue/answer")
	@Gateway(requestChannel = "greetingChannel")
	String greeting(String payload);
}
	
//@Controller
//public class WebSocketController {	
//	@Autowired MessageChannel requestChannel
//	
//	@RequestMapping("/hi/{name}")
//	public void send(@PathVariable String name) {
//		requestChannel().send(MessageBuilder.withPayload(name).build());
//	}
//}