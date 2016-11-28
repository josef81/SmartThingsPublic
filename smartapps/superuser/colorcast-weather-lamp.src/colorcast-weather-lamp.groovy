/**
 *  ColorCast Weather Lamp
 *
 *	Inspired by and based in large part on the original Color Changing Smart Weather lamp by Jim Kohlenberger.
 *	See Jim's original SmartApp at http://community.smartthings.com/t/color-changing-smart-weather-lamp-app/12046 which includes an option for high pollen notifications
 *	
 *	This weather lantern app turns on with motion and turns a Phillips hue (or LifX) lamp different colors based on the weather.  
 *	It uses dark sky's weather API to micro-target weather. 
 *
 *	Colors definitions
 *
 *	Purple 		Rain: Rain is forecast for specified time period.
 *	Blue 		Cold: It's going to be at or below the specified minimum temperature
 *	Pink		Snow: Snow is forecast for specified time period.
 *	Red 		Hot:  It's going to be at or above the specified maximum temperature
 *	Yellow 		Wind: Wind is forecast to meet or exceed the specified maximum wind speed
 *	Green		All clear
 *	Blinking any color indicates that there is a weather advisory for your location
 *
 *  With special thanks to insights from the SmartThings Hue mood lighting script and the light on motion script by kennyyork@centralite.com
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
import java.util.regex.*

definition(
	name: "ColorCast Weather Lamp",
	namespace: "",
	author: "Joe DiBenedetto",
	description: "Get a simple visual indicator for the days weather whenever you leave home. ColorCast will change the color of one or more Hue or LifX lights to match the weather forecast whenever it senses motion",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Developers/smart-light-timer@2x.png"
)

preferences {
	page(name: "pageAPI", title: "API Key", nextPage: "pageSettings", install: false, uninstall: true) {
	
		section("First Things First") {
			paragraph "To use this SmartApp you need an API Key from forecast.io (https://developer.forecast.io/). To obtain your key, you will need to register a free account on their site."
			paragraph "You will be asked for payment information, but you can ignore that part. Payment is only required if you access the data more than 1,000 times per day. If you don't give a credit card number and you somehow manage to exceed the 1,000 calls, the app will stop working until the following day when the counter resets."
		}
	
		section("API Key") {
			href(name: "hrefNotRequired",
			title: "Get your Forecast.io API key",
			required: false,
			style: "external",
			url: "https://developer.forecast.io/",
			description: "tap to view Forecast.io website in mobile browser")
	
			input "apiKey", "text", title: "Enter your new key", required:true
		}
	}
	
	page(name: "pageSettings", title: "", install: true, uninstall: true) {
		section("Select Motion Detector") {
			input "motion_detector", "capability.motionSensor", title: "Where?", required:false //Select motion sensor(s). Optional because app can be triggered manually
		}
		section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true //Select bulbs
			input "brightnessLevel", "number", title: "Brightness Level (1-100)?", required:false, defaultValue:100 //Select brightness
		}

		section ("Forecast Range") {
			// Get the number of hours to look ahead. Weather for the next x hours will be parsed to compare against user specified values.
			input "lookAheadHours", "enum", title: "Get weather for the next...", options: [
				"Current conditions", 
				"1 Hour", 
				"2 Hours", 
				"3 Hours", 
				"4 Hours", 
				"5 Hours", 
				"6 Hours", 
				"7 Hours", 
				"8 Hours", 
				"9 Hours", 
				"10 Hours", 
				"11 Hours", 
				"12 Hours", 
				"13 Hours", 
				"14 Hours", 
				"15 Hours", 
				"16 Hours", 
				"17 Hours", 
				"18 Hours", 
				"19 Hours", 
				"20 Hours", 
				"21 Hours", 
				"22 Hours", 
				"23 Hours", 
				"24 Hours"
			], required: true, defaultValue:"Current conditions"
		}

		section ("Weather Triggers") {
			input "tempMinTrigger", "number", title: "Low Temperature", required: false, defaultValue:35 //Set the minumum temperature to trigger the "Cold" color
			input "tempMaxTrigger", "number", title: "High Temperature", required: false, defaultValue:80 //Set the maximum temperature to trigger the "Hot" color
			input "windTrigger", "number", title: "High Wind Speed", required: false, defaultValue:24 //Set the maximum temperature to trigger the "Windy" color
		}

		section([mobileOnly:true]) {
			label title: "Assign a name", required: false //Allow custom name for app. Usefull if the app is installed multiple times for different modes
			mode title: "Set for specific mode(s)", required: false //Allow app to be assigned to different modes. Usefull if user wants different setting for different modes
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}


def initialize() {
	log.info "Initializing, subscribing to motion event at ${motionHandler} on ${motion_detector}"
	subscribe(motion_detector, "motion", motionHandler)
	subscribe(app, appTouchHandler)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	
	unsubscribe()
	unschedule()
	initialize()
}


def checkForWeather() {
	def defaultColor ="Green" //Set the "all clear" color

	def colors = [] //Initialze colors array
	
	//Initialize weather events
	def willRain=false;
	def willSnow=false;
	def windy=false;
	def tempLow
	def tempHigh
	def weatherAlert=false
	
	def forecastUrl="https://api.forecast.io/forecast/$apiKey/$location.latitude,$location.longitude" //Create api url

	httpGet(forecastUrl) {response -> 

		if (response.data) { //API response was successfull
			def i=0
			if (lookAheadHours=="Current conditions") { //Get current weather conditions
				def currentConditions=response.data.currently
				if (currentConditions.precipProbability.floatValue()>=0.15) { //Consider it raining/snowing if precip probabilty is greater than 15%
					if (currentConditions.precipType=='rain') {
						willRain=true //Precipitation type is rain
					} else {
						willSnow=true //Precipitation type is NOT rain. This would include snow, sleet, hail, etc.
					}
				}
				tempLow=tempHigh=currentConditions.temperature //High and low temps are the same for current conditions
				if (currentConditions.windSpeed>=windTrigger) windy=true //Compare to user defined value for wid speed.
			} else { //Get hourly data
				for (hour in response.data.hourly.data){ //Iterate over hourly data
					if (lookAheadHours.replaceAll(/\D/,"").toInteger()<++i) { //Break if we've processed all of the specified look ahead hours. Need to strip non-numeric characters(i.e. "hours") from string so we can cast to an integer
						break
					} else {
						if (hour.precipProbability.floatValue()>=0.15) { //Consider it raining/snowing if precip probabilty is greater than 15%
							if (hour.precipType=='rain') {
								willRain=true //Precipitation type is rain
							} else {
								willSnow=true //Precipitation type is NOT rain. This would include snow, sleet, hail, etc.
							}
						}
						if (tempLow==null || tempLow>hour.temperature) tempLow=hour.temperature //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
						if (tempHigh==null || tempHigh<hour.temperature) tempHigh=hour.temperature //Compare the stored high temp to the current iteration temp. If it's higher overwrite the stored high with this temp
						if (hour.windSpeed>=windTrigger) windy=true //Compare to user defined value for wid speed.
					}
				}
			}

			if (response.data.alerts) weatherAlert=true //Is there currently a weather alert

			//Add color strings to the colors array to be processed later
			if (tempLow<=tempMinTrigger.floatValue()) {
				colors.push('Blue')
				log.debug "Cold"
			}
			if (tempHigh>=tempMaxTrigger.floatValue()) {
				colors.push('Red')
				log.debug "Hot"
			}
			if (willSnow) {
				colors.push('Pink')
				log.debug "Snow"			
			}
			if (willRain) {
				colors.push('Purple') 
				log.debug "Rain"
			}
			if (windy) {
				colors.push('Yellow')
				log.debug "Windy"
			}
		} else { //API response was NOT successfull
			log.debug "HttpGet Response data unsuccesful."
		}
	}

	//If the colors array is empty, assign the "all clear" color
	if (colors.size()==0) colors.push(defaultColor)
	
	def delay=2000 //The amount of time to leave each color on
	def iterations=1 //The number of times to show each color
	if (weatherAlert) {
		//When there's an active weather alert, shorten the duration that each color is shown but show the color multiple times. This will cause individual colors to flash when there is a weather alert
		delay = 550 
		iterations=3
	}
	
	colors.each { //Iterate over each color
		for (int i = 0; i<iterations; i++) {
			sendcolor(it) //Turn light on with specified color
			pause(delay) //leave the light on for the specified time
			if (weatherAlert) {
				//If there's a weather alert, turn off the light for the same amount of time it was on
				//When a weather alert is active, each color will be looped x times, creating the blinking effect by turning the light on then off x times
				hues.off()
				pause(delay)
			}
		}
	}
	
	setLightsToOriginal() //The colors have been sent to the lamp and all colors have been shown. Now revert the lights to their original settings
}

def sendcolor(color) {
	//Initialize the hue and saturation
	def hueColor = 0
	def saturation = 100
	
	//Use the user specified brightness level. If they exceeded the min or max values, overwrite the brightness with the actual min/max
	if (brightnessLevel<1) {
		brightnessLevel=1
	} else if (brightnessLevel>100) {
		brightnessLevel=100
	}
	
	//Set the hue and saturation for the specified color.
	switch(color) {
		case "White":
			hueColor = 0
			saturation = 0
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 
			break;
		case "Blue":
			hueColor = 65
			break;
		case "Green":
			hueColor = 33
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 82
			saturation = 100
			break;
		case "Pink":
			hueColor = 90.78
			saturation = 67.84
			break;
		case "Red":
			hueColor = 0
			break;
	}

	//Change the color of the light
	def newValue = [hue: hueColor, saturation: saturation, level: brightnessLevel]  
	hues*.setColor(newValue)
}


def setLightsToOriginal() {
	//This is intended to revert the lights to their original on/off state and original color. Unfortunatly, SmartThings doesn't recognize the original colors when they are set outside of SmartThings by using the Hue, or other, app. At this time it just makes more sense to turn the light off
	hues.off()
}

/// HANDLE MOTION
def motionHandler(evt) {
	if (evt.value == "active") {// If there is movement then trigger the weather display
		log.debug "Motion detected, turning on light"
		checkForWeather()
	} 
}

def appTouchHandler(evt) {// If the button is pressed then trigger the weather display
	checkForWeather()	
	log.debug "App triggered with button press."
}