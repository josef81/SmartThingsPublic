/**
 *  Color Changing Smart Weather lamp
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

definition(
    name: "WxGlobeTEST",
    namespace: "",
    author: "Josef Kurlinkus",
    description: "TestofNew WeatherBulb App",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png")

// PREFERENCE SETTINGS

preferences{

	// Preference Section 1: Setting the devices that will activate the WeatherGlobe

    section("Select the device(s) to activate the WxGlobe") {
    
    	input "motion_detector", "capability.motionSensor",
        	title: "Motion Trigger", required:false, multiple: true
        
        input "triggerswitch", "capability.switch",
        	title: "Switch(es)", required:false, multiple: true
    }
 
 
 	// Preference Section 2: Setting WeatherGlobe device & brightness level (multiple globes are allowed)
    section("Select the device for the WeatherGlobe") {
		
        input "wxbulbs", "capability.colorControl", 
        	title: "Which Hue Bulbs?", required:true, multiple:true
     
        input "brightnessLevel", "number", 
        	title: "Brightess level (1-100) [default is 60%]", required: false 
		}
    
	// Preference Section 3: Setting the Location
    section ("Set your Location - if empty, device coordinates will be used") {
		
        input "zipcode", "number", 
        	title: "Zip Code [optional]", required: false
	}
    
   // Preference Section 4: DarkSky API Setup
   section ("DarkSky API ID#") {
		input "darkskyapi", "text", 
        	title: "DarkSky API ID", required: false
	}
    
}


// DEFFINITIONS

	// Definition: installed with.
    
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
    scheduleJob()  
}

	// Definition: updated with.

def updated() {
	log.debug "Updated with settings: ${settings}"
    log.debug "Weather Light: oldKeys: $oldKeys"
 
    unsubscribe()
    unschedule()
    scheduleJob()
	initialize()
}

	// Definition: auto turn off of the app.

def scheduleJob() {
   runEvery5Minutes("turnOff")
}


/** GETTING THE WEATHER - [DarkSky.io]
 *
 * Obtaining and Parsing the weather with a forcast.io developer key and API
 *
**/

	//Definition: IGNORE ALL OF THIS CRAP Checking for the Weather

def checkForWeather() {
    def color ="Warm White"


//	PURPLE RAIN FORCAST
	log.debug "Checking Forecast.io Weather"
    httpGet("https://api.forecast.io/forecast/631cb9063a07f3ad6953487ed8ce0962/$location.latitude,$location.longitude") {response -> 
    log.debug "API IS: $darkskyapi"
    log.debug "WXSUMMARY IS: $location.latitude"
            
if (response.data) {
       		
    		def precipprob = response.data.currently.precipProbability.floatValue() // A numerical value between 0 and 1 (inclusive) representing the probability of precipitation occurring at the given time. 
			def tempFar = response.data.currently.temperature.floatValue()
			def thisHour = response.data.hourly.data[0].precipProbability.floatValue() //this top of hour  	
			def nextHour = response.data.hourly.data[1].precipProbability.floatValue() //next top of hour
            def summary = response.data.hourly.data.summary
            
            state.CurrentTemp = tempFar
            log.debug "state temp: ${state.CurrentTemp}"
            
            log.debug "WXSUMMARY2 IS: ${summary}"
			log.debug "Actual current temp: ${tempFar}, Precipitation probability now: ${precipprob}, thisHour: ${thisHour}, nextHour ${nextHour}"
    		if ((thisHour >0.15) || (nextHour >0.15)) {
	    		color = "Purple" 
    	    	log.debug "Greater than 15% chance of rain in this or the next hour, setting light to Purple."
    		}
       	}   else {
        	log.debug "HttpGet Response data unsuccesful."
        }
    //}
    
    //  PINK SNOW
   // def f = getWeatherFeature("forecast", zipcode) //get("forecast")
	//def f1= f?.forecast?.simpleforecast?.forecastday
	//if (f1) {
	//	value = f1[0].snow_day 
	//}
	//else {
	//	log.warn "Forecast not found"
	//}
  	
  //	log.debug "The chance of snow = ${value}"
   // if (!value.toString().contains("0.0")) {
    //	if (!value.toString().contains("null")) {
    //		color = "Pink"
    //		log.debug "Weather shows chance of snow, setting light to Pink."
      // }
    }


	//  SUBFREEZING BLUE	
    if (state.CurrentTemp < 20 ) {
        	color = "SFZBlue"
        log.debug "Weather temp below 20F, its freezing out so setting light to SFZBlue."
        log.debug "state temp under color section: ${state.CurrentTemp}"
        log.debug "tempFar under color section: ${tempFar}"
    }
    
    //  SUBFREEZING BLUE	
    if (state.CurrentTemp >= 20 ) {
        	color = "SFZBlue"
        log.debug "Weather temp above 20F, its freezing out so setting light to SFZBlue."
    }
   	
    //  FREEZING BLUE	
    if (state.CurrentTemp >= 32 ) {
        	color = "FZBlue"
        log.debug "Weather temp above 32F, its freezing out so setting light to FZBlue."
    }
     //  COOL BLUE	
    if (state.CurrentTemp >= 40 ) {
        	color = "CoolBlue"
        log.debug "Weather temp above 40F, its freezing out so setting light to CoolBlue."
    }
    
    //  WARM YELLOW
	if (state.CurrentTemp >= 50 )  {
        color = "WMYellow"
        log.debug "Weather temp above 50F, setting light to WMYellow."
    }
     
     //  WARMER YELLOW
	if (state.CurrentTemp >= 60 )  {
        color = "WMRYellow"
        log.debug "Weather temp above 60F, setting light to WMYellow."
    }
     
     //  WARM ORANGE
	if (state.CurrentTemp >= 70 )  {
        color = "WMOrange"
        log.debug "Weather temp above 70F, setting light to WMOrange."
    }
     
     //  WARMER ORANGE
	if (state.CurrentTemp >= 80 )  {
        color = "WMROrange"
        log.debug "Weather temp above 80F, setting light to WMROrange."
    }
   
   //  HOT RED	
    if (state.CurrentTemp >= 90 ) {
        	color = "HotRed"
        log.debug "Weather temp above 90F, its freezing out so setting light to HotRed."
    }
   
	sendcolor(color)
      
	def newKeys = alerts?.collect{it.type + it.date_epoch} ?: []
	log.debug "Severe Weather Alert: newKeys: $newKeys"

	def oldKeys = state.alertKeys ?: []
	log.debug "Severe Weather Alert: oldKeys: $oldKeys"

	if (newKeys != oldKeys) {

		state.alertKeys = newKeys

		alerts.each {alert ->
			if (!oldKeys.contains(alert.type + alert.date_epoch) && descriptionFilter(alert.description)) {
                color = "Red"
                sendcolor(color)
				flashLights()

			}
		}
	}
}

def descriptionFilter(String description) {
	def filterList = ["special", "statement", "test"]
	def passesFilter = true
	filterList.each() { word ->
		if(description.toLowerCase().contains(word)) { passesFilter = false }
	}
	passesFilter
}


def locationIsDefined() {
	zipcodeIsValid() || location.zipCode || ( location.latitude && location.longitude )
}

def zipcodeIsValid() {
	zipcode && zipcode.isNumber() && zipcode.size() == 5
}

private send(message) {
	sendPush message
	if (settings.phone1) {
		sendSms phone1, message
	}
	if (settings.phone2) {
		sendSms phone2, message
	}
	if (settings.phone3) {
		sendSms phone3, message
	}
}

def sendcolor(color) {
	log.debug "Sendcolor = $color"
    def hueColor = 0
    def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
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
			saturation = 80 //83
			break;
		case "SFZBlue":
			hueColor = 70.61437908496731
			break;
		case "FZBlue":
			hueColor = 70.61437908496731
			break;
		case "CoolBlue":
			hueColor = 66.40318627450979
			break;
		case "WMYellow":
			hueColor = 16.80851063829787
			break;
		case "WMRYellow":
			hueColor = 14.53784860557769 
			break;
		case "WMOrange":
			hueColor = 11.41830065359477
			break;
        case "WMROrange":
			hueColor = 6.124183006535949
			break;
		case "HotRed":
			hueColor = 4.1
			break;
        case "Red":
			hueColor = 100
			break;
        case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
        case "Red":
			hueColor = 100
			break;
	}

	state.previous = [:]

	hues.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
           
		]
	}
	
	log.debug "current values = $state.previous"
    
    // CHECK for GREEN button on
    if (mySwitch != null) {
    	if (mySwitch.latestValue("switch") == "on" ) {   
    		log.debug "mySwitch is on so setting light to GREEN and closing switch"
        	if (color != "Red") { //If its red, then override green
        		hueColor = 39
            	mySwitch.off()  
        	}
    	}
    }
    
  	def lightLevel = 60
    if (brightnessLevel != null) {
    	lightLevel = brightnessLevel 
    }
     
	def newValue = [hue: hueColor, saturation: saturation, level: lightLevel]  
	log.debug "new value = $newValue"

	hues*.setColor(newValue)
}

/// HANDLE MOTION

def turnOff() {
	log.debug "Timer fired, turning off light(s)"
    hues.off()
}

def motionHandler(evt) {
	if (evt.value == "active") {                // If there is movement then...
        log.debug "Motion detected, turning on light and killing timer"
        checkForWeather()
        unschedule( turnOff )                   // ...we don't need to turn it off.
    }
    else {                                      // If there is no movement then...
        def delay = 100 				
        log.debug "Motion cleared, turning off switches in (${delay})."
        pause(delay)
        hues.off()
    }
}

def initialize() {
	log.info "Initializing, subscribing to motion event at ${motionHandler} on ${motion_detector}"
    subscribe(motion_detector, "motion", motionHandler)
	subscribe(app, appTouchHandler)
}
def appTouchHandler(evt) {
	checkForWeather()
    def delay = 4000 				
    log.debug "App triggered with button press, turning off switches in (${delay})."
    pause(delay)
    hues.off()
}

private flashLights() {
	def doFlash = true
	def onFor = onFor ?: 1000
	def offFor = offFor ?: 1000
	def numFlashes = numFlashes ?: 3

	log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
	}

	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 1L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
            hues.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
            hues.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
        //delay += offFor
        //s.off(delay:delay)
	}
}