/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */

 package programmingtheiot.gda.connection.handlers;

 import org.eclipse.californium.core.coap.CoAP.ResponseCode;
 import org.eclipse.californium.core.coap.MediaTypeRegistry;
 import org.eclipse.californium.core.server.resources.CoapExchange;
 
 import programmingtheiot.common.IActuatorDataListener;
 import programmingtheiot.data.ActuatorData;
 import programmingtheiot.data.DataUtil;
 
 /**
  * Shell representation of class for student implementation.
  *
  */
 public class GetActuatorCommandResourceHandler extends GenericCoapResourceHandler implements IActuatorDataListener {
     // static
 
     // params
 
     private ActuatorData actuatorData = null;
 
     // constructors
 
     public GetActuatorCommandResourceHandler(String resourceName) {
         super(resourceName);
         this.actuatorData = new ActuatorData();
         // set the resource to be observable
         super.setObservable(true);
     }
 
     public boolean onActuatorDataUpdate(ActuatorData data) {
         if (data != null && this.actuatorData != null) {
             this.actuatorData.updateData(data);
 
             // notify all connected clients
             super.changed();
 
             _Logger.fine("Actuator data updated for URI: " + super.getURI() + ": Data value = "
                     + this.actuatorData.getValue());
 
             return true;
         }
 
         return false;
     }
 
     @Override
     public void handleGET(CoapExchange context) {
 
         // validate 'context'
         if (context == null) {
             _Logger.warning("CoapExchange context is null. Cannot process request.");
             return;
         }
 
         _Logger.info(
                 "GET request received for resource: " + super.getURI() + " with query: " + context.getRequestText());
 
         // accept the request
         context.accept();
 
         // Convert the locally stored ActuatorData to JSON using DataUtil
         String jsonData = DataUtil.getInstance().actuatorDataToJson(this.actuatorData);
 
         // send an appropriate response
         context.respond(ResponseCode.CONTENT, jsonData, MediaTypeRegistry.APPLICATION_JSON);
     }
 
     @Override
     public void handlePUT(CoapExchange context) {
 
         // validate 'context'
         if (context == null) {
             _Logger.warning("CoapExchange context is null. Cannot process PUT request.");
             return;
         }
 
         _Logger.info(
                 "PUT request received for resource: " + super.getURI() + " with payload: " + context.getRequestText());
 
         // accept the request
         context.accept();
 
         // parse the payload to update actuatorData
         try {
             ActuatorData updatedData = DataUtil.getInstance().jsonToActuatorData(context.getRequestText());
             this.actuatorData = updatedData;
 
             context.respond(ResponseCode.CHANGED, "ActuatorData successfully modified.");
         } catch (Exception e) {
             _Logger.severe("Failed to process PUT payload: " + e.getMessage());
             context.respond(ResponseCode.BAD_REQUEST, "Invalid JSON format.");
         }
     }
 
     @Override
     public void handlePOST(CoapExchange context) {
 
         // validate 'context'
         if (context == null) {
             _Logger.warning("CoapExchange context is null. Cannot process POST request.");
             return;
         }
 
         _Logger.info(
                 "POST request received for resource: " + super.getURI() + " with payload: " + context.getRequestText());
 
         // accept the request
         context.accept();
 
         // parse the payload to update actuatorData
         try {
             ActuatorData newData = DataUtil.getInstance().jsonToActuatorData(context.getRequestText());
             this.actuatorData = newData;
 
             context.respond(ResponseCode.CHANGED, "ActuatorData updated successfully.");
         } catch (Exception e) {
             _Logger.severe("Failed to parse POST payload: " + e.getMessage());
             context.respond(ResponseCode.BAD_REQUEST, "Invalid JSON format.");
         }
     }
 
     @Override
     public void handleDELETE(CoapExchange context) {
 
         // validate 'context'
         if (context == null) {
             _Logger.warning("CoapExchange context is null. Cannot process DELETE request.");
             return;
         }
 
         _Logger.info("DELETE request received for resource: " + super.getURI());
 
         // accept the request
         context.accept();
 
         // clear the data
         this.actuatorData = new ActuatorData();
 
         context.respond(ResponseCode.DELETED, "ActuatorData has been cleared.");
     }
 
 }