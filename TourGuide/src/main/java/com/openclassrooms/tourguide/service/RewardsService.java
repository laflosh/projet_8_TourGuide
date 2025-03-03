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

	public void calculateRewards(User user) {

		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();

		for(VisitedLocation visitedLocation : userLocations) {

			for(Attraction attraction : attractions) {

				boolean alreadyExist = false;

				for(UserReward existingReward : user.getUserRewards()) {

            		if(existingReward.attraction.attractionName.equals(attraction.attractionName)) {

            			alreadyExist = true;
            			break;

            		}

            	}

				if(!alreadyExist) {

					if(nearAttraction(visitedLocation, attraction)) {

						UserReward newReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));

						user.addUserReward(newReward);

					}

				}

			}

		}

	}

	public Void calculateRewards(List<User> users) {

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for(User user : users) {

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

				calculateRewards(user);

			}, executor);

			futures.add(future);

		}

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

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

	public Integer getRewardPoints(Attraction attraction, User user) {

		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());

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
