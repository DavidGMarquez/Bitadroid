/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bitalino.comm;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO replace description strings with properties for internationalization.
 */
public enum BITalinoErrorTypes {
	BT_DEVICE_NOT_CONNECTED(0, "Bluetooth Device not connected"),
	PORT_COULD_NOT_BE_OPENED(
	        1,
	        "The communication port could not be initialized. The provided parameters could not be set."),
	DEVICE_NOT_IDLE(2, "Device not in idle mode"),
	DEVICE_NOT_IN_ACQUISITION_MODE(3, "Device not is acquisition mode"),
	UNDEFINED_SAMPLING_RATE_NOT(4,
	        "The Sampling Rate chose cannot be set in BITalino. Choose 1000,100,10 or 1"),
	LOST_COMMUNICATION(5, "The Computer lost communication"),
	INVALID_PARAMETER(6, "Invalid parameter"),
	INVALID_THRESHOLD(7, "The threshold value must be between 0 and 63"),
	INVALID_ANALOG_CHANNELS(8,
	        "The number of analog channels available are between 0 and 5"),
	DECODE_INVALID_DATA(9, "Incorrect data to be decoded"),
	INVALID_DIGITAL_CHANNELS(
	        10,
	        "To set the digital outputs, the input array must have 4 items, one for each channel."),
	INVALID_MAC_ADDRESS(11, "MAC address not valid."),
	UNDEFINED(12, "UNDEFINED ERROR");

	private final int value;
	private final String description;

	private static Map<Integer, BITalinoErrorTypes> map = new HashMap<Integer, BITalinoErrorTypes>();

	static {
		for (BITalinoErrorTypes error : BITalinoErrorTypes.values())
      map.put(error.getValue(), error);
	}

	private BITalinoErrorTypes(final int value, final String description) {
		this.value = value;
		this.description = description;
	}

	public int getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static BITalinoErrorTypes fromValue(final int value) {
		return map.get(value);
	}

}