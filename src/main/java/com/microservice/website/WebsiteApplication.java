package com.microservice.website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@EnableDiscoveryClient
public class WebsiteApplication implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(WebsiteApplication.class);


	RestTemplate searchClient = new RestTemplate();

	RestTemplate bookingClient = new RestTemplate();

	RestTemplate checkInClient = new RestTemplate();

	//RestTemplate restClient= new RestTemplate();

	public static void main(String[] args) {
		SpringApplication.run(WebsiteApplication.class, args);
	}


	@Override
	public void run(String... strings) throws Exception {
		//Search for a flight
		SearchQuery searchQuery2 = new SearchQuery("NYC","SFO","22-JAN-18");
		SearchQuery searchQuery = new SearchQuery("SEA","SFO","22-JAN-16");
//		Flight[] flights = searchClient.postForObject("http://localhost:8083/search/get", searchQuery, Flight[].class); // chapter 6
		Flight[] flights2 = searchClient.postForObject("http://localhost:8090/search/get", searchQuery2, Flight[].class);
		Flight[] flights = searchClient.postForObject("http://localhost:8090/search/get", searchQuery, Flight[].class); // chapter 7

		//Flight[] flights = searchClient.postForObject("http://localhost:8083/search/get", searchQuery, Flight[].class);


//		ㅈㅜ석 처리하면 config server, search, website 으로만 스프링 cloud config 기능을 간단하게 테스트할수있다.
		Arrays.asList(flights).forEach(flight -> logger.info(" flight >"+ flight));

		//create a booking only if there are flights.
		if(flights == null || flights.length == 0){
			return;
		}
		Flight flight = flights[0];
		BookingRecord booking = new BookingRecord(flight.getFlightNumber(),flight.getOrigin(),
				flight.getDestination(), flight.getFlightDate(),null,
				flight.getFares().getFare());
		Set<Passenger> passengers = new HashSet<Passenger>();
		passengers.add(new Passenger("Gavin","Franc","Male", booking));
		booking.setPassengers(passengers);
		long bookingId =0;
		try {
			//long bookingId = bookingClient.postForObject("http://book-service/booking/create", booking, long.class);
//			bookingId = bookingClient.postForObject("http://localhost:8080/booking/create", booking, long.class);
			bookingId = bookingClient.postForObject("http://localhost:8060/booking/create", booking, long.class);
			logger.info("Booking created "+ bookingId);
		}catch (Exception e){
			logger.error("BOOKING SERVICE NOT AVAILABLE...!!!");
		}

		//check in passenger
		if(bookingId == 0) return;
		try {
			CheckInRecord checkIn = new CheckInRecord("Franc", "Gavin", "28C", null, "BF101","22-JAN-18", bookingId);
//			long checkinId = checkInClient.postForObject("http://localhost:8081/checkin/create", checkIn, long.class);
			long checkinId = checkInClient.postForObject("http://localhost:8070/checkin/create", checkIn, long.class);
			logger.info("Franc Gavin 28C BF101 BookingId " + bookingId);
			logger.info("Checked IN, checkedIn id:"+ checkinId);
		}catch (Exception e){
			logger.error("CHECK IN SERVICE NOT AVAILABLE...!!!");
		}
	}
}


@Configuration
class MyConfiguration {

	@LoadBalanced
	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

}