//package com.bahpps.cahue.util.json;
//
//import android.location.Location;
//
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonSerializationContext;
//import com.google.gson.JsonSerializer;
//
//import java.lang.reflect.Type;
//
///**
// * Created by Francesco on 05/02/2015.
// */
//public class LocationSerializer implements JsonSerializer<Location> {
//
//    public JsonElement serialize(Location location, Type type, JsonSerializationContext jsc) {
//        JsonObject jo = new JsonObject();
//        jo.addProperty("latitude", location.getLatitude());
//        jo.addProperty("latitude", location.getLatitude());
//        jo.addProperty("accuracy", location.getAccuracy());
//        return jo;
//    }
//
//}