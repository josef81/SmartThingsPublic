/**
 *  WeatherGlobe
 *
 *  Copyright 2015 Josef Kurlinkus
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 /*
 Color the weather
 Daniel Root
 www.danielroot.info
 
 Should work (but not yet tested with) Phillips Hue devices and any SmartThings device that supports "capability.colorControl".
 See https://codebender.cc/sketch:28062 for Arduino code that works with the Arduino SmartShield.
 */
 
// Automatically generated. Make future change here.
definition(
    name: "WeatherGlobe",
    namespace: "Weatherglobe",
    author: "Josef Kurlinkus",
    description: "Weather to Hue",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	 section("Check the weather in"){
		input "zipcode", "text", title: "Zipcode?"
	 }
     section("At this time"){
     	input "startTime", "time", title: "Start time"
     }
     section("Or when this switch is on")
     {
     	input "testSwitch", "capability.switch", title:"Switch"
	 }
     section("Set these color devices"){
     	input "colorDevices", "capability.colorControl", title: "Color devices", multiple:true
     }
	 section("To these colors"){
		 input "rainColor", "text", title:"Rain", defaultValue:'#08088A'
         input "snowColor", "text", title:"Snow", defaultValue:'#00CCFF'
         input "clearColor", "text", title:"Clear", defaultValue:'#cccccc'
         input "sunnyColor","text", title:"Sunny", defaultValue:'#FFFF00'
         input "hotColor","text", title:"Hot", defaultValue:'#FF3300'
         input "cloudyColor","text", title:"Cloudy", defaultValue:'#4C4C4C'
	 }
    
     section("Then turn them off at"){
     	input "endTime", "time", title: "End Time"
     }
 }
 

def installed() {
	log.debug "Installed: $settings"
    subscribe(testSwitch,"switch.on","scheduleCheck")
        
	schedule(startTime, "scheduleCheck")
    schedule(endTime, "endScheduleCheck")
}

def updated() {
	log.debug "Updated: $settings"
	unschedule()
    unsubscribe()
    subscribe(testSwitch,"switch.on","scheduleCheck")
	schedule(startTime, "scheduleCheck")
    schedule(endTime, "endScheduleCheck")   
}

def scheduleCheck(evt) {
	log.debug "checking the weather"
	def response = getWeatherFeature("forecast", zipcode)
    log.debug "got some weather, reading it..."
    def forecastColor = parseForecast(response)
    log.debug "setting color to $forecastColor"
   
    colorDevices.each { 
        it?.on()
    	it?.setColor(forecastColor) 
    }
}

def endScheduleCheck(){
	colorDevices.each {
		it?.off()
	}
}

private textContainsAnyOf(text, keywords)
{
	def result = '';
	for (int i = 0; i < keywords.size(); i++) {
		result = text.contains(keywords[i])
        if (result == true) return result
	}
    return result;
}

private parseForecast(json)
{
	def snowKeywords = ['snow','flurries','sleet']
    def rainKeywords = ['rain', 'showers', 'sprinkles', 'precipitation']
    def clearKeywords = ['clear']
    def sunnyKeywords = ['sunny']
    def hotKeywords = ['hot']
    def cloudyKeywords = ['overcast','cloudy']
    def result = '#000000';
    
    
	def forecast = json?.forecast?.txt_forecast?.forecastday?.first()
	if (forecast) {
		def text = forecast?.fcttext?.toLowerCase()
        def day = forecast?.title
        log.debug text
		if (text) {
            if(textContainsAnyOf(text,cloudyKeywords)) result = cloudyColor
			if(textContainsAnyOf(text,clearKeywords)) result = clearColor
			if(textContainsAnyOf(text,sunnyKeywords)) result = sunnyColor
            if(textContainsAnyOf(text,hotKeywords)) result = hotColor
            if(textContainsAnyOf(text,rainKeywords)) result = rainColor
            if(textContainsAnyOf(text,snowKeywords)) result = snowColor
        }
        sendNotificationEvent("Weather for $day : $text")
        sendNotificationEvent("Setting weather lights to $result")
    }
    else
    {
    	 sendNotificationEvent("Could not get weather!")
    }
    log.debug result
    return result
}

