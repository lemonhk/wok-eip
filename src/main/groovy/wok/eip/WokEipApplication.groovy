package wok.eip

import java.math.RoundingMode

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Description
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.messaging.MessageChannel
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@SpringBootApplication
@Controller
@EnableAutoConfiguration
@ComponentScan
@EnableIntegration
class WokEipApplication {
	@Value('${displayMessage}')
	String displayMessage ;
	int counter = 0;

	static void main(String[] args) {
		SpringApplication.run WokEipApplication, args
	}

	@RequestMapping("/")
	@ResponseBody
	String home() {
		return displayMessage + counter;
	}

	/* --------------------------------*/

	@Bean
	@Description("Entry to the messaging system through the gateway.")
	public MessageChannel rtqChannel() {
		return new DirectChannel();
	}

	@InboundChannelAdapter(value = "rtqChannel", poller = @Poller(fixedRate = "2000", maxMessagesPerPoll = '1'))
	public BigDecimal foo() {
		new BigDecimal(new Random().nextDouble() * 100)
	}


	@ServiceActivator(inputChannel = "rtqChannel")
	public void counter(BigDecimal value) {
		def scaled = value.setScale(2, RoundingMode.HALF_EVEN)
		println "${counter}: ${scaled}"
		counter++
	}


	@MessagingGateway
	public interface TempConverter {

		@Gateway(requestChannel = "convert.input")//	@InboundChannelAdapter(value = "fooChannel", poller = @Poller(fixed-rate = "5000"))
		float fahrenheitToCelcius(float fahren);
	}
}
