/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 by Andrew D. King
 */ 

package programmingtheiot.part04.integration.connection;

import static org.junit.Assert.*;

import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.gda.app.DeviceDataManager;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains integration tests for CloudClientConnector.
 * It implements three test cases:
 * 1. Basic sensor data publishing to cloud
 * 2. LED actuation event triggering and handling
 * 3. End-to-end integration test with CDA and GDA
 */
public class CloudClientConnectorTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnectorTest.class.getName());
	
	// member var's
	
	private ICloudClient cloudClient = null;
	private DefaultDataMessageListener dataMsgListener = null;
	private MqttClientConnector mqttClient = null;
	
	// test setup methods
	
	@Before
	public void setUp() throws Exception
	{
		this.cloudClient = new CloudClientConnector();
		this.dataMsgListener = new DefaultDataMessageListener();
		this.cloudClient.setDataMessageListener(this.dataMsgListener);
		
		// Get the MQTT client instance for connection state verification
		if (this.cloudClient instanceof CloudClientConnector) {
			this.mqttClient = ((CloudClientConnector) this.cloudClient).getMqttClient();
		}
	}
	
	@After
	public void tearDown() throws Exception
	{
		if (this.cloudClient != null) {
			// Only attempt disconnect if we have a valid connection
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.cloudClient.disconnectClient();
				// Wait for disconnect to complete
				Thread.sleep(1000L);
			}
		}
	}
	
	/**
	 * Test 1: Basic sensor data publishing to cloud
	 * - Create CloudClientConnector instance
	 * - Generate and publish SensorData
	 * - Verify data received in cloud service
	 */
	@Test
	public void testSensorDataPublishing()
	{
		_Logger.info("Starting Test 1: Basic sensor data publishing");
		
		assertTrue("Failed to connect to cloud service", this.cloudClient.connectClient());
		
		try {
			// Wait for connection to establish
			Thread.sleep(2000L);
			
			// Verify connection is established
			assertTrue("MQTT client not connected", this.mqttClient != null && this.mqttClient.isConnected());
			
			// Create sample sensor data
			SensorData sensorData = new SensorData();
			sensorData.setName(ConfigConst.TEMP_SENSOR_NAME);
			sensorData.setValue(92.0f);
			
			// Publish to cloud
			assertTrue("Failed to publish sensor data", 
				this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
			
			// Wait for data to be processed
			Thread.sleep(5000L);
			
			// TODO: Add verification of data in cloud service
			// This would typically involve querying the cloud service API
			// to verify the data was received and stored
			
		} catch (Exception e) {
			fail("Test failed with exception: " + e.getMessage());
		} finally {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				assertTrue("Failed to disconnect from cloud service", this.cloudClient.disconnectClient());
			}
		}
		
		_Logger.info("Test 1 complete.");
	}
	
	/**
	 * Test 2: LED actuation event triggering and handling
	 * - Create CloudClientConnector instance
	 * - Subscribe to LED actuator topic
	 * - Generate threshold-crossing sensor data
	 * - Verify actuation event triggered and received
	 */
	@Test
	public void testLedActuationEvent()
	{
		_Logger.info("Starting Test 2: LED actuation event testing");
		
		try {
			// First ensure we're disconnected
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.cloudClient.disconnectClient();
				Thread.sleep(1000L);
			}
			
			// Connect to cloud service
			assertTrue("Failed to connect to cloud service", this.cloudClient.connectClient());
			
			// Wait for connection to establish with multiple retries
			int maxRetries = 10;
			int retryCount = 0;
			boolean isConnected = false;
			
			while (!isConnected && retryCount < maxRetries) {
				Thread.sleep(1000L);
				if (this.mqttClient != null) {
					isConnected = this.mqttClient.isConnected();
					_Logger.info("Connection attempt " + (retryCount + 1) + ": " + (isConnected ? "Connected" : "Not connected"));
				}
				retryCount++;
			}
			
			assertTrue("MQTT client not connected after " + maxRetries + " retries", isConnected);
			
			// Subscribe to LED actuator topic
			assertTrue("Failed to subscribe to LED actuator topic",
				this.cloudClient.subscribeToCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));
			
			// Generate multiple sensor readings that cross threshold
			for (int i = 0; i < 5; i++) {
				// Verify connection is still established before each publish
				assertTrue("MQTT client not connected", this.mqttClient != null && this.mqttClient.isConnected());
				
				SensorData sensorData = new SensorData();
				sensorData.setName(ConfigConst.TEMP_SENSOR_NAME);
				sensorData.setValue(95.0f + i); // Values above threshold
				
				assertTrue("Failed to publish sensor data",
					this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
				
				Thread.sleep(1000L);
			}
			
			// Wait for actuation event to be triggered and received
			Thread.sleep(10000L);
			
			// TODO: Add verification of actuation event in cloud service
			// This would typically involve querying the cloud service API
			// to verify the actuation event was triggered
			
			// Verify message was received by CloudClientConnector
			// This would be handled by the DefaultDataMessageListener
			// which should have received the actuation event
			
		} catch (Exception e) {
			fail("Test failed with exception: " + e.getMessage());
		} finally {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				assertTrue("Failed to unsubscribe from LED actuator topic",
					this.cloudClient.unsubscribeFromCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));
				assertTrue("Failed to disconnect from cloud service", this.cloudClient.disconnectClient());
			}
		}
		
		_Logger.info("Test 2 complete.");
	}
	
	/**
	 * Test 3: End-to-end integration test
	 * - Start GDA with CloudClientConnector
	 * - Start CDA
	 * - Verify end-to-end message flow
	 */
	@Test
	public void testEndToEndIntegration()
	{
		_Logger.info("Starting Test 3: End-to-end integration test");
		
		DeviceDataManager ddm = new DeviceDataManager();
		
		try {
			// Start the GDA
			ddm.startManager();
			
			// Wait for GDA to initialize and connect
			Thread.sleep(5000L);
			
			// Verify GDA is connected to cloud service
			CloudClientConnector cloudConnector = ddm.getCloudClientConnector();
			MqttClientConnector mqttConnector = cloudConnector.getMqttClient();
			assertTrue("GDA not connected to cloud service", mqttConnector != null && mqttConnector.isConnected());
			
			// TODO: Start the CDA here
			// This would typically involve starting the CDA application
			// and waiting for it to initialize and connect to the GDA
			
			// Run the test for 5 minutes
			Thread.sleep(300000L); // 5 minutes
			
			// TODO: Add verification of end-to-end message flow
			// This would involve checking:
			// 1. CDA to GDA communication
			// 2. GDA to cloud service communication
			// 3. Cloud service actuation events
			// 4. GDA to CDA actuation events
			
		} catch (Exception e) {
			fail("Test failed with exception: " + e.getMessage());
		} finally {
			ddm.stopManager();
		}
		
		_Logger.info("Test 3 complete.");
	}
}
