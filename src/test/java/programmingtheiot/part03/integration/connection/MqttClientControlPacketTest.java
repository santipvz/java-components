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
 import programmingtheiot.common.ResourceNameEnum;
 import programmingtheiot.gda.connection.MqttClientConnector;
 
 /**
  * Integration tests for MqttClientConnector, including control packets.
  */
 public class MqttClientControlPacketTest {
	 private static final Logger _Logger =
		 Logger.getLogger(MqttClientControlPacketTest.class.getName());
 
	 private MqttClientConnector mqttClient;
 
	 @Before
	 public void setUp() throws Exception {
		 this.mqttClient = new MqttClientConnector();
	 }
 
	 @After
	 public void tearDown() throws Exception {
		 if (this.mqttClient != null) {
			 this.mqttClient.disconnectClient();
		 }
	 }
 
	 /**
	  * Test basic connect/disconnect control packets (CONNECT, CONNACK, DISCONNECT).
	  */
	 @Test
	 public void testConnectAndDisconnect() throws InterruptedException {
		 int delay = ConfigUtil.getInstance()
				 .getInteger(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
 
		 assertTrue("First connect should succeed", this.mqttClient.connectClient());
		 assertFalse("Second connect should be no-op (already connected)", this.mqttClient.connectClient());
 
		 // Give a moment to process
		 Thread.sleep(2000);
 
		 assertTrue("First disconnect should succeed", this.mqttClient.disconnectClient());
		 assertFalse("Second disconnect should be no-op (already disconnected)", this.mqttClient.disconnectClient());
	 }
 
	 /**
	  * Test automatic PINGREQ/PINGRESP control packets via keep-alive.
	  */
	 @Test
	 public void testServerPing() throws InterruptedException {
		 int delay = ConfigUtil.getInstance()
				 .getInteger(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
 
		 assertTrue("Connect for ping test should succeed", this.mqttClient.connectClient());
 
		 // Wait longer than keep-alive interval to trigger automatic ping
		 Thread.sleep(delay * 1000 + 2000);
 
		 // Connection should remain alive (connectClient returns false if already connected)
		 assertFalse("Connect after ping interval should be no-op (still connected)", this.mqttClient.connectClient());
 
		 assertTrue("Disconnect after ping should succeed", this.mqttClient.disconnectClient());
	 }
 
	 /**
	  * Test publish/subscribe control packets for QoS 1 and 2.
	  */
	 @Test
	 public void testPubSub() throws InterruptedException {
		 final int qos1 = 1;
		 final int qos2 = 2;
 
		 assertTrue("Connect for pub/sub should succeed", this.mqttClient.connectClient());
 
		 // Subscribe with QoS 1
		 assertTrue("Subscribe QoS1 should succeed", this.mqttClient.subscribeToTopic(
				 ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos1));
		 Thread.sleep(2000);
 
		 String payload1 = "TEST: QoS1 payload";
		 assertTrue("Publish QoS1 should succeed", this.mqttClient.publishMessage(
				 ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, payload1, qos1));
		 Thread.sleep(2000);
 
		 assertTrue("Unsubscribe QoS1 should succeed", this.mqttClient.unsubscribeFromTopic(
				 ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE));
 
		 // Subscribe with QoS 2
		 assertTrue("Subscribe QoS2 should succeed", this.mqttClient.subscribeToTopic(
				 ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos2));
		 Thread.sleep(2000);
 
		 String payload2 = "TEST: QoS2 payload";
		 assertTrue("Publish QoS2 should succeed", this.mqttClient.publishMessage(
				 ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, payload2, qos2));
		 Thread.sleep(2000);
 
		 assertTrue("Unsubscribe QoS2 should succeed", this.mqttClient.unsubscribeFromTopic(
				 ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE));
 
		 assertTrue("Disconnect after pub/sub should succeed", this.mqttClient.disconnectClient());
	 }
 }
 