package com.openclassrooms.tourguide.model;

import gpsUtil.location.Location;

public class NearbyAttraction {

	String attractionName;
	
	Location attractionLocation;
	
	Location userLocation;
	
	double distanceBetweenUserAndAttraction;
	
	double rewardPoints;

	public String getAttractionName() {
		return attractionName;
	}

	public void setAttractionName(String attractionName) {
		this.attractionName = attractionName;
	}

	public Location getAttractionLocation() {
		return attractionLocation;
	}

	public void setAttractionLocation(Location attractionLocation) {
		this.attractionLocation = attractionLocation;
	}

	public Location getUserLocation() {
		return userLocation;
	}

	public void setUserLocation(Location userLocation) {
		this.userLocation = userLocation;
	}

	public double getDistanceBetweenUserAndAttraction() {
		return distanceBetweenUserAndAttraction;
	}

	public void setDistanceBetweenUserAndAttraction(double distanceBetweenUserAndAttraction) {
		this.distanceBetweenUserAndAttraction = distanceBetweenUserAndAttraction;
	}

	public double getRewardPoints() {
		return rewardPoints;
	}

	public void setRewardPoints(double rewardPoints) {
		this.rewardPoints = rewardPoints;
	}
	
}
