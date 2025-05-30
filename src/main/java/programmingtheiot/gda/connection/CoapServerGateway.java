/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */

package programmingtheiot.gda.connection;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.UdpConfig;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.handlers.GenericCoapResourceHandler;
import programmingtheiot.gda.connection.handlers.GetActuatorCommandResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateTelemetryResourceHandler;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class CoapServerGateway {
	// static

	private static final Logger _Logger = Logger.getLogger(CoapServerGateway.class.getName());

	static {
		CoapConfig.register();
		UdpConfig.register();
	}

	// params

	private CoapServer coapServer = null;

	private IDataMessageListener dataMsgListener = null;

	// constructors

	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener) {
		super();

		this.dataMsgListener = dataMsgListener;

		initServer();
	}

	// public methods

	public void addResource(ResourceNameEnum resource) {
	}

	public boolean hasResource(String name) {
		return false;
	}

	public void setDataMessageListener(IDataMessageListener listener) {
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}

	public boolean startServer() {
		try {
			if (this.coapServer != null) {
				// Check if server is already running
				if (!this.coapServer.isRunning()) {
					this.coapServer.start();
					_Logger.info("CoAP server started successfully.");

					// for message logging
					for (Endpoint ep : this.coapServer.getEndpoints()) {
						ep.addInterceptor(new MessageTracer());
					}
				} else {
					_Logger.info("CoAP server is already running.");
				}
				return true;
			} else {
				_Logger.warning("CoAP server START failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start CoAP server.", e);
		}

		return false;
	}

	public boolean stopServer() {
		try {
			if (this.coapServer != null) {
				if (this.coapServer.isRunning()) {
					this.coapServer.stop();
					_Logger.info("CoAP server stopped successfully.");
				} else {
					_Logger.info("CoAP server is already stopped.");
				}
				return true;
			} else {
				_Logger.warning("CoAP server STOP failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to stop CoAP server.", e);
		}

		return false;
	}

	// private methods

	private Resource createResourceChain(ResourceNameEnum resource) {
		if (resource == null) {
			return null;
		}

		List<String> resourceNames = resource.getResourceNameChain();
		if (resourceNames == null || resourceNames.isEmpty()) {
			return null;
		}

		// Create the root resource if it doesn't exist
		Resource parentResource = this.coapServer.getRoot();
		if (parentResource == null) {
			parentResource = new CoapResource(resourceNames.get(0));
			this.coapServer.add(parentResource);
		}

		// Create the resource chain
		for (int i = 1; i < resourceNames.size(); i++) {
			String resourceName = resourceNames.get(i);
			Resource nextResource = parentResource.getChild(resourceName);

			if (nextResource == null) {
				if (i == resourceNames.size() - 1) {
					// This is the last resource in the chain, create it with the handler
					nextResource = new CoapResource(resourceName);
				} else {
					// This is an intermediate resource, create it as a container
					nextResource = new CoapResource(resourceName);
				}
				parentResource.add(nextResource);
			}

			parentResource = nextResource;
		}

		return parentResource;
	}

	private void initServer(ResourceNameEnum... resources) {
		try {
			// Create server with default configuration
			this.coapServer = new CoapServer();
			
			// Create non-secure endpoint
			CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
			builder.setInetSocketAddress(new InetSocketAddress("127.0.0.1", 56830));
			
			// Add endpoint to server
			CoapEndpoint endpoint = builder.build();
			this.coapServer.addEndpoint(endpoint);
			
			// Initialize resources
			if (resources != null && resources.length > 0) {
				for (ResourceNameEnum resource : resources) {
					_Logger.info("Initializing handler for resource: " + resource.name());
				}
			} else {
				_Logger.info("No resources provided for server initialization.");
				initDefaultResources();
			}
			
			// Start the server
			this.coapServer.start();
			
			_Logger.info("CoAP server started successfully on port 56830");
		} catch (Exception e) {
			_Logger.severe("Failed to initialize CoAP server: " + e.getMessage());
			throw new IllegalStateException("Failed to initialize CoAP server", e);
		}
	}

	private void initDefaultResources() {
		// initialize pre-defined resources
		GetActuatorCommandResourceHandler getActuatorCmdResourceHandler = new GetActuatorCommandResourceHandler(
				ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceType());

		if (this.dataMsgListener != null) {
			this.dataMsgListener.setActuatorDataListener(null, getActuatorCmdResourceHandler);
		}

		addResource(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, null, getActuatorCmdResourceHandler);

		UpdateTelemetryResourceHandler updateTelemetryResourceHandler = new UpdateTelemetryResourceHandler(
				ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceType());

		updateTelemetryResourceHandler.setDataMessageListener(this.dataMsgListener);

		addResource(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, null, updateTelemetryResourceHandler);

		UpdateSystemPerformanceResourceHandler updateSystemPerformanceResourceHandler = new UpdateSystemPerformanceResourceHandler(
				ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceType());

		updateSystemPerformanceResourceHandler.setDataMessageListener(this.dataMsgListener);

		addResource(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, null, updateSystemPerformanceResourceHandler);
	}

	public void addResource(ResourceNameEnum resourceType, String endName, Resource resource) {
		// TODO: while not needed for this exercise, you may want to include
		// the endName parameter as part of this resource chain creation process

		if (resourceType != null && resource != null) {
			// break out the hierarchy of names and build the resource
			// handler generation(s) as needed, checking if any parent already
			// exists - and if so, add to the existing resource
			createAndAddResourceChain(resourceType, resource);
		}
	}

	private void createAndAddResourceChain(ResourceNameEnum resourceType, Resource resource) {
		_Logger.info("Adding server resource handler chain: " + resourceType.getResourceName());

		List<String> resourceNames = resourceType.getResourceNameChain();
		Queue<String> queue = new ArrayBlockingQueue<>(resourceNames.size());

		queue.addAll(resourceNames);

		// check if we have a parent resource
		Resource parentResource = this.coapServer.getRoot();

		// if no parent resource, add it in now (should be named "PIOT")
		if (parentResource == null) {
			parentResource = new CoapResource(queue.poll());
			this.coapServer.add(parentResource);
		}

		while (!queue.isEmpty()) {
			// get the next resource name
			String resourceName = queue.poll();
			Resource nextResource = parentResource.getChild(resourceName);

			if (nextResource == null) {
				if (queue.isEmpty()) {
					nextResource = resource;
					nextResource.setName(resourceName);
				} else {
					nextResource = new CoapResource(resourceName);
				}

				parentResource.add(nextResource);
			}

			parentResource = nextResource;
		}
	}
}