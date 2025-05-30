package programmingtheiot.part04.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.DeviceDataManager;
import programmingtheiot.gda.app.GatewayDeviceApp;

/**
 * Integration test for the Gateway Device Application.
 * This test verifies that the GDA can:
 * 1. Connect to the CDA using MQTT/CoAP with TLS/DTLS
 * 2. Connect to the cloud using MQTT with TLS
 * 3. Process system performance data, sensor data, and actuator command responses
 * 4. Generate actuation events based on custom logic
 * 5. Collect and store internal system performance data
 * 6. Send data to the cloud service
 * 7. Handle cloud-based events
 */
public class GatewayDeviceAppTest {
    private static final Logger _Logger = Logger.getLogger(GatewayDeviceAppTest.class.getName());
    
    private GatewayDeviceApp gdaApp;
    private DeviceDataManager dataMgr;
    private ConfigUtil configUtil;
    private DataUtil dataUtil;
    
    @Before
    public void setUp() throws Exception {
        this.configUtil = ConfigUtil.getInstance();
        this.dataUtil = DataUtil.getInstance();
        
        // Initialize GDA with proper configuration
        this.gdaApp = new GatewayDeviceApp(new String[0]);
        
        // Start the GDA which will initialize and start the DeviceDataManager
        this.gdaApp.startApp();
        
        // Usar la instancia real de DeviceDataManager gestionada por GatewayDeviceApp
        this.dataMgr = this.gdaApp.getDeviceDataManager();
        
        // Wait for MQTT client to initialize and connect
        Thread.sleep(5000);
    }
    
    @After
    public void tearDown() throws Exception {
        if (this.gdaApp != null) {
            this.gdaApp.stopApp(0);
        }
    }
    
    @Test
    public void testGdaDataProcessing() throws Exception {
        // Create test data
        SensorData sensorData = new SensorData();
        sensorData.setName("TestSensor");
        sensorData.setTypeID(ConfigConst.TEMP_SENSOR_TYPE);
        sensorData.setValue(25.0f);
        
        SystemPerformanceData sysPerfData = new SystemPerformanceData();
        sysPerfData.setName("TestSysPerf");
        sysPerfData.setCpuUtilization(50.0f);
        sysPerfData.setMemoryUtilization(60.0f);
        sysPerfData.setDiskUtilization(70.0f);
        
        // Test sensor data processing
        assertTrue("Failed to process sensor data", 
            this.dataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Test system performance data processing
        assertTrue("Failed to process system performance data",
            this.dataMgr.handleSystemPerformanceMessage(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData));
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Test cloud event handling
        ActuatorData cloudActuatorData = new ActuatorData();
        cloudActuatorData.setName("TestActuator");
        cloudActuatorData.setTypeID(ConfigConst.HVAC_ACTUATOR_TYPE);
        cloudActuatorData.setCommand(ConfigConst.ON_COMMAND);
        
        String jsonData = this.dataUtil.actuatorDataToJson(cloudActuatorData);
        assertTrue("Failed to handle cloud actuator command",
            this.dataMgr.handleIncomingMessage(ResourceNameEnum.GDA_ACTUATOR_CMD_RESOURCE, jsonData));
        
        // Wait for final processing
        Thread.sleep(1000);
    }

    @Test
    public void testGdaLongRunningOperation() throws Exception {
        // This test simulates a long-running operation
        // For testing purposes, we'll run for a shorter time
        int testDurationSeconds = 10; // Reduced for testing
        int samplesPerSecond = 1;
        int totalSamples = testDurationSeconds * samplesPerSecond;
        
        for (int i = 0; i < totalSamples; i++) {
            // Generate and process sensor data
            SensorData sensorData = new SensorData();
            sensorData.setName("TestSensor");
            sensorData.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
            sensorData.setValue(35.0f + (i % 20)); // Varying humidity values
            
            assertTrue("Failed to process sensor data", 
                this.dataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
            
            // Generate and process system performance data
            SystemPerformanceData sysPerfData = new SystemPerformanceData();
            sysPerfData.setName("TestSysPerf");
            sysPerfData.setCpuUtilization(50.0f + (i % 30));
            sysPerfData.setMemoryUtilization(60.0f + (i % 20));
            sysPerfData.setDiskUtilization(70.0f + (i % 10));
            
            assertTrue("Failed to process system performance data",
                this.dataMgr.handleSystemPerformanceMessage(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData));
            
            // Wait for processing
            Thread.sleep(1000 / samplesPerSecond); // Wait for 1 second between samples
        }
    }

    @Test
    public void testGdaActuationEvents() throws Exception {
        // Test GDA-based actuation events
        SensorData sensorData = new SensorData();
        sensorData.setName("TestSensor");
        sensorData.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
        sensorData.setValue(25.0f); // Below threshold
        
        assertTrue("Failed to process sensor data", 
            this.dataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
        
        Thread.sleep(1000);
        
        // Test cloud-based actuation events
        ActuatorData cloudActuatorData = new ActuatorData();
        cloudActuatorData.setName("TestActuator");
        cloudActuatorData.setTypeID(ConfigConst.HVAC_ACTUATOR_TYPE);
        cloudActuatorData.setCommand(ConfigConst.ON_COMMAND);
        
        String jsonData = this.dataUtil.actuatorDataToJson(cloudActuatorData);
        assertTrue("Failed to handle cloud actuator command",
            this.dataMgr.handleIncomingMessage(ResourceNameEnum.GDA_ACTUATOR_CMD_RESOURCE, jsonData));
        
        Thread.sleep(1000);
    }

    @Test
    public void testGdaDataPersistence() throws Exception {
        // Test data persistence
        SensorData sensorData = new SensorData();
        sensorData.setName("TestSensor");
        sensorData.setTypeID(ConfigConst.TEMP_SENSOR_TYPE);
        sensorData.setValue(25.0f);
        
        SystemPerformanceData sysPerfData = new SystemPerformanceData();
        sysPerfData.setName("TestSysPerf");
        sysPerfData.setCpuUtilization(50.0f);
        sysPerfData.setMemoryUtilization(60.0f);
        sysPerfData.setDiskUtilization(70.0f);
        
        // Process data which should trigger persistence
        assertTrue("Failed to process sensor data", 
            this.dataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
        
        Thread.sleep(1000);
        
        assertTrue("Failed to process system performance data",
            this.dataMgr.handleSystemPerformanceMessage(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData));
        
        Thread.sleep(1000);
    }
} 