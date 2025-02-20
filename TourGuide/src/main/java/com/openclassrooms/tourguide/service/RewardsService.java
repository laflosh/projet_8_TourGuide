package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executor = Executors.newFixedThreadPool(65);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
		
	}

	public void setProximityBuffer(int proximityBuffer) {
		
		this.proximityBuffer = proximityBuffer;
		
	}

	public void setDefaultProximityBuffer() {
		
		proximityBuffer = defaultProximityBuffer;
		
	}

	public CompletableFuture<Void> calculateRewards(User user) {
		
		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for(VisitedLocation visitedLocation : userLocations) {
			
			for(Attraction attraction : attractions) {
				
				CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
					
					boolean alreadyExist = false;
					
					for(UserReward existingReward : user.getUserRewards()) {
	            		
	            		if(existingReward.attraction.attractionName.equals(attraction.attractionName)) {
	            			
	            			alreadyExist = true;
	            			break;
	            			
	            		}
	            		
	            	}
					
					return alreadyExist;
					
				}, executor).thenCompose(alreadyExist -> {
					
					if(alreadyExist == false) {
						
						if(nearAttraction(visitedLocation, attraction)) {
							
							 	return getRewardPoints(attraction, user).thenAccept(rewardPoints -> {
								
								UserReward newReward = new UserReward(visitedLocation, attraction, rewardPoints);
								
								user.addUserReward(newReward);
								
							});
							
						}
						
					}
					
					return CompletableFuture.completedFuture(null);
					
				});
					
				
				futures.add(future);
				
			}
		
		}
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		
		double distance = getDistance(attraction, location);
		
		if(distance > attractionProximityRange) {
			return false;
		} else {
			return true;
		}
		
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		
		double distance = getDistance(attraction, visitedLocation.location);
		
		if(distance > proximityBuffer) {
			return false;
		} else {
			return true;
		}
		
	}

	public CompletableFuture<Integer> getRewardPoints(Attraction attraction, User user) {
		
		return CompletableFuture.supplyAsync(() -> rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId())
				,executor);
		
	}
	
	public void shutDownExecutor() {
		
		executor.shutdown();
		
	}

	public double getDistance(Location loc1, Location loc2) {
		
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        
        return statuteMiles;
        
	}

}
