/**
 *  Sample Web Services Application
 *
 *  Author: SmartThings
 */
// Automatically generated. Make future change here.

definition(
    name: "WebAPI",
    namespace: "625alex",
    author: "Alex Malikov",
    description: "SmartTiles Dashboard, a SmartThings web client.",
    category: "SmartThings Labs",
    iconUrl: "https://625alex.github.io/SmartTiles/prod/icon.png",
    iconX2Url: "https://625alex.github.io/SmartTiles/prod/icon.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Allow a web application to control these things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
		input "motions", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
		input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
	}
}

mappings {
	path("/list") {
		action: [
			GET: "listAll"
		]
	}

	path("/events/:id") {
		action: [
			GET: "showEvents"
		]
	}

	path("/switches") {
		action: [
			GET: "GetLightNames",
			PUT: "updateSwitches"
		]
	}
	path("/switches/:id") {
		action: [
			GET: "showSwitch",
			PUT: "updateSwitch"
		]
	}
	path("/switches/subscriptions") {
		action: [
			POST: "addSwitchSubscription"
		]
	}
	path("/switches/subscriptions/:id") {
		action: [
			DELETE: "removeSwitchSubscription"
		]
	}

	path("/motionSensors") {
		action: [
			GET: "listMotions",
			PUT: "updateMotions"
		]
	}
	path("/motionSensors/:id") {
		action: [
			GET: "showMotion",
			PUT: "updateMotion"
		]
	}
	path("/motionSensors/subscriptions") {
		action: [
			POST: "addMotionSubscription"
		]
	}
	path("/motionSensors/subscriptions/:id") {
		action: [
			DELETE: "removeMotionSubscription"
		]
	}

	path("/locks") {
		action: [
			GET: "listLocks",
			PUT: "updateLock"
		]
	}
	path("/locks/:id") {
		action: [
			GET: "showLock",
			PUT: "updateLock"
		]
	}
	path("/locks/subscriptions") {
		action: [
			POST: "addLockSubscription"
		]
	}
	path("/locks/subscriptions/:id") {
		action: [
			DELETE: "removeLockSubscription"
		]
	}

	path("/state") {
		action: [
			GET: "currentState"
		]
	}

}

def installed() {
log.trace "Installed"

}

def updated() {
log.trace "Updated"
}


def listAll() {
	listSwitches() + listMotions() + listLocks()
}


def GetLightNames() {
	switches.collect{device(it,"switch")}
}
void updateSwitches() {
	updateAll(switches)
}
def showSwitch() {
	show(switches, "switch")
}
void updateSwitch() {
	update(switches)
}
def addSwitchSubscription() {
	addSubscription(switches, "switch")
}
def removeSwitchSubscription() {
	removeSubscription(switches)
}




def listMotions() {
	motions.collect{device(it,"motionSensor")}
}
void updateMotions() {
	updateAll(motions)
}
def showMotion() {
	show(motions, "motionSensor")
}
void updateMotion() {
	update(motions)
}
def addMotionSubscription() {
	addSubscription(motions, "motion")
}
def removeMotionSubscription() {
	removeSubscription(motions)
}

def listLocks() {
	locks.collect{device(it,"lock")}
}
void updateLocks() {
	updateAll(locks)
}
def showLock() {
	show(locks, "lock")
}
void updateLock() {
	update(locks)
}
def addLockSubscription() {
	addSubscription(locks, "lock")
}
def removeLockSubscription() {
	removeSubscription(locks)
}

def deviceHandler(evt) {
	def deviceInfo = state[evt.deviceId]
	if (deviceInfo) {
		httpPostJson(uri: deviceInfo.callbackUrl, path: '', body: [evt: [value: evt.value]]) {
			log.debug "Event data successfully posted"
		}
	} else {
		log.debug "No subscribed device found"
	}
}

def currentState() {
	state
}

def showStates() {
	def device = (switches + motions + locks).find { it.id == params.id }
	if (!device) {
		httpError(404, "Switch not found")
	}
	else {
		device.events(params)
	}
}

private void updateAll(devices) {
	def command = request.JSON?.command
	if (command) {
		devices."$command"()
	}
}

private void update(devices) {
	log.debug "update, request: ${request.JSON}, params: ${params}, devices: $devices.id"
	def command = request.JSON?.command
	if (command) {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
			device."$command"()
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}

private addSubscription(devices, attribute) {
	def deviceId = request.JSON?.deviceId
	def callbackUrl = request.JSON?.callbackUrl
	def myDevice = devices.find { it.id == deviceId }
	if (myDevice) {
		if (state[deviceId]) {
			log.debug "Switch subscription already exists, unsubcribing"
			unsubscribe(myDevice)
		}
		log.debug "Adding switch subscription" + callbackUrl
		state[deviceId] = [callbackUrl: callbackUrl]
		log.debug "Added state: $state"
		subscribe(myDevice, "switch", deviceHandler)
	}
}

private removeSubscription(devices) {
	def deviceId = params.id
	def device = devices.find { it.id == deviceId }
	if (device) {
		log.debug "Removing $device.displayName subscription"
		state.remove(device.id)
		unsubscribe(device)
	}
}

private device(it, type) {
	it ? [id: it.id, label: it.displayName, type: type] : null
}
