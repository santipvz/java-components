/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */

 package programmingtheiot.gda.connection.handlers;

 import java.util.logging.Logger;
 
 import org.eclipse.californium.core.CoapResource;
 import org.eclipse.californium.core.coap.CoAP.ResponseCode;
 import org.eclipse.californium.core.server.resources.CoapExchange;
 
 import programmingtheiot.common.ConfigConst;
 import programmingtheiot.common.ConfigUtil;
 import programmingtheiot.common.IDataMessageListener;
 import programmingtheiot.common.ResourceNameEnum;
 
 /**
  * Shell representation of class for student implementation.
  *
  */
 public class GenericCoapResourceHandler extends CoapResource {
	 // static
 
	 protected static final Logger _Logger = Logger.getLogger(GenericCoapResourceHandler.class.getName());
	 private IDataMessageListener dataMsgListener = null;
 
	 // params
 
	 // constructors
 
	 /**
	  * Constructor.
	  * 
	  * @param resource
	  *            Basically, the path (or topic)
	  */
	 public GenericCoapResourceHandler(ResourceNameEnum resource) {
		 this(resource.getResourceName());
	 }
 
	 /**
	  * Constructor.
	  * 
	  * @param resourceName
	  *            The name of the resource.
	  */
	 public GenericCoapResourceHandler(String resourceName) {
		 super(resourceName);
	 }
 
	 public void setDataMessageListener(IDataMessageListener listener) {
		 if (listener != null) {
			 this.dataMsgListener = listener;
		 }
	 }
 
 }