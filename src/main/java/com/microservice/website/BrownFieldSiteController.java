package com.microservice.website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Controller
public class BrownFieldSiteController {
	private static final Logger logger = LoggerFactory.getLogger(BrownFieldSiteController.class);

	@Autowired
	RestTemplate restClient;


  	RestTemplate searchClient = new RestTemplate();

  	RestTemplate bookingClient = new RestTemplate();

  	RestTemplate checkInClient = new RestTemplate();

  	/*
  	@ModelAttribute 선언 후 자동으로 진행되는 작업들은 다음과 같다.

    ① 파라미터로 넘겨 준 타입의 오브젝트를 자동으로 생성한다.
         위의 코드에서는 MemberInfo 클래스의 객체 info를 자동으로 생성한다.
         이때 @ModelAttribute가 지정되는 클래스는 빈 클래스라야 한다.
         즉 MemberInfo 클래스는 beans 클래스라야 한다.
         그리고 getter와 setter가 명명 규칙에 맞게 만들어져 있어야 한다.

   	② 생성된 오브젝트에(info) HTTP로 넘어 온 값들을 자동으로 바인딩한다.
       위의 코드의 경우는 name=Gildong&age=25seq=327 이렇게 들어오는
       name, age, seq의 값이 MemberInfo의 해당 변수의 setter를 통해서
       해당 멤버 변수에로 binding된다.

   	③ @ModelAttribute 어노테이션이 붙은 객체가(여기서는 MemberInfo 객체) 자동으로 Model 객체에 추가되고
       따라서 MemberInfo 객체가 .jsp 뷰단까지 전달이 된다.
  	* */

	
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String greetingForm(Model model) {
    	SearchQuery query = new  SearchQuery("NYC","SFO","22-JAN-18");
    	UIData uiData = new UIData();
    	uiData.setSearchQuery(query);
        model.addAttribute("uidata",uiData );
        return "search";
    }   

   @RequestMapping(value="/search", method=RequestMethod.POST)
   public String greetingSubmit(@ModelAttribute UIData uiData, Model model) {
//		Flight[] flights = searchClient.postForObject("http://localhost:8083/search/get", uiData.getSearchQuery(), Flight[].class);	//chapter 6
//	   Flight[] flights = searchClient.postForObject("http://localhost:8090/search/get", uiData.getSearchQuery(), Flight[].class);	// chapter 7 part 1
//		uiData.setFlights(Arrays.asList(flights));

		// chapter 7 part 2
		Flight[] flights = restClient.postForObject("http://searchservice/search/get", uiData.getSearchQuery(), Flight[].class);
		uiData.setFlights(Arrays.asList(flights));


		model.addAttribute("uidata", uiData);
       	return "result";
   }
   
   @RequestMapping(value="/book/{flightNumber}/{origin}/{destination}/{flightDate}/{fare}", method=RequestMethod.GET)
   public String bookQuery(@PathVariable String flightNumber, 
		   @PathVariable String origin, 
		   @PathVariable String destination, 
		   @PathVariable String flightDate, 
		   @PathVariable String fare, 
		   Model model) {
   		UIData uiData = new UIData();
   		Flight flight = new Flight(flightNumber, origin,destination,flightDate,new Fares(fare,"AED"));
   		uiData.setSelectedFlight(flight);
   		uiData.setPassenger(new Passenger());
	   model.addAttribute("uidata",uiData);
       return "book"; 
   }
   @RequestMapping(value="/confirm", method=RequestMethod.POST)
   public String ConfirmBooking(@ModelAttribute UIData uiData, Model model) {
	   	Flight flight= uiData.getSelectedFlight();
		BookingRecord booking = new BookingRecord(flight.getFlightNumber(),flight.getOrigin(),
				  flight.getDestination(), flight.getFlightDate(),null,
				  flight.getFares().getFare());
		Set<Passenger> passengers = new HashSet<Passenger>();
		Passenger pax = uiData.getPassenger();
		pax.setBookingRecord(booking);
		passengers.add(uiData.getPassenger());
	 		booking.setPassengers(passengers);
		long bookingId =0;
		try { 

//			 bookingId = bookingClient.postForObject("http://localhost:8080/booking/create", booking, long.class);	// chapter 6
//			bookingId = bookingClient.postForObject("http://localhost:8060/booking/create", booking, long.class);	//chapter 7 part 1
			bookingId = restClient.postForObject("http://bookservice/booking/create", booking, long.class); 	// chapter 7 part 2

			logger.info("Booking created "+ bookingId);
		}catch (Exception e){
			logger.error("BOOKING SERVICE NOT AVAILABLE...!!!");
		}
		model.addAttribute("message", "Your Booking is confirmed. Reference Number is "+ bookingId);
		return "confirm";
   }
   @RequestMapping(value="/search-booking", method=RequestMethod.GET)
   public String searchBookingForm(Model model) {
   		UIData uiData = new UIData();
   		uiData.setBookingid("5");
   		model.addAttribute("uidata",uiData );
   		return "bookingsearch";
   }   

	@RequestMapping(value="/search-booking-get", method=RequestMethod.POST)
	public String searchBookingSubmit(@ModelAttribute UIData uiData, Model model) {
		Long id = new Long(uiData.getBookingid());
// 		BookingRecord booking = bookingClient.getForObject("http://localhost:8080/booking/get/"+id, BookingRecord.class);	//chapter 6
		BookingRecord booking = bookingClient.getForObject("http://127.0.0.1:8060/booking/get/"+id, BookingRecord.class);	//chapter 7 part 1
		Flight flight = new Flight(booking.getFlightNumber(), booking.getOrigin(),booking.getDestination()
				,booking.getFlightDate(),new Fares(booking.getFare(),"AED"));
		Passenger pax = booking.getPassengers().iterator().next();
		Passenger paxUI = new Passenger(pax.getFirstName(),pax.getLastName(),pax.getGender(),null);
		uiData.setPassenger(paxUI);
		uiData.setSelectedFlight(flight);
		uiData.setBookingid(id.toString()); 
		model.addAttribute("uidata", uiData);
	   return "bookingsearch";
	}
	
	@RequestMapping(value="/checkin/{flightNumber}/{origin}/{destination}/{flightDate}/{fare}/{firstName}/{lastName}/{gender}/{bookingid}", method=RequestMethod.GET)
	public String bookQuery(@PathVariable String flightNumber, 
			   @PathVariable String origin, 
			   @PathVariable String destination, 
			   @PathVariable String flightDate, 
			   @PathVariable String fare, 
			   @PathVariable String firstName, 
			   @PathVariable String lastName, 
			   @PathVariable String gender, 
			   @PathVariable String bookingid, 
			   Model model) {
		
 
			CheckInRecord checkIn = new CheckInRecord(firstName, lastName, "28C", null,
					flightNumber, flightDate, new Long(bookingid).longValue());

//			long checkinId = checkInClient.postForObject("http://localhost:8081/checkin/create", checkIn, long.class); // chapter 6
//		long checkinId = checkInClient.postForObject("http://localhost:8070/checkin/create", checkIn, long.class); // chapter 7 part 1
			long checkinId = restClient.postForObject("http://checkinservice/checkin/create", checkIn, long.class);


	   		model.addAttribute("message","Checked In, Seat Number is 28c , checkin id is "+ checkinId);
	       return "checkinconfirm"; 
	}	
}