package programmingtheiot.gda.app;

import java.util.logging.Logger;
import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.gda.connection.CloudClientConnector;

public class CustomActuationManager {
    private static final Logger _Logger = Logger.getLogger(CustomActuationManager.class.getName());
    
    private CloudClientConnector cloudClient;
    private float lastTemperature = 0.0f;
    private float temperatureThreshold = 25.0f;
    private boolean isActuatorOn = false;
    
    public CustomActuationManager(CloudClientConnector cloudClient) {
        this.cloudClient = cloudClient;
    }
    
    public void handleSensorData(SensorData data) {
        if (data != null && data.getName().equals(ConfigConst.TEMP_SENSOR_NAME)) {
            float currentTemp = data.getValue();
            
            // Check if temperature crosses threshold
            if (currentTemp > temperatureThreshold && !isActuatorOn) {
                // Temperature exceeded threshold, turn on actuator
                sendActuationCommand(true);
                isActuatorOn = true;
            } else if (currentTemp <= temperatureThreshold && isActuatorOn) {
                // Temperature below threshold, turn off actuator
                sendActuationCommand(false);
                isActuatorOn = false;
            }
            
            lastTemperature = currentTemp;
        }
    }
    
    private void sendActuationCommand(boolean turnOn) {
        ActuatorData actuatorData = new ActuatorData();
        actuatorData.setName("CustomActuator");
        actuatorData.setCommand(turnOn ? 1 : 0);
        actuatorData.setValue(turnOn ? 1.0f : 0.0f);
        
        String topic = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceName() + "-CustomActuator";
        String payload = DataUtil.getInstance().actuatorDataToJson(actuatorData);
        
        if (cloudClient != null) {
            // Publish directly to MQTT topic since we're using a custom actuator
            cloudClient.getMqttClient().publishMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, payload, 1);
            _Logger.info("Published custom actuation command: " + (turnOn ? "ON" : "OFF"));
        }
    }
} 