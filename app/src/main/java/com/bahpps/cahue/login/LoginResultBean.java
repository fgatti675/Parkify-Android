package com.bahpps.cahue.login;

import com.bahpps.cahue.cars.Car;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginResultBean {

    String email;

    String googleId;

    Set<Car> cars = new HashSet<>();

    String authToken;

}
