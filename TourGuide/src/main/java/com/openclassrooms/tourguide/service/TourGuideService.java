package com.openclassrooms.tourguide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.model.NearbyAttraction;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;
import com.openclassrooms.tourguide.tracker.Tracker;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final RewardCentral rewardCentral = new RewardCentral();
	private final TripPricer tripPricer = new TripPricer();
	private final ExecutorService executor = Executors.newFixedThreadPool(65);
	
	public final Tracker tracker;
	
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
			
		}
		
		tracker = new Tracker(this);
		addShutDownHook();
		
	}

	public List<UserReward> getUserRewards(User user) {
		
		return user.getUserRewards();
		
	}

	public CompletableFuture<VisitedLocation> getUserLocation(User user) {
		
		if(user.getVisitedLocations().size() > 0){
			
			return CompletableFuture.completedFuture(user.getLastVisitedLocation());
			
		} else {
			
			return trackUserLocation(user);
			
		}
		
	}

	public User getUser(String userName) {
		
		return internalUserMap.get(userName);
		
	}

	public List<User> getAllUsers() {
		
		return internalUserMap.values().stream().collect(Collectors.toList());
		
	}

	public void addUser(User user) {
		
		if (!internalUserMap.containsKey(user.getUserName())) {
			
			internalUserMap.put(user.getUserName(), user);
			
		}
		
	}

	public List<Provider> getTripDeals(User user) {
		
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		
		return providers;
		
	}

	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		
		return CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()))
				.thenApply(visitedLocation -> {
					
					user.addToVisitedLocations(visitedLocation);
					CompletableFuture.runAsync(() -> {
						rewardsService.calculateRewards(user);
					});
					
					return visitedLocation;
					
				})
	            .exceptionally(ex -> {
	            	
	                System.err.println("Erreur lors de la récupération de la position GPS : " + ex.getMessage());
	                return null;
	                
	            });
		
	}

	public List<NearbyAttraction> getNearByAttractions(VisitedLocation visitedLocation) {
		
		List<NearbyAttraction> nearbyAttractions = new ArrayList<>();
		List<Attraction> attractions = gpsUtil.getAttractions();
			
		for (Attraction attraction : attractions) {
			
			double distanceAttraction = rewardsService.getDistance(attraction, visitedLocation.location);
			
			NearbyAttraction nearbyAttraction = new NearbyAttraction();
			Location location = new Location(attraction.latitude, attraction.longitude);
			
			nearbyAttraction.setAttractionName(attraction.attractionName);
			nearbyAttraction.setAttractionLocation(location);
			nearbyAttraction.setUserLocation(visitedLocation.location);
			nearbyAttraction.setDistanceBetweenUserAndAttraction(distanceAttraction);
			nearbyAttraction.setRewardPoints(rewardCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId));
			
			if (nearbyAttraction != null) {
				
				nearbyAttractions.add(nearbyAttraction);
				
			}
			
		}
		
		List<NearbyAttraction> sortedNearbyAttraction = nearbyAttractions.stream()
				.sorted(Comparator.comparingDouble(attraction -> attraction.getDistanceBetweenUserAndAttraction()))
				.limit(5)
				.toList();
		
		return sortedNearbyAttraction;
		

		
		//attractions.stream().map((a)-> new Tuple(a, rewardsService.getDistance(attraction, visitedLocation.location))).sort(Comparator...).limit(
		
	}

	private void addShutDownHook() {
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			@Override
			public void run() {
				
				tracker.stopTracking();
				rewardsService.shutDownExecutor();
				
			}
			
		});
		
	}

	/**********************************************************************************
	 *
	 * Methods Below: For Internal Testing
	 *
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
			
		});
		
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
		
	}

	private void generateUserLocationHistory(User user) {
		
		IntStream.range(0, 3).forEach(i -> {
			
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
			
		});
		
	}

	private double generateRandomLongitude() {
		
		double leftLimit = -180;
		double rightLimit = 180;
		
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
		
	}

	private double generateRandomLatitude() 
	{
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
		
	}

	private Date getRandomTime() {
		
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
		
	}

}
