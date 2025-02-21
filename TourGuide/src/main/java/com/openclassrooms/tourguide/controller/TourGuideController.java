package com.openclassrooms.tourguide.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openclassrooms.tourguide.model.NearbyAttraction;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;
import com.openclassrooms.tourguide.service.TourGuideService;

import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @RequestMapping("/")
    public String index() {

        return "Greetings from TourGuide!";

    }

    @RequestMapping("/getLocation")
    public VisitedLocation getLocation(@RequestParam String userName) {

    	return tourGuideService.getUserLocation(getUser(userName)).join();

    }

    @RequestMapping("/getNearbyAttractions")
    public List<NearbyAttraction> getNearbyAttractions(@RequestParam String userName) {

    	return tourGuideService.getUserLocation(getUser(userName)).thenApply(visitedLocation ->

    		tourGuideService.getNearByAttractions(visitedLocation)

    	).join();


    }

    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {

    	return tourGuideService.getUserRewards(getUser(userName));

    }

    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {

    	return tourGuideService.getTripDeals(getUser(userName));

    }

    private User getUser(String userName) {

    	return tourGuideService.getUser(userName);

    }


}