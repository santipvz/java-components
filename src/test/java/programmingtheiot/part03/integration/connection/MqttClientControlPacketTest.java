/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 by Andrew D. King
 */

 package programmingtheiot.part03.integration.connection;

 import static org.junit.Assert.*;
 
 import java.util.logging.Logger;
 
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
 import programmingtheiot.common.ConfigConst;
 import programmingtheiot.common.ConfigUtil;
 import programmingtheiot.common.IDataMessageListener;
 import programmingtheiot.common.ResourceNameEnum;
 import programmingtheiot.data.*;
 import programmingtheiot.gda.connection.*;
 
 /**
  * This test case class contains very basic integration tests for
  * MqttClientControlPacketTest. It should not be considered complete, but serve
  * as a starting point for the student implementing additional functionality
  * within their Programming the IoT environment.
  *
  */
 public class MqttClientControlPacketTest {
	 // static
 
	 private static final Logger _Logger = Logger.getLogger(MqttClientControlPacketTest.class.getName());
 
	 // member var's
 
	 private MqttClientConnector mqttClient = null;
	 private int delay;
 
	 // test setup methods
 
	 @Before
	 public void setUp() throws Exception {
		 this.mqttClient = new MqttClientConnector();
		 this.delay = ConfigUtil.getInstance().getInteger(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY,
				 ConfigConst.DEFAULT_KEEP_ALIVE);
	 }
 
	 @After
	 public void tearDown() throws Exception {
	 }
 
	 // test methods
 
	 @Test
	 public void testConnectAndDisconnect() {
		 assertTrue(this.mqttClient.connectClient());
		 assertFalse(this.mqttClient.connectClient());
 
		 try {
			 Thread.sleep(delay * 1000 + 5000);
		 } catch (Exception e) {
			 // ignore
		 }
 
		 assertTrue(this.mqttClient.disconnectClient());
		 assertFalse(this.mqttClient.disconnectClient());
		 _Logger.info("testConnectAndDisconnect() is complete.");
	 }
 
	 @Test
	 public void testServerPing() {
		 assertTrue(this.mqttClient.connectClient());
		 assertTrue(this.mqttClient.isConnected());
 
		 try {
			 Thread.sleep(delay * 1000 + 5000);
		 } catch (Exception e) {
			 // ignore
		 }
 
		 assertTrue(this.mqttClient.isConnected());
		 assertTrue(this.mqttClient.disconnectClient());
		 _Logger.info("testServerPing() is complete.");
	 }
 
	 @Test
	 public void testPubSub() {
		 // IMPORTANT: be sure to use QoS 1 and 2 to see ALL control packets
		 int[] qosLevels = {0, 1, 2};
 
		 assertTrue(this.mqttClient.connectClient());
 
		 for (int qos : qosLevels) {
			 assertTrue(this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE, qos));
 
			 try {
				 Thread.sleep(delay * 1000);
			 } catch (Exception e) {
				 // ignore
			 }
			 assertTrue(this.mqttClient.publishMessage(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE,
					 "Test message: QoS " + qos, qos));
 
			 assertTrue(this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE));
		 }
 
		 assertTrue(this.mqttClient.disconnectClient());
		 _Logger.info("testPubSub() is complete.");
	 }
 
 }