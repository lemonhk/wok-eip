package wok.eip

import groovy.json.JsonSlurper

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Splitter
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.channel.MessageChannels
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.handler.annotation.Payload

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException



@Configuration
@EnableIntegration
@IntegrationComponentScan
class RtqFlow {

	String nowFinanceQuoteUrl = 'http://finance.now.com/api/getAfeQuote.php?callback=d123131&item={ticker}&fidlist=100'

	String[] tickers = [
		"00064",
		"00123",
		"00348",
		"715",
		"735"
	]
	int nextIdx = 0
	
	@Value('${rtq.batchsize:3}') Integer batchSize
	@Autowired QuoteSubscriptionRegistry subscriptionRegistry

	@Bean
	public QuoteSubscriptionRegistry subscriptionRegistry() {
		def registry = new QuoteSubscriptionRegistry()
		tickers.each {ticker -> registry.subscribe(ticker) }
		registry.subscribe("00123")
		registry
	}

	/* Flow def */

	@InboundChannelAdapter(value = "batchQuoteChannel", poller = @Poller(maxMessagesPerPoll = "1", fixedRate = "5000"))
	@Description("Start by fetch a batch of tickers")
	public List<String> quoteRequestQueue() {
		subscriptionRegistry.take(batchSize).collect { t ->
			def ticker = nowFinanceTicker t
		}
	}
	
	@Bean
	public MessageChannel batchQuoteChannel() {
		return MessageChannels.direct().get()
	}

	@Splitter(inputChannel="batchQuoteChannel", outputChannel="quoteRequestChannel")
	@Description("Split the batch, each into a quote request message")
	public List<Message> splitBatch(@Payload List<String> tickers) {
		tickers.collect { ticker ->
			MessageBuilder.withPayload(ticker)
					.setHeader("ticker", ticker)
					.build()
		}
	}
	
	@Bean
	public MessageChannel quoteRequestChannel() {
		return MessageChannels.direct().get()
	}

	@Bean
	@ServiceActivator(inputChannel = "quoteRequestChannel")
	@Description("Fetch the quote for a ticker specified in the message header.")
	public MessageHandler quoteRequest() {
		SpelExpressionParser expressionParser = new SpelExpressionParser()

		Map<String, Expression> uriVariableExpressions = new HashMap<String, Expression>(1)
		uriVariableExpressions.put("ticker", expressionParser.parseExpression("headers.ticker"))

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(nowFinanceQuoteUrl);
		handler.setUriVariableExpressions uriVariableExpressions
		handler.setExpectedResponseType(String.class)
		handler.setOutputChannelName("transformChannel")
		return handler;
	}

	@Transformer( inputChannel = "transformChannel", outputChannel = "quoteData" )
	public Map transformToCommonFormat(String jsonp) throws JsonMappingException, JsonParseException, IOException{
		def matcher = jsonp =~ /d123131\((.*)\)/
		try {
			def json = new JsonSlurper().parseText(matcher[0][1].replace("\\", ""))     // jsonsluper does not like backslashes char.
			if (!json.riclist && json.ric)  // when requesting a single stock, the result is not a list!!
				json = [riclist: [json]]

			def flds = json.riclist.ric.inject([:]) { dm, stk ->
				def stkCode = stk.name.replace("*", "").replace(".HK", "").padLeft(5, "0")
				def stkData = stk.fid.inject([:]) { m, f ->
					m["fid${f.id}"] = f.value
					m
				}
				dm[stkCode] = stkData
				dm
			}
		} catch (e) {
			throw e
		}
	}

	@ServiceActivator(inputChannel = "quoteData")
	public void counter(Map quote) {
		println "quote: ${quote}"
	}

	String nowFinanceTicker(String ticker) {
		def l = ticker.length()
		if (l < 4 ) {
			ticker = ticker.padLeft 4, "0"
		} else if (l > 4) {
			ticker = ticker[(l - 4)..-1]
		}
		ticker
	}
}
