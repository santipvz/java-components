/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */
package programmingtheiot.gda.app;

import java.util.logging.Logger;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.californium.core.CoapServer;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.BaseIotData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;

// Connection classes (stubs or implementations)
import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;

// Updated import for SystemPerformanceManager
import programmingtheiot.gda.system.SystemPerformanceManager;

public class DeviceDataManager implements IDataMessageListener
{
    // static
    private static final Logger _Logger =
        Logger.getLogger(DeviceDataManager.class.getName());
    
    // private variables (flags)
    private boolean enableMqttClient = true;
    private boolean enableCoapServer = false;
    private boolean enableCloudClient = false;
    private boolean enableSmtpClient = false;
    private boolean enablePersistenceClient = false;
    private boolean enableSystemPerf = false;
    
    // private variables (connection and manager instances)
    private IActuatorDataListener actuatorDataListener = null;
    private IPubSubClient mqttClient = null;
    private IPubSubClient cloudClient = null;
    private IPersistenceClient persistenceClient = null;
    private IRequestResponseClient smtpClient = null;
    private CoapServerGateway coapServer = null;
    private SystemPerformanceManager sysPerfMgr = null;

    // Humidity threshold variables
    private ActuatorData latestHumidifierActuatorData = null;
    private ActuatorData latestHumidifierActuatorResponse = null;
    private SensorData latestHumiditySensorData = null;
    private OffsetDateTime latestHumiditySensorTimeStamp = null;
    private boolean handleHumidityChangeOnDevice = false;
    private int lastKnownHumidifierCommand = ConfigConst.OFF_COMMAND;
    private long humidityMaxTimePastThreshold = 300; // seconds
    private float nominalHumiditySetting = 40.0f;
    private float triggerHumidifierFloor = 30.0f;
    private float triggerHumidifierCeiling = 50.0f;
    
    // constructors
    
    /**
     * Default constructor. Uses ConfigUtil to set flags and initializes the manager.
     */
    public DeviceDataManager()
    {
        super();
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        this.enableMqttClient =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
        this.enableCoapServer =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
        this.enableCloudClient =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
        this.enablePersistenceClient =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);

        // Load humidity threshold configuration
        this.handleHumidityChangeOnDevice =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");
        this.humidityMaxTimePastThreshold =
            configUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
        this.nominalHumiditySetting =
            configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
        this.triggerHumidifierFloor =
            configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
        this.triggerHumidifierCeiling =
            configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");

        // Validate timing threshold
        if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
            this.humidityMaxTimePastThreshold = 300;
        }
        
        initManager();
    }
    
    /**
     * Overloaded constructor that allows external flag setting.
     */
    public DeviceDataManager(
        boolean enableMqttClient,
        boolean enableCoapServer,
        boolean enableCloudClient,
        boolean enableSmtpClient,
        boolean enablePersistenceClient)
    {
        super();
        
        this.enableMqttClient = enableMqttClient;
        this.enableCoapServer = enableCoapServer;
        this.enableCloudClient = enableCloudClient;
        this.enableSmtpClient = enableSmtpClient;
        this.enablePersistenceClient = enablePersistenceClient;
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
        
        initManager();
    }
    
    
    // public methods
    
    @Override
    public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
    {
        if (data != null) {
            _Logger.info("Handling actuator response: " + data.getName());
            // Optionally perform further analysis
            handleIncomingDataAnalysis(resourceName, data);
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for ActuatorData instance.");
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
    {
        if (data != null) {
            _Logger.info("Handling actuator command request: " + data.getName());
            
            // Propagate the command to the actuator data listener
            if (this.actuatorDataListener != null) {
                this.actuatorDataListener.onActuatorDataUpdate(data);
            }
            
            // Optionally perform further analysis
            handleIncomingDataAnalysis(resourceName, data);
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for ActuatorData instance.");
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
    {
        if (msg != null) {
            _Logger.info("Handling incoming generic message: " + msg);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
    {
        if (data != null) {
            _Logger.fine("Handling sensor message: " + data.getName());

            if (data.hasError()) {
                _Logger.warning("Error flag set for SensorData instance.");
            }

            String jsonData = DataUtil.getInstance().sensorDataToJson(data);
            _Logger.fine("JSON [SensorData] -> " + jsonData);

            // Get QoS from config
            int qos = ConfigConst.DEFAULT_QOS;

            // Store data if persistence is enabled
            if (this.enablePersistenceClient && this.persistenceClient != null) {
                this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
            }

            // Process the sensor data
            this.handleIncomingDataAnalysis(resourceName, data);

            // Handle upstream transmission
            this.handleUpstreamTransmission(resourceName, jsonData, qos);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
    {
        if (data != null) {
            _Logger.info("Handling system performance message: " + data.getName());
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for SystemPerformanceData instance.");
            }
            return true;
        } else {
            return false;
        }
    }
    
    public void setActuatorDataListener(String name, IActuatorDataListener listener)
    {
        this.actuatorDataListener = listener;
    }
    
    /**
     * Starts the manager and all enabled connections/manager instances.
     */
    public void startManager()
    {
        if (this.mqttClient != null) {
            if (this.mqttClient.connectClient()) {
                _Logger.info("Successfully connected MQTT client to broker.");
    
                // add necessary subscriptions
    
                int qos = ConfigConst.DEFAULT_QOS;
                
                this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
            } else {
                _Logger.severe("Failed to connect MQTT client to broker.");
    
                // TODO: take appropriate action
            }
        }
    
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.startManager();
        }

        if (this.enableCoapServer && this.coapServer != null) {
            if (this.coapServer.startServer()) {
                _Logger.info("CoAP server started.");
            } else {
                _Logger.severe("Failed to start CoAP server.");
            }
        }
    }
    
    /**
     * Stops the manager and disconnects all enabled connections.
     */
    public void stopManager()
    {
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.stopManager();
        }
    
        if (this.mqttClient != null) {
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
    
            if (this.mqttClient.disconnectClient()) {
                _Logger.info("Successfully disconnected MQTT client from broker.");
            } else {
                _Logger.severe("Failed to disconnect MQTT client from broker.");
            }
        }

        if (this.enableCoapServer && this.coapServer != null) {
            if (this.coapServer.stopServer()) {
                _Logger.info("CoAP server stopped.");
            } else {
                _Logger.severe("Failed to stop CoAP server. Check log file for details");
            }
        }
    }
    
    
    // private methods
    
    /**
     * Initializes the manager and creates instances of connection and performance classes.
     */
    private void initManager()
    {
        ConfigUtil configUtil = ConfigUtil.getInstance();
    
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE,  ConfigConst.ENABLE_SYSTEM_PERF_KEY);
    
        if (this.enableSystemPerf) {
            this.sysPerfMgr = new SystemPerformanceManager();
            this.sysPerfMgr.setDataMessageListener(this);
        }
    
        // NOTE: This is new - creating the MQTT client connector instance
        if (this.enableMqttClient) {
            this.mqttClient = new MqttClientConnector();
    
            // NOTE: The next line isn't technically needed until Lab Module 10
            this.mqttClient.setDataMessageListener(this);
        }
    
        if (this.enableCoapServer) {
            this.coapServer = new CoapServerGateway(this);
        }
    
        if (this.enableCloudClient) {
            // TODO: implement this in Lab Module 10
        }
    
        if (this.enablePersistenceClient) {
            // TODO: implement this as an optional exercise in Lab Module 5
        }
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
    {
        _Logger.fine("handleIncomingDataAnalysis (ActuatorData) called for resource: " + resourceName);
        // TODO: Implement further analysis and potential downstream processing.
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
    {
        _Logger.fine("handleIncomingDataAnalysis (SystemStateData) called for resource: " + resourceName);
        // TODO: Implement analysis logic for system state data.
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SensorData data)
    {
        // Check if this is humidity sensor data
        if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE) {
            handleHumiditySensorAnalysis(resourceName, data);
        }
    }

    private void handleHumiditySensorAnalysis(ResourceNameEnum resourceName, SensorData data)
    {
        _Logger.fine("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());

        boolean isLow = data.getValue() < this.triggerHumidifierFloor;
        boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;

        if (isLow || isHigh) {
            _Logger.fine("Humidity data from CDA exceeds nominal range.");

            if (this.latestHumiditySensorData == null) {
                // Set properties then exit - nothing more to do until the next sample
                this.latestHumiditySensorData = data;
                this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);

                _Logger.fine(
                    "Starting humidity nominal exception timer. Waiting for seconds: " +
                    this.humidityMaxTimePastThreshold);

                return;
            } else {
                OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);

                long diffSeconds =
                    ChronoUnit.SECONDS.between(
                        this.latestHumiditySensorTimeStamp, curHumiditySensorTimeStamp);

                _Logger.fine("Checking Humidity value exception time delta: " + diffSeconds);

                if (diffSeconds >= this.humidityMaxTimePastThreshold) {
                    ActuatorData ad = new ActuatorData();
                    ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
                    ad.setLocationID(data.getLocationID());
                    ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
                    ad.setValue(this.nominalHumiditySetting);

                    if (isLow) {
                        ad.setCommand(ConfigConst.ON_COMMAND);
                    } else if (isHigh) {
                        ad.setCommand(ConfigConst.OFF_COMMAND);
                    }

                    _Logger.info(
                        "Humidity exceptional value reached. Sending actuation event to CDA: " +
                        ad);

                    this.lastKnownHumidifierCommand = ad.getCommand();
                    sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);

                    // Set ActuatorData and reset SensorData (and timestamp)
                    this.latestHumidifierActuatorData = ad;
                    this.latestHumiditySensorData = null;
                    this.latestHumiditySensorTimeStamp = null;
                }
            }
        } else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
            // Check if we need to turn off the humidifier
            if (this.latestHumidifierActuatorData != null) {
                // Check the value - if the humidifier is on, but not yet at nominal, keep it on
                if (this.latestHumidifierActuatorData.getValue() >= this.nominalHumiditySetting) {
                    this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);

                    _Logger.info(
                        "Humidity nominal value reached. Sending OFF actuation event to CDA: " +
                        this.latestHumidifierActuatorData);

                    sendActuatorCommandtoCda(
                        ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);

                    // Reset ActuatorData and SensorData (and timestamp)
                    this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
                    this.latestHumidifierActuatorData = null;
                    this.latestHumiditySensorData = null;
                    this.latestHumiditySensorTimeStamp = null;
                } else {
                    _Logger.fine("Humidifier is still on. Not yet at nominal levels (OK).");
                }
            } else {
                // Shouldn't happen, unless some other logic nullifies the class-scoped ActuatorData instance
                _Logger.warning(
                    "ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");
            }
        }
    }

    private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
    {
        // Handle CoAP server case
        if (this.actuatorDataListener != null) {
            this.actuatorDataListener.onActuatorDataUpdate(data);
        }

        // Handle MQTT case
        if (this.enableMqttClient && this.mqttClient != null) {
            String jsonData = DataUtil.getInstance().actuatorDataToJson(data);

            if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
                _Logger.info(
                    "Published ActuatorData command from GDA to CDA: " + data.getCommand());
            } else {
                _Logger.warning(
                    "Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
            }
        }
    }

    private OffsetDateTime getDateTimeFromData(BaseIotData data)
    {
        OffsetDateTime odt = null;

        try {
            odt = OffsetDateTime.parse(data.getTimeStamp());
        } catch (Exception e) {
            _Logger.warning(
                "Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");

            // This won't be accurate, but should be reasonably close, as the CDA will
            // most likely have recently sent the data to the GDA
            odt = OffsetDateTime.now();
        }

        return odt;
    }

    private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
    {
        _Logger.fine("handleUpstreamTransmission called for resource: " + resourceName + " with QoS: " + qos);
        // TODO: Implement upstream transmission logic in Part 04
        return false;
    }
}
