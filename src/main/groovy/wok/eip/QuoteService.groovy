package wok.eip

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * @author Mark Fisher
 */
@MessageEndpoint
public class QuoteService {

	@ServiceActivator(inputChannel="tickers", outputChannel="quotes")
	public Quote lookupQuote(String ticker) {
		BigDecimal price = new BigDecimal(new Random().nextDouble() * 100);
		return new Quote(ticker, price.setScale(2, RoundingMode.HALF_EVEN));
	}

}


public class Quote {
	BigDecimal price
	String ticker
	
	Quote(String ticker, BigDecimal price) {
		this.ticker = ticker
		this.price = price
	}
}