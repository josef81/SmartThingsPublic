/**
 *  Hue Might Need an Umbrella
 *
 *  Copyright 2014 Colin Butler
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
    name: "Hue Might Need an Umbrella",
    namespace: "",
    author: "Colin Butler",
    description: "Light your Hue device(s) to indicate the weather: \nDeeper colors = colder temperature\nWhite = warm/dry | Light blue: cool/dry | Deep blue: cold/dry\nLight green: warm/rain | Mid green: cool/rain | Deep green: cold/rain (40+% chance)\nPurple = ice, sleet, or snow",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png"
)


 /* BASED ON, THANKS TO, AND USING A TON OF CODE FROM:
 Color the weather
 Daniel Root
 www.danielroot.info
 
 Should work (but not yet tested with) Phillips Hue devices and any SmartThings device that supports "capability.colorControl".
 See https://codebender.cc/sketch:28062 for Arduino code that works with the Arduino SmartShield.
 */
 preferences {
	 section("Check the weather in"){
		input "zipcode", "text", title: "ZIP code"
	 }
     section("Between this time"){
     	input "startTime", "time", title: "Start time"
     }
     section("And this time"){
     	input "endTime", "time", title: "End Time"
     }
     section("Or when this switch is on")
     {
     	input "testSwitch", "capability.switch", title:"Switch"
	 }
     section("Set these color devices"){
     	input "colorDevices", "capability.colorControl", title: "Color devices", multiple:true
     }
     section("\"Cool\" is lower than"){
        input "coolTemp", "number", title: "Cool temp (Â°F)", defaultValue: "60"
     }
     section("\"Cold\" is lower than"){
        input "coldTemp", "number", title: "Cold temp (Â°F)", defaultValue: "40"
     }
     section("\"Rainy\" chance of precipitation") {
        input "precipThreshold", "number", title: "Precipitation threshold (%)", defaultValue: "40"
     }
    
 }

private colorFromValues(h,s,b)
{
    /* Convert from Photoshop values (0-360, 0-100, 0-100) */
    def result = [hue: Math.round(h/360.0*100), saturation: s, level: b]
    return result;
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

private parseForecast(json)
{
    /* Hue/Saturation/Brightness, as pulled from Photoshop (0-360, 0-100, 0-100) */
    def result = colorFromValues(0,100,100)
    
    def dryWarm = colorFromValues(285,0,100)
    def dryCool = colorFromValues(234,85,57)
    def dryCold = colorFromValues(234,100,35)
    def wetWarm = colorFromValues(124,53,79)
    def wetCool = colorFromValues(124,85,57)
    def wetCold = colorFromValues(124,100,35)
    def iceSnow = colorFromValues(285,50,81)
    
    def colorName = "Error"
    
        def richforecast = json?.forecast?.simpleforecast?.forecastday?.first()
        if (richforecast) {
            
            def rain = (richforecast?.pop >= precipThreshold)
            def ice = (richforecast?.icon == 9 || richforecast?.icon == 16 || richforecast?.icon == 18 || richforecast?.icon == 19 || richforecast?.icon == 20 || richforecast?.icon == 21 || richforecast?.icon == 22 || richforecast?.icon == 23 || richforecast?.icon == 24)
            def cold = (richforecast?.low?.fahrenheit?.toInteger() < coldTemp)
            def cool = (richforecast?.low?.fahrenheit?.toInteger() < coolTemp)

            log.debug("Precip: ${richforecast?.pop}%, Ice: ${ice}, Temp(low): ${richforecast?.low?.fahrenheit?.toInteger()}")
             if( ice ) {
                result = iceSnow
                colorName = "Ice or snow"
            } else if( cold ) {
                result = dryCold
                colorName = "Dry and cold"
                if( rain ) {
                    result = wetCold
                    colorName = "Rainy and cold"
                }
            } else if( cool ) {
                result = dryCool
                colorName = "Dry and cool"
                if( rain ) {
                    result = wetCool
                    colorName = "Rainy and cool"
                }
            } else {
                result = dryWarm
                colorName = "Dry and warm"
                if( rain ) {
                    result = wetWarm
                    colorName = "Rainy and warm"
                }
            }
        
            sendNotificationEvent("Forecast for ${zipcode}:\nPrecip: ${richforecast?.pop}%, Ice/snow: ${ice?"Yes":"No"},\nTemp(low): ${richforecast?.low?.fahrenheit?.toInteger()}Â°F")
            sendNotificationEvent("Setting weather lights to $colorName ($result)")
    } else {
    	 sendNotificationEvent("Could not get weather!")
    }

    log.debug result
    return result
}