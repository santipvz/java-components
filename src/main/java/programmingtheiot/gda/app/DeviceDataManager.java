/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */
package programmingtheiot.gda.app;

import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
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
        // Implementation can be added as needed.
        return false;
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
            _Logger.info("Handling sensor message: " + data.getName());
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for SensorData instance.");
            }
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
        _Logger.info("Starting DeviceDataManager...");
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.startManager();
        }
        // TODO: Add calls to start other connection clients/servers as implemented
    }
    
    /**
     * Stops the manager and disconnects all enabled connections.
     */
    public void stopManager()
    {
        _Logger.info("Stopping DeviceDataManager...");
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.stopManager();
        }
        // TODO: Add calls to stop/disconnect other connection clients/servers as implemented
    }
    
    
    // private methods
    
    /**
     * Initializes the manager and creates instances of connection and performance classes.
     */
    private void initManager()
    {
        ConfigUtil configUtil = ConfigUtil.getInstance();
        
        // Re-read the system performance enablement flag
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
        
        if (this.enableSystemPerf) {
            this.sysPerfMgr = new SystemPerformanceManager();
            this.sysPerfMgr.setDataMessageListener(this);
        }
        
        if (this.enableMqttClient) {
            // TODO: Instantiate the MQTT client (e.g., new MqttClientConnector())
        }
        
        if (this.enableCoapServer) {
            // TODO: Instantiate the CoAP server (e.g., new CoapServerGateway())
        }
        
        if (this.enableCloudClient) {
            // TODO: Instantiate the Cloud client (e.g., new CloudClientConnector())
        }
        
        if (this.enablePersistenceClient) {
            // TODO: Instantiate the Persistence client (e.g., new RedisPersistenceAdapter())
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

    private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
    {
        _Logger.fine("handleUpstreamTransmission called for resource: " + resourceName + " with QoS: " + qos);
        // TODO: Implement upstream transmission logic.
        return false;
    }
}
