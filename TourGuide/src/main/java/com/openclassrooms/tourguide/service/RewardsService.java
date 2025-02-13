package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

	public void calculateRewards(User user) {
		
		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();
		
	    List<UserReward> newRewards = new ArrayList<>();

		for(VisitedLocation visitedLocation : userLocations) {
			
			for(Attraction attraction : attractions) {
				
				boolean alreadyExist = false;
				
				for(UserReward existingReward : user.getUserRewards()) {
            		
            		if(existingReward.attraction.attractionName.equals(attraction.attractionName)) {
            			
            			alreadyExist = true;
            			break;
            			
            		}
            		
            	}
				
				if(alreadyExist == false) {
					
					if(nearAttraction(visitedLocation, attraction)) {
						
						UserReward newReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));
						
						newRewards.add(newReward);
						
					}
					
				}
				
				if(!newRewards.isEmpty()) {
					
					addAllNewRewards(newRewards, user);
					
				}
				
				newRewards.clear();
				
			}
		
		}
		
	}
	
	public void addAllNewRewards(List<UserReward> newRewards, User user) {
		
		for(UserReward newReward : newRewards) {
			
			user.addUserReward(newReward);
			
		}
		
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

	private int getRewardPoints(Attraction attraction, User user) {
		
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
		
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
