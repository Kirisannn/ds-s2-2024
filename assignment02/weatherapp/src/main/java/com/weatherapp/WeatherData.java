package com.weatherapp;

/**
 * Represents weather data for a specific weather station.
 * This class holds various attributes related to the weather conditions,
 * including temperature, humidity, wind speed, and location details.
 */
public class WeatherData {
    private String id; // Unique identifier for the weather station
    private String name; // Name of the weather station
    private String state; // State where the weather station is located
    private String timeZone; // Time zone of the weather station
    private double lat; // Latitude of the weather station
    private double lon; // Longitude of the weather station
    private String localDateTime; // Local date and time at the weather station
    private String localDateTimeFull; // Full local date and time (including seconds, if applicable)
    private double airTemp; // Air temperature in degrees Celsius
    private double apparentTemp; // Apparent temperature (feels-like) in degrees Celsius
    private String cloud; // Cloud cover description
    private double dewpt; // Dew point temperature in degrees Celsius
    private double press; // Atmospheric pressure in hPa
    private int relHum; // Relative humidity percentage
    private String windDir; // Wind direction (e.g., "N", "E", "S", "W")
    private double windSpdKmh; // Wind speed in kilometers per hour
    private double windSpdKt; // Wind speed in knots

    // Getters and setters for each field

    /**
     * Gets the unique identifier of the weather station.
     * 
     * @return the unique identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the weather station.
     * 
     * @param id the unique identifier to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the name of the weather station.
     * 
     * @return the name of the weather station
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the weather station.
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the state of the weather station.
     * 
     * @return the state of the weather station
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state of the weather station.
     * 
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the time zone of the weather station.
     * 
     * @return the time zone of the weather station
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone of the weather station.
     * 
     * @param timeZone the time zone to set
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Gets the latitude of the weather station.
     * 
     * @return the latitude
     */
    public double getLat() {
        return lat;
    }

    /**
     * Sets the latitude of the weather station.
     * 
     * @param lat the latitude to set
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /**
     * Gets the longitude of the weather station.
     * 
     * @return the longitude
     */
    public double getLon() {
        return lon;
    }

    /**
     * Sets the longitude of the weather station.
     * 
     * @param lon the longitude to set
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /**
     * Gets the local date and time at the weather station.
     * 
     * @return the local date and time
     */
    public String getLocalDateTime() {
        return localDateTime;
    }

    /**
     * Sets the local date and time at the weather station.
     * 
     * @param localDateTime the local date and time to set
     */
    public void setLocalDateTime(String localDateTime) {
        this.localDateTime = localDateTime;
    }

    /**
     * Gets the full local date and time at the weather station.
     * 
     * @return the full local date and time
     */
    public String getLocalDateTimeFull() {
        return localDateTimeFull;
    }

    /**
     * Sets the full local date and time at the weather station.
     * 
     * @param localDateTimeFull the full local date and time to set
     */
    public void setLocalDateTimeFull(String localDateTimeFull) {
        this.localDateTimeFull = localDateTimeFull;
    }

    /**
     * Gets the air temperature at the weather station.
     * 
     * @return the air temperature in degrees Celsius
     */
    public double getAirTemp() {
        return airTemp;
    }

    /**
     * Sets the air temperature at the weather station.
     * 
     * @param airTemp the air temperature to set in degrees Celsius
     */
    public void setAirTemp(double airTemp) {
        this.airTemp = airTemp;
    }

    /**
     * Gets the apparent temperature at the weather station.
     * 
     * @return the apparent temperature in degrees Celsius
     */
    public double getApparentTemp() {
        return apparentTemp;
    }

    /**
     * Sets the apparent temperature at the weather station.
     * 
     * @param apparentTemp the apparent temperature to set in degrees Celsius
     */
    public void setApparentTemp(double apparentTemp) {
        this.apparentTemp = apparentTemp;
    }

    /**
     * Gets the cloud cover description at the weather station.
     * 
     * @return the cloud cover description
     */
    public String getCloud() {
        return cloud;
    }

    /**
     * Sets the cloud cover description at the weather station.
     * 
     * @param cloud the cloud cover description to set
     */
    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    /**
     * Gets the dew point temperature at the weather station.
     * 
     * @return the dew point temperature in degrees Celsius
     */
    public double getDewpt() {
        return dewpt;
    }

    /**
     * Sets the dew point temperature at the weather station.
     * 
     * @param dewpt the dew point temperature to set in degrees Celsius
     */
    public void setDewpt(double dewpt) {
        this.dewpt = dewpt;
    }

    /**
     * Gets the atmospheric pressure at the weather station.
     * 
     * @return the atmospheric pressure in hPa
     */
    public double getPress() {
        return press;
    }

    /**
     * Sets the atmospheric pressure at the weather station.
     * 
     * @param press the atmospheric pressure to set in hPa
     */
    public void setPress(double press) {
        this.press = press;
    }

    /**
     * Gets the relative humidity at the weather station.
     * 
     * @return the relative humidity percentage
     */
    public int getRelHum() {
        return relHum;
    }

    /**
     * Sets the relative humidity at the weather station.
     * 
     * @param relHum the relative humidity percentage to set
     */
    public void setRelHum(int relHum) {
        this.relHum = relHum;
    }

    /**
     * Gets the wind direction at the weather station.
     * 
     * @return the wind direction
     */
    public String getWindDir() {
        return windDir;
    }

    /**
     * Sets the wind direction at the weather station.
     * 
     * @param windDir the wind direction to set
     */
    public void setWindDir(String windDir) {
        this.windDir = windDir;
    }

    /**
     * Gets the wind speed at the weather station in kilometers per hour.
     * 
     * @return the wind speed in km/h
     */
    public double getWindSpdKmh() {
        return windSpdKmh;
    }

    /**
     * Sets the wind speed at the weather station in kilometers per hour.
     * 
     * @param windSpdKmh the wind speed to set in km/h
     */
    public void setWindSpdKmh(double windSpdKmh) {
        this.windSpdKmh = windSpdKmh;
    }

    /**
     * Gets the wind speed at the weather station in knots.
     * 
     * @return the wind speed in knots
     */
    public double getWindSpdKt() {
        return windSpdKt;
    }

    /**
     * Sets the wind speed at the weather station in knots.
     * 
     * @param windSpdKt the wind speed to set in knots
     */
    public void setWindSpdKt(double windSpdKt) {
        this.windSpdKt = windSpdKt;
    }
}
